package com.mstarc.wearablelauncher.service;

import android.app.ActivityOptions;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mstarc.wearablelauncher.MainActivity;
import com.mstarc.wearablelauncher.battery.Charge;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Created by wangxinzhi on 17-8-13.
 */

public class LauncherService extends Service {

    public static final String INTENT_AMBIENT_ON = "com.mstarc.ambient.on";
    public static final String INTENT_AMBIENT_OFF = "com.mstarc.ambient.off";
    private static final String TAG = LauncherService.class.getSimpleName();
    boolean isPowerPlugged = false;
    boolean isScreenOn = false;
    boolean isAmbientOn = false;
    boolean isLauncherShowing = false;


    boolean isChargeShowing = false;

    CommonReceiver mCommonReceiver = new CommonReceiver();

    public class ServiceBinder extends Binder {
        public LauncherService getService() {
            return LauncherService.this;
        }
    }


//    class ServiceHandler extends Handler {
//        public final static int MSG_START_CHARGE_UI = 1;
//
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            switch (msg.what) {
//                case MSG_START_CHARGE_UI:
//                    startChargeUI();
//                    break;
//                default:
//                    break;
//            }
//        }
//
//        public void startChargeUIDelayed() {
//            cancleStartUIMessage();
//            sendEmptyMessageDelayed(MSG_START_CHARGE_UI, 500);
//        }
//
//        public void cancleStartUIMessage() {
//            removeMessages(MSG_START_CHARGE_UI);
//        }
//
//    }

    public void setLauncherShowing(boolean launcherShowing) {
        isLauncherShowing = launcherShowing;
    }

    public boolean isLauncherShowing() {
        return isLauncherShowing;
    }

    public boolean isChargeShowing() {
        return isChargeShowing;
    }

    public void setChargeShowing(boolean chargeShowing) {
        isChargeShowing = chargeShowing;
    }

    public boolean isPowerPlugged() {
        return isPowerPlugged;
    }

    public boolean isScreenOn() {
        return isScreenOn;
    }

    public boolean isAmbientOn() {
        return isAmbientOn;
    }

    public void startChargeUI() {
        if (isChargeShowing) {
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(this, Charge.class);
        Bundle translateBundle = ActivityOptions.makeCustomAnimation(LauncherService.this, 0, 0).toBundle();
        startActivity(intent, translateBundle);
        Log.d(TAG, "startChargeUI");
    }

    void startHomeUI() {
        if (isLauncherShowing) {
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(this, MainActivity.class);
        Bundle translateBundle = ActivityOptions.makeCustomAnimation(LauncherService.this, 0, 0).toBundle();
        startActivity(intent, translateBundle);
        Log.d(TAG, "startHomeUI");
    }

    class CommonReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive action: " + action);
            switch (action) {
                case Intent.ACTION_POWER_CONNECTED:
                    startChargeUI();
                    isPowerPlugged = true;
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    isPowerPlugged = false;
                    break;
                case Intent.ACTION_SCREEN_ON:
                    isScreenOn = true;
                    if (isPowerPlugged) {
                        startChargeUI();
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    isScreenOn = false;
                    if (isPowerPlugged) {
                        startChargeUI();
                    } else {
                        startHomeUI();
                    }
                    break;
                case INTENT_AMBIENT_ON:
                    isAmbientOn = true;
                    if (!isPowerPlugged) {
                        startHomeUI();
                    } else {
                        startChargeUI();
                    }
                    break;
                case INTENT_AMBIENT_OFF:
                    isAmbientOn = false;
                    break;

            }

        }
    }

    void registerCommonReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(INTENT_AMBIENT_ON);
        filter.addAction(INTENT_AMBIENT_OFF);
        filter.setPriority(Integer.MAX_VALUE);
        registerReceiver(mCommonReceiver, filter);
    }

    void unRegisterCommonReceiver() {
        unregisterReceiver(mCommonReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        registerCommonReceiver();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unRegisterCommonReceiver();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(fd, writer, args);
    }
}
