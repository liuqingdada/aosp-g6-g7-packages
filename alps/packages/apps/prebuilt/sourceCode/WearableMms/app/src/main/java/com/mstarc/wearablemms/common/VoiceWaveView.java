package com.mstarc.wearablemms.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.mstarc.wearablemms.R;

/**
 * Created by wangxinzhi on 17-3-11.
 */

public class VoiceWaveView extends View {
    private static final String TAG = VoiceWaveView.class.getSimpleName();
    static final float WAVESPENDFACTOR = 1f;
    Paint mPaint;
    Path mPath;
    int mFrequency = 2;
    final static int WAVE_SAMPLE = 10;
    int mTime;
    //view with per millisecond
    float mMoveSpeed;
    Rect mRect;
    boolean mAnimationRunning;

    float mAmp = 1f;
    final UiHandler mHandler = new UiHandler();
    Object mLock = new Object();


    class UiHandler extends Handler {
        public static final int MSG_ANMIATION_START = 0;
        public static final int MSG_ANIMATION_STOP = 1;
        public static final int MSG_INVALIDATE = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ANMIATION_START: {
                    mAnimationRunning = true;
                    sendEmptyMessage(MSG_INVALIDATE);
                    break;
                }
                case MSG_ANIMATION_STOP:
                    mAnimationRunning = false;
                    removeMessages(MSG_INVALIDATE);
                    invalidate();
                    break;
                case MSG_INVALIDATE:
                    if (mAnimationRunning) {
                        invalidate();
                        sendEmptyMessage(MSG_INVALIDATE);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public VoiceWaveView(Context context) {
        super(context);
        init();
    }

    public VoiceWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VoiceWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(ThemeUtils.getCurrentPrimaryColor());
        mPaint.setStrokeWidth(1);
        mPaint.setAntiAlias(true);
        mPath = new Path();
        mRect = new Rect();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mRect.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        mMoveSpeed = WAVESPENDFACTOR * mRect.width() / 1000.0F;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int x, y, lastX, lastY;
        lastX = x = mRect.left;
        lastY = y = mRect.centerY();
        if (mAnimationRunning) {
            synchronized (mLock) {
                int amp = (int) (mAmp * mRect.height());
                int waveLenth = mRect.width() / mFrequency;
                long uptime = SystemClock.uptimeMillis();
//                Log.d(TAG, "uptime: " + uptime + " mMoveSpeed: " + mMoveSpeed + " uptime * mMoveSpeed: " + (uptime * mMoveSpeed));
                mPath.reset();
                for (x = mRect.left; x <= mRect.right; x += waveLenth / WAVE_SAMPLE) {
                    y = (int) (mRect.centerY() + amp / 2 * Math.sin((x + uptime * mMoveSpeed) * 2 * Math.PI / waveLenth));
                    if (x == mRect.left) {
                        lastX = x;
                        lastY = y;
                        mPath.moveTo(lastX, lastY);
                    }
                    mPath.quadTo(lastX, lastY, (lastX + x) / 2, (lastY + y) / 2);
                    canvas.drawPath(mPath, mPaint);
                    lastX = x;
                    lastY = y;
                }
            }
        } else {
            canvas.drawLine(x, y, mRect.right, y, mPaint);
        }
    }

    public void startAnimation() {
        mHandler.sendEmptyMessage(mHandler.MSG_ANMIATION_START);
    }

    public void stopAnimation() {
        mHandler.sendEmptyMessage(mHandler.MSG_ANIMATION_STOP);
    }

    public int getFrequency() {
        return mFrequency;
    }

    public void setFrequency(int frequency) {
        synchronized (mLock) {
            mFrequency = frequency;
        }
    }

    public float getAmp() {
        return mAmp;
    }

    public void setAmp(float amp) {
        synchronized (mLock) {
            this.mAmp = amp;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            stopAnimation();
        }
    }
}
