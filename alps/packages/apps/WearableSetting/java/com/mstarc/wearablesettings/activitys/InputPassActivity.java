package com.mstarc.wearablesettings.activitys;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.utils.SharedPreferencesHelper;
import com.mstarc.wearablesettings.views.LocusPassWordView;

/**
 * Created by Sye on 2015/10/14.
 */
public class InputPassActivity extends BaseActivity {

    final static String ACTION_DISABLE_KEYGAURD = "ACTION_DISABLE_KEYGAURD";
    final static String ACTION_LAUNCHER_KEEPFOREGROUND = "com.mstart.launcher.poweroff.keepFOREGROUND";
    private WindowManager mManager;
    private String mPwd;
    private SharedPreferencesHelper sph;
    private CallRecevier mCallRecevier;
    private View keyguardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        sph = SharedPreferencesHelper.getInstance(getApplicationContext());
        sph.putBoolean(SharedPreferencesHelper.IS_SHOW_PW, true);
        mPwd = Settings.Secure.getString(getContentResolver(), "wearable_password");
        showKeyguardView(true);
        mCallRecevier = new CallRecevier();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DISABLE_KEYGAURD);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(ACTION_LAUNCHER_KEEPFOREGROUND); // launcher power off
        registerReceiver(mCallRecevier, filter);
        Log.i("MyService", "lock start");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mCallRecevier != null) {
                unregisterReceiver(mCallRecevier);
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Receiver not registered")) {
            } else {
                throw e;
            }
        }
    }

    private View showKeyguardView(boolean showSettings) {
        keyguardView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.activity_service_pass, null);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        wmParams.format = PixelFormat.TRANSPARENT;
        wmParams.flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        wmParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        mManager.addView(keyguardView, wmParams);
        final TextView tip = (TextView) keyguardView.findViewById(R.id.tip);
        tip.setText(getString(R.string.input_password));
        final LocusPassWordView passView = (LocusPassWordView) keyguardView.findViewById(R.id.locusPassWordView);
        passView.setOnCompleteListener(new LocusPassWordView.OnCompleteListener() {

            @Override
            public void onComplete(String password) {
                if (password.equals(mPwd)) {
                    mManager.removeView(keyguardView);
                    keyguardView = null;
                    if (mCallRecevier != null) {
                        unregisterReceiver(mCallRecevier);
                    }
                    tip.setText(getString(R.string.unlock_success));
                    sph.putBoolean(SharedPreferencesHelper.IS_SHOW_PW, false);
                    finish();
                } else {
                    passView.clearPassword(100);
                    tip.setText(getString(R.string.pwd_error_input_try));
                }
            }

            @Override
            public void shortPassword() {
                passView.clearPassword(100);
                tip.setText(getString(R.string.pwd_error_input_try));
            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i("MyService", "set touch");
                passView.enableTouch();
            }
        }, 500);
        return keyguardView;
    }

    private class CallRecevier extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_DISABLE_KEYGAURD) || intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (keyguardView != null && mManager != null) {
                    mManager.removeView(keyguardView);
                    sph.putBoolean(SharedPreferencesHelper.IS_SHOW_PW, false);
                    sph.putBoolean(SharedPreferencesHelper.IS_CALL, true);
                    finish();
                }
            } else if (intent.getAction().equals(ACTION_LAUNCHER_KEEPFOREGROUND)) {
                mManager.removeView(keyguardView);
                sph.putBoolean(SharedPreferencesHelper.IS_SHOW_PW, false);
                finish();
            }
        }
    }

    @Override
    protected void onStop() {
        Log.i("MyService", "lock end");
        super.onStop();
    }
}
