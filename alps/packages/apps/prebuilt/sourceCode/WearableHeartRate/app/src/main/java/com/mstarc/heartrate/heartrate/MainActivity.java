package com.mstarc.heartrate.heartrate;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mstarc.watchbase.sensor.heartrate.HeartRate;
import com.mstarc.watchbase.sensor.heartrate.HeartRateData;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends Activity implements HeartRate.ISensorDataListener<HeartRateData> {

    private ImageView mHeartImg;
    //private ImageView mWaveImg;
    private TextView mHeartCount;
    private TextView mHeartCountEven;
    private TextView mHeartCountMax;
    private RelativeLayout mRateCountLayout;
    private AnimationDrawable mAnimationHeart;
    NotificationManager mNotificationManager;
    //private AnimationDrawable mAnimationWave;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int NOTIFICATION_ID = 11;
    //RingView mringView;
    GifImageView mGifView;

    boolean hasResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "gln onCreate-----------");
        setContentView(R.layout.activity_main);
        mHeartImg = (ImageView) findViewById(R.id.heart_rate_heart_img);
        //mWaveImg = (ImageView) findViewById(R.id.heart_rate_wave);
        //mringView =(RingView) findViewById(R.id.heart_rate_wave);
        mGifView = (GifImageView) findViewById(R.id.heart_rate_wave);

        mHeartCount = (TextView) findViewById(R.id.heart_rate_count_num);
        mHeartCountEven = (TextView) findViewById(R.id.heart_rate_count_even);
        mHeartCountMax = (TextView) findViewById(R.id.heart_rate_count_max);
        mRateCountLayout = (RelativeLayout) findViewById(R.id.heart_rate_count_layout);

        try {
            // 如果加载的是gif动图，第一步需要先将gif动图资源转化为GifDrawable
            // 将gif图资源转化为GifDrawable
            GifDrawable gifDrawable = new GifDrawable(getResources(), R.drawable.heart_rate_animation);

            // gif1加载一个动态图gif
            mGifView.setImageDrawable(gifDrawable);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initView();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "gln 心率添加常亮-----------");
        mNotificationManager = ((NotificationManager) this.getSystemService(NOTIFICATION_SERVICE));

        mHandler.sendEmptyMessageDelayed(MSG_SHOW, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initSensor();
    }

    @Override
    protected void onStop() {
        super.onStop();
        int onwrist = Settings.System.getInt(getContentResolver(), "onwrist", 0);
        Log.d(TAG, "执行timer" + onwrist);
        if (onwrist == 0) {
            mHandler.sendEmptyMessage(MSG_SHOW);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    final int MSG_SHOW = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW:
                    int onwrist = Settings.System.getInt(getContentResolver(), "onwrist", 1);
                    Log.d(TAG, "执行Handler" + onwrist);
                    if (onwrist == 0) {
                        if (!isFinishing()) {
                            showDialog();
                        }
                    }
                    break;
            }
        }
    };

    private void showDialog() {
        final Dialog dialog = new Dialog(this, R.style.tip_dialog);//指定自定義樣式
        //1. 先获取布局的view
        RelativeLayout view = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.switch_dialog, null);
        //2. 加载 view
        dialog.setContentView(view);//指定自定義layout
        dialog.setCanceledOnTouchOutside(false);
        //3. 获取dialog view 下的控件
        ImageView ok = (ImageView) view.findViewById(R.id.btn_Y);
        //4.对控件做设置或者设置listenner
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.sendEmptyMessageDelayed(MSG_SHOW, 2000);
                dialog.dismiss();
                if (hasResult) {
                    HeartRate.getInstance().close();
                    initView();
                    initSensor();
                    hasResult = false;
                }
            }
        });
        //  5. 直接
        dialog.show();
    }

    private void rmNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    void sendNotification() {
        rmNotification();
        PendingIntent localPendingIntent = PendingIntent.getActivity(this,
                0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification_hr)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(this.getText(R.string.hr_notification_content))
                .setContentText(this.getText(R.string.hr_notification_content))
                .setContentIntent(localPendingIntent)
                .setAutoCancel(false);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "gln 心率关闭常亮-----------");
        sendNotification();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HeartRate.getInstance().close();
        rmNotification();

        Log.d(TAG, "gln onDestroy-----------");
    }

    private void initSensor() {
        HeartRate.getInstance().init(this, this);
        HeartRate.getInstance().open();
        beginCheck();
    }

    //初始化
    private void initView() {
        mHeartCount.setVisibility(View.INVISIBLE);
        mRateCountLayout.setVisibility(View.INVISIBLE);
        mHeartImg.setVisibility(View.VISIBLE);
        mGifView.setVisibility(View.VISIBLE);
    }

    //监测开始
    private void beginCheck() {
        mHeartImg.setVisibility(View.VISIBLE);
        mGifView.setVisibility(View.VISIBLE);
        mHeartCount.setVisibility(View.VISIBLE);
        mHeartCount.setText("心率测量中");
        mHeartCount.setTextSize(20);
        mRateCountLayout.setVisibility(View.INVISIBLE);
        mAnimationHeart = (AnimationDrawable) mHeartImg.getBackground();// 获取到动画资源
        mAnimationHeart.setOneShot(false); // 设置是否重复播放
        mAnimationHeart.start();// 开始动画
    }


    @Override
    public void onSensorDataReceived(HeartRateData data) {
        hasResult = true;
        mAnimationHeart.stop();
        mGifView.setVisibility(View.INVISIBLE);
        mHeartCount.setVisibility(View.VISIBLE);
        mRateCountLayout.setVisibility(View.VISIBLE);
        mHeartCountMax.setText(String.valueOf(data.getMax()));
        mHeartCountEven.setText(String.valueOf(data.getAverage()));
        mHeartCount.setText(String.valueOf(data.getHeartrate()));
        mHeartCount.setTextSize(62);
    }
}
