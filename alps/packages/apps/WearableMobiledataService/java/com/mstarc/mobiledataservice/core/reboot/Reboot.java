package com.mstarc.mobiledataservice.core.reboot;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import com.android.internal.os.storage.ExternalStorageFormatter;

/**
 * Created by liuqing
 * 17-11-2.
 * Email: 1239604859@qq.com
 */

public class Reboot {
    private Context mContext;

    public Reboot(Context context){
        mContext = context;
    }

    public void reboot() {
        PowerManager powerManger = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        powerManger.reboot(null);
    }

    public void shutDown() {
        Intent i = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
        i.putExtra("android.intent.extra.KEY_CONFIRM", false);
        i.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    public void restoreFactory(boolean isEraseSdCard) {
        if (isEraseSdCard) {
            Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
            intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
            intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            mContext.startService(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
            mContext.sendBroadcast(intent);
            // Intent handling is asynchronous -- assume it will happen soon.
        }
    }
}
