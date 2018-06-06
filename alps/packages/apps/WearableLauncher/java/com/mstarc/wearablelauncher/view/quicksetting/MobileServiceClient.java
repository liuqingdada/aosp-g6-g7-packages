package com.mstarc.wearablelauncher.view.quicksetting;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.mstarc.mobiledataservice.IMobileDataAidl;

/**
 * Created by mstarc on 17-11-22.
 */

public class MobileServiceClient {
    private static MobileServiceClient ourInstance;
    private IMobileDataAidl mIMobileDataAidl;
    private MServiceConnection mMServiceConnection;

    public static MobileServiceClient getInstance() {
        if (ourInstance == null) {
            ourInstance = new MobileServiceClient();
        }
        return ourInstance;
    }

    private MobileServiceClient() {
    }

    private class MServiceConnection implements ServiceConnection {
        private Context mContext;

        MServiceConnection(Context context) {
            mContext = context;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIMobileDataAidl = IMobileDataAidl.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIMobileDataAidl = null;
            if (mContext != null) {
                initMobileService(mContext);
            }
        }
    }

    /**
     * the M service:
     * the time settings, the watchface, reboot functions
     */
    public void initMobileService(Context context) {
        Intent intent = new Intent();
        intent.setAction("com.mstarc.aidl.mobiledataservice");
        intent.setPackage("com.mstarc.mobiledataservice");
        context.startService(intent);
        mMServiceConnection = new MServiceConnection(context);
        context.bindService(intent, mMServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public IMobileDataAidl getmIMobileDataAidl() {
        return mIMobileDataAidl;
    }

    public void unbind(Context context) {
        if (mMServiceConnection != null) {
            context.unbindService(mMServiceConnection);
        }
    }
}
