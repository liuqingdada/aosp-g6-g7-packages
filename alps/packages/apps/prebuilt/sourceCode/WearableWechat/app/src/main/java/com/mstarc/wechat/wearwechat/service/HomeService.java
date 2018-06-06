package com.mstarc.wechat.wearwechat.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.utils.NetUtil;
import com.mstarc.wechat.wearwechat.utils.WxHome;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class HomeService extends Service {
    private CallBack callBack = null;
    private String deviceId = "";
    private static String syncKey = "";
    private Timer timer;
    private Token token;
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Toast.makeText(HomeService.this, "网络已断开,无法继续同步!", Toast.LENGTH_LONG).show();
        }

    };


    public IBinder onBind(Intent paramIntent) {
        this.deviceId = paramIntent.getStringExtra("deviceId");
        this.token = new Token();
        Bundle localBundle = paramIntent.getBundleExtra("token");
        this.token.fromBundle(localBundle);
        this.syncKey = paramIntent.getStringExtra("syncKey");
        Log.i("TAG", "HomeService_onBind:token=" + JSON.toJSONString(this.token) + " deviceId=" + this.deviceId + " syncKey=" + this.syncKey);
        return new HomeBinder();
    }

    public void onCreate() {
        super.onCreate();
    }

    public void onDestroy() {
        stopTimer();
        super.onDestroy();
    }

    public void setCallBack(CallBack paramCallBack) {
        this.callBack = paramCallBack;
    }

    public void setSyncKey(String paramString) {
        this.syncKey = paramString;
    }

    public void startTimer() {
        this.timer = new Timer();
        this.timer.schedule(new HomeTimerTask(), 5L, 60000L);
    }

    public void stopTimer() {
        this.timer.cancel();
        this.timer.purge();
    }

    public interface CallBack {
        void handleServiceData(Properties paramProperties);
    }

    public class HomeBinder extends Binder {
        private HomeService service = HomeService.this;

        public HomeBinder() {
        }

        public HomeService getService() {
            return this.service;
        }

        public void startTimer() {
            this.service.startTimer();
        }

        public void stopTimer() {
            this.service.stopTimer();
        }

        public void updateSyncKey(String paramString) {
            this.service.setSyncKey(paramString);
        }
    }


    private class HomeTimerTask extends TimerTask {
        HomeTimerTask() {

        }

        public void run() {


            if (!NetUtil.hasNet(getApplicationContext())) {
                handler.sendEmptyMessage(0);
                SystemClock.sleep(2000);
            }

            Properties localProperties = WxHome.syncCheck(HomeService.this.token, HomeService.this.deviceId, HomeService.this.syncKey);
            if (HomeService.this.callBack != null)
                HomeService.this.callBack.handleServiceData(localProperties);
        }
    }

}

