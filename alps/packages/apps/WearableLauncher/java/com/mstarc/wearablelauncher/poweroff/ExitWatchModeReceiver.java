package com.mstarc.wearablelauncher.poweroff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mstarc.wearablelauncher.LauncherApplication;


/**
 * Created by wangxinzhi on 17-7-10.
 */

public class ExitWatchModeReceiver extends BroadcastReceiver {
    PowerConfirmDialog mPowerConfirmDialog;
    Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((LauncherApplication)context.getApplicationContext()).onReceive(intent);
    }
}
