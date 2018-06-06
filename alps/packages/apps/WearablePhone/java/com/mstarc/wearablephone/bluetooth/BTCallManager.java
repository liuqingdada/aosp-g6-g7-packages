package com.mstarc.wearablephone.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.ContactPhotoManager;
import com.mstarc.wearablephone.DialInActivity;
import com.mstarc.wearablephone.DialOutActivity;
import com.mstarc.wearablephone.TalkingActivity;
import com.mstarc.wearablephone.util.InterceptCall;
import com.mstarc.wearablephone.view.ContractsFragment;
import com.mstarc.wearablephonebtadapter.IRemoteService;
import com.mstarc.wearablephonebtadapter.IRemoteServiceCallback;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mstarc_os_api.mstarc_os_api_msg;

import static android.bluetooth.BluetoothHeadsetClientCall.CALL_STATE_ACTIVE;
import static android.bluetooth.BluetoothHeadsetClientCall.CALL_STATE_DIALING;
import static android.bluetooth.BluetoothHeadsetClientCall.CALL_STATE_INCOMING;
import static android.bluetooth.BluetoothHeadsetClientCall.CALL_STATE_TERMINATED;
import static com.mstarc.wearablephone.bluetooth.DataProviderContract.INCOMING_TYPE;
import static com.mstarc.wearablephone.bluetooth.DataProviderContract.MISSED_TYPE;
import static com.mstarc.wearablephone.bluetooth.DataProviderContract.OUTGOING_TYPE;


/**
 * Created by wangxinzhi on 17-4-29.
 */

public class BTCallManager {
    public static final int ACTIVITY_TYPE_DIAL_OUT = 0;
    public static final int ACTIVITY_TYPE_DIAL_IN = 1;
    public static final int ACTIVITY_TYPE_DIAL_TALKING = 2;
    public static final int ACTIVITY_TYPE_COUNT = 3;

    private static final String TAG = BTCallManager.class.getSimpleName();
    private static BTCallManager sBTCallManager;
    private static final int MSG_UPDATE_CALL = 1;
    private static final int MSG_BTADAPTER_INCOMMING_CALL = 2;
    private static final int MSG_BTADAPTER_EVENT = 3;
    private mstarc_os_api_msg m_api_msg;
    private boolean m_api_msg_flag;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadsetClientCall mCall;
    private Context mContext;
    private HashMap<Integer, Activity> mActivityMap = new HashMap<>();
    private HashMap<Integer, Class<?>> mStartActivityMap = new HashMap<>();
    private boolean[] mUIstate;
    private int mBTState = BluetoothProfile.STATE_DISCONNECTED;
    ContactPhotoManager mContactPhotoManager;
    private HashMap<String, ContactInfo> mNumberPersonMap = new HashMap<>();
    boolean isIncommingCallMissed = false;
    PowerManager.WakeLock mWakeLock;
    private String mBleRemoteDeviceAddress = null;
    BluetoothDevice mBtDevice = null;
    private BluetoothHeadsetClient mBluetoothHeadsetClient = null;

