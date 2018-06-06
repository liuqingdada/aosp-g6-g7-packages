package com.mstarc.g7.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.mstarc.g7.watchface.weather.Weather;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by hawking on 17-4-21.
 */

public class DiamondWatchFace extends CanvasWatchFaceService {

    private static final String TAG = DiamondWatchFace.class.getSimpleName();

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

        private Calendar mCalendar;

        private boolean mRegisteredTimeZoneReceiver = false;

        Date mDate;

        Face mNormalFace, mAmbientFace;

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
            mNormalFace.onSurfaceChanged(width, height);
            mAmbientFace.onSurfaceChanged(width, height);
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

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            super.onApplyWindowInsets(insets);
        }

        private class AmbientFace extends Face {
            Drawable mHourHand;
            Drawable mMinuteHand;
            //Drawable mSecondHand;
            //Drawable mBatteryDrawable;
            //Drawable mStepDrawable;
            //Paint mStepTextPaint;
            //Paint mBatteryTextPaint;

            float mStepXOffset, mStepYOffset, mBatteryXOffset, mBatteryYOffset;

            float mCenterX;

            float mCenterY;

            Drawable mBackgroundDrawable;

            public AmbientFace() {
                //AssetManager mgr = getAssets();
                //Typeface typeface = Typeface.createFromAsset(mgr, "fonts/qingyuan.ttc");
                //mStepTextPaint = new Paint();
                //mStepTextPaint.setTypeface(typeface);
                //mStepTextPaint.setAntiAlias(true);
                //mStepTextPaint.setColor(0xFF636363);
                //mStepTextPaint.setTextSize(getResources().getDimension(R.dimen.diamond_step_text_size));
                //mStepTextPaint.setTextAlign(Paint.Align.LEFT);
                //mBatteryTextPaint = new Paint(mStepTextPaint);
                //mBatteryTextPaint.setTextSize(getResources().getDimension(R.dimen.diamond_battery_text_size));
                mStepXOffset = getResources().getDimensionPixelSize(R.dimen.diamond_step_center_x);
                mStepYOffset = getResources().getDimensionPixelSize(R.dimen.diamond_step_text_baseline);
                mBatteryXOffset = getResources().getDimensionPixelSize(R.dimen.diamond_battery_center_x);
                mBatteryYOffset = getResources().getDimensionPixelSize(R.dimen.diamond_battery_base_line);

                mBackgroundDrawable = getDrawable(R.drawable.diamond_night_bg);
                mHourHand = getDrawable(R.drawable.diamond_night_hour);
                mMinuteHand = getDrawable(R.drawable.diamond_night_min);
                //mSecondHand = getDrawable(R.drawable.diamond_day_second);
                //mBatteryDrawable = getDrawable(R.drawable.diamond_night_battery_selector);
                //mStepDrawable = getDrawable(R.drawable.diamond_night_step);

                //int textColor = getResources().getColor(R.color.diamondBatteryColorNight);
                //mBatteryTextPaint.setColor(textColor);
                //textColor = getResources().getColor(R.color.diamondStepColorNight);
                //mStepTextPaint.setColor(textColor);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                mCenterX = width / 2f;
                mCenterY = height / 2f;
            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {
                mBackgroundDrawable.setBounds(bounds);
                mHourHand.setBounds(bounds);
                mMinuteHand.setBounds(bounds);
                //mBatteryDrawable.setBounds(bounds);
                //mStepDrawable.setBounds(bounds);
                mBackgroundDrawable.draw(canvas);
                //mBatteryDrawable.setLevel(getBatteryLevel());
                //mBatteryDrawable.draw(canvas);
                //mStepDrawable.draw(canvas);

                //Drawable weatherDrawable = getWeatherCommonDrawable();
                //weatherDrawable.setTint(0xFF636363);
                //weatherDrawable.setBounds(100,107,100+30,107+30);
                //weatherDrawable.draw(canvas);

                //canvas.drawText(getBatteryLevelString(), mBatteryXOffset, mBatteryYOffset, mBatteryTextPaint);
                //canvas.drawText(""+getSteps(), mStepXOffset, mStepYOffset, mStepTextPaint);
                //Weather weather = getWeather();
                //if (weather != null) {
                //    canvas.drawText(""+weather.getTemperature() + "℃", 142, 130, mStepTextPaint);
                //}
             /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
                final float seconds =
                        (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
                final float secondsRotation = seconds * 6f;

                final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

                final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
                final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
                canvas.save();

                canvas.rotate(hoursRotation, mCenterX, mCenterY);
                mHourHand.draw(canvas);

                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                mMinuteHand.draw(canvas);

                canvas.restore();

            }
        }

        private class NormalFace extends Face {
            Drawable mHourHand;
            Drawable mMinuteHand;
            Drawable mSecondHand;
            Drawable mBatteryDrawable;
            Drawable mStepDrawable;
            Paint mStepTextPaint;
            Paint mBatteryTextPaint;

            float mStepXOffset, mStepYOffset, mBatteryXOffset, mBatteryYOffset;

            float mCenterX;

            float mCenterY;

            Drawable mBackgroundDrawable;

            public NormalFace() {
                AssetManager mgr = getAssets();
                Typeface typeface = Typeface.createFromAsset(mgr, "fonts/qingyuan.ttc");
                mStepTextPaint = new Paint();
                mStepTextPaint.setTypeface(typeface);
                mStepTextPaint.setAntiAlias(true);
                mStepTextPaint.setTextSize(getResources().getDimension(R.dimen.diamond_step_text_size));
                mStepTextPaint.setTextAlign(Paint.Align.LEFT);
                mBatteryTextPaint = new Paint(mStepTextPaint);
                mBatteryTextPaint.setTextSize(getResources().getDimension(R.dimen.diamond_battery_text_size));
                mStepXOffset = getResources().getDimensionPixelSize(R.dimen.diamond_step_center_x);
                mStepYOffset = getResources().getDimensionPixelSize(R.dimen.diamond_step_text_baseline);
                mBatteryXOffset = getResources().getDimensionPixelSize(R.dimen.diamond_battery_center_x);
                mBatteryYOffset = getResources().getDimensionPixelSize(R.dimen.diamond_battery_base_line);

                mBackgroundDrawable = getDrawable(R.drawable.diamond_day_bg);
                mHourHand = getDrawable(R.drawable.diamond_day_hour);
                mMinuteHand = getDrawable(R.drawable.diamond_day_min);
                mSecondHand = getDrawable(R.drawable.diamond_day_second);
                mBatteryDrawable = getDrawable(R.drawable.diamond_day_battery_selector);
                mStepDrawable = getDrawable(R.drawable.diamond_day_step);

                int textColor = getResources().getColor(R.color.diamondBatteryTextColorDay);
                mBatteryTextPaint.setColor(textColor);
                textColor = getResources().getColor(R.color.diamondStepTextColorDay);
                mStepTextPaint.setColor(textColor);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                mCenterX = width / 2f;
                mCenterY = height / 2f;
            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {
                mBackgroundDrawable.setBounds(bounds);
                mHourHand.setBounds(bounds);
                mMinuteHand.setBounds(bounds);
                mSecondHand.setBounds(bounds);
                mBatteryDrawable.setBounds(bounds);
                mStepDrawable.setBounds(bounds);
                mBackgroundDrawable.draw(canvas);
                mBatteryDrawable.setLevel(getBatteryLevel());
                mBatteryDrawable.draw(canvas);
                mStepDrawable.draw(canvas);

                Drawable weatherDrawable = getWeatherCommonDrawable();
                weatherDrawable.setTint(0xFF6d0000);
                weatherDrawable.setBounds(100,107,100+30,107+30);
                weatherDrawable.draw(canvas);

                canvas.drawText(getBatteryLevelString(), mBatteryXOffset, mBatteryYOffset, mBatteryTextPaint);
                canvas.drawText(""+getSteps(), mStepXOffset, mStepYOffset, mStepTextPaint);
                Weather weather = getWeather();
                if (weather != null) {
                    canvas.drawText(weather.getTemperature() + "℃", 142, 130, mStepTextPaint);
                }
             /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
                final float seconds =
                        (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
                final float secondsRotation = seconds * 6f;

                final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

                final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
                final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
                canvas.save();

                canvas.rotate(hoursRotation, mCenterX, mCenterY);
                mHourHand.draw(canvas);

                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                mMinuteHand.draw(canvas);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                mSecondHand.draw(canvas);
            /* Restore the canvas' original orientation. */
                canvas.restore();

            }
        }

    }
}
