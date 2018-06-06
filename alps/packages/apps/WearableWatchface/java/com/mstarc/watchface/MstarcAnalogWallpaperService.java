package com.mstarc.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by hawking on 17-4-21.
 */

public class MstarcAnalogWallpaperService extends CanvasWatchFaceService {

    private static final String TAG = MstarcAnalogWallpaperService.class.getSimpleName();

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

        private Drawable mHourHand;

        private Drawable mMinuteHand;

        private Drawable mSecondHand;

        private boolean mRegisteredTimeZoneReceiver = false;

        private float mCenterX;

        private float mCenterY;

        Drawable mBackgroundDrawable;
        Drawable mBackgroundGrayDrawable;
        Drawable mSportDrawable;

        private boolean mChanged;

        Date mDate;
        SimpleDateFormat mDayOfWeekFormat;
        Paint mDatePaint;
        Paint mDayofWeekaint;
        Paint mStepPaint;
        float mDateXOffset;
        float mDateYOffset;
        float mDayofWeekXOffset;
        float mDayofWeekYOffset;
        float mSportIconXOffset;
        float mSportIconYOffset;
        float mStepXOffset;
        float mStepYOffset;


        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
                    mCalendar.setTimeZone(TimeZone.getDefault());
                    initFormats();
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
            mBackgroundDrawable = getDrawable(R.drawable.mstarc_analog_background);
            mBackgroundGrayDrawable = getDrawable(R.drawable.mstarc_analog_background_gray);
            mBackgroundGrayDrawable.setTint(Color.WHITE);
            mSportDrawable = getDrawable(R.drawable.mstarc_analog_sport);
            mSportDrawable.setTint(Color.RED);
            mHourHand = getDrawable(R.drawable.mstarc_analog_hour);
            mMinuteHand = getDrawable(R.drawable.mstarc_analog_min);
            mSecondHand = getDrawable(R.drawable.mstarc_analog_second);
            mDatePaint = createTextPaint(
                    getResources().getColor(R.color.mstarc_analog_text_color));
            mDayofWeekaint = createTextPaint(
                    getResources().getColor(R.color.mstarc_analog_text_color));
            mStepPaint = createTextPaint(
                    getResources().getColor(R.color.mstarc_analog_text_color));
            mCalendar = Calendar.getInstance();
            mDate = new Date();
            initFormats();
        }

        private void initFormats() {
            mDayOfWeekFormat = new SimpleDateFormat("E", Locale.getDefault());
            mDayOfWeekFormat.setCalendar(mCalendar);
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
                initFormats();
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
            mCenterX = width / 2f;
            mCenterY = height / 2f;
            mChanged = true;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.v(TAG, "onDraw");
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            canvas.drawColor(Color.BLUE);
            if (mChanged) {
                mBackgroundDrawable.setBounds(bounds);
                mBackgroundGrayDrawable.setBounds(bounds);
                int sportWidth = (int) getResources().getDimension(R.dimen.mstarc_analog_sporticon_width);
                int sportHeight = (int) getResources().getDimension(R.dimen.mstarc_analog_sporticon_height);
                mSportDrawable.setBounds(new Rect((int) mSportIconXOffset, (int) mSportIconYOffset, (int) mSportIconXOffset + sportWidth, (int) mSportIconYOffset + sportHeight));
                mHourHand.setBounds(bounds);
                mMinuteHand.setBounds(bounds);
                mSecondHand.setBounds(bounds);
                mChanged = false;
            }
            if (!isInAmbientMode()) {
                mBackgroundDrawable.draw(canvas);
            } else {
                canvas.drawColor(Color.BLACK);
                mBackgroundGrayDrawable.draw(canvas);
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
            if (!isInAmbientMode()) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                mSecondHand.draw(canvas);
            /* Restore the canvas' original orientation. */
                canvas.restore();
                // Date
                canvas.drawText(
                        "" + mDate.getDate(),
                        mDateXOffset, mDateYOffset, mDatePaint);
                // Day of week
                canvas.drawText(
                        mDayOfWeekFormat.format(mDate),
                        mDayofWeekXOffset, mDayofWeekYOffset, mDayofWeekaint);
                mSportDrawable.draw(canvas);
                canvas.drawText(
                        ""+getSteps(),
                        mStepXOffset, mStepYOffset, mStepPaint);
            }

        }

        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MstarcAnalogWallpaperService.this.getResources();
            mDateXOffset = resources.getDimension(R.dimen.mstarc_analog_date_xoffset);
            mDateYOffset = resources.getDimension(R.dimen.mstarc_analog_date_yoffset);
            mDayofWeekXOffset = resources.getDimension(R.dimen.mstarc_analog_dayofweek_xoffset);
            mDayofWeekYOffset = resources.getDimension(R.dimen.mstarc_analog_dayofweek_yoffset);
            mSportIconXOffset = resources.getDimension(R.dimen.mstarc_analog_sporticon_xoffset);
            mSportIconYOffset = resources.getDimension(R.dimen.mstarc_analog_sporticon_yoffset);
            mStepXOffset = resources.getDimension(R.dimen.mstarc_analog_sportstep_xoffset);
            mStepYOffset = resources.getDimension(R.dimen.mstarc_analog_sportstep_yoffset);
            mDatePaint.setTextSize(resources.getDimension(R.dimen.mstarc_analog_date_text_size));
            mDayofWeekaint.setTextSize(resources.getDimension(R.dimen.mstarc_analog_dayofweek_text_size));
            mStepPaint.setTextSize(resources.getDimension(R.dimen.mstarc_analog_sportstep_text_size));
            mDatePaint.setAntiAlias(true);
            mDayofWeekaint.setAntiAlias(true);
            mStepPaint.setAntiAlias(true);

        }
    }
}
