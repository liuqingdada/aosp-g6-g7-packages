package com.mstarc.wearablelauncher;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.mstarc.fakewatch.bind.BindWizard;

import java.util.ArrayList;

import mstarc_os_api.ScreenBrightness;

import mstarc_os_api.mode_change;
import mstarc_os_api.mstarc_os_api_msg;

import static mstarc_os_api.mode_change.STR_POWERMODE_TYPE;


/**
 * Created by wangxinzhi on 17-3-26.
 */

public class CommonManager {
    public static final String POWERMODE_SHAREP = "PowerMode";
    public static final int POWERMODE_WATCH = 1;
    public static final int POWERMODE_NORMAL = 2;
    public static final int POWERMODE_FLIGHT = 3;
    public static final int DEFAULT_POWERMODE = POWERMODE_NORMAL;
    public static final String LAUNCHER_SETTING_POWERMODE = "launcher_settings_powermode";
    public static final String LAUNCHER_SETTING_FTE_FINISHED = "fte_finished";
    private static final String TAG = CommonManager.class.getSimpleName();
    int mPowerMode = DEFAULT_POWERMODE;
    SettingsValueChangeContentObserver mContentObserver;
    final static boolean DEBUG = false;
    boolean mFteFinished = false;
    private final int isFteTest = SystemProperties.getInt("persist.fte.test", 0);
    private final boolean disablePowerMode = SystemProperties.getBoolean("persist.launcher.power.disable", false);
    private mstarc_os_api_msg m_api_msg;
    boolean isServiceReady = false;
    private final BluetoothAdapter mBluetoothAdapter;
    private int mBTState = BluetoothProfile.STATE_DISCONNECTED;
    boolean isBtConnected = false;
    BtReceiver mBtReceiver;
    UiHandler mHandler;
    int mBrightness = 128;
    static CommonManager sInstance;
    Context mContext;
    ArrayList<OnPowerModeChangeListener> mPowerModeListeners = new ArrayList<>();
    mode_change mModeChange;
    HandlerThread mWorkThread;
    WorkHandler mWorkHandler;
    BindWizard mBindWizard;

