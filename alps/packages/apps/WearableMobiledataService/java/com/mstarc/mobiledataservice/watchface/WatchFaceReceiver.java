package com.mstarc.mobiledataservice.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WatchFaceReceiver extends BroadcastReceiver {
    //开机
    private static final String BOOT_ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BOOT_ACTION.equals(action)) {
            Log.d("WatchFaceReceiver", "开机启动表盘服务");
            startService(context);
        }
    }

    private void startService(Context context) {
        Intent intent = new Intent(context, WatchFaceService.class);
        context.startService(intent);
    }
}
