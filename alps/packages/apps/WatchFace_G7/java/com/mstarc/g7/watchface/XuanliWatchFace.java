package com.mstarc.g7.watchface;

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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by hawking on 17-4-21.
 */

public class XuanliWatchFace extends CanvasWatchFaceService {

    private static final String TAG = XuanliWatchFace.class.getSimpleName();

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

        private Calendar mCalendar;

        private boolean mRegisteredTimeZoneReceiver = false;

        Date mDate;

        Face mNormalFace, mAmbientFace;

        SimpleDateFormat mDataFormat = new SimpleDateFormat("yyyy/MM/dd");

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

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mAmbientFace.onSurfaceChanged(width, height);
            mNormalFace.onSurfaceChanged(width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.v(TAG, "onDraw");
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            if (isInAmbientMode()) {
                mAmbientFace.onDraw(canvas, bounds);
            } else {
                mNormalFace.onDraw(canvas, bounds);
            }
        }

        class NormalFace extends Face {
            Drawable mBackgroundDrawable;

            private Drawable mSecondHand;

            private Drawable mBatteryDrawable;

            private Drawable mStepDrawable;

            private Paint mDataTextPaint, mMinuteTextPaint, mHourTextPaint;

            private float mCenterX;

            private float mCenterY;

            public NormalFace() {
                AssetManager mgr = getAssets();
                Typeface typeface = Typeface.createFromAsset(mgr, "fonts/twcenmt.ttf");
                mDataTextPaint = new Paint();
                mDataTextPaint.setTypeface(typeface);
                mDataTextPaint.setAntiAlias(true);
                mDataTextPaint.setTextAlign(Paint.Align.CENTER);
                mDataTextPaint.setColor(Color.WHITE);
                mDataTextPaint.setTextSize(getResources().getDimension(R.dimen.xuanli_data_text_size));

                mMinuteTextPaint = new Paint(mDataTextPaint);
                mMinuteTextPaint.setTextSize(getResources().getDimension(R.dimen.xuanli_minute_text_size));
                mHourTextPaint = new Paint(mDataTextPaint);
                mHourTextPaint.setTextSize(getResources().getDimension(R.dimen.xuanli_hour_text_size));

                mBackgroundDrawable = getDrawable(R.drawable.xuanli_day_bg);
                mSecondHand = getDrawable(R.drawable.xuanli_day_second);

                mBatteryDrawable = getDrawable(R.drawable.xuanli_day_battery_selector);
                mStepDrawable = getDrawable(R.drawable.xuanli_day_step_selector);
            }

            public void onSurfaceChanged(int width, int height) {
                mCenterX = width / 2f;
                mCenterY = height / 2f;
            }

            public void onDraw(Canvas canvas, Rect bounds) {
                Log.v(TAG, "onDraw");
                mBackgroundDrawable.setBounds(bounds);
                mSecondHand.setBounds(bounds);
                mBatteryDrawable.setBounds(bounds);
                mStepDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);
                int level = getBatteryLevel();
                Log.d(TAG,"battery level: "+level);
                mBatteryDrawable.setLevel(level);
                mBatteryDrawable.draw(canvas);
                level = (int) (getStepPercents() * 7f );
                Log.d(TAG,"step level: "+level);
                mStepDrawable.setLevel(level);
                mStepDrawable.draw(canvas);

                canvas.drawText(mDataFormat.format(mDate), mCenterX, 90, mDataTextPaint);

                String minutes = String.valueOf(mCalendar.get(Calendar.MINUTE));
                if (minutes.length() == 1) {
                    minutes = 0 + minutes;
                }
                canvas.drawText(minutes, mCenterX, 196, mMinuteTextPaint);

                String hours = String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY));
                if (hours.length() == 1) {
                    hours = 0 + hours;
                }
                canvas.drawText(hours, mCenterX, 255, mHourTextPaint);

             /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
                final float seconds =
                        (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
                final float secondsRotation = seconds * 6f;


                canvas.save();

                canvas.rotate(secondsRotation, mCenterX, mCenterY);
                mSecondHand.draw(canvas);

                canvas.restore();

            }
        }

        class AmbientFace extends Face {
            Drawable mBackgroundDrawable;

            //private Drawable mBatteryDrawable;

            //private Drawable mStepDrawable;

            private Paint mDataTextPaint, mMinuteTextPaint, mHourTextPaint;

            private float mCenterX;

            private float mCenterY;

            public AmbientFace() {
                AssetManager mgr = getAssets();
                Typeface typeface = Typeface.createFromAsset(mgr, "fonts/twcenmt.ttf");
                mDataTextPaint = new Paint();
                mDataTextPaint.setTypeface(typeface);
                mDataTextPaint.setAntiAlias(true);
                mDataTextPaint.setTextAlign(Paint.Align.CENTER);
                mDataTextPaint.setColor(0xFFDEDEDE);
                mDataTextPaint.setTextSize(getResources().getDimension(R.dimen.xuanli_data_text_size));

                mMinuteTextPaint = new Paint(mDataTextPaint);
                mMinuteTextPaint.setTextSize(getResources().getDimension(R.dimen.xuanli_minute_text_size));
                mHourTextPaint = new Paint(mDataTextPaint);
                mHourTextPaint.setTextSize(getResources().getDimension(R.dimen.xuanli_hour_text_size));

                mBackgroundDrawable = getDrawable(R.drawable.xuanli_night_bg);

                //mBatteryDrawable = getDrawable(R.drawable.xuanli_night_battery_selector);
                //mStepDrawable = getDrawable(R.drawable.xuanli_night_step_selector);
            }

            public void onSurfaceChanged(int width, int height) {
                mCenterX = width / 2f;
                mCenterY = height / 2f;
            }

            public void onDraw(Canvas canvas, Rect bounds) {
                Log.v(TAG, "onDraw");
                mBackgroundDrawable.setBounds(bounds);
                //mBatteryDrawable.setBounds(bounds);
                //mStepDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);
                //mBatteryDrawable.setLevel(getBatteryLevel());
                //mBatteryDrawable.draw(canvas);
                //mStepDrawable.setLevel((int) (getStepPercents() * 7f));
                //mStepDrawable.draw(canvas);

                canvas.drawText(mDataFormat.format(mDate), mCenterX, 90, mDataTextPaint);

                String minutes = String.valueOf(mCalendar.get(Calendar.MINUTE));
                if (minutes.length() == 1) {
                    minutes = 0 + minutes;
                }
                canvas.drawText(minutes, mCenterX, 196, mMinuteTextPaint);

                String hours = String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY));
                if (hours.length() == 1) {
                    hours = 0 + hours;
                }
                canvas.drawText(hours, mCenterX, 255, mHourTextPaint);

            }
        }
    }
}

