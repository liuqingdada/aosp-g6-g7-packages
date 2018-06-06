package com.mstarc.wearablesettings.activitys;

import android.os.Bundle;
import android.app.Activity;

import com.mstarc.fakewatch.ota.api.bean.OTAUpdate;
import com.mstarc.fakewatch.settings.Settings;

import mstarc_os_api.mstarc_os_api_msg;


public class BaseActivity extends Activity {

    private static OTAUpdate sOTAUpdate;
    Settings mSettings;
    static boolean mIsSettingReady = false;
    static mstarc_os_api_msg m_api_msg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mSettings == null) {
            mSettings = Settings.getInstance();
        }
    }


    public static OTAUpdate getOTAUpdate() {
        return sOTAUpdate;
    }

    public static void setOTAUpdate(OTAUpdate otaUpdate) {
        sOTAUpdate = otaUpdate;
    }
}
