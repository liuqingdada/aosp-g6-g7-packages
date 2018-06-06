package com.mstarc.wearablemms.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.mstarc.wearablemms.R;

/**
 * Created by wangxinzhi on 17-3-11.
 */

public class VoiceWaveSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private SurfaceHolder mHolder;
    private Canvas mCanvas;

    private static final String TAG = VoiceWaveSurfaceView.class.getSimpleName();
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
    Object mLock = new Object();

    public VoiceWaveSurfaceView(Context context) {
        super(context);
        init();
    }

    public VoiceWaveSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VoiceWaveSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(ThemeUtils.getCurrentPrimaryColor());
        mPaint.setStrokeWidth(1);
        mPaint.setAntiAlias(true);
        mPath = new Path();
        mRect = new Rect();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        mAnimationRunning = true;
        new Thread(this).start();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged");
        mRect.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        mMoveSpeed = WAVESPENDFACTOR * mRect.width() / 1000.0F;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        mAnimationRunning = false;
    }

    @Override
    public void run() {
        Log.d(TAG, "run");
        long start = System.currentTimeMillis();

        while (mAnimationRunning) {
            draw();
        }
    }

    private void draw() {
        Log.d(TAG, "draw");
        mCanvas = mHolder.lockCanvas();
        if (mCanvas != null) {
            try {
//                drawCanvas(mCanvas);
                mCanvas.drawColor(Color.YELLOW);
                mHolder.unlockCanvasAndPost(mCanvas);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    protected void drawCanvas(Canvas canvas) {
        Log.d(TAG, "drawCanvas"+mRect);

        int x, y, lastX, lastY;
        lastX = x = mRect.left;
        lastY = y = mRect.centerY();
        if (false) {
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
            canvas.drawColor(Color.WHITE);
//            canvas.drawLine(x, y, mRect.right, y, mPaint);
        }
    }
}
