package com.mstarc.wearablephone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.wearablephone.bluetooth.BTCallManager;
import com.mstarc.wearablephone.view.common.RippleLayout;

/**
 * Created by wangxinzhi on 17-3-7.
 */

public class DialOutActivity extends Activity implements View.OnClickListener {
    public final static String INTENT_DIAL_BY_PHONE_BOOLEAN = "INTENT_DIAL_BY_PHONE_BOOLEAN";
    public final static String INTENT_DIAL_BY_PHONE_NUMBER = "INTENT_DIAL_BY_PHONE_NUMBER";
    private static final String TAG = "DialOutActivity";
    boolean mDialByPhone = false;
    ImageView mImageWatch, mImagePhone;
    RippleLayout mRipple;
    String mDialPhoneNumber;
    TextView mLabel;
    ImageView mProfile;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyG7Theme();
        setContentView(R.layout.dial_out);
        mImageWatch = (ImageView) findViewById(R.id.image_dial_out_watch);
        mImagePhone = (ImageView) findViewById(R.id.image_dial_out_phone);
        mProfile = (ImageView) findViewById(R.id.image_dial_out_profile);
        mLabel = (TextView)findViewById(R.id.text_dial_out_name);
        Intent intent = getIntent();

        mDialByPhone = intent.getBooleanExtra(INTENT_DIAL_BY_PHONE_BOOLEAN, false);
        if (mDialByPhone) {
            mImagePhone.setVisibility(View.VISIBLE);
            mImageWatch.setVisibility(View.INVISIBLE);
            mDialPhoneNumber = intent.getStringExtra(INTENT_DIAL_BY_PHONE_NUMBER);

        } else {
            mImagePhone.setVisibility(View.INVISIBLE);
            mImageWatch.setVisibility(View.VISIBLE);
        }
        mRipple = (RippleLayout) findViewById(R.id.ripple_layout);
        Settings.System.putInt(getContentResolver(),
                               "mstarc_BT_PHONE_STATE",
                               1);
    }

    @Override
    protected void onPause() {
        mRipple.stopRippleAnimation();
        super.onPause();
        if (isFinishing()) {
            BTCallManager.getInstance(getApplicationContext()).unsetActivity(BTCallManager.ACTIVITY_TYPE_DIAL_OUT);
        }
        BTCallManager.getInstance(getApplicationContext()).onUiHide(BTCallManager.ACTIVITY_TYPE_DIAL_OUT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BTCallManager.getInstance(getApplicationContext()).unsetActivity(BTCallManager.ACTIVITY_TYPE_DIAL_OUT);
        Settings.System.putInt(getContentResolver(),
                               "mstarc_BT_PHONE_STATE",
                               0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BTCallManager.getInstance(getApplicationContext()).updateActivity(BTCallManager.ACTIVITY_TYPE_DIAL_OUT, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "mDialByPhone: " + mDialByPhone + " mDialPhoneNumber: " + mDialPhoneNumber);
        mRipple.startRippleAnimation();
        if (mDialByPhone && mDialPhoneNumber != null) {
            BTCallManager.getInstance(this.getApplicationContext()).prepareDial(mDialPhoneNumber);
            BTCallManager.getInstance(this.getApplicationContext()).get_people_image(mDialPhoneNumber, mLabel, mProfile);
            BTCallManager.getInstance(this.getApplicationContext()).onUiShown(BTCallManager.ACTIVITY_TYPE_DIAL_OUT);
        } else{
            try {
                mImagePhone.setVisibility(View.VISIBLE);
                mImageWatch.setVisibility(View.INVISIBLE);
                mDialPhoneNumber = BTCallManager.getInstance(this.getApplicationContext()).getCurrentCallNumber();
                BTCallManager.getInstance(this.getApplicationContext()).get_people_image(mDialPhoneNumber, mLabel, mProfile);
                BTCallManager.getInstance(this.getApplicationContext()).onUiShown(BTCallManager.ACTIVITY_TYPE_DIAL_OUT);
            } catch (Exception e){
                Log.e(TAG, "onResume: ", e);
                Toast.makeText(this, "请和手机完成蓝牙电话配对", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mDialByPhone = intent.getBooleanExtra(INTENT_DIAL_BY_PHONE_BOOLEAN, false);
        if (mDialByPhone) {
            mImagePhone.setVisibility(View.VISIBLE);
            mImageWatch.setVisibility(View.INVISIBLE);
        } else {
            mImagePhone.setVisibility(View.INVISIBLE);
            mImageWatch.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick");
        BTCallManager.getInstance(getApplicationContext()).terminateCall();
        finish();
    }

    void applyG7Theme() {
        int theme = ((PhoneApplication)getApplication()).getThemeStyle();
        if (theme != 0) {
            setTheme(theme);
        }
    }
}
