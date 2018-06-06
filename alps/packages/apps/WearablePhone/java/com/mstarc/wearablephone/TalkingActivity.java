package com.mstarc.wearablephone;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mstarc.wearablephone.bluetooth.BTCallManager;


/**
 * Created by wangxinzhi on 17-3-7.
 */

public class TalkingActivity extends Activity implements View.OnClickListener {
    private static final String TAG = TalkingActivity.class.getSimpleName();
    ImageButton mKeyPadButton, mVoumeButton;
    ImageView mProfile;
    TextView mNumberTextView, mLabel, mTalkingTimeTextView;
    View mKeyPadView, mVoumeView;
    ProgressBar mVoumeBar;
    AudioManager mAudioManager;
    int mAudioMaxVolume;
    long mTalkingStartTime = 0;
    UiHandler mHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyG7Theme();
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

        getWindow().addFlags(flags);
        setContentView(R.layout.dial_talking);
        ViewGroup rootView = (ViewGroup) findViewById(R.id.talking_root_view);
        addOnClickListenersRecursive(rootView);
        mKeyPadButton = (ImageButton) findViewById(R.id.image_talking_keypad_button);
        mVoumeButton = (ImageButton) findViewById(R.id.image_talking_volume_button);
        mKeyPadView = findViewById(R.id.keypad);
        mVoumeView = findViewById(R.id.talking_volume);
        mNumberTextView = (TextView) findViewById(R.id.dial_number);
        mNumberTextView.setText("");
        mVoumeBar = (ProgressBar) findViewById(R.id.progressBar);
        mProfile = (ImageView) findViewById(R.id.image_talking_profile);
        mLabel = (TextView) findViewById(R.id.text_talking_name);
        findViewById(R.id.image_talking_watch).setVisibility(View.GONE);
        mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        mAudioMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        mVoumeBar.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)*100/mAudioMaxVolume);
        mTalkingTimeTextView = (TextView) findViewById(R.id.talking_time);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler = new UiHandler();
    }

    private void addOnClickListenersRecursive(ViewGroup vg) {
        vg.setOnClickListener(this);
        for (int i = 0; i < vg.getChildCount(); ++i) {
            View nextChild = vg.getChildAt(i);
            if (nextChild instanceof ViewGroup) addOnClickListenersRecursive((ViewGroup) nextChild);
            else nextChild.setOnClickListener(this);
        }
    }

    private void sendDTMF(byte code) {
        BTCallManager btCallManager = BTCallManager.getInstance(this);
        if (btCallManager.isBTPhoneEnnable()) {
            btCallManager.getBluetoothHeadsetClient().sendDTMF(btCallManager.getBtDevice(), code);
        }
    }

    @Override
    public void onClick(View v) {
        char pressedNumber = 0;
        String currentNumber = mNumberTextView.getText().toString();
        boolean actionShowVoumeBar = false;
        boolean actionShowKeypad = false;
        boolean actionTerminate = false;
        boolean actionVoumeDec = false;
        boolean actionVoumeAdd = false;
        boolean actionNumber = true;

        switch (v.getId()) {
            case R.id.button0:
                pressedNumber = '0';
                break;
            case R.id.button1:
                pressedNumber = '1';
                break;
            case R.id.button2:
                pressedNumber = '2';
                break;
            case R.id.button3:
                pressedNumber = '3';
                break;
            case R.id.button4:
                pressedNumber = '4';
                break;
            case R.id.button5:
                pressedNumber = '5';
                break;
            case R.id.button6:
                pressedNumber = '6';
                break;
            case R.id.button7:
                pressedNumber = '7';
                break;
            case R.id.button8:
                pressedNumber = '8';
                break;
            case R.id.button9:
                pressedNumber = '9';
                break;
            case R.id.buttonstar:
                pressedNumber = '*';
                break;
            case R.id.buttonhex:
                pressedNumber = '#';
                break;
            case R.id.image_talking_keypad_button:
                actionShowKeypad = true;
                actionNumber = false;
                break;
            case R.id.image_talking_volume_button:
                actionShowVoumeBar = true;
                actionNumber = false;
                break;
            case R.id.talking_volume_dec:
                actionVoumeDec = true;
                actionNumber = false;
                break;
            case R.id.talking_volume_add:
                actionVoumeAdd = true;
                actionNumber = false;
                break;
            case R.id.image_talking_teminate:
                BTCallManager.getInstance(this.getApplicationContext()).terminateCall();
                finish();
                break;
            default:
                return;
        }
        // code ASCII code to device
        sendDTMF((byte) pressedNumber);

        if (actionNumber) {
            currentNumber += pressedNumber;
            mNumberTextView.setText(currentNumber);
        } else if (actionShowKeypad) {
            mVoumeView.setVisibility(View.GONE);
            if (mKeyPadView.getVisibility() == View.VISIBLE) {
                mKeyPadView.setVisibility(View.GONE);
            } else {
                mKeyPadView.setVisibility(View.VISIBLE);
            }
        } else if (actionShowVoumeBar) {
            mKeyPadView.setVisibility(View.GONE);
            if (mVoumeView.getVisibility() == View.VISIBLE) {
                mVoumeView.setVisibility(View.GONE);
            } else {
                mVoumeView.setVisibility(View.VISIBLE);
            }
        } else if (actionTerminate) {
            mKeyPadView.setVisibility(View.GONE);
            mVoumeView.setVisibility(View.GONE);
        } else if (actionVoumeDec) {
            mVoumeBar.setProgress(mVoumeBar.getProgress() - 10);
            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mVoumeBar.getProgress()*mAudioMaxVolume/100, 0);
        } else if (actionVoumeAdd) {
            mVoumeBar.setProgress(mVoumeBar.getProgress() + 10);
            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mVoumeBar.getProgress()*mAudioMaxVolume/100, 0);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        BTCallManager.getInstance(this.getApplicationContext()).get_people_image_talking(mLabel, mProfile);
        BTCallManager.getInstance(getApplicationContext()).onUiShown(BTCallManager.ACTIVITY_TYPE_DIAL_TALKING);
        mTalkingStartTime = System.currentTimeMillis() ;
        mHandler.sendEmptyMessageDelayed(UiHandler.MSG_UPDATE_TIME, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.sendEmptyMessageDelayed(UiHandler.MSG_UPDATE_TIME,1000);
        if (isFinishing()) {
            BTCallManager.getInstance(getApplicationContext()).unsetActivity(BTCallManager.ACTIVITY_TYPE_DIAL_TALKING);
        }
        BTCallManager.getInstance(this).onUiHide(BTCallManager.ACTIVITY_TYPE_DIAL_TALKING);
        Log.d(TAG,"onPause");
        PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if(powerManager.isInteractive()) {
            Log.v("BTCallManager", "onPause: terminateCall, finish");
            BTCallManager.getInstance(this.getApplicationContext()).terminateCall();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BTCallManager.getInstance(getApplicationContext()).unsetActivity(BTCallManager.ACTIVITY_TYPE_DIAL_TALKING);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BTCallManager.getInstance(getApplicationContext()).updateActivity(BTCallManager.ACTIVITY_TYPE_DIAL_TALKING, this);
    }

    class UiHandler extends Handler{
        public static final int MSG_UPDATE_TIME = 1;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_UPDATE_TIME:
                    final long duration = System.currentTimeMillis() - mTalkingStartTime;
                    String callTimeElapsed = DateUtils.formatElapsedTime(duration / 1000);
                    mTalkingTimeTextView.setText(callTimeElapsed);
                    sendEmptyMessageDelayed(MSG_UPDATE_TIME,1000);
                    break;
            }
        }
    }
    void applyG7Theme() {
        int theme = ((PhoneApplication)getApplication()).getThemeStyle();
        if (theme != 0) {
            setTheme(theme);
        }
    }

}