    public final static int PENDING_ACTION_NONE = 0;
    public final static int PENDING_ACTION_ACCEPT_CALL = 1;
    public final static int PENDING_ACTION_DIAL_OUT = 2;
    int mPendingAction = PENDING_ACTION_NONE;
    String mPendingDialNumber = null;
    String mIncommingNumber = null;
    SettingsValueChangeContentObserver mBleSettingsContentObserver;
    Object mLock = new Object();
    boolean bAcceptedByWatch = false;
    IRemoteService mService = null;
    IRemoteServiceCallback.Stub mCallback = new IRemoteServiceCallback.Stub() {
        @Override
        public void inCommingCall(String name, String number) throws RemoteException { // show view
            Log.v(TAG, "inCommingCall: name = " + name + ", number = " + number);

            if (isBTPhoneEnnable()) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_BTADAPTER_INCOMMING_CALL, number));
            }
        }

        /**
         * @param event PHONE_OFFHOOK = 2; PHONE_END_CALL = 4
         */
        @Override
        public void onEvent(int event) throws RemoteException {
            Log.v(TAG, "onEvent: " + event);

            if (isBTPhoneEnnable()) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_BTADAPTER_EVENT, event, 0));
            }
        }
    };

    public boolean isBTPhoneEnnable() {
        if (mBleRemoteDeviceAddress != null && mBluetoothAdapter != null && isBTConnected()) {
            for (BluetoothDevice bluetoothDevice : mBluetoothAdapter.getBondedDevices()) {
                if (bluetoothDevice.getAddress()
                                   .equals(mBleRemoteDeviceAddress)) {
                    return true;
                }
            }
        } else {
            initBindDevice();
        }
        return false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = IRemoteService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
                Log.d(TAG, "onServiceConnected " + className);
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            Log.d(TAG, "onServiceConnected " + className);
        }
    };

    private void initRemoteService() {
        Intent i = new Intent();
        i.setClassName("com.mstarc.wearablephonebtadapter",
                       "com.mstarc.wearablephonebtadapter.RemoteService");
        boolean connectedFlag = mContext.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    class ContactInfo {
        String name;
        String number;
        long photoId;
        String photoUri;
        Long mRecentRecordRowId;

        public ContactInfo(String name, String number, long photoId, String photoUri) {
            this.name = name;
            this.number = number;
            this.photoId = photoId;
            this.photoUri = photoUri;
        }
    }

    private void initData() {
        mBleSettingsContentObserver = new SettingsValueChangeContentObserver();
        try {
            mContext.getContentResolver()
                    .registerContentObserver(
                            Settings.System.getUriFor("bleRemoteDevice"), true,
                            mBleSettingsContentObserver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initBindDevice();
    }

    private void initBtPhone() {
        BluetoothProfileServiceListener conn = new BluetoothProfileServiceListener();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBTState = mBluetoothAdapter.getConnectionState();
        mBluetoothAdapter.getProfileProxy(mContext, conn, BluetoothProfile.HEADSET_CLIENT);

    }

    private void upgradingHFP(boolean connect) {
        Log.d(TAG, "upgradingHFP: do connect = " + connect);
        Log.d(TAG, "upgradingHFP: mBluetoothHeadsetClient " + mBluetoothHeadsetClient +
                " mBtDevice " + mBtDevice);

        if (mBluetoothHeadsetClient != null && mBtDevice != null) {
            if (connect) {
                mBluetoothHeadsetClient.connect(mBtDevice);
                //mBluetoothHeadsetClient.connectAudio();
                //mBluetoothHeadsetClient.startVoiceRecognition(mBtDevice);

            } else {
                //mBluetoothHeadsetClient.disconnectAudio();
                //mBluetoothHeadsetClient.stopVoiceRecognition(mBtDevice);
                mBluetoothHeadsetClient.disconnect(mBtDevice);
            }
        } else {
            if (mBtDevice == null) {
                initBindDevice();
            }
            if (mBluetoothHeadsetClient == null) {
                initBtPhone();
                Log.e(TAG, "upgradingHFP: init");
            }
        }
    }

    private Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BTADAPTER_INCOMMING_CALL: {
                    if (m_api_msg_flag) {
                        m_api_msg.mstarc_api_walkup();
                    }
                    mIncommingNumber = (String) msg.obj;
                    Log.d(TAG, " mIncommingNumber: " + mIncommingNumber);
                    stopUI(ACTIVITY_TYPE_DIAL_OUT);
                    stopUI(ACTIVITY_TYPE_DIAL_TALKING);
                    isIncommingCallMissed = true;
                    // start dial in activity;
                    startUI(ACTIVITY_TYPE_DIAL_IN);
                    lightScreen();
                }
                break;
                case MSG_BTADAPTER_EVENT: {
                    int event = msg.arg1;
                    switch (event) {
                        case 1:
                            //PHONE_CALL_READY
                            break;
                        case 2:
                            //RequestCode.PHONE_OFFHOOK)
                            //upgradingHFP(false);
                            stopUI(ACTIVITY_TYPE_DIAL_IN);
                            stopUI(ACTIVITY_TYPE_DIAL_OUT);
                            //stopUI(ACTIVITY_TYPE_DIAL_TALKING);
                            resetPendingAction();
                            //if(!bAcceptedByWatch) {
                            //releaseScreen();
                            //}
                            break;
                        case 3:
                            //RequestCode.WATCH_OFFHOOK
                            break;
                        case 4: {
                            //RequestCode.PHONE_END_CALL
                            upgradingHFP(false);
                            stopUI(ACTIVITY_TYPE_DIAL_IN);
                            stopUI(ACTIVITY_TYPE_DIAL_OUT);
                            stopUI(ACTIVITY_TYPE_DIAL_TALKING);
                            resetPendingAction();
                            releaseScreen();
                            bAcceptedByWatch = false;
                        }
                        break;
                    }
                }
                break;
                case MSG_UPDATE_CALL: {
                    BluetoothHeadsetClientCall call = (BluetoothHeadsetClientCall) msg.obj;
                    if (call == null) {
                        stopUI(ACTIVITY_TYPE_DIAL_IN);
                        stopUI(ACTIVITY_TYPE_DIAL_OUT);
                        stopUI(ACTIVITY_TYPE_DIAL_TALKING);
                        releaseScreen();
                        mCall = null;
                        break;
                    }

                    if (mCall == null
                            || mCall.getId() != call.getId()
                            || mCall.getState() != call.getState()) {
                        if (call != null) {
                            if (call.getState() == CALL_STATE_DIALING && call.isOutgoing()) {
                                // start dial out activity;
                                stopUI(ACTIVITY_TYPE_DIAL_IN);
                                stopUI(ACTIVITY_TYPE_DIAL_TALKING);
                                startUI(ACTIVITY_TYPE_DIAL_OUT);
                                lightScreen();
                            } else if (call.getState() == CALL_STATE_INCOMING) {

                                //                                stopUI(ACTIVITY_TYPE_DIAL_OUT);
                                //                                stopUI
                                // (ACTIVITY_TYPE_DIAL_TALKING);
                                //                                isIncommingCallMissed = true;
                                //                                // start dial in activity;
                                //                                startUI(ACTIVITY_TYPE_DIAL_IN);
                                //                                lightScreen();
                                //                                vibrateForIncommingCall();
                            } else if (call.getState() == CALL_STATE_ACTIVE) {
                                //start talking activity;
                                stopUI(ACTIVITY_TYPE_DIAL_IN);
                                stopUI(ACTIVITY_TYPE_DIAL_OUT);
                                startUI(ACTIVITY_TYPE_DIAL_TALKING);
                                lightScreen();
                            } else if (call.getState() == CALL_STATE_TERMINATED) {
                                // stop all activity
                                call = null;
                                bAcceptedByWatch = false;
                                stopUI(ACTIVITY_TYPE_DIAL_IN);
                                stopUI(ACTIVITY_TYPE_DIAL_OUT);
                                stopUI(ACTIVITY_TYPE_DIAL_TALKING);
                                if (isIncommingCallMissed) {
                                    updateCallLog(getContactInfo(mCall.getNumber()), MISSED_TYPE,
                                                  true);
                                }
                                releaseScreen();
                                upgradingHFP(false);
                            }
                        } else {
                            stopUI(ACTIVITY_TYPE_DIAL_IN);
                            stopUI(ACTIVITY_TYPE_DIAL_OUT);
                            stopUI(ACTIVITY_TYPE_DIAL_TALKING);
                            releaseScreen();
                        }
                    }
                    mCall = call;
                }
                break;
                default:
                    break;
            }
        }
    };

    public BTCallManager(Context context) {
        InterceptCall.getInstance()
                     .init(context);

        mContext = context.getApplicationContext();
        mStartActivityMap.put(ACTIVITY_TYPE_DIAL_OUT, DialOutActivity.class);
        mStartActivityMap.put(ACTIVITY_TYPE_DIAL_IN, DialInActivity.class);
        mStartActivityMap.put(ACTIVITY_TYPE_DIAL_TALKING, TalkingActivity.class);
        mContactPhotoManager = ContactPhotoManager.getInstance(mContext);

        m_api_msg = new mstarc_os_api_msg(mContext) {
            @Override
            public void onServiceConnected() {
                super.onServiceConnected();
                m_api_msg_flag = true;
            }
        };

        mUIstate = new boolean[ACTIVITY_TYPE_COUNT];
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "BtCall");
        //vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        initRemoteService();
        initBtPhone();
        initData();
    }

    private class BluetoothProfileServiceListener implements BluetoothProfile.ServiceListener {

        @Override
        public void onServiceConnected(int state, BluetoothProfile profile) {
            Log.i(TAG, "onServiceConnected");

            if (state == BluetoothProfile.HEADSET_CLIENT) {
                synchronized (mLock) {
                    mBluetoothHeadsetClient = (BluetoothHeadsetClient) profile;
                    Log.d(TAG, "HFP connected");
                }
            }
        }

        @Override
        public void onServiceDisconnected(int state) {
            if (state == BluetoothProfile.HEADSET_CLIENT) {
                synchronized (mLock) {
                    mBluetoothHeadsetClient = null;
                    initBtPhone();
                    Log.d(TAG, "HFP disconnected");
                }
            }
        }
    }

    private void stopUI(int type) {
        Activity activity = mActivityMap.get(type);
        if (activity != null && mUIstate[type]) {
            Log.d(TAG, "stopUI " + type);
            activity.finish();
        }
    }

    private void startUI(int type) {
        Class<?> activityClass = mStartActivityMap.get(type);
        if (activityClass != null && !mUIstate[type]) {
            //            Intent intent = new Intent(mContext, activityClass);
            //            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            final Intent intent = new Intent(Intent.ACTION_MAIN, null);
            //            intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            //                    | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setClass(mContext, activityClass);
            mContext.startActivity(intent);
            Log.d(TAG, "startUI " + type);
        }
    }

    public static synchronized BTCallManager getInstance(Context context) {
        if (sBTCallManager == null) {
            sBTCallManager = new BTCallManager(context);
        }
        return sBTCallManager;
    }

    public void updateActivity(int type, Activity activity) {
        Log.d(TAG, "updateActivity " + type + " " + activity);
        mActivityMap.put(type, activity);
    }

    public void unsetActivity(int type) {
        mActivityMap.remove(type);
    }

    public String getCurrentCallNumber() {
        if (mCall == null) {
            return "";
        }

        return mCall.getNumber();
    }

    public BluetoothHeadsetClient getBluetoothHeadsetClient() {
        return this.mBluetoothHeadsetClient;
    }

    public BluetoothDevice getBtDevice() {
        return this.mBtDevice;
    }

    public boolean isBTConnected() {
        boolean result = false;
        try {
            result = mService.isAdvertiserConnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void get_people_image_incall(TextView lable, ImageView profile) {
        if (mCall != null) { get_people_image(mIncommingNumber, lable, profile); }
    }

    public void get_people_image_talking(TextView lable, ImageView profile) {
        if (mCall != null) { get_people_image(mCall.getNumber(), lable, profile); }
    }

    public ContactInfo getContactInfo(String x_number) {

        ContactInfo contactInfo = null;
        if (mNumberPersonMap.get(x_number) == null) {
            String name = null, number = null, photoUri = null;
            long photoid = 0;
            Cursor cursorCantacts = mContext.getContentResolver()
                                            .query(
                                                    Uri.withAppendedPath(
                                                            ContactsContract.CommonDataKinds
                                                                    .Phone.CONTENT_FILTER_URI,
                                                            Uri.encode(x_number)),
                                                    ContractsFragment.PhoneQuery.PROJECTION,
                                                    null,
                                                    null,
                                                    null);
            if (cursorCantacts.getCount() > 0) {
                try {
                    cursorCantacts.moveToFirst();
                    name = cursorCantacts.getString(ContractsFragment.PhoneQuery.DISPLAY_NAME);
                    photoid = cursorCantacts.getLong(ContractsFragment.PhoneQuery.PHOTO_ID);
                    number = cursorCantacts.getString(ContractsFragment.PhoneQuery.PHONE_NUMBER);

                    photoUri = cursorCantacts.getString(ContractsFragment.PhoneQuery.PHOTO_URI);
                    contactInfo = new ContactInfo(name, number, photoid, photoUri);
                    mNumberPersonMap.put(number, contactInfo);
                    Log.d(TAG,
                          "get_people_image displayName: " + name + " number: " + number + " " +
                                  "photoid: " + photoid + " photoUri: " + photoUri);
                } finally {
                    cursorCantacts.close();
                }
            }
        } else {
            contactInfo = mNumberPersonMap.get(x_number);
        }
        if (contactInfo == null) {
            contactInfo = new ContactInfo(null, x_number, 0, null);
        }
        return contactInfo;
    }

    public void get_people_image(final String x_number, final TextView lable,
                                 final ImageView profile) {
        final ContactInfo contactInfo = getContactInfo(x_number);
        if (contactInfo != null
                && contactInfo.name != null
                && !contactInfo.name.isEmpty()) { lable.setText(contactInfo.name); } else {
            lable.setText(x_number);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (contactInfo != null) {
                    if (contactInfo.photoId == 0 && contactInfo.photoUri != null && !contactInfo
                            .photoUri.isEmpty()) {
                        // use photo uri
                        mContactPhotoManager.loadPhoto(profile, Uri.parse(contactInfo.photoUri),
                                                       profile.getWidth(),
                                                       false /* darkTheme */, true /* isCircular */,
                                                       null);
                    } else if (contactInfo.photoId != 0) {
                        mContactPhotoManager.loadThumbnail(profile, contactInfo.photoId, false,
                                                           true, null);
                    }
                }
            }
        });

    }

    boolean isBlHSCConnected() {
        return (BluetoothProfile.STATE_CONNECTED == mBluetoothAdapter.getProfileConnectionState(
                BluetoothProfile.HEADSET_CLIENT)
                && mBluetoothHeadsetClient != null
                && mBtDevice != null);
    }

    private ExecutorService btPhoneThread = Executors.newCachedThreadPool();

    public void dial(String number) {
        Log.i(TAG, "dial ->" + number);
        if (TextUtils.isEmpty(number)) {
            return;
        }
        dialNum = number;
        btPhoneThread.execute(dialRunnable);
        //        synchronized (mLock) {
        //            if (isBlHSCConnected()) {
        //                mBluetoothHeadsetClient.dial(mBtDevice, number);
        //            }
        //        }
        //        updateCallLog(getContactInfo(number), OUTGOING_TYPE, true);
    }

    private String dialNum;
    private Runnable dialRunnable = new Runnable() {
        @Override
        public void run() {
            connectHFP();
            SystemClock.sleep(500);
            Log.d(TAG, "dialRunnable: " + mBluetoothHeadsetClient);
            if (mBluetoothHeadsetClient != null && mBtDevice != null) {
                mBluetoothHeadsetClient.dial(mBtDevice, dialNum);
            } else {
                initBindDevice();
            }
            updateCallLog(getContactInfo(dialNum), OUTGOING_TYPE, true);
        }
    };

    /**
     * before accept call shuold be connect hfp
     */
    public void acceptCall() {
        Log.i(TAG, "acceptCall: ");
        bAcceptedByWatch = true;
        btPhoneThread.execute(acceptCallRunnable);
        //        synchronized (mLock) {
        //            if (isBlHSCConnected()) {
        //                mBluetoothHeadsetClient.acceptCall(mBtDevice, BluetoothHeadsetClient
        // .CALL_ACCEPT_NONE);
        //            }
        //        }
        //        isIncommingCallMissed = false;
        // updateCallLog(getContactInfo(mCall.getNumber()), INCOMING_TYPE, true);
    }

    private Runnable acceptCallRunnable = new Runnable() {
        @Override
        public void run() {
            connectHFP();
            Log.d(TAG, "acceptCallRunnable: " + acceptCallRunnable);

            for (int i = 0; i < 5; i++) {
                if (mBluetoothHeadsetClient != null && mBtDevice != null) {
                    boolean acceptCall = mBluetoothHeadsetClient.acceptCall(mBtDevice,
                                                                            BluetoothHeadsetClient.CALL_ACCEPT_NONE);
                    Log.d(TAG, "acceptCallRunnable: acceptCall = " + acceptCall);
                } else {
                    initBindDevice();
                }
                SystemClock.sleep(300);
            }

            isIncommingCallMissed = false;
            updateCallLog(getContactInfo(getCurrentCallNumber()), INCOMING_TYPE, true);
        }
    };

    public void rejectCall() {
        Log.i(TAG, "rejectCall");
        //        synchronized (mLock) {
        //            if (isBlHSCConnected()) {
        //                mBluetoothHeadsetClient.rejectCall(mBtDevice);
        //            }
        //        }
        //        isIncommingCallMissed = false;
        //        if (mCall != null) {
        //            updateCallLog(getContactInfo(mCall.getNumber()), MISSED_TYPE, true);
        //        }
        btPhoneThread.execute(rejectCallRunnable);
    }

    private Runnable rejectCallRunnable = new Runnable() {
        @Override
        public void run() {
            connectHFP();

            Log.d(TAG, "rejectCallRunnable: " + rejectCallRunnable);
            for (int i = 0; i < 5; i++) {
                if (mBluetoothHeadsetClient != null && mBtDevice != null) {
                    mBluetoothHeadsetClient.rejectCall(mBtDevice);
                } else {
                    initBindDevice();
                }
                SystemClock.sleep(200);
            }

            isIncommingCallMissed = false;
            if (mCall != null) {
                updateCallLog(getContactInfo(getCurrentCallNumber()), MISSED_TYPE, true);
            }
        }
    };

    public void terminateCall() {
        Log.i(TAG, "terminateCall");
        btPhoneThread.execute(terminateCallRunnable);
        //        synchronized (mLock) {
        //            if (isBlHSCConnected()) {
        //                mBluetoothHeadsetClient.terminateCall(mBtDevice, 0);
        //            }
        //        }
        //        isIncommingCallMissed = false;
        //        if (mCall != null) {
        //            updateCallLog(getContactInfo(mCall.getNumber()), MISSED_TYPE, true);
        //        }
    }

    private Runnable terminateCallRunnable = new Runnable() {
        @Override
        public void run() {
            connectHFP();

            Log.d(TAG, "terminateCallRunnable: " + terminateCallRunnable);
            for (int i = 0; i < 5; i++) {
                if (mBluetoothHeadsetClient != null && mBtDevice != null) {
                    mBluetoothHeadsetClient.terminateCall(mBtDevice, 0);

                } else {
                    initBindDevice();
                }
                SystemClock.sleep(200);
            }

            isIncommingCallMissed = false;
            if (mCall != null) {
                updateCallLog(getContactInfo(getCurrentCallNumber()), MISSED_TYPE, true);
            }
            upgradingHFP(false);
        }
    };

    /**
     * connect hfp try three times
     */
    private void connectHFP() {
        int time = 40; // 4s
        for (int j = 0; j < 3; j++) { // try three times
            upgradingHFP(true);
            for (int i = 0; i < time; i++) {
                if (mBluetoothHeadsetClient != null) {
                    int connectionState = mBluetoothHeadsetClient.getConnectionState(mBtDevice);
                    Log.i(TAG, "connectHFP: connectionState = " + connectionState);

                    if (connectionState == BluetoothHeadsetClient.STATE_AUDIO_CONNECTED) {
                        return;
                    }
                    SystemClock.sleep(100);
                }
            }
            time *= 2; // 8s 16s 32s ... ...
        }
    }

    private void updateCallLog(ContactInfo contactInfo, int type, boolean isNew) {
        ContentValues values = new ContentValues();
        if (isNew) {
            values.put(DataProviderContract.NUMBER, contactInfo.number);
            values.put(DataProviderContract.NAME, contactInfo.name);
            values.put(DataProviderContract.TYPE, type);
            values.put(DataProviderContract.DATE, System.currentTimeMillis());
            Uri uri = mContext.getContentResolver()
                              .insert(DataProviderContract.CONTENT_URI, values);
            contactInfo.mRecentRecordRowId = Long.parseLong(uri.getLastPathSegment());
        } else {
            values.put(DataProviderContract.TYPE, type);
            values.put(DataProviderContract.DATE, System.currentTimeMillis());
            mContext.getContentResolver()
                    .update(DataProviderContract.CONTENT_URI,
                            values,
                            DataProviderContract._ID + "=" + contactInfo.mRecentRecordRowId,
                            null);
        }
    }

    public void onBroadCastReceive(Context context, Intent intent) {
        Log.i(TAG, "Intent=" + intent.getAction());

        if (intent.getAction()
                  .equals(BluetoothHeadsetClient.ACTION_CALL_CHANGED)) {
            BluetoothHeadsetClientCall call = intent.getParcelableExtra(
                    BluetoothHeadsetClient.EXTRA_CALL);
            Log.w(TAG, "" + call.getState());
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_CALL, call));

            switch (call.getState()) {
                case 0:
                case 2:
                case 3:
                case 4:
                    Settings.System.putInt(context.getContentResolver(),
                                           "mstarc_BT_PHONE_STATE",
                                           1);
                    break;

                default:
                    Settings.System.putInt(context.getContentResolver(),
                                           "mstarc_BT_PHONE_STATE",
                                           0);
                    break;
            }

        } else if (intent.getAction()
                         .equals(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, mBTState);
            Log.d(TAG, "" + intent.getAction() + " state: " + state);
            mBTState = state;
            if (mBTState == BluetoothProfile.STATE_DISCONNECTING
                    || mBTState == BluetoothProfile.STATE_DISCONNECTED) {
                //                mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_CALL,
                // null));
                //                resetPendingAction();
            }

            if (mBTState == BluetoothProfile.STATE_CONNECTED) { // step2
                if (mPendingAction == PENDING_ACTION_DIAL_OUT) {
                    //dial(mPendingDialNumber);
                    resetPendingAction();
                }
            }
        }
    }

    public void onUiShown(int type) {
        mUIstate[type] = true;

    }

    final static String ACTION_DISABLE_KEYGAURD = "ACTION_DISABLE_KEYGAURD";
    final static String ACTION_ENABLE_KEYGAURD = "ACTION_ENABLE_KEYGAURD";
    private boolean mDismissKeyguard = false;

    public void dismissKeyguard(boolean dismiss) {
        if (mDismissKeyguard == dismiss) {
            return;
        }
        mDismissKeyguard = dismiss;
        if (dismiss) {
            mContext.sendBroadcast(new Intent(ACTION_DISABLE_KEYGAURD));
        } else {
            mContext.sendBroadcast(new Intent(ACTION_ENABLE_KEYGAURD));
        }
    }

    public void onUiHide(int type) {
        mUIstate[type] = false;
    }

    public void lightScreen() {
        if (!mWakeLock.isHeld()) {
            Log.d(TAG, "aquire wakelock");
            mWakeLock.acquire();
        }
        dismissKeyguard(true);
    }

    public void releaseScreen() {
        if (mWakeLock.isHeld()) {
            Log.d(TAG, "release wakelock");
            mWakeLock.release();
        }
        dismissKeyguard(false);
    }

    class SettingsValueChangeContentObserver extends ContentObserver {

        public SettingsValueChangeContentObserver() {
            super(new Handler(mContext.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            initBindDevice();
        }
    }

    private void initBindDevice() {
        mBleRemoteDeviceAddress = Settings.System.getString(mContext.getContentResolver(),
                                                            "bleRemoteDevice");
        Log.i(TAG, "Settings.System.getString: " + mBleRemoteDeviceAddress);
        if (mBleRemoteDeviceAddress != null) {
            if (!BluetoothAdapter.checkBluetoothAddress(mBleRemoteDeviceAddress)) { //检查是否是有效的蓝牙地址
                Log.w(TAG, "initBle: invalidate bleRemoteDevice");
            }

            btPhoneThread.execute(new Runnable() {
                @Override
                public void run() {
                    Set<BluetoothDevice> dev = mBluetoothAdapter.getBondedDevices();
                    if (dev == null || dev.size() == 0) {
                        for (int i = 0; i < 6; i++) {
                            SystemClock.sleep(10000);
                            dev = mBluetoothAdapter.getBondedDevices();
                            Log.d(TAG, "dev: " + dev.size());
                            if (dev.size() > 0) {
                                break;
                            }
                        }
                    }
                    Log.d(TAG, "dev: " + dev.size());
                    for (BluetoothDevice bluetoothDevice : dev) {
                        Log.i(TAG, "bluetoothDevice: " + bluetoothDevice.getAddress());

                        if (bluetoothDevice.getAddress()
                                           .equals(mBleRemoteDeviceAddress)) {
                            synchronized (mLock) {
                                mBtDevice = bluetoothDevice;
                            }
                            Log.i(TAG, "initBle: " + mBtDevice);
                        }
                    }
                }
            });
        }
    }

    public mstarc_os_api_msg getM_api_msg() {
        return m_api_msg;
    }

    //public void vibrateForIncommingCall() {
    //}

    //public void cancealVibrate() {
    //}

    /**
     * step1: connect hfp
     * step2: if hfp connected, dial by mobilephone
     */
    public void prepareDial(String number) {
        if (isBTConnected()) {
            mPendingAction = PENDING_ACTION_DIAL_OUT;
            mPendingDialNumber = number;
        }
        dial(number);
    }

    private void resetPendingAction() {
        mPendingAction = PENDING_ACTION_NONE;
        mPendingDialNumber = null;
        mIncommingNumber = null;
    }

}


