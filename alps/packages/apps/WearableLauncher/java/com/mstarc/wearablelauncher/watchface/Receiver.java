package com.mstarc.wearablelauncher.watchface;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Created by wangxinzhi on 17-7-9.
 */

public class Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action == Intent.ACTION_BOOT_COMPLETED){
            Intent i = new Intent(context.getApplicationContext(), WatchFaceService.class);
            context.getApplicationContext().startService(i);
        }
    }
}
