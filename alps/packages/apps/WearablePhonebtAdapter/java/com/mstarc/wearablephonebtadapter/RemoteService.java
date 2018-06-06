package com.mstarc.wearablephonebtadapter;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.mstarc.commonbase.communication.aidl.AidlCommunicate;
import com.mstarc.commonbase.communication.listener.ICommonAidlListener;
import com.mstarc.commonbase.communication.message.RequestCode;
import com.mstarc.commonbase.communication.message.transmite.BtPhone;

;


/**
 * Created by hawking on 2017/9/13.
 */
public class RemoteService extends Service {
    public static final String TAG = RemoteService.class.getSimpleName();
    /**
     * This is a list of callbacks that have been registered with the
     * service.  Note that this is package scoped (instead of private) so
     * that it can be accessed more efficiently from inner classes.
     */
    final RemoteCallbackList<IRemoteServiceCallback> mCallbacks
            = new RemoteCallbackList<>();

    private AidlCommunicate mAidlCommunicate;


    @Override
    public void onCreate() {

        Log.d(TAG, "RemoteService onCreate");
        initBle();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mHandler.removeMessages(MSG_INCOMMING_CALL);
        mHandler.removeMessages(MSG_EVENT);
        // Unregister all callbacks.
        mCallbacks.kill();
    }

    // BEGIN_INCLUDE(exposing_a_service)
    @Override
    public IBinder onBind(Intent intent) {
        // Select the interface to return.  If your service only implements
        // a single interface, you can just return it here without checking
        // the Intent.
        Log.d(TAG, "RemoteService" + "onBind:intent " + intent);
        return mBinder;
    }

    /**
     * The IRemoteInterface is defined through IDL
     */
    private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
        public void registerCallback(IRemoteServiceCallback cb) {
            Log.d(TAG, "registerCallback: " + cb);
            if (cb != null) mCallbacks.register(cb);
        }

        public void unregisterCallback(IRemoteServiceCallback cb) {
            Log.d(TAG, "unregisterCallback: " + cb);
            if (cb != null) mCallbacks.unregister(cb);
        }

        @Override
        public void doWatchCallByPhone() throws RemoteException {
            mAidlCommunicate.sendMessage(RequestCode.WATCH_CALL_BY_PHONE, "");

        }

        @Override
        public void doWatchReceiveCall() throws RemoteException {
            mAidlCommunicate.sendMessage(RequestCode.WATCH_RECEIVE_CALL, "");
        }

        @Override
        public void doWatchRejectCall() throws RemoteException {
            mAidlCommunicate.sendMessage(RequestCode.WATCH_REJECT_CALL, "");
        }

        @Override
        public boolean isAdvertiserConnect() throws RemoteException {
            return mAidlCommunicate.isAdvertiserConnect();

        }
    };


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task removed: " + rootIntent);
    }

    private static final int MSG_INCOMMING_CALL = 1;
    private static final int MSG_EVENT = 2;

    /**
     * Our Handler used to execute operations on the main thread.  This is used
     * to schedule increments of our value.
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                // It is time to bump the value!
                case MSG_INCOMMING_CALL: {

                    BtPhone btPhone = (BtPhone) msg.obj;
                    final int N = mCallbacks.beginBroadcast();
                    for (int i = 0; i < N; i++) {
                        try {
                            mCallbacks.getBroadcastItem(i).inCommingCall(btPhone.getName(), btPhone.getNumber());
                        } catch (RemoteException e) {
                            // The RemoteCallbackList will take care of removing
                            // the dead object for us.
                        }
                    }
                    mCallbacks.finishBroadcast();

                }
                break;
                case MSG_EVENT:
                    int event = msg.arg1;
                    final int N = mCallbacks.beginBroadcast();
                    for (int i = 0; i < N; i++) {
                        try {
                            mCallbacks.getBroadcastItem(i).onEvent(event);
                        } catch (RemoteException e) {
                            // The RemoteCallbackList will take care of removing
                            // the dead object for us.
                        }
                    }
                    mCallbacks.finishBroadcast();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };


    /**
     * mAidlCommunicate服务绑定后可调用ble
     */
    private void initBle() {
        mAidlCommunicate = AidlCommunicate.getInstance();
        mAidlCommunicate.setAidlServiceConnectListener(
                new AidlCommunicate.WatchServiceConnectListener() {
                    @Override
                    public void onAidlServiceConnected() {
                        Log.i(TAG, "onAidlServiceConnected: ");
                    }

                    @Override
                    public void onAidlServiceDisconnected() {
                        Log.w(TAG, "onAidlServiceDisconnected: ");
                    }
                });
        mAidlCommunicate.addOnAidlCallBack(new ICommonAidlListener<BtPhone>() {
            @Override
            public void onReceiveBleData(BtPhone btPhone) {
                Log.d(TAG, "onReceiveBleData: " + btPhone);
                String incommingNumber = btPhone.getNumber();
                Log.d(TAG, " mIncommingNumber: " + incommingNumber);
                // phone incomming telegram, display the view
                mHandler.sendMessage(Message.obtain(mHandler, MSG_INCOMMING_CALL, btPhone));
            }
        }, BtPhone.class);

        mAidlCommunicate.addOnAidlCallBack(new ICommonAidlListener<String>() {
            @Override
            public void onReceiveBleData(String s) {
                Log.v(TAG, "onReceiveBleData: " + s);
                if (s == null) {
                    return;
                } else if (s.equals(RequestCode.PHONE_CALL_READY)) { // 手机HFP就绪
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_EVENT, 1, 0));

                } else if (s.equals(RequestCode.PHONE_OFFHOOK)) {
                    // dissmiss the incomming catelegramll view
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_EVENT, 2, 0));

                } else if (s.equals(RequestCode.WATCH_OFFHOOK)) {
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_EVENT, 3, 0));
                    // watch can accept call

                } else if (s.equals(RequestCode.PHONE_END_CALL)) {
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_EVENT, 4, 0 ));

                }
            }
        }, String.class);

        mAidlCommunicate.initAIDL(this);
    }

}
