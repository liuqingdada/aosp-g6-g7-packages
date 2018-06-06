package com.mstarc.wearablephone.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by wangxinzhi on 17-4-29.
 */

public class BTCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        BTCallManager.getInstance(context.getApplicationContext()).onBroadCastReceive(context, intent);
    }
}
