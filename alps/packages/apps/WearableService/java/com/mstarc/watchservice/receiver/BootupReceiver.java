package com.mstarc.watchservice.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(
                    Context.BLUETOOTH_SERVICE);
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothAdapter.enable();

        } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
            Log.e("BootupReceiver", "android.intent.action.ACTION_SHUTDOWN");
        }
    }
}
