package com.mstarc.wearablephone;

import android.app.Application;
import android.os.SystemProperties;
import android.util.Log;

import com.iflytek.cloud.SpeechUtility;
import com.mstarc.wearablephone.bluetooth.BTCallManager;
import com.mstarc.wearablephone.database.DatabaseWizard;

/**
 * Created by hawking on 17-5-22.
 */

public class PhoneApplication extends Application {
    private static final String TAG = "PhoneApplication";
    private int mThemeResID = -1;

    @Override
    public void onCreate() {
        applyG7Theme();
        super.onCreate();
        Log.d(TAG, "onCreate");
        BTCallManager.getInstance(this);
        SpeechUtility.createUtility(PhoneApplication.this, "appid=" + getString(R.string.app_id));
        DatabaseWizard.getInstance().setDatabase(this);
    }

    void applyG7Theme() {
        if (getThemeStyle() != 0) {
            setTheme(mThemeResID);
        }
    }

    public int getThemeStyle() {
        if (mThemeResID != -1) {
            return mThemeResID;
        }
        int theme = SystemProperties.getInt("persist.watch_theme_debug", -1);
        if (theme == -1) {
            theme = SystemProperties.getInt("ro.product.watch_theme_g7", -1);
        } else {
            Log.d(TAG, "setTheme to debug value " + theme);
        }
        switch (theme) {
            case 1:
                mThemeResID = R.style.ThemeRoseGolden;
                break;
            case 2:
                mThemeResID = R.style.ThemeHighBlack;
                break;
            case 3:
                mThemeResID = R.style.ThemeAppleGreen;
                break;
            default:
                mThemeResID = 0;
                Log.d(TAG, "no theme defined");
                break;
        }
        Log.d(TAG, "setTheme to " + theme);
        return mThemeResID;
    }
}
