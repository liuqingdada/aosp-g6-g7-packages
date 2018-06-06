package com.mstarc.mobiledataservice.core.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mstarc.mobiledataservice.IMobileDataAidl;
import com.mstarc.mobiledataservice.R;
import com.mstarc.mobiledataservice.core.reboot.Reboot;
import com.mstarc.mobiledataservice.core.receiver.SettingsReceiver;

import java.lang.reflect.Method;

public class MobileDataService extends Service {
    private static final String TAG = "MobileDataService";
    private SettingsReceiver mSettingsReceiver;

    private boolean isEraseSdCard = true;
    private Reboot mReboot;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        mSettingsReceiver = new SettingsReceiver();
        IntentFilter intentFilter = new IntentFilter("com.mstarc.watch.time");
        registerReceiver(mSettingsReceiver, intentFilter);
        Log.d(TAG, "onCreate: registerReceiver");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroound();
        return START_STICKY;
    }

    private void startForegroound() {
        Intent notificationIntent = new Intent(getApplicationContext(), MobileDataService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1,
                                                               notificationIntent,
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                MobileDataService.this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("设置服务")
                .setContentText("")
                .setContentIntent(pendingIntent);
        Notification notification = mNotifyBuilder.build();
        startForeground(0, notification); // 0 不在通知栏显示    1 显示
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSettingsReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mReboot == null) {
            mReboot = new Reboot(this);
            isEraseSdCard = true;
        }
        return mMobileDataAidl;
    }

    private IMobileDataAidl.Stub mMobileDataAidl = new IMobileDataAidl.Stub() {
        @Override
        public void setMobileDataState(boolean state) throws RemoteException {
            MobileDataService.this.setMobileDataState(getApplicationContext(), state);
        }

        @Override
        public boolean getMobileDataState() throws RemoteException {
            return MobileDataService.this.getMobileDataState(getApplicationContext());
        }

        @Override
        public void reboot() throws RemoteException {
            mReboot.reboot();
        }

        @Override
        public void shutDown() throws RemoteException {
            mReboot.shutDown();
        }

        @Override
        public void restoreFactory() throws RemoteException {
            mReboot.restoreFactory(isEraseSdCard);
        }
    };

    private boolean getMobileDataState(Context cxt) {

        TelephonyManager telephonyService = (TelephonyManager) cxt.getSystemService(
                Context.TELEPHONY_SERVICE);
        try {
            Method getMobileDataEnabledMethod = telephonyService.getClass()
                                                                .getDeclaredMethod(
                                                                        "getDataEnabled");
            if (null != getMobileDataEnabledMethod) {
                return (boolean) getMobileDataEnabledMethod.invoke(
                        telephonyService);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void setMobileDataState(Context cxt, boolean mobileDataEnabled) {
        TelephonyManager telephonyService = (TelephonyManager) cxt.getSystemService(
                Context.TELEPHONY_SERVICE);
        try {
            Method setMobileDataEnabledMethod = telephonyService.getClass()
                                                                .getDeclaredMethod("setDataEnabled",
                                                                                   boolean.class);
            if (null != setMobileDataEnabledMethod) {
                setMobileDataEnabledMethod.invoke(telephonyService, mobileDataEnabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
