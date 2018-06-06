package com.mstarc.watchservice;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.mstarc.commonbase.communication.IntelligentPush;
import com.mstarc.commonbase.database.DatabaseWizard;
import com.wanjian.cockroach.Cockroach;

/**
 * Created by liuqing
 * 2017/4/17.
 * Email: 1239604859@qq.com
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Cockroach.install(new Cockroach.ExceptionHandler() {
            @Override
            public void handlerException(final Thread thread, final Throwable throwable) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.e("Cockroach", thread + "\n" + throwable.toString());
                            throwable.printStackTrace();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        IntelligentPush.getInstance()
                       .initIntelligentPush(this);
        DatabaseWizard.getInstance()
                      .setDatabase(this);
        MultiDex.install(this);
    }
}
