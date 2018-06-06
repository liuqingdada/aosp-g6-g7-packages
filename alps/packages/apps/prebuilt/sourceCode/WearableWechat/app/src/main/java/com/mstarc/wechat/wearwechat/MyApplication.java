package com.mstarc.wechat.wearwechat;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class MyApplication extends Application {
    private static Context mAppContext;
    private static MyApplication mInstance;

    public static Context getAppContext() {
        return mAppContext;
    }

    public static MyApplication getInstance() {
        return mInstance;
    }

    public void onCreate() {
        super.onCreate();
        Log.d("dingyichen", "onCreate init speech utility!");
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5926df8c");
        //SpeechUtility.createUtility(this, SpeechConstant.APPID + "=12345678");
        mInstance = this;
        setAppContext(getApplicationContext());
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    public void setAppContext(Context paramContext) {
        mAppContext = paramContext;
    }
}
