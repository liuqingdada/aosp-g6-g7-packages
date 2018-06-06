package com.mstarc.wechat.wearwechat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class LaunchActivity extends Activity {

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPrefs = getSharedPreferences("isLogin", Activity.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPrefs.getBoolean("islogin", false)) {
            Log.d("TAG", "LoginActivity_已登录");
            Intent intent = new Intent(LaunchActivity.this, CommunicationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //intent.putExtras(token.toBundle());
            startActivity(intent);
            finish();
        } else {
            Log.d("TAG", "LoginActivity_未登录");
            Intent intent = new Intent();
            intent.setClass(LaunchActivity.this, QrcodeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
}
