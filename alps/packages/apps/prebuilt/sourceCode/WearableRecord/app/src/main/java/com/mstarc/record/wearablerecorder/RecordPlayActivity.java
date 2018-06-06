package com.mstarc.record.wearablerecorder;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kw.rxbus.RxBus;
import com.mstarc.record.wearablerecorder.bean.RecordBus;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

public class RecordPlayActivity extends FragmentActivity {
    private static final String TAG = "RecordSuhen";

    private TextView mTimeView;
    private ImageView mControl;
    private CountDownTimer mCountDownTimer;
    private long mCountDownTime;

    private final static int PLAY_PRAPARE = 0;
    private final static int PLAY_ING = 1;
    private final static int PLAY_PAUSE = 2;

    private int mPlayState = PLAY_PRAPARE;
    private int mRecordDuration = 0;

    private ProgressBar mProgressBar;
    private int mMaxVolume;

    private long mLastCmdTime;
    private static final int DEFAULT_ANIMATION_GAP = 300;
    private boolean bMenuShowed = false;


    ImageView mArrowUp;
    LinearLayout mArrowUpLayout;
    LinearLayout mVolumeControl;
    LinearLayout mLayoutAnimation;
    ImageView mVoiceVolume;
    private String mMRecordPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_play);
        Intent intent = getIntent();
        int position = 0;
        if (intent.hasExtra("position")) {
            position = intent.getIntExtra("position", 0);
        }
        TextView name = (TextView) findViewById(R.id.record_name);
        name.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mTimeView = (TextView) findViewById(R.id.content_text);
        mControl = (ImageView) findViewById(R.id.play_control);

        ImageView volumeDown = (ImageView) findViewById(R.id.volume_down);
        ImageView volumeUp = (ImageView) findViewById(R.id.volume_up);
        mArrowUp = (ImageView) findViewById(R.id.arrow_up);
        mArrowUpLayout = (LinearLayout) findViewById(R.id.arrow_up_layout);
        mVolumeControl = (LinearLayout) findViewById(R.id.volume_control);
        mLayoutAnimation = (LinearLayout) findViewById(R.id.layout_animation);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mVoiceVolume = (ImageView) findViewById(R.id.voice_volume);
        updateImageView(mVoiceVolume, R.drawable.icon_record_volume_two);
        updateImageView(volumeDown, R.drawable.icon_record_volume_decrease);
        updateImageView(volumeUp, R.drawable.icon_record_volume_increase);
        updateImageView(mControl, R.drawable.record_start_bg);

        int volume = RecordManager.getInstance().getVolume();
        mMaxVolume = RecordManager.getInstance().getMaxVolume();
        initProgressBar(volume);

        mVolumeControl.post(new Runnable() {
            @Override
            public void run() {
                mLayoutAnimation.setTranslationY(mVolumeControl.getHeight());
            }
        });

        mArrowUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_ANIMATION_GAP) {
                    return;
                }
                mLastCmdTime = System.currentTimeMillis();
                showMenu(bMenuShowed);
            }
        });
        mArrowUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_ANIMATION_GAP) {
                    return;
                }
                mLastCmdTime = System.currentTimeMillis();
                showMenu(bMenuShowed);
            }
        });

        volumeUp.setClickable(true);
        volumeDown.setClickable(true);
        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecordManager.getInstance().volumeDown();
                Log.d("dingyichen", "volume = " + RecordManager.getInstance().getVolume());
                initProgressBar(RecordManager.getInstance().getVolume());
            }
        });

        volumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecordManager.getInstance().volumeUp();
                Log.d("dingyichen", "volume = " + RecordManager.getInstance().getVolume());
                initProgressBar(RecordManager.getInstance().getVolume());
            }
        });

        final String recordname = RecordManager.getInstance().getReverseList().get(position);
        name.setText(recordname);
        mMRecordPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Anhry/" + this.getPackageName() + "/Record" + "/" + recordname + ".aac";
        MediaPlayer mp = MediaPlayer.create(RecordPlayActivity.this, Uri.parse(mMRecordPath));
        if (mp != null) {
            mRecordDuration = mp.getDuration();
            mp.release();
        }
        //int duration = 20000;
        initTimeView(mRecordDuration);

        initCountDownTimer(mRecordDuration);
        mCountDownTime = mRecordDuration;
        mControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controlPlay();
            }
        });

        RxBus.getInstance()
             .toObservable(RecordBus.class)
             .observeOn(AndroidSchedulers.mainThread())
             .subscribe(new Consumer<RecordBus>() {
                 @Override
                 public void accept(RecordBus recordBus) {
                     Log.i(TAG, "accept: " + recordBus);
                     Log.i(TAG, "accept: mPlayState = " + mPlayState);
                     try {
                         if (recordBus.isStartOrPausePlay()) {
                             controlPlay();

                         } else if (recordBus.isStartPlay()) {
                             switch (mPlayState) {
                                 case PLAY_PRAPARE:
                                     if (RecordManager.getInstance()
                                                      .playRrecord(mMRecordPath)) {
                                         mCountDownTimer.start();
                                         updateImageView(mControl, R.drawable.record_stop_bg);
                                         mPlayState = PLAY_ING;
                                     } else {
                                         updateImageView(mControl, R.drawable.record_start_bg);
                                         mPlayState = PLAY_PRAPARE;
                                     }
                                     break;
                                 case PLAY_PAUSE:
                                     initCountDownTimer(mCountDownTime);
                                     RecordManager.getInstance()
                                                  .ContinuePlay();
                                     updateImageView(mControl, R.drawable.record_stop_bg);
                                     mCountDownTimer.start();
                                     mPlayState = PLAY_ING;
                                     break;
                             }

                         } else if (recordBus.isPausePlay()) {
                             switch (mPlayState) {
                                 case PLAY_ING:
                                     RecordManager.getInstance()
                                                  .pausePlay();
                                     mCountDownTimer.cancel();
                                     updateImageView(mControl, R.drawable.record_start_bg);
                                     mPlayState = PLAY_PAUSE;
                                     break;
                             }
                         } else {}
                     } catch (Exception e) {
                         Log.e(TAG, "accept: ", e);
                     }
                 }
             });
    }

    private void controlPlay() {
        switch (mPlayState) {
            case PLAY_PRAPARE:
                if (RecordManager.getInstance()
                                 .playRrecord(mMRecordPath)) {
                    mCountDownTimer.start();
                    updateImageView(mControl, R.drawable.record_stop_bg);
                    mPlayState = PLAY_ING;
                } else {
                    updateImageView(mControl, R.drawable.record_start_bg);
                    mPlayState = PLAY_PRAPARE;
                }
                break;
            case PLAY_ING:
                RecordManager.getInstance()
                             .pausePlay();
                mCountDownTimer.cancel();
                updateImageView(mControl, R.drawable.record_start_bg);
                mPlayState = PLAY_PAUSE;
                break;
            case PLAY_PAUSE:
                initCountDownTimer(mCountDownTime);
                RecordManager.getInstance()
                             .ContinuePlay();
                updateImageView(mControl, R.drawable.record_stop_bg);
                mCountDownTimer.start();
                mPlayState = PLAY_ING;
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MSessionWizard.getInstance(this).setPlayView(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        RecordManager.getInstance().stopPlay();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MSessionWizard.getInstance(this).setPlayView(false);
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(RecordPlayActivity.this, resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    private void initProgressBar(int volume) {
        if (volume <= mMaxVolume / 5) {
            mProgressBar.setProgress(20);
            updateImageView(mVoiceVolume, R.drawable.icon_record_volume_one);
        } else if (volume <= mMaxVolume * 2 / 5) {
            mProgressBar.setProgress(40);
            updateImageView(mVoiceVolume, R.drawable.icon_record_volume_two);
        } else if (volume <= mMaxVolume * 3 / 5) {
            mProgressBar.setProgress(60);
            updateImageView(mVoiceVolume, R.drawable.icon_record_volume_two);
        } else if (volume <= mMaxVolume * 4 / 5) {
            mProgressBar.setProgress(80);
            updateImageView(mVoiceVolume, R.drawable.icon_record_volume_two);
        } else {
            mProgressBar.setProgress(100);
            updateImageView(mVoiceVolume, R.drawable.icon_record_volume_three);
        }
    }

    private void initTimeView(long duration) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String str = df.format(duration);
        mTimeView.setText(str);
    }

    private void initCountDownTimer(long duration) {
        mCountDownTimer = new CountDownTimer(duration, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                initTimeView(millisUntilFinished);
                mCountDownTime = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                initTimeView(mRecordDuration);
                updateImageView(mControl, R.drawable.record_start_bg);
                mPlayState = PLAY_PRAPARE;
            }
        };
    }

    private void showMenu(boolean menuShowed) {
        if (menuShowed) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mLayoutAnimation, "translationY", 0, mVolumeControl.getHeight()).setDuration(300);
            animator.setInterpolator(new AccelerateInterpolator());
            animator.start();
            ObjectAnimator.ofFloat(mArrowUp, "rotation", 180, 0).setDuration(200).start();
            ObjectAnimator.ofObject(mVolumeControl, "backgroundColor", new ArgbEvaluator(),
                    0xff000000, 0xff171717).setDuration(300).start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
//                    mLayoutShadows.setVisibility(View.GONE);
                    mVolumeControl.setBackgroundColor(0xff171717);
                    bMenuShowed = !bMenuShowed;
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        } else {
//            mLayoutShadows.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mLayoutAnimation, "translationY", mVolumeControl.getHeight(), 0).setDuration(300);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
            ObjectAnimator.ofFloat(mArrowUp, "rotation", 0, 180).setDuration(200).start();
//            ObjectAnimator.ofFloat(mLayoutShadows, "alpha", 0, 1).setDuration(300).start();
            ObjectAnimator.ofObject(mVolumeControl, "backgroundColor", new ArgbEvaluator(),
                    0xff171717, 0xff000000).setDuration(300).start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
//                    mLayoutShadows.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    bMenuShowed = !bMenuShowed;
                    mVolumeControl.setBackgroundColor(0xff000000);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
    }
}
