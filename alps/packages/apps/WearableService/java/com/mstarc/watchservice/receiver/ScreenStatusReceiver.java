package com.mstarc.watchservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;

import com.mstarc.watchservice.service.CommunicateService;

public class ScreenStatusReceiver extends BroadcastReceiver {
    public static final String LAUNCHER_SETTING_POWERMODE = "launcher_settings_powermode";

    /**
     * 进入手环模式：
     * 屏幕亮： 发广播 -- 进入微光
     * 屏幕暗： 无操作
     * <p>
     * 退出手环模式并且微光功能打开：
     * 屏幕亮： 发广播 -- 退出微光
     * 屏幕暗： 发广播 -- 进入微光
     * <p>
     * ---------------------------------------
     * <p>
     * 屏幕亮变暗；不是手环模式并且微光功能打开
     * 发广播 -- 进入微光
     * <p>
     * 屏幕暗变亮；不是手环模式
     * 发广播 -- 退出微光
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (intent.getAction()
                      .equals("com.mstarc.modechanged")) {
                int powermode = intent.getIntExtra("powermode", 2);
                if (powermode == 1) { // 切换到手环模式
                    if (pm.isInteractive()) { // 屏幕亮
                        context.sendBroadcast(new Intent("com.mstarc.ambient.on"));
                    }

                } else if (CommunicateService.getInstance()
                                             .isConstantLight()) { // 切换到其他模式并且微光功能打开
                    if (pm.isInteractive()) { // 屏幕亮
                        context.sendBroadcast(new Intent("com.mstarc.ambient.off"));

                    } else { // 屏幕暗
                        context.sendBroadcast(new Intent("com.mstarc.ambient.on"));
                    }
                }
            }

            // **************************************************************************
            // **************************************************************************

            int powerMode;
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                powerMode = Settings.System.getInt(context.getContentResolver(),
                                                   LAUNCHER_SETTING_POWERMODE);
                if (powerMode != 1) { // 不是手环模式
                    context.sendBroadcast(new Intent("com.mstarc.ambient.off"));
                }

            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                powerMode = Settings.System.getInt(context.getContentResolver(),
                                                   LAUNCHER_SETTING_POWERMODE);
                if (powerMode != 1) { // 不是手环模式并且微光功能打开
                    if (CommunicateService.getInstance()
                                          .isConstantLight()) {
                        context.sendBroadcast(new Intent("com.mstarc.ambient.on"));
                    }
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }
}
