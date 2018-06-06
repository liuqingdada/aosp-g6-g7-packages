package com.mstarc.watchservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mstarc.commonbase.communication.bluetooth.utils.BtUtils;
import com.mstarc.commonbase.communication.utils.Constant;

/**
 * Created by liuqing
 * 2017/3/28.
 * Email: 1239604859@qq.com
 */

public class BatteryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()
                  .equals(Intent.ACTION_BATTERY_CHANGED)) {
            int current = intent.getExtras()
                                .getInt("level");// 获得当前电量
            int total = intent.getExtras()
                              .getInt("scale");// 获得总电量
            int percent = current * 100 / total;

            BtUtils.putString(context, Constant.BATTERY, percent + "%");
        }
    }
}
