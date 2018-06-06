package com.mstarc.music.wearablemusic.fragment;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kw.rxbus.RxBus;
import com.mstarc.music.ThemeUtils;
import com.mstarc.music.wearablemusic.MusicManager;
import com.mstarc.music.wearablemusic.R;
import com.mstarc.music.wearablemusic.data.MusicBus;
import com.mstarc.music.wearablemusic.view.MyRadioButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * A simple {@link Fragment} subclass.
 */
public class MusciFragment extends Fragment implements
        View.OnClickListener, MusicManager.Listener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener {

    private long mLastCmdTime;
    private static final int DEFAULT_COMMAND_GAP = 600;

    TextView mChronometer;
    LinearLayout mMusicLayout;
    LinearLayout mMusicControlLayout;
    LinearLayout mArrowUpLayout;
    ImageView mArrowUp;
    RadioGroup mRgUp;
    RadioGroup mRgDown;
    MyRadioButton mRadioBtShuffle;
    MyRadioButton mRadioBtOrder;
    MyRadioButton mRadioBtLoopAll;
    MyRadioButton mRadioBtLoopOne;
    ImageView mPlay;
    ImageView mPlayPre;
    ImageView mPlayNext;
    TextView mMusicName;
    SeekBar mSeekBarTime;
    ImageView mPlayTypeImg;
    TextView mPlayType;
    ProgressBar mProgressBar;
    private int mMaxVolume;

    private SimpleDateFormat mTimeFormat = new SimpleDateFormat("m:ss", Locale.US);

    private boolean bMenuShowed = false;
    public MusciFragment() {
        // Required empty public constructor
        m_eState = MP_STATES.MPS_IDLE;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_music, container, false);
        mChronometer = (TextView) view.findViewById(R.id.chronometer_music);
        mMusicLayout = (LinearLayout) view.findViewById(R.id.music_play_layout);
        mMusicControlLayout = (LinearLayout) view.findViewById(R.id.music_play_control_layout);
        mArrowUp = (ImageView) view.findViewById(R.id.arrow_up);
        mArrowUpLayout = (LinearLayout) view.findViewById(R.id.arrow_up_layout);
        mPlay = (ImageView) view.findViewById(R.id.play);
        mPlayNext = (ImageView) view.findViewById(R.id.play_next);
        mPlayPre = (ImageView) view.findViewById(R.id.play_before);
        mMusicName = (TextView) view.findViewById(R.id.music_name);
        mSeekBarTime = (SeekBar) view.findViewById(R.id.seek_bar);
        mPlayTypeImg = (ImageView) view.findViewById(R.id.music_play_type);
        mPlayType = (TextView) view.findViewById(R.id.music_play_type_text);

//        updateImageView(mPlay, R.drawable.music_start_bg);
        updateImageView(mPlayNext, R.drawable.music_next_bg);
        updateImageView(mPlayPre, R.drawable.music_before_bg);

        MusicManager.getInstance().setListener(this);
        mArrowUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(bMenuShowed);
            }
        });

        mRgUp = (RadioGroup) view.findViewById(R.id.rg_up);
        mRgDown = (RadioGroup) view.findViewById(R.id.rg_down);
        mRadioBtShuffle = (MyRadioButton) view.findViewById(R.id.play_shuffle);
        mRadioBtOrder = (MyRadioButton) view.findViewById(R.id.play_order);
        mRadioBtLoopAll = (MyRadioButton) view.findViewById(R.id.play_loop_all);
        mRadioBtLoopOne = (MyRadioButton) view.findViewById(R.id.play_loop_one);

        ImageView volumeDown = (ImageView) view.findViewById(R.id.volume_down);
        ImageView volumeUp = (ImageView) view.findViewById(R.id.volume_up);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        int volume = MusicManager.getInstance().getVolume(getContext());
        mMaxVolume = MusicManager.getInstance().getMaxVolume();
        initProgressBar(volume);

        updateImageView(volumeDown, R.drawable.icon_music_volume_decrease);
        updateImageView(volumeUp, R.drawable.icon_music_volume_increase);

        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicManager.getInstance().volumeDown(getContext());
                Log.d("dingyichen", "volume = " + MusicManager.getInstance().getVolume(getContext()));
                initProgressBar(MusicManager.getInstance().getVolume(getContext()));
            }
        });

        volumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicManager.getInstance().volumeUp(getContext());
                Log.d("dingyichen", "volume = " + MusicManager.getInstance().getVolume(getContext()));
                initProgressBar(MusicManager.getInstance().getVolume(getContext()));
            }
        });

        mRadioBtShuffle.setOnClickListener(this);
        mRadioBtOrder.setOnClickListener(this);
        mRadioBtLoopAll.setOnClickListener(this);
        mRadioBtLoopOne.setOnClickListener(this);
        mRadioBtShuffle.setClickable(true);
        mRadioBtOrder.setClickable(true);
        mRadioBtLoopAll.setClickable(true);
        mRadioBtLoopOne.setClickable(true);
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        mPlayPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPreMusic();
            }
        });

        mPlayNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_eState = MP_STATES.MPS_INITIALIZED;
                playNextMusic();
            }

        });

        RxBus.getInstance()
             .toObservable(MusicBus.class)
             .observeOn(AndroidSchedulers.mainThread())
             .subscribe(new Consumer<MusicBus>() {
                 @Override
                 public void accept(MusicBus musicBus) {
                     try {
                         if (musicBus.isStart()) {
                             if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_COMMAND_GAP) {
                                 return;
                             }

                             MusicManager.getInstance()
                                         .play();
                             updateImageView(mPlay, R.drawable.music_stop_bg);
                             m_eState = MP_STATES.MPS_STARTED;

                             mLastCmdTime = System.currentTimeMillis();
                             initListener();

                         } else if (musicBus.isPause()) {
                             if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_COMMAND_GAP) {
                                 return;
                             }

                             MusicManager.getInstance().pasue();
                             updateImageView(mPlay, R.drawable.music_start_bg);
                             m_eState = MP_STATES.MPS_PAUSED;

                             mLastCmdTime = System.currentTimeMillis();
                             initListener();

                         } else if (musicBus.isStartOrPause()) {
                             play();

                         } else if (musicBus.isNext()) {
                             playNextMusic();

                         } else if (musicBus.isPre()) {
                             playPreMusic();

                         } else {}
                     } catch (Exception e) {
                         Log.e("MusicSuhen", "accept: ", e);
                     }
                 }
             });

        //initView();
        return view;
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = view.getContext().getResources().getDrawable(resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    private void updateTextColor(final TextView view) {
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setTextColor(ThemeUtils.getCurrentPrimaryColor());
            }
        });
    }

    private void play() {
        if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_COMMAND_GAP) {
            return;
        }
        MusicManager.getInstance().playOrPause();
        if (MusicManager.getInstance().getPlayer().isPlaying()) {
            updateImageView(mPlay, R.drawable.music_stop_bg);
            m_eState = MP_STATES.MPS_STARTED;
        } else {
            updateImageView(mPlay, R.drawable.music_start_bg);
            m_eState = MP_STATES.MPS_PAUSED;
        }
        mLastCmdTime = System.currentTimeMillis();
        initListener();
    }

    private void playNextMusic() {
        MusicManager.getInstance().playNext();
        mPlay.post(new Runnable() {
            @Override
            public void run() {
                if (MusicManager.getInstance().getPlayer().isPlaying()) {
                    updateImageView(mPlay, R.drawable.music_stop_bg);
                    m_eState = MP_STATES.MPS_STARTED;
                } else {
                    updateImageView(mPlay, R.drawable.music_start_bg);
                    m_eState = MP_STATES.MPS_PAUSED;
                }
            }
        });
        initListener();
    }

    private void playPreMusic() {
        Log.d("dingyichen", "play previous!!");
        m_eState = MP_STATES.MPS_INITIALIZED;
        MusicManager.getInstance().playPrevious();
        if (MusicManager.getInstance().getPlayer().isPlaying()) {
            updateImageView(mPlay, R.drawable.music_stop_bg);
            m_eState = MP_STATES.MPS_STARTED;
        } else {
            updateImageView(mPlay, R.drawable.music_start_bg);
            m_eState = MP_STATES.MPS_PAUSED;
        }
        initListener();
    }

    private void initListener() {
//        MusicManager.getInstance().getPlayer().setOnCompletionListener(this);
        MusicManager.getInstance().getPlayer().setOnErrorListener(this);
        MusicManager.getInstance().getPlayer().setOnInfoListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (MusicManager.getInstance().getPlayer().isPlaying()) {
                initView();
                initListener();
            }
        } catch (IllegalStateException e) {

        }
    }

    private void initView() {
        Log.d("dingyichen", "initView " + MusicManager.getInstance().getPlayType());
        if (MusicManager.getInstance().getPlayer().isPlaying()) {
            updateImageView(mPlay, R.drawable.music_stop_bg);
            m_eState = MP_STATES.MPS_STARTED;
        } else {
            updateImageView(mPlay, R.drawable.music_start_bg);
            m_eState = MP_STATES.MPS_PAUSED;
        }
        mMusicName.setText(MusicManager.getInstance().getCurrentName());
        int color = ThemeUtils.getCurrentPrimaryColor();
        switch (MusicManager.getInstance().getPlayType()) {
            case PLAY_TYPE_SHUFFLE:
                mPlayType.setText(R.string.music_play_shuffle);
                mPlayType.setTextColor(color);
                updateImageView(mPlayTypeImg, R.drawable.icon_music_play_shuffle_selected);
                mRadioBtShuffle.setChecked(true);
                break;
            case PLAY_TYPE_ORDER:
                mPlayType.setText(R.string.music_play_order);
                mPlayType.setTextColor(color);
                updateImageView(mPlayTypeImg, R.drawable.icon_music_play_order_selected);
                mRadioBtOrder.setChecked(true);
                break;
            case PLAY_TYPE_LOOP_ALL:
                mPlayType.setText(R.string.music_play_loop_all);
                mPlayType.setTextColor(color);
                updateImageView(mPlayTypeImg, R.drawable.icon_music_play_loop_all_selected);
                mRadioBtLoopAll.setChecked(true);
                break;
            case PLAY_TYPE_LOOP_ONE:
                mPlayType.setText(R.string.music_play_loop_one);
                mPlayType.setTextColor(color);
                updateImageView(mPlayTypeImg, R.drawable.icon_music_play_loop_one_selected);
                mRadioBtLoopOne.setChecked(true);
                break;
        }
        updateTextColor(mPlayType);
        if (MusicManager.getInstance().getMusicList().size() == 0) {
            mMusicName.setText(R.string.demo_name);
            mChronometer.setText(R.string.demo_time);
            mSeekBarTime.setProgress(0);
            mSeekBarTime.setEnabled(false);
        } else {
            mSeekBarTime.setEnabled(true);
            handler.postDelayed(runnable, 100);
        }
    }

    private void initProgressBar(int volume) {
        if (volume <= mMaxVolume / 5) {
            mProgressBar.setProgress(20);
        } else if (volume <= mMaxVolume * 2 / 5) {
            mProgressBar.setProgress(40);
        } else if (volume <= mMaxVolume * 3 / 5) {
            mProgressBar.setProgress(60);
        } else if (volume <= mMaxVolume * 4 / 5) {
            mProgressBar.setProgress(80);
        } else {
            mProgressBar.setProgress(100);
        }
    }

    public android.os.Handler handler = new android.os.Handler();
    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (m_eState == MP_STATES.MPS_PREPARED
                    || m_eState == MP_STATES.MPS_STARTED) {
                mSeekBarTime.setProgress(MusicManager.getInstance().getCurrentPosition());
                mSeekBarTime.setMax(MusicManager.getInstance().getDuration());
                mChronometer.setText(mTimeFormat.format(MusicManager.getInstance().getCurrentPosition()));
                mSeekBarTime.setProgress(MusicManager.getInstance().getCurrentPosition());
            }
            mSeekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        Log.d("dingyichen", "max = " + seekBar.getMax() + ", current = " + seekBar.getProgress());
                        MusicManager.getInstance().seekTo(seekBar.getProgress());
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            handler.postDelayed(runnable, 100);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_shuffle:
                Log.d("dingyichen", "onClick!!!");
                setChecked(mRadioBtShuffle);
                mRgDown.clearCheck();
                MusicManager.getInstance().setPlayType(MusicManager.PLAY_TYPE.values()[0]);
                break;
            case R.id.play_order:
                setChecked(mRadioBtOrder);
                mRgDown.clearCheck();
                MusicManager.getInstance().setPlayType(MusicManager.PLAY_TYPE.values()[1]);
                break;
            case R.id.play_loop_all:
                setChecked(mRadioBtLoopAll);
                mRgUp.clearCheck();
                MusicManager.getInstance().setPlayType(MusicManager.PLAY_TYPE.values()[2]);
                break;
            case R.id.play_loop_one:
                setChecked(mRadioBtLoopOne);
                mRgUp.clearCheck();
                MusicManager.getInstance().setPlayType(MusicManager.PLAY_TYPE.values()[3]);
                break;
        }
        initView();
    }

    private void setChecked(MyRadioButton button) {
        mRadioBtShuffle.setChecked(false);
        mRadioBtOrder.setChecked(false);
        mRadioBtLoopAll.setChecked(false);
        mRadioBtLoopOne.setChecked(false);
        button.setChecked(true);
    }

    private void showMenu(boolean menuShowed) {
        if (menuShowed) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mMusicControlLayout, "translationY", 0, mMusicControlLayout.getHeight()).setDuration(300);
            animator.setInterpolator(new AccelerateInterpolator());
            animator.start();
            ObjectAnimator.ofFloat(mArrowUp, "rotation", 180, 0).setDuration(200).start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
