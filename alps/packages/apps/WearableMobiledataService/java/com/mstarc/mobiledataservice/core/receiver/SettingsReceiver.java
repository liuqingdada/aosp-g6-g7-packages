package com.mstarc.mobiledataservice.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;

import com.mstarc.mobiledataservice.core.service.SettingsIntentService;
import com.mstarc.mobiledataservice.core.utils.Constant;
import com.mstarc.mobiledataservice.core.utils.ServiceUtils;

public class SettingsReceiver extends BroadcastReceiver {
    private static final String TAG = "SettingsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()
                  .equals(Constant.WATCH_TIME)) {
            Log.d(TAG, "onReceive: set time");

            Intent i = new Intent(context, SettingsIntentService.class);
            i.putExtra(SettingsIntentService.KEY, SettingsIntentService.SETTINGS_TIME);
            i.putExtra(Constant.TIME_KEY, intent.getLongExtra(Constant.TIME_KEY, 0L));
            UserHandle userHandle = ServiceUtils.getUserHandle(ServiceUtils.CURRENT_OR_SELF);
            Log.d(TAG, "onReceive: userHandle -- " + userHandle);
            ServiceUtils.startServiceAsUser(context, i, userHandle);
            Log.d(TAG, "onReceive: start intentservice for settings");
        }
    }
}
