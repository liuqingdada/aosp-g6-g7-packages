package com.mstarc.wearablemms;

import android.app.Application;

import com.mstarc.wearablemms.database.DatabaseWizard;

/**
 * Created by liuqing
 * 17-11-22.
 * Email: 1239604859@qq.com
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DatabaseWizard.getInstance()
                      .setDatabase(this);
    }
}
