package com.mstarc.fte.wearablefte;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mstarc.fte.wearablefte.view.SlideAnimationView;

public class MainActivity extends Activity implements SlideAnimationView.onLayout {

    SlideAnimationView animationView;
    LinearLayout mWelcomLayout;
    ImageView mLaunchDemoImgLayout;
    FrameLayout mSlideLayout;
    FrameLayout mRotatelayout;
    ImageView mRotateImg;

    RelativeLayout mTextType1Layout;
    RelativeLayout mTextType2Layout;
    RelativeLayout mTextType3Layout;

    TextView mTextType1Up;
    TextView mTextType1Down;
    TextView mTextType2Up;
    TextView mTextType2Down;
    TextView mTextType3Up;
    TextView mTextType3Down;

    Button mBtnSkip;
    Button mBtnIn;

    //手指按下的点为(x1, y1)手指离开屏幕的点为(x2, y2)
    float x1 = 0;
    float x2 = 0;
    float y1 = 0;
    float y2 = 0;

    ObjectAnimator mLeftAnimator;
    private ObjectAnimator mRotateAnimator;
    private static final int KEEP_SCREEN_ON_INTERVAL = 25 * 1000;

    PowerManager.WakeLock mWakeLock;
    private static final String MSTARC_IN_FTE = "mstarc_fte_setting";
    private static final String SHOW_WATCH_FACE = "watchface_selector_firsttime_show";

    private static int[] mSlideAnimationDirctions = {
            SlideAnimationView.SLIDE_DIRECTION_LEFT, SlideAnimationView.SLIDE_DIRECTION_DOWN,
            SlideAnimationView.SLIDE_DIRECTION_LEFT, SlideAnimationView.SLIDE_DIRECTION_LEFT,
            SlideAnimationView.SLIDE_DIRECTION_UP, SlideAnimationView.SLIDE_DIRECTION_RIGHT,
            SlideAnimationView.SLIDE_DIRECTION_LEFT,SlideAnimationView.SLIDE_DIRECTION_UP,
            SlideAnimationView.SLIDE_DIRECTION_DOWN
    };

    private static int[] mLauncherDemoResIds = {
            R.drawable.icon_watch_face, R.drawable.icon_baterry,
            R.drawable.icon_mode_switch, R.drawable.icon_weather,
            R.drawable.icon_watch_face, R.drawable.icon_alipay,
            R.drawable.icon_watch_face, R.drawable.icon_notification,
            R.drawable.icon_watch_face
    };
    private int mCurrentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        animationView = (SlideAnimationView) findViewById(R.id.slide_animatin);
        animationView.addListener(this);
        mWelcomLayout = (LinearLayout) findViewById(R.id.welcome_layout);
        mLaunchDemoImgLayout = (ImageView) findViewById(R.id.launch_demo_img);
        mSlideLayout = (FrameLayout) findViewById(R.id.slide_layout);

        mTextType1Layout = (RelativeLayout) findViewById(R.id.text_type_1);
        mTextType2Layout = (RelativeLayout) findViewById(R.id.text_type_2);
        mTextType3Layout = (RelativeLayout) findViewById(R.id.text_type_3);

