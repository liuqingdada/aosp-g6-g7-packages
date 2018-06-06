package com.mstarc.watchservice;

import android.app.Application;
import android.support.multidex.MultiDex;

import com.mstarc.commonbase.communication.IntelligentPush;
import com.mstarc.commonbase.database.DatabaseWizard;

/**
 * Created by liuqing
 * 2017/4/17.
 * Email: 1239604859@qq.com
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        IntelligentPush.getInstance()
                       .initIntelligentPush(this);
        DatabaseWizard.getInstance()
                      .setDatabase(this);
        MultiDex.install(this);
    }
}
