package com.mstarc.fte.wearablefte.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.mstarc.fte.wearablefte.R;

/**
 * description
 * <p/>
 * Created by andyding on 2017/8/9.
 */

public class SlideAnimationView extends FrameLayout {
    private final static String TAG = SlideAnimationView.class.getSimpleName();
    private boolean mDebug = false;
    ImageView mImgBg;
    ImageView mImgSlide;

    //    TranslateAnimation mTranslateAnimation;

    AnimatorSet mAnimatorSet;
    private onLayout mListener = null;

    public final static int SLIDE_DIRECTION_LEFT = 0;
    public final static int SLIDE_DIRECTION_RIGHT = 1;
    public final static int SLIDE_DIRECTION_UP = 2;
    public final static int SLIDE_DIRECTION_DOWN = 3;

    private int mCurrentDirection = SLIDE_DIRECTION_LEFT;

    public SlideAnimationView(Context context) {
        super(context);
        initView(context);
    }

    public SlideAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public SlideAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    protected void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_slide_animation_layout,
                this, true);
        mImgBg = (ImageView) findViewById(R.id.img_animation_bg);
        mImgSlide = (ImageView) findViewById(R.id.img_animation_slide);
        mImgSlide.setVisibility(INVISIBLE);
        mAnimatorSet = new AnimatorSet();
    }

    public void initImgView(final int bgResId, final int slideResId) {
        mImgSlide.setVisibility(INVISIBLE);
        mImgBg.setImageResource(bgResId);
        mImgSlide.setImageResource(slideResId);
        ViewTreeObserver vto = mImgBg.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mDebug) {
                    Log.d(TAG, "onGlobalLayout onLayoutleft = " + mImgBg.getLeft() + " , right = " + mImgBg.getRight()
                            + " , down = " + mImgBg.getBottom() + ", up = " + mImgBg.getTop());
                }
                mListener.onlayout();
                getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this);
            }
        });

    }

    public void initDirection(int direction) {
        mCurrentDirection = direction;
    }

    public void addListener(onLayout layout) {
        mListener = layout;
    }

    public void initAnimationTranslation() {
        if (mDebug) {
            Log.d(TAG, "left = " + mImgBg.getLeft() + " , right = " + mImgBg.getRight()
                    + " , down = " + mImgBg.getBottom() + ", up = " + mImgBg.getTop());

            Log.d(TAG, "left = " + mImgSlide.getLeft() + " , right = " + mImgSlide.getRight()
                    + " , down = " + mImgSlide.getBottom() + ", up = " + mImgSlide.getTop());
            Log.d(TAG, "mCurrentDirection = " + mCurrentDirection);
        }
        ObjectAnimator animatorX;
        ObjectAnimator animatorY;
        mAnimatorSet = new AnimatorSet();
        switch (mCurrentDirection) {
            case SLIDE_DIRECTION_RIGHT:
                animatorX = ObjectAnimator.ofFloat(mImgSlide, "translationX",
                        mImgBg.getLeft()- mImgSlide.getWidth() - mImgSlide.getLeft(), mImgBg.getRight() - mImgSlide.getWidth() - mImgSlide.getLeft());
                animatorY = ObjectAnimator.ofFloat(mImgSlide, "translationY", 0, 0);
                break;
            case SLIDE_DIRECTION_UP:
                animatorX = ObjectAnimator.ofFloat(mImgSlide, "translationX", 0, 0);
                animatorY = ObjectAnimator.ofFloat(mImgSlide, "translationY",
                        mImgBg.getBottom() - mImgSlide.getTop(), mImgBg.getTop() - mImgSlide.getTop());
                break;
            case SLIDE_DIRECTION_DOWN:
                animatorX = ObjectAnimator.ofFloat(mImgSlide, "translationX", 0, 0);
                animatorY = ObjectAnimator.ofFloat(mImgSlide, "translationY",
                        mImgBg.getTop() - mImgSlide.getHeight() - mImgSlide.getTop(), mImgBg.getBottom() - mImgSlide.getHeight() - mImgSlide.getTop());
                break;
            default: //left
                animatorY = ObjectAnimator.ofFloat(mImgSlide, "translationY", 0, 0);
                animatorX = ObjectAnimator.ofFloat(mImgSlide, "translationX",
                        mImgBg.getRight() - mImgSlide.getLeft() , mImgBg.getLeft() - mImgSlide.getLeft());
                break;
        }

        animatorX.setDuration(1200);
        animatorX.setRepeatMode(ValueAnimator.RESTART);
        animatorX.setRepeatCount(ValueAnimator.INFINITE);
        animatorX.setStartDelay(500);
        animatorY.setDuration(1200);
        animatorY.setRepeatMode(ValueAnimator.RESTART);
        animatorY.setRepeatCount(ValueAnimator.INFINITE);
        animatorY.setStartDelay(500);
        mAnimatorSet.play(animatorX).with(animatorY);
        mAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                myHander.sendEmptyMessageDelayed(MSG_SHOW_SLIDE_VIEW, 500);
//                mImgSlide.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mImgSlide.setVisibility(GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeListener(this);
                animation.setDuration(0);
                ((ValueAnimator) animation).reverse();
            }
        });
        animatorY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeListener(this);
                animation.setDuration(0);
                ((ValueAnimator) animation).reverse();
            }
        });
    }

    private static final int MSG_SHOW_SLIDE_VIEW = 0;
    private Handler myHander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SHOW_SLIDE_VIEW:
                    mImgSlide.setVisibility(VISIBLE);
                    break;
            }
        }
    };

    public void startAnimation() {
        mAnimatorSet.start();
    }

    public void stopAnimation() {
        if (mAnimatorSet != null) {
            mAnimatorSet.removeAllListeners();
            mAnimatorSet.end();
            mAnimatorSet.cancel();

        }
        mImgSlide.clearAnimation();
        mImgBg.clearAnimation();
    }

    public interface onLayout {
        void onlayout();
    }
}
