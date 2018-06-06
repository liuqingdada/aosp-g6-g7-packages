package com.mstarc.g6.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by hawking on 17-4-21.
 */

public class GoldWatchFace extends CanvasWatchFaceService {

    private static final String TAG = GoldWatchFace.class.getSimpleName();

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public CanvasWatchFaceService.Engine onCreateEngine() {
        return new Engine();
    }

    class Engine extends CanvasWatchFaceService.Engine {

        private static final int MSG_UPDATE_TIME = 0;

        private boolean mRegisteredTimeZoneReceiver = false;

        private Calendar mCalendar;

        Date mDate;

        Face mNormalFace, mAmbientFace;

        SimpleDateFormat mDataFormat = new SimpleDateFormat("d");

        SimpleDateFormat mMouthFormat = new SimpleDateFormat("M");

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
                    mCalendar.setTimeZone(TimeZone.getDefault());
                    invalidate();
                }
            }
        };

        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {

                Log.d(TAG, "updating time");
                invalidate();
                if (shouldTimerBeRunning()) {
                    long timeMs = System.currentTimeMillis();
                    long delayMs = INTERACTIVE_UPDATE_RATE_MS
                            - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                    mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                }

            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            mWeatherEnabled = true;
            super.onCreate(surfaceHolder);
            mCalendar = Calendar.getInstance();
            mDate = new Date();
            mNormalFace = new NormalFace();
            mAmbientFace = new AmbientFace();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            getApplicationContext().registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            getApplicationContext().unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            invalidate();
            updateTimer();
        }

        @Override
        public void onTimeTick() {
            invalidate();
        }

        private void updateTimer() {
            Log.d(TAG, "updateTimer");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mAmbientFace.onSurfaceChanged(width, height);
            mNormalFace.onSurfaceChanged(width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.v(TAG, "onDraw: " + bounds);
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            if (isInAmbientMode()) {
                mAmbientFace.onDraw(canvas, bounds);
            } else {
                mNormalFace.onDraw(canvas, bounds);
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            super.onApplyWindowInsets(insets);
        }

        class NormalFace extends Face {

            private Drawable mBackgroundDrawable;

            private Drawable mHourHand;

            private Drawable mMinuteHand;

            private Drawable mStepDrawable;

            private Drawable mWifiDrawable;

            private Drawable mSimDrawable;

            private Drawable mBtDrawable;

            private Paint mStepTextPaint, mTempTextPaint, mTempCellTextPaint;

            float mStepXOffset, mStepYOffset;


            private float mCenterX;

            private float mCenterY;

            public NormalFace() {
                mBackgroundDrawable = getDrawable(R.drawable.gold_background);
                mHourHand = getDrawable(R.drawable.gold_hour);
                mMinuteHand = getDrawable(R.drawable.gold_min);
                mStepDrawable = getDrawable(R.drawable.gold_step);
                mWifiDrawable = getDrawable(R.drawable.gold_wifi_selector);
                mSimDrawable = getDrawable(R.drawable.gold_sim_selector);
                mBtDrawable = getDrawable(R.drawable.gold_bt_selector);
                AssetManager mgr = getAssets();
                Typeface typeface = Typeface.createFromAsset(mgr, "fonts/twcenmt.ttf");
                mStepTextPaint = new Paint();
                mStepTextPaint.setTypeface(typeface);
                mStepTextPaint.setAntiAlias(true);
                mStepTextPaint.setTextSize(50);
                mStepTextPaint.setColor(0xffffdf85);
                mStepTextPaint.setTextAlign(Paint.Align.CENTER);

                mTempTextPaint = new Paint(mStepTextPaint);
                mTempTextPaint.setTextSize(20);
                mTempTextPaint.setColor(Color.WHITE);
                mTempTextPaint.setTextAlign(Paint.Align.RIGHT);

                mTempCellTextPaint = new Paint();
                mTempCellTextPaint.setAntiAlias(true);
                mTempCellTextPaint.setTextSize(15);
                mTempCellTextPaint.setColor(Color.WHITE);
                mTempCellTextPaint.setTextAlign(Paint.Align.LEFT);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                mCenterX = width / 2f;
                mCenterY = height / 2f;
                mStepXOffset = mCenterX;
                mStepYOffset = mCenterY+20;
            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {
                mBackgroundDrawable.setBounds(bounds);
                mHourHand.setBounds(bounds);
                mMinuteHand.setBounds(bounds);
                mStepDrawable.setBounds(bounds);
                mWifiDrawable.setBounds(bounds);
                mSimDrawable.setBounds(bounds);
                mBtDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);
                mStepDrawable.draw(canvas);
                mWifiDrawable.setLevel(getWifiSignalLeve());
                mWifiDrawable.draw(canvas);
                mSimDrawable.setLevel(mSimLevel);
                mSimDrawable.draw(canvas);
                mBtDrawable.setLevel(isBtConnected() ? 1 : 0);
                mBtDrawable.draw(canvas);
                canvas.drawText(""+getSteps(), mStepXOffset, mStepYOffset, mStepTextPaint);
                Drawable weatherDrawable = getWeatherCommonDrawable();
                weatherDrawable.setTint(Color.WHITE);
                weatherDrawable.setBounds(114,81,114+40,81+40);
                weatherDrawable.draw(canvas);
                com.mstarc.g6.watchface.weather.Weather weather = getWeather();
                if (getWeather() != null) {
                    canvas.drawText("" + weather.getTemperature(), 188, 108, mTempTextPaint);
                    canvas.drawText("℃", 188, 108, mTempCellTextPaint);
                }


             /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
                final float seconds =
                        (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);

                final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

                final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
                final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

                canvas.save();

                canvas.rotate(hoursRotation, mCenterX, mCenterY);
                mHourHand.draw(canvas);

                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                mMinuteHand.draw(canvas);

                canvas.restore();
            }
        }

        class AmbientFace extends Face {


            private Drawable mBackgroundDrawable;

            private Drawable mHourHand;

            private Drawable mMinuteHand;

            //private Drawable mStepDrawable;

            //private Drawable mWifiDrawable;

            //private Drawable mSimDrawable;

            //private Drawable mBtDrawable;

            //private Paint mStepTextPaint, mTempTextPaint, mTempCellTextPaint;

            //float mStepXOffset, mStepYOffset;

            private float mCenterX;

            private float mCenterY;

            public AmbientFace() {
                mBackgroundDrawable = getDrawable(R.drawable.gold_ambient_bg);
                mHourHand = getDrawable(R.drawable.gold_ambient_hour);
                mMinuteHand = getDrawable(R.drawable.gold_ambient_min);
                //mStepDrawable = getDrawable(R.drawable.gold_ambient_step);
                //mWifiDrawable = getDrawable(R.drawable.gold_ambient_wifi_selector);
                //mSimDrawable = getDrawable(R.drawable.gold_ambient_sim_selector);
                //mBtDrawable = getDrawable(R.drawable.gold_ambient_bt_selector);
                //AssetManager mgr = getAssets();
                //Typeface typeface = Typeface.createFromAsset(mgr, "fonts/twcenmt.ttf");
                //mStepTextPaint = new Paint();
                //mStepTextPaint.setTypeface(typeface);
                //mStepTextPaint.setAntiAlias(true);
                //mStepTextPaint.setTextSize(50);
                //mStepTextPaint.setColor(0xffffffff);
                //mStepTextPaint.setTextAlign(Paint.Align.CENTER);

                //mTempTextPaint = new Paint(mStepTextPaint);
                //mTempTextPaint.setTextSize(20);
                //mTempTextPaint.setColor(Color.WHITE);
                //mTempTextPaint.setTextAlign(Paint.Align.RIGHT);

                //mTempCellTextPaint = new Paint();
                //mTempCellTextPaint.setAntiAlias(true);
                //mTempCellTextPaint.setTextSize(15);
                //mTempCellTextPaint.setColor(Color.WHITE);
                //mTempCellTextPaint.setTextAlign(Paint.Align.LEFT);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                mCenterX = width / 2f;
                mCenterY = height / 2f;
                //mStepXOffset = mCenterX;
                //mStepYOffset = mCenterY+20;
            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {
                mBackgroundDrawable.setBounds(bounds);
                mHourHand.setBounds(bounds);
                mMinuteHand.setBounds(bounds);
                //mStepDrawable.setBounds(bounds);
                //mWifiDrawable.setBounds(bounds);
                //mSimDrawable.setBounds(bounds);
                //mBtDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);
                //mStepDrawable.draw(canvas);
                //mWifiDrawable.setLevel(getWifiSignalLeve());
                //mWifiDrawable.draw(canvas);
                //mSimDrawable.setLevel(mSimLevel);
                //mSimDrawable.draw(canvas);
                //mBtDrawable.setLevel(isBtConnected() ? 1 : 0);
                //mBtDrawable.draw(canvas);
                //canvas.drawText(""+getSteps(), mStepXOffset, mStepYOffset, mStepTextPaint);
                //Drawable weatherDrawable = getWeatherCommonDrawable();
                //weatherDrawable.setTint(Color.WHITE);
                //weatherDrawable.setBounds(114,81,114+40,81+40);
                //weatherDrawable.draw(canvas);
                //com.mstarc.g6.watchface.weather.Weather weather = getWeather();
                //if (getWeather() != null) {
                //    canvas.drawText("" + weather.getTemperature(), 188, 108, mTempTextPaint);
                //    canvas.drawText("℃", 188, 108, mTempCellTextPaint);
                //}


             /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */

                final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

                final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
                final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

                canvas.save();

                canvas.rotate(hoursRotation, mCenterX, mCenterY);
                mHourHand.draw(canvas);

                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                mMinuteHand.draw(canvas);

                canvas.restore();
            }
        }

    }
}