        mTextType1Up = (TextView) findViewById(R.id.text_type_1_up);
        mTextType1Down = (TextView) findViewById(R.id.text_type_1_down);
        mTextType2Up = (TextView) findViewById(R.id.text_type_2_up);
        mTextType2Down = (TextView) findViewById(R.id.text_type_2_down);
        mTextType3Up = (TextView) findViewById(R.id.text_type_3_up);
        mTextType3Down = (TextView) findViewById(R.id.text_type_3_down);
        mBtnSkip = (Button) findViewById(R.id.btn_skip);
        mBtnIn = (Button) findViewById(R.id.btn_in);
        mRotatelayout = (FrameLayout) findViewById(R.id.rotate_layout);
        mRotateImg = (ImageView) findViewById(R.id.rotate_img);
        mRotatelayout.setVisibility(View.GONE);
        mRotatelayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Settings.System.putInt(getContentResolver(), MSTARC_IN_FTE, 0);
                startWatchFaceApplication();
                finish();
                return false;
            }
        });
        mBtnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings.System.putInt(getContentResolver(), MSTARC_IN_FTE, 0);
                finish();
            }
        });
        mBtnIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentStep = 1;
                mWelcomLayout.setVisibility(View.GONE);
                updateView(mCurrentStep);

                Settings.System.putInt(getContentResolver(), MSTARC_IN_FTE, 1);
            }
        });
        switch (ThemeUtils.getCurrentProduct()) {
            case ThemeUtils.PRODUCT_COLOR_G6:
                mBtnIn.setTextColor(getResources().getColorStateList(R.color.skin_color_button_g6_golden));
                mBtnSkip.setTextColor(getResources().getColorStateList(R.color.skin_color_button_g6_golden));
                break;
            case ThemeUtils.PRODUCT_COLOR_APPLE_GREEN:
                mBtnIn.setTextColor(getResources().getColorStateList(R.color.skin_color_button_g7_apple));
                mBtnSkip.setTextColor(getResources().getColorStateList(R.color.skin_color_button_g7_apple));
                break;
            case ThemeUtils.PRODUCT_COLOR_ROSE_GOLDEN:
                mBtnIn.setTextColor(getResources().getColorStateList(R.color.skin_color_button_g7_rose));
                mBtnSkip.setTextColor(getResources().getColorStateList(R.color.skin_color_button_g7_rose));
                break;
            case ThemeUtils.PRODUCT_COLOR_HIGH_BLACK:
                mBtnIn.setTextColor(getResources().getColorStateList(R.color.skin_color_button_g7_black));
                mBtnSkip.setTextColor(getResources().getColorStateList(R.color.skin_color_button_g7_black));
                break;
        }
        //initAnimator();
    }

    private void startWatchFaceApplication() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(SHOW_WATCH_FACE, true);
        ComponentName cn = new ComponentName("com.mstarc.wearablelauncher",
                "com.mstarc.wearablelauncher.view.clock.WatchFaceSelectActivity");
        intent.setComponent(cn);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
        mWakeLock.acquire();
        myHander.sendEmptyMessage(MSG_KEEP_SCEEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWakeLock.release();
        myHander.removeMessages(MSG_KEEP_SCEEN_ON);
    }

    private void initAnimator() {
        mLeftAnimator= ObjectAnimator.ofFloat(mLaunchDemoImgLayout, "translationX",
                mLaunchDemoImgLayout.getWidth(), 0);
        mLeftAnimator.setInterpolator(new LinearInterpolator());
        mLeftAnimator.setDuration(1500);
        mLeftAnimator.setRepeatMode(ValueAnimator.RESTART);
        mLeftAnimator.setRepeatCount(1);
        mLeftAnimator.start();
    }
    private void initLeftAnimation() {
        animationView.initImgView(R.drawable.icon_slide_left_bg, R.drawable.icon_slide_left);
        animationView.initDirection(SlideAnimationView.SLIDE_DIRECTION_LEFT);
    }
    private void initRightAnimation() {
        animationView.initImgView(R.drawable.icon_slide_right_bg, R.drawable.icon_slide_right);
        animationView.initDirection(SlideAnimationView.SLIDE_DIRECTION_RIGHT);
    }
    private void initUpAnimation() {
        animationView.initImgView(R.drawable.icon_slide_up_bg, R.drawable.icon_slide_up);
        animationView.initDirection(SlideAnimationView.SLIDE_DIRECTION_UP);
    }
    private void initDownAnimation() {
        animationView.initImgView(R.drawable.icon_slide_down_bg, R.drawable.icon_slide_down);
        animationView.initDirection(SlideAnimationView.SLIDE_DIRECTION_DOWN);
    }


    @Override
    public void onlayout() {
        animationView.initAnimationTranslation();
        animationView.startAnimation();
    }

    private void updateView(int step) {
        if (step > 9) {
            return;
        }
        mLaunchDemoImgLayout.setVisibility(View.INVISIBLE);
        mLaunchDemoImgLayout.setImageResource(mLauncherDemoResIds[step - 1]);
        updateSlideView(step - 1);
        updateSlideTextView(step);
    }

    private void updateRotateView() {
        mSlideLayout.setVisibility(View.GONE);
        mLaunchDemoImgLayout.setVisibility(View.INVISIBLE);
        mRotatelayout.setVisibility(View.VISIBLE);
        mRotateAnimator = ObjectAnimator.ofFloat(mRotateImg, "rotation", 0, 359);
        mRotateAnimator.setInterpolator(new LinearInterpolator());
        mRotateAnimator.setDuration(1500);
        mRotateAnimator.setRepeatMode(ValueAnimator.RESTART);
        mRotateAnimator.setRepeatCount(-1);
        mRotateAnimator.start();
    }

    private void updateSlideView(int step) {
        mSlideLayout.setVisibility(View.VISIBLE);
        switch (mSlideAnimationDirctions[step]) {
            case SlideAnimationView.SLIDE_DIRECTION_LEFT:
                initLeftAnimation();
                break;
            case SlideAnimationView.SLIDE_DIRECTION_RIGHT:
                initRightAnimation();
                break;
            case SlideAnimationView.SLIDE_DIRECTION_UP:
                initUpAnimation();
                break;
            case SlideAnimationView.SLIDE_DIRECTION_DOWN:
                initDownAnimation();
                break;
        }
    }

    private void updateSlideTextView(int step) {
        switch (step) {
            case 1:
                mTextType1Layout.setVisibility(View.VISIBLE);
                mTextType2Layout.setVisibility(View.GONE);
                mTextType3Layout.setVisibility(View.GONE);
                mTextType1Down.setVisibility(View.GONE);
                mTextType1Up.setText(R.string.slide_left_first);
                break;
            case 2:
                mTextType1Layout.setVisibility(View.GONE);
                mTextType2Layout.setVisibility(View.VISIBLE);
                mTextType3Layout.setVisibility(View.GONE);
                mTextType2Up.setText(R.string.slide_down_fist);
                mTextType2Down.setText(R.string.slide_down_first_info);
                break;
            case 3:
                mTextType1Layout.setVisibility(View.VISIBLE);
                mTextType2Layout.setVisibility(View.GONE);
                mTextType3Layout.setVisibility(View.GONE);
                mTextType1Up.setText(R.string.slide_left);
                mTextType1Down.setVisibility(View.VISIBLE);
                mTextType1Down.setText(R.string.slide_left_info_mode);
                break;
            case 4:
                mTextType1Layout.setVisibility(View.VISIBLE);
                mTextType2Layout.setVisibility(View.GONE);
                mTextType3Layout.setVisibility(View.GONE);
                mTextType1Down.setVisibility(View.VISIBLE);
                mTextType1Up.setText(R.string.slide_left);
                mTextType1Down.setText(R.string.slide_left_info_weather);
                break;
            case 5:
                mTextType1Layout.setVisibility(View.GONE);
                mTextType2Layout.setVisibility(View.GONE);
                mTextType3Layout.setVisibility(View.VISIBLE);
                mTextType3Up.setText(R.string.slide_up);
                mTextType3Down.setText(R.string.slide_info_back_face);
                break;
            case 6:
                mTextType1Layout.setVisibility(View.VISIBLE);
                mTextType2Layout.setVisibility(View.GONE);
                mTextType3Layout.setVisibility(View.GONE);
                mTextType1Down.setVisibility(View.VISIBLE);
                mTextType1Up.setText(R.string.slide_right);
                mTextType1Down.setText(R.string.slide_right_info);
                break;
            case 7:
                mTextType1Layout.setVisibility(View.VISIBLE);
                mTextType2Layout.setVisibility(View.GONE);
                mTextType3Layout.setVisibility(View.GONE);
                mTextType1Down.setVisibility(View.VISIBLE);
                mTextType1Up.setText(R.string.slide_left);
                mTextType1Down.setText(R.string.slide_info_back_face);
                break;
            case 8:
                mTextType1Layout.setVisibility(View.GONE);
                mTextType2Layout.setVisibility(View.GONE);
                mTextType3Layout.setVisibility(View.VISIBLE);
                mTextType3Up.setText(R.string.slide_up);
                mTextType3Down.setText(R.string.slide_up_info_notification);
                break;
            case 9:
                mTextType1Layout.setVisibility(View.GONE);
                mTextType2Layout.setVisibility(View.VISIBLE);
                mTextType3Layout.setVisibility(View.GONE);
                mTextType2Up.setText(R.string.slide_down);
                mTextType2Down.setText(R.string.slide_info_back_face);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //继承了Activity的onTouchEvent方法，直接监听点击事件
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            //当手指按下的时候
            x1 = event.getX();
            y1 = event.getY();
        }
        if(event.getAction() == MotionEvent.ACTION_UP) {
            //当手指离开的时候
            x2 = event.getX();
            y2 = event.getY();
            if(y1 - y2 > 50) { // up
                if (mCurrentStep == 5
                        || mCurrentStep == 8) {
                    animationView.stopAnimation();
                    mSlideLayout.setVisibility(View.GONE);
                    mLaunchDemoImgLayout.setVisibility(View.VISIBLE);
                    myHander.sendEmptyMessageDelayed(MSG_SHOW_NEXT_SLIDE_VIEW, 2000);
                }
            } else if(y2 - y1 > 50) { //down
                if (mCurrentStep == 2
                        || mCurrentStep == 9) {
                    animationView.stopAnimation();
                    mSlideLayout.setVisibility(View.GONE);
                    mLaunchDemoImgLayout.setVisibility(View.VISIBLE);
                    myHander.sendEmptyMessageDelayed(MSG_SHOW_NEXT_SLIDE_VIEW, 2000);
                }
            } else if(x1 - x2 > 50) { // left
                if (mCurrentStep == 1
                        || mCurrentStep == 3
                        || mCurrentStep == 4
                        || mCurrentStep == 7) {
                    animationView.stopAnimation();
                    mSlideLayout.setVisibility(View.GONE);
                    mLaunchDemoImgLayout.setVisibility(View.VISIBLE);
                    //mLeftAnimator.start();
                    myHander.sendEmptyMessageDelayed(MSG_SHOW_NEXT_SLIDE_VIEW, 2500);
                }
            } else if(x2 - x1 > 50) { // right
                if (mCurrentStep == 6 ){
                    animationView.stopAnimation();
                    mSlideLayout.setVisibility(View.GONE);
                    mLaunchDemoImgLayout.setVisibility(View.VISIBLE);
                    myHander.sendEmptyMessageDelayed(MSG_SHOW_NEXT_SLIDE_VIEW, 2000);
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private static final int MSG_SHOW_NEXT_SLIDE_VIEW = 0;
    private static final int MSG_SHOW_NEXT_ROTATE_VIEW = 1;
    private static final int MSG_KEEP_SCEEN_ON = 2;
    private Handler myHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SHOW_NEXT_SLIDE_VIEW:
                    mCurrentStep++;
                    updateView(mCurrentStep);
                    if (mCurrentStep == 9) {
                        sendEmptyMessageDelayed(MSG_SHOW_NEXT_ROTATE_VIEW, 2000);
                    }
                    break;
                case MSG_SHOW_NEXT_ROTATE_VIEW:
                    updateRotateView();
                    break;
                case MSG_KEEP_SCEEN_ON:
                    Settings.System.putLong(getContentResolver(), "navigation", System.currentTimeMillis());
                    sendEmptyMessageDelayed(MSG_KEEP_SCEEN_ON, KEEP_SCREEN_ON_INTERVAL);
                    break;
            }
        }
    };
}
