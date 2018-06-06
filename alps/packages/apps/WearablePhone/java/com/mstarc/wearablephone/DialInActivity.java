package com.mstarc.wearablephone;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablephone.bluetooth.BTCallManager;
import com.mstarc.wearablephone.view.common.ClickHelper;
import com.mstarc.wearablephone.view.common.RippleLayout;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by wangxinzhi on 17-3-7.
 */

public class DialInActivity extends Activity implements View.OnClickListener {
    private static final String TAG = DialInActivity.class.getSimpleName();
    RippleLayout mRipple;
    TextView mLabel;
    ImageView mProfile;

    private String defaultRingstoneFilePath = "/system/media/audio/ringtones/Ring_Synth_04.ogg";
    private SoundPool mSoundPool;
    private Vibrator mVibrator;
    private AudioManager mAudioManager;
    private ScheduledExecutorService vibrateSchduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        applyG7Theme();
        setContentView(R.layout.dial_in);
        ViewGroup rootView = (ViewGroup) findViewById(R.id.dial_in);
        ClickHelper.addOnClickListenersRecursive(rootView, this);
        mRipple = (RippleLayout) findViewById(R.id.ripple_layout);
        mProfile = (ImageView) findViewById(R.id.image_dial_in_profile);
        mLabel =(TextView) findViewById(R.id.text_dial_in_name);
        findViewById(R.id.image_dial_in_watch).setVisibility(View.GONE);
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

        getWindow().addFlags(flags);
        Settings.System.putInt(getContentResolver(),
                               "mstarc_BT_PHONE_STATE",
                               1);
    }

    private void initVibrateSchduler() {
        shutdownVibrateSchduler();
        vibrateSchduler = Executors.newSingleThreadScheduledExecutor();
    }

    private void shutdownVibrateSchduler() {
        if (vibrateSchduler != null) {
            vibrateSchduler.shutdownNow();
            vibrateSchduler = null;
        }
    }

    private void loadWatchRing() {
        initVibrateSchduler();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        String ringstoneFilePath = getIntent().getStringExtra(
                "bt_phone_ringstone_file_path");
        if (ringstoneFilePath != null) {
            defaultRingstoneFilePath = ringstoneFilePath;
        }

        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setMaxStreams(1);

        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_RING);
        builder.setAudioAttributes(attrBuilder.build());

        mSoundPool = builder.build();

        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.v(TAG, "onLoadComplete: sampleId = " + sampleId + "\nstatus = " + status);

                /*
                 * RINGER_MODE_SILENT = 0
                 * RINGER_MODE_VIBRATE = 1
                 * RINGER_MODE_NORMAL = 2
                 */
                int ringerMode = mAudioManager.getRingerMode();
                switch (ringerMode) {
                    case 1:
                        vibrateSchduler.scheduleWithFixedDelay(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mVibrator.vibrate(500);
                                    }
                                }, 0, 1800, TimeUnit.MILLISECONDS);
                        break;
                    case 2:
                        if (soundPool.play(sampleId,
                                           1.0f,
                                           1.0f,
                                           1,
                                           -1,
                                           1.0f) != 0) {
                            vibrateSchduler.scheduleWithFixedDelay(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            mVibrator.vibrate(500);
                                        }
                                    }, 0, 1800, TimeUnit.MILLISECONDS);
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        //
        File watchRingFile = new File(defaultRingstoneFilePath);
        if (watchRingFile.exists()) {
            int soundId = mSoundPool.load(watchRingFile.getPath(), 1);
            Log.v(TAG, "testRingstone: soundId = " + soundId);
        } else {
            Log.d(TAG, "loadWatchRing: ringstone file is not exists.");
        }
    }

    private void stopWatchRing() {
        mSoundPool.release();
        shutdownVibrateSchduler();
    }

    @Override
    protected void onPause() {
        mRipple.stopRippleAnimation();
        super.onPause();
        Log.d(TAG, "onPause");
        if (isFinishing()) {
            BTCallManager.getInstance(getApplicationContext()).unsetActivity(BTCallManager.ACTIVITY_TYPE_DIAL_IN);
        }
        BTCallManager.getInstance(getApplicationContext()).onUiHide(BTCallManager.ACTIVITY_TYPE_DIAL_IN);
        //BTCallManager.getInstance(this.getApplicationContext()).cancealVibrate();
        stopWatchRing();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWatchRing();
        mRipple.startRippleAnimation();
        Log.d(TAG, "onResume");
        BTCallManager.getInstance(this.getApplicationContext()).get_people_image_incall(mLabel, mProfile);
        BTCallManager.getInstance(this.getApplicationContext()).onUiShown(BTCallManager.ACTIVITY_TYPE_DIAL_IN);
        //BTCallManager.getInstance(this.getApplicationContext()).vibrateForIncommingCall();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWatchRing();
        BTCallManager.getInstance(getApplicationContext()).unsetActivity(BTCallManager.ACTIVITY_TYPE_DIAL_IN);
        Settings.System.putInt(getContentResolver(),
                               "mstarc_BT_PHONE_STATE",
                               0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BTCallManager.getInstance(getApplicationContext()).updateActivity(BTCallManager.ACTIVITY_TYPE_DIAL_IN, this);
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick: "+v);
        if (v.getId() == R.id.button_dial_in_accept) {
//            Intent intent = new Intent(this, TalkingActivity.class);
//            startActivity(intent);
            BTCallManager.getInstance(getApplicationContext()).acceptCall();
            stopWatchRing();
            finish();
        } else if (v.getId() == R.id.button_dial_in_deny) {
            BTCallManager.getInstance(getApplicationContext()).rejectCall();
            stopWatchRing();
            finish();
        }
    }
    void applyG7Theme() {
        int theme = ((PhoneApplication)getApplication()).getThemeStyle();
        if (theme != 0) {
            setTheme(theme);
        }
    }
}
