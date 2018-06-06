package com.mstarc.mobiledataservice.core.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.mstarc.mobiledataservice.core.utils.Constant;
import com.mstarc.mobiledataservice.core.utils.SystemTimeUtils;

public class SettingsIntentService extends IntentService {
    private static final String TAG = "SettingsIntentService";
    public static final String KEY = "settings";

    public static final String SETTINGS_TIME = "settings_time";

    public SettingsIntentService() {
        super("SettingsIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String key = intent.getStringExtra(KEY);
            switch (key) {
                case SETTINGS_TIME:
                    Log.d(TAG, "onHandleIntent: set time");
                    long timestamp = intent.getLongExtra(Constant.TIME_KEY, 0L);
                    if (timestamp != 0) {
                        SystemTimeUtils.setAutoTimeZone(this, 0);
                        SystemTimeUtils.setSysTime(this, timestamp);
                        SystemClock.setCurrentTimeMillis(timestamp);
                        SystemTimeUtils.setTimeZone2(this, "Asia/Shanghai");

                        SystemTimeUtils.setAutoDateTime(this, 1);
                        SystemTimeUtils.setHourFormat(this, SystemTimeUtils.TIME_24);
                    }
                    Log.d(TAG, "onHandleIntent: set time " + timestamp);
                    break;

                default: break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }
}
