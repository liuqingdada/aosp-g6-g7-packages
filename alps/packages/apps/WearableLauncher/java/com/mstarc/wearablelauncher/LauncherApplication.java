package com.mstarc.wearablelauncher;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.mstarc.wearablelauncher.poweroff.PowerConfirmDialog;
import com.mstarc.wearablelauncher.service.LauncherService;

import static com.mstarc.wearablelauncher.CommonManager.POWERMODE_NORMAL;

/**
 * Created by wangxinzhi on 17-6-4.
 */

public class LauncherApplication extends Application implements PowerConfirmDialog.Listener {
    PowerConfirmDialog mPowerConfirmDialog;
    public LauncherService mLauncherService;
    public final ServiceConnection mServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLauncherService = ((LauncherService.ServiceBinder) service).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLauncherService = null;
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        Intent startIntent = new Intent();
        startIntent.setAction("com.mstarc.wearablelauncher.started");
        sendBroadcast(startIntent);

        Intent ServiceIntent = new Intent(this, LauncherService.class);
        startService(ServiceIntent);
        bindService(ServiceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onTerminate() {
        unbindService(mServiceConn);
        super.onTerminate();
    }

    public void onReceive(Intent intent) {
        String action = intent.getAction();
        if (action == "com.mstarc.powerkey.exitwatchmode") {
            if (mPowerConfirmDialog != null) {
                if (mPowerConfirmDialog.isShowing()) {
                    mPowerConfirmDialog.dismiss();
                    return;
                }
            } else {
                mPowerConfirmDialog = new PowerConfirmDialog(this,
                        this,
                        getResources().getString(R.string.power_exit_watch_mode_promtion));
            }
            mPowerConfirmDialog.show();
        }
    }

    @Override
    public void onConfirm() {
        CommonManager.getInstance(this).setmPowerMode(POWERMODE_NORMAL);
    }

    @Override
    public void onCancel() {

    }
}
