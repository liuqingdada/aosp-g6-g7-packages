package com.mstarc.weather.weather;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

/**
 * description
 * <p/>
 * Created by andyding on 2017/6/30.
 */

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
