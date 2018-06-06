
package com.mstarc.wearablesettings.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.mstarc.wearablesettings.activitys.InputPassActivity;
import com.mstarc.wearablesettings.utils.SharedPreferencesHelper;


public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("MyService","BOOT_COMPLETED");
        int mode = 2;
        try {
            mode = android.provider.Settings.System.getInt(context.getContentResolver(),"launcher_settings_powermode");
        } catch (android.provider.Settings.SettingNotFoundException e) {
            e.printStackTrace();
            Log.i("MyService","start" + e + "end");

        }
        final String pwd = android.provider.Settings.Secure.getString(context.getContentResolver(),"wearable_password");
        context.startService(new Intent(context,MyService.class));
        SharedPreferencesHelper sph = SharedPreferencesHelper.getInstance(context);
        sph.putBoolean(SharedPreferencesHelper.IS_CALL,false);
        if(mode != 1 && sph.getBoolean(SharedPreferencesHelper.IS_NEED_PW,true) && !TextUtils.isEmpty(pwd)){
            Log.i("MyService","get boot completed and start lock");
            Intent i = new Intent(context, InputPassActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

}
