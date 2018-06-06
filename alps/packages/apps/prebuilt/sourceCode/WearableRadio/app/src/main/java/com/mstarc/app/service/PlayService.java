package com.mstarc.app.service;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.alibaba.fastjson.JSON;
import com.mstarc.app.Tools.SharedTool;
import com.mstarc.app.Tools.Tools;
import com.mstarc.app.radio.PlayInfo;
import com.mstarc.app.radio.R;
import com.mstarc.app.radio.Radio;
import com.mstarc.app.radio.RadioActivity;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class PlayService extends Service {
    public static VideoView mVideoView;
    private MediaController mediaController;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams wmParams;
    private LinearLayout windowLayout;
    private final String RADIO_PLAY_URL = "RADIO_URL";
    private String playUrl;
    private AudioManager mAudioManager;
    private static PlayStateListener mStateListener;

    public PlayService() {
    }

    public static void setPlayStateListener(PlayStateListener playStateListener) {
        mStateListener = playStateListener;
    }

    public interface PlayStateListener {
        void onStop();

        void onStart();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return super.onStartCommand(intent, flags, startId);
        playUrl = intent.getStringExtra(RADIO_PLAY_URL);
        //Log.e("resultttt", "onStartCommand---------------" + playUrl);
        startNewRadio(playUrl);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mVideoView == null)
            initWindowManager();
    }

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                mAudioManager.abandonAudioFocus(afChangeListener);
                mVideoView.pause();
                if (mStateListener != null) {
                    mStateListener.onStop();
                }
                // Pause playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                mAudioManager.abandonAudioFocus(afChangeListener);
                mVideoView.stopPlayback();
                if (mStateListener != null) {
                    mStateListener.onStop();
                }
                // Stop playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback or Raise it back to normal
                mAudioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                mVideoView.resume();
                if (mStateListener != null) {
                    mStateListener.onStart();
                }
            }
        }
    };

    private void initWindowManager() {
        // 获取的是LocalWindowManager对象
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        // 获取LayoutParams对象
        wmParams = new WindowManager.LayoutParams();

        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = 1;
        wmParams.height = 1;
        // wmParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        // wmParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        windowLayout = (LinearLayout) inflater.inflate(R.layout.player_audio_service, null);
        windowLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                wmParams.x = (int) event.getRawX() - windowLayout.getWidth() / 2;
                // 25为状态栏高度
                wmParams.y = (int) event.getRawY() - windowLayout.getHeight() / 2 - 40;
                mWindowManager.updateViewLayout(windowLayout, wmParams);
                return false;
            }
        });
        mVideoView = (VideoView) windowLayout.findViewById(R.id.vv);
        mWindowManager.addView(windowLayout, wmParams);
        mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    void startNewRadio(String path) {
        mVideoView.setVideoURI(Uri.parse(path));
        // mVideoView.setVideoPath(path);
        if (mediaController == null) {
            mediaController = new MediaController(this);
            mediaController.setMediaPlayer(mVideoView);
            mVideoView.setMediaController(mediaController);
        }
        mVideoView.requestFocus();

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // optional need Vitamio 4.0
                mediaPlayer.setPlaybackSpeed(1.0f);
                Log.d("PlayService", "setOnPreparedListener");
            }
        });
        //Log.e("resulttt", "--------------" + mVideoView.getCurrentPosition());
        mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:// 缓存完成，继续播放

                        break;

                }
                return true;
            }

        });

        mVideoView.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (percent > 2) {
                    return;
                }
            }
        });

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                RadioActivity.currentRadioIndex += RadioActivity.isRight ? 1 : -1;
                playNewRadio(RadioActivity.mRadioList.get(RadioActivity.currentRadioIndex));
                return false;
            }
        });
        mVideoView.start();
        if (mStateListener != null) {
            mStateListener.onStart();
        }

    }

    private void playNewRadio(Radio radio) {
        if (RadioActivity.radioNameTv != null) {
            RadioActivity.radioNameTv.setText(radio.getTitle());
        }
        String imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        String mediaUrl = "https://ls.qingting.fm/live/" + radio.getId() + ".m3u8?bitrate=0&deviceid=" + imei;
        startNewRadio(mediaUrl);
    }

    public static int lastCallState = TelephonyManager.CALL_STATE_IDLE;


    public static class PhoneStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        connectionChange(context);
                    }
                }, 1500);

                return;
            }
            //android.intent.action.PHONE_STATE
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int currentCallState = telephonyManager.getCallState();
            String gb_callstate_action = "android.intent.action.LWQCALL_STATE_RECIVER";
            boolean jietong = intent.getAction().equals(gb_callstate_action);
            boolean laidian = (lastCallState == TelephonyManager.CALL_STATE_IDLE) && (currentCallState == TelephonyManager.CALL_STATE_RINGING);
            boolean qudian = intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL);
            if (jietong || laidian || qudian)// 来电，暂停
            {
                if (mVideoView != null && mVideoView.isPlaying()) {
                    mVideoView.pause();
                    if (mVideoView.isPlaying()) {
                        mVideoView.pause();
                    }
                    if (mStateListener != null) {
                        mStateListener.onStop();
                    }
                }
            }
            if ((lastCallState == TelephonyManager.CALL_STATE_RINGING || lastCallState == TelephonyManager.CALL_STATE_OFFHOOK) && currentCallState == TelephonyManager.CALL_STATE_IDLE) {
                // 播放按钮的状态
                if (mVideoView != null && SharedTool.getInstance().getPlayState(context)) {
                    mVideoView.start();
                    if (mStateListener != null) {
                        mStateListener.onStart();
                    }
                }
            }
            lastCallState = currentCallState;
        }

        /**
         * 接口回调 修改按钮状态
         *
         * @param context
         */
        private void connectionChange(Context context) {

            if (!Tools.isNetworkAvailable()) {
                if (RadioActivity.radioNameTv != null) {
                    RadioActivity.radioNameTv.setText(R.string.network_anomaly);
                }

                if (mVideoView != null)// 播放按钮的状态==>暂停
                {
                    mVideoView.pause();
                    if (mStateListener != null) {
                        mStateListener.onStop();
                    }
                }
            } else {
                // 连上网
                String radioName = "";
                if (RadioActivity.radioNameTv != null) {
                    radioName = RadioActivity.radioNameTv.getText().toString();
                }
                if (context.getResources().getString(R.string.network_anomaly).equals(radioName)) {
                    // 发广播--重新网络请求
                    ((Activity) RadioActivity.mContext).finish();
                    if (!Tools.isRunningForeground(RadioActivity.mContext)) {
                        return;
                    }
                    context.startActivity(new Intent(context, RadioActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("temp", -1));
                }
                // 播放中断-连，不作处理，处暂停状态
            }
        }
    }

    @Override
    public void onDestroy() {

        if (SharedTool.getInstance().getPlayState(this)) {
            String info = SharedTool.getInstance().getSharedPlayInfo(this);
            if (!"".equals(info)) {
                PlayInfo currPlayInfo = JSON.parseObject(info, PlayInfo.class);
                Intent serviceIntent = new Intent(this, PlayService.class);
                serviceIntent.putExtra(RADIO_PLAY_URL, currPlayInfo.getUrl());
                startService(serviceIntent);
            }
        }

    }
}
