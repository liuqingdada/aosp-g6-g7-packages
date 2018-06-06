package com.mstarc.watchservice.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mstarc.commonbase.communication.bluetooth.ble.periphery.Advertiser;
import com.mstarc.watchservice.service.CommunicateService;

public class BluetoothStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()
                  .equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                           BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF: // 手机蓝牙关闭
                    Advertiser.getInstance()
                              .setStartFlag(false);
                    CommunicateService.bleFlag = false;
                    break;

                case BluetoothAdapter.STATE_TURNING_OFF: // 手机蓝牙正在关闭
                    Advertiser.getInstance()
                              .setStartFlag(false);
                    CommunicateService.bleFlag = false;
                    break;

                case BluetoothAdapter.STATE_ON: // 手机蓝牙开启
                    CommunicateService.bleFlag = true;
                    context.startService(new Intent(context, CommunicateService.class));
                    break;

                case BluetoothAdapter.STATE_TURNING_ON: // 手机蓝牙正在开启
                    break;
            }
        }
    }
}