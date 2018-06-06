package com.mstarc.wearablesettings.common;

import android.app.Application;

import com.mstarc.fakewatch.ota.OTAWizard;
import com.mstarc.fakewatch.settings.Settings;
import mstarc_os_api.mstarc_os_api_msg;
/**
 * Created by liuqing
 * 17-12-13.
 * Email: 1239604859@qq.com
 */

public class App extends Application {
    Settings mSettings;
    static boolean mIsSettingReady = false;
    static mstarc_os_api_msg m_api_msg;
    @Override
    public void onCreate() {
        super.onCreate();
        OTAWizard.getInstance()
                 .init(this);
        mIsSettingReady = false;
        if (mSettings == null) {
            mSettings = Settings.getInstance();
        }
        mSettings.setStWatchServiceConnectListener(new Settings.WatchServiceConnectListener() {

            @Override
            public void onWatchServiceConnected() {
                mIsSettingReady = true;
                m_api_msg = mSettings.getM_api_msg();
            }

            @Override
            public void onWatchServiceDisconnected() {
                mIsSettingReady = false;
                m_api_msg = null;
                mSettings.init(getApplicationContext());
            }
        });
        mSettings.init(this);
    }
}
