package com.mstarc.wearablesettings.activitys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.utils.SharedPreferencesHelper;
import com.mstarc.wearablesettings.views.LocusPassWordView;

public class ServicePassActivity extends Activity {

    public enum PWSTATUS {
        NOPW, HASPW;
    }

    private PWSTATUS mCurrentPWStatus;
    private LocusPassWordView mPwdView;
    private int mErrorTime = 0;
    private int mNewPasswordTime = 0;
    private int mPWInputTime = 0;
    private Handler mHandler;
    private FinishRunnable mRunnable;
    private HideErrorInfoRunnable mHideRunnable;
    private Bundle mBundle;
    private TextView mErrorInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_pass);
        mHandler  = new Handler();
        mRunnable = new FinishRunnable();
        mHideRunnable = new HideErrorInfoRunnable();
        final SharedPreferencesHelper sph = SharedPreferencesHelper.getInstance(getApplicationContext());
        final String pwd = Settings.Secure.getString(getContentResolver(),"wearable_password");
        final TextView tv = (TextView) findViewById(R.id.tip);
        mBundle = getIntent().getExtras();
        if(mBundle == null) {
            tv.setText(R.string.input_password);
        }else{
            if(TextUtils.isEmpty(pwd) || pwd.length() == 0) {
                tv.setText(R.string.input_password);
                mCurrentPWStatus = PWSTATUS.NOPW;
            }else{
                tv.setText(R.string.input_old_password);
                mCurrentPWStatus = PWSTATUS.HASPW;
            }
        }
        mErrorInfo = (TextView)findViewById(R.id.error_info);
        mPwdView = (LocusPassWordView) this.findViewById(R.id.locusPassWordView);
        mPwdView.enableTouch();
        mPwdView.setOnCompleteListener(new LocusPassWordView.OnCompleteListener() {
            @Override
            public void onComplete(String mPassword) {
                if(mBundle == null) {
                    if (pwd.equals(mPassword)) {
                        //start launcher
                        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
                        sendBroadcast(intent);
                    } else {
                        if (mErrorTime < 3) {
                            mErrorTime++;
                            mPwdView.markError();
                            Toast.makeText(ServicePassActivity.this, getResources().getString(R.string.pwd_error_input_again), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ServicePassActivity.this, getResources().getString(R.string.pwd_error_input_next_time), Toast.LENGTH_LONG).show();
                            mHandler.post(mRunnable);
                            //lock screen
                        }
                    }
                } else if(mCurrentPWStatus == PWSTATUS.NOPW) {
                    if(mPWInputTime == 0) {
                        sph.putString("tmppassword",mPassword);
                        mPWInputTime++;
                        tv.setText(R.string.input_password_again);
                        mPwdView.clearPassword(100);
                    }else if(mPWInputTime == 1) {

                        String tmpPassword = sph.getString("tmppassword", "");
                        if (mPassword.equals(tmpPassword)) {
                            Settings.Secure.putString(getContentResolver(),"wearable_password",mPassword);
                            tv.setText(R.string.set_password_sc);
                            mHandler.postDelayed(mRunnable,2000);
                        }else{
                            tv.setText(R.string.pwd_not_right);
                            mPwdView.markError();
                        }
                    }
                }else if(mCurrentPWStatus == PWSTATUS.HASPW) {
                    if(mPWInputTime == 0) {
                        if(pwd.equals(mPassword)) {
                            mPwdView.clearPassword(100);
                            tv.setText(R.string.input_new_password);
                            mPWInputTime ++;
                        }else{
                            if(mErrorTime <3) {
                                mErrorTime ++;
                                mPwdView.markError();
                                tv.setText(R.string.pwd_not_right);
                            } else {
                                Toast.makeText(ServicePassActivity.this, getResources().getString(R.string.pwd_error_input_next_time), Toast.LENGTH_LONG).show();
                                mHandler.postDelayed(mRunnable, 2000);
                            }
                        }
                    }else if(mPWInputTime == 1) {
                        mPwdView.clearPassword(100);
                        tv.setText(R.string.input_new_password_again);
                        mPWInputTime ++;
                        sph.putString("tmppassword",mPassword);
                    }else if(mPWInputTime == 2) {
                        String tmpPassword = sph.getString("tmppassword", "");
                        if (mPassword.equals(tmpPassword)) {
                            Settings.Secure.putString(getContentResolver(),"wearable_password",mPassword);
                            mErrorInfo.setVisibility(View.VISIBLE);
                            mErrorInfo.setText(R.string.set_password_sc);
                            mHandler.postDelayed(mRunnable,2000);
                        }else{
                            mNewPasswordTime ++;
                            if(mNewPasswordTime >3) {
                                Toast.makeText(ServicePassActivity.this, getResources().getString(R.string.pwd_error_input_next_time), Toast.LENGTH_LONG).show();
                                finish();
                            }else {
                                tv.setText(R.string.pwd_not_right);
                                mPwdView.markError();
                            }
                        }
                    }
                }
            }

            @Override
            public void shortPassword() {
                mErrorInfo.setVisibility(View.VISIBLE);
            }
        });
    }
    class FinishRunnable implements Runnable {

        @Override
        public void run() {
            finish();
        }
    }

    class HideErrorInfoRunnable implements Runnable {

        @Override
        public void run() {
            mErrorInfo.setVisibility(View.GONE);
        }
    }

    public void errorInfoClick(View v) {
        mHandler.postDelayed(mHideRunnable,2000);
    }
}