    public CommonManager(Context context) {
        this.mContext = context;
        try {
            mPowerMode = Settings.System.getInt(context.getContentResolver(), LAUNCHER_SETTING_POWERMODE);
        } catch (Settings.SettingNotFoundException e) {
            mPowerMode = DEFAULT_POWERMODE;
            Settings.System.putInt(mContext.getContentResolver(), LAUNCHER_SETTING_POWERMODE, mPowerMode);
        }
        try {
            mBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception SettingNotFoundException) {
            mBrightness = 128;
        }
        mContentObserver = new SettingsValueChangeContentObserver();
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                true,
                mContentObserver);
        ScreenBrightness.screenBrightness_check(mContext);
        if (DEBUG) {
            mFteFinished = false;
        } else {
            try {
                mFteFinished = Settings.System.getInt(context.getContentResolver(), LAUNCHER_SETTING_FTE_FINISHED) == 1;
            } catch (Settings.SettingNotFoundException e) {
                mFteFinished = false;
                Settings.System.putInt(mContext.getContentResolver(), LAUNCHER_SETTING_FTE_FINISHED, 0);
            }
        }
        if (!disablePowerMode) {
            m_api_msg = new mstarc_os_api_msg(mContext) {
                @Override
                public void onServiceConnected() {
                    super.onServiceConnected();
                    isServiceReady = true;
                    Log.d(TAG, "onServiceConnected");
                    mWorkHandler.updatePowerMode();
                }
            };
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mBTState = mBluetoothAdapter.getConnectionState();
        }
        Log.d(TAG, "constructor mBTState: " + mBTState);
        mBtReceiver = new BtReceiver();
        IntentFilter filter = new IntentFilter(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mBtReceiver, filter);
        mHandler = new UiHandler();
        Log.d(TAG, "persist.launcher.power.disable" + disablePowerMode);
        mWorkThread = new HandlerThread("handler-thread");
        mWorkThread.start();
        mWorkHandler = new WorkHandler(mWorkThread.getLooper());
        mBindWizard = new BindWizard(mContext);
        try {
            isIOS = mBindWizard.getPhoneType() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static CommonManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CommonManager(context);
        }
        return sInstance;
    }

    public interface OnPowerModeChangeListener {
        public void OnPowerModeChange(int powermode);
    }

    public void addPowerModeListener(OnPowerModeChangeListener listener) {
        if (!mPowerModeListeners.contains(listener)) {
            mPowerModeListeners.add(listener);
        }
    }

    public void removePowerModeListener(OnPowerModeChangeListener listener) {
        if (mPowerModeListeners.contains(listener)) {
            mPowerModeListeners.remove(listener);
        }
    }

    public int getmPowerMode() {
        return mPowerMode;
    }

    public void setmPowerMode(int powerMode) {
        if (powerMode == mPowerMode) {
            return;
        }
        mPowerMode = powerMode;
        mWorkHandler.updatePowerMode();
        Settings.System.putInt(mContext.getContentResolver(), LAUNCHER_SETTING_POWERMODE, mPowerMode);
        try {
            m_api_msg.mstarc_put_data(mContext, STR_POWERMODE_TYPE, mPowerMode);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "listener size: " + mPowerModeListeners.size());
        for (OnPowerModeChangeListener listener : mPowerModeListeners) {
            if (listener != null) {
                Log.d(TAG, "listener: " + listener);
                listener.OnPowerModeChange(powerMode);
            }
        }
    }

    private void updatePowerMode() {
        if (!isServiceReady) {
            Log.d(TAG, "updatePowerMode isServiceReady " + isServiceReady);
            return;
        }
        Log.d(TAG, "try updatePowerMode to: " + mPowerMode + " persist.launcher.power.disable " + disablePowerMode);
        Intent intent = new Intent();
        intent.setAction("com.mstarc.modechanged");
        intent.putExtra("powermode", mPowerMode);
        Log.e(TAG, "begin send broadcast");
        mContext.sendBroadcast(intent);
        Log.e(TAG, "end send broadcast");

        m_api_msg.mstarc_api_adb(mContext,"echo 40 > /sys/class/lwq/lwq/val");
        switch (mPowerMode) {
            case POWERMODE_WATCH:
                if (!disablePowerMode) {
                    m_api_msg.mstarc_api_setairplane_modem(mContext, true, 0, 0);
                    m_api_msg.mstarc_api_setlcmwgevent_modem(mContext, true);
                }

                break;
            case POWERMODE_NORMAL:
                if (!disablePowerMode) {
                    m_api_msg.mstarc_api_setairplane_modem(mContext, false, 0, 0);
                    m_api_msg.mstarc_api_setlcmwgevent_modem(mContext, false);
                }
                break;
            case POWERMODE_FLIGHT:
                if (!disablePowerMode) {
                    m_api_msg.mstarc_api_setairplane_modem(mContext, true, 0, 0);
                    m_api_msg.mstarc_api_setlcmwgevent_modem(mContext, false);
                }
                break;
        }
         m_api_msg.mstarc_api_adb(mContext,"echo 41 > /sys/class/lwq/lwq/val");

        Log.d(TAG, "finish updatePowerMode to: " + mPowerMode);
    }

    class SettingsValueChangeContentObserver extends ContentObserver {

        public SettingsValueChangeContentObserver() {
            super(new Handler(mContext.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!selfChange) {
                try {
                    mBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getBrightness() {
        Log.d(TAG, "getBrightness: " + mBrightness);
        return mBrightness;
    }

    public void setBrightness(int brightness) {
        this.mBrightness = brightness;
        ContentResolver contentResolver = mContext.getContentResolver();
        Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, brightness);
        Log.d(TAG, "setBrightness: " + mBrightness);
    }

    public boolean ifFteFinished() {
        if (isFteTest == 1) {
            Log.w(TAG, "enable FTE by persist.fte.test");
            return false;
        }
//        return mFteFinished;
        return true;
    }

    public void setFteFinished() {
        mFteFinished = true;
        Settings.System.putInt(mContext.getContentResolver(), LAUNCHER_SETTING_FTE_FINISHED, 1);
    }

    public void setServicePassword(String password) {
        Log.d(TAG, "setServicePassword " + password);
        Settings.Secure.putString(mContext.getContentResolver(), "wearable_password", password);

    }

    class BtReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, mBTState);
                mBTState = state;
                Log.d(TAG, "onReceive mBTState: " + mBTState);
                mHandler.sendEmptyMessage(UiHandler.MSG_UPDATE_BT_STATE);
            }
        }
    }

    class UiHandler extends Handler {
        public static final int MSG_UPDATE_BT_STATE = 1;
        public static final int MSG_UPDATE_PHONE_BATTERY_TEXT = 2;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_BT_STATE:
                    boolean connected = mBTState == BluetoothProfile.STATE_CONNECTED;
                    if (connected != isBtConnected) {
                        isBtConnected = connected;
                        for (IBtListener listener : mBtlisteners) {
                            listener.OnBtStatusChanged(isBtConnected);
                        }
                    }
                    break;
                case MSG_UPDATE_PHONE_BATTERY_TEXT:
                    for (IPhoneBatteryListener listener : mPhoneBatteryChangeListeners) {
                        listener.OnPhoneBatteryhanged(mPhoneBatteryString);
                    }
                    break;
            }
        }
    }

    public boolean isBtConnected() {
        boolean connected = mBTState == BluetoothProfile.STATE_CONNECTED;
        if (connected != isBtConnected) {
            isBtConnected = connected;
        }
        return isBtConnected;
    }

    public interface IBtListener {
        void OnBtStatusChanged(boolean connected);
    }

    ArrayList<IBtListener> mBtlisteners = new ArrayList<>();

    public void addBtListener(IBtListener listener) {
        if (mBtlisteners != null && listener != null && !mBtlisteners.contains(listener)) {
            mBtlisteners.add(listener);
        }
    }

    public void removeBtListener(IBtListener listener) {
        if (mBtlisteners != null && listener != null && mBtlisteners.contains(listener)) {
            mBtlisteners.remove(listener);
        }
    }

    ArrayList<IPhoneBatteryListener> mPhoneBatteryChangeListeners = new ArrayList<>();

    String mPhoneBatteryString;

    public interface IPhoneBatteryListener {
        void OnPhoneBatteryhanged(String batteryText);
    }

    public void addPhoneBatteryListener(IPhoneBatteryListener listener) {

        if (mPhoneBatteryChangeListeners != null && listener != null && !mPhoneBatteryChangeListeners.contains(listener)) {
            mPhoneBatteryChangeListeners.add(listener);
        }
    }

    public void removePhoneBatteryListener(IPhoneBatteryListener listener) {
        if (mPhoneBatteryChangeListeners != null && listener != null && mPhoneBatteryChangeListeners.contains(listener)) {
            mPhoneBatteryChangeListeners.remove(listener);
        }
    }

    public void setPhoneBatteryString(String battery) {
        mPhoneBatteryString = battery;
        mHandler.sendEmptyMessage(UiHandler.MSG_UPDATE_PHONE_BATTERY_TEXT);
    }

    class WorkHandler extends Handler {
        public final static int MSG_UPDATE_POWERMODE = 1;

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_POWERMODE:
                    CommonManager.this.updatePowerMode();
                    break;
            }
        }

        public void updatePowerMode() {
            removeMessages(MSG_UPDATE_POWERMODE);
            sendEmptyMessage(MSG_UPDATE_POWERMODE);
        }
    }

    boolean isIOS = false;
    IIosBoundListener mIosBoundListener;

    public boolean isIOSbound() {
        int debug = SystemProperties.getInt("persist.bindtype", -1);
        if(debug==1){
            return true;
        }else if(debug == 0){
            return false;
        }
        try {
            isIOS = mBindWizard.getPhoneType() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  isIOS;
    }

    public void setIOSBound(boolean isios) {
        if(isIOS == isios)return;
        isIOS = isios;
        if (mIosBoundListener != null) {
            try {
                mIosBoundListener.boundTypeChanged(isIOS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setIosBoundListener(IIosBoundListener listener) {
        mIosBoundListener = listener;
    }

    public interface IIosBoundListener {
        void boundTypeChanged(boolean isios);
    }
}