//                    mLayoutShadows.setVisibility(View.GONE);
                    mMusicControlLayout.setVisibility(View.VISIBLE);
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
            mMusicControlLayout.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mMusicControlLayout, "translationY", mMusicControlLayout.getHeight(), 0).setDuration(300);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
            ObjectAnimator.ofFloat(mArrowUp, "rotation", 0, 180).setDuration(200).start();
//            ObjectAnimator.ofFloat(mLayoutShadows, "alpha", 0, 1).setDuration(300).start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
//                    mLayoutShadows.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    bMenuShowed = !bMenuShowed;
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

    @Override
    public void onDataChanged() {
        try {
            initView();
        } catch (IllegalStateException e) {

        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d("dingyichen", "onError !!!!");
        m_eState = MP_STATES.MPS_IDLE;
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

//    @Override
//    public void onPrepared(MediaPlayer mp) {
//        Log.d("dingyichen", "onPrepared !!!! current postion = " + MusicManager.getInstance().getCurrentPosition()
//        + " , duration = " + MusicManager.getInstance().getCurrentPosition());
//        mSeekBarTime.setProgress(MusicManager.getInstance().getCurrentPosition());
//        mSeekBarTime.setMax(MusicManager.getInstance().getDuration());
//        if (m_eState == MP_STATES.MPS_INITIALIZED) {
//            MusicManager.getInstance().getPlayer().start();
//            m_eState = MP_STATES.MPS_STARTED;
//        } else {
//            m_eState = MP_STATES.MPS_PREPARED;
//        }
////        if (MusicManager.getInstance().getPlayer() != null) {
////
////            m_eState = MP_STATES.MPS_STARTED;
////        }
//
//    }

    static enum MP_STATES
    {
        MPS_IDLE,
        MPS_INITIALIZED,
        MPS_PREPARING,
        MPS_PREPARED,
        MPS_STARTED,
        MPS_STOPPED,
        MPS_PAUSED,
        MPS_PLAYBACK_COMPLETED,
        MPS_ERROR,
        MPS_END,
    }
    private MP_STATES m_eState;
}
