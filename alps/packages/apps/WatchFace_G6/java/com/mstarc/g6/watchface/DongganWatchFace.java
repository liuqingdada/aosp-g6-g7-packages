package com.mstarc.g6.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
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

public class DongganWatchFace extends CanvasWatchFaceService {

    private static final String TAG = DongganWatchFace.class.getSimpleName();

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

        // 7 1 2 3 4 5 6
        final float WeekRotation[] = {135, -135, -90, -45, 0, 45, 90};

        class NormalFace extends Face {

            private Drawable mBackgroundDrawable;

            private Drawable mHourHand;

            private Drawable mMinuteHand;

            private Drawable mSecondHand;


            private Drawable mWeekDrawable;

            private Drawable mDayDrawable;

            private Paint mStepTextPaint;

            private Paint mDateTextPaint;

            Paint stepArcPaint;

            private float mCenterX;

            private float mCenterY;

            public NormalFace() {
                mBackgroundDrawable = getDrawable(R.drawable.donggan_bg);
                mHourHand = getDrawable(R.drawable.donggan_hour);
                mMinuteHand = getDrawable(R.drawable.donggan_min);
                mSecondHand = getDrawable(R.drawable.donggan_seconds);
                mWeekDrawable = getDrawable(R.drawable.donggan_week);
                mDayDrawable = getDrawable(R.drawable.donggan_day);

                mStepTextPaint = new Paint();
                mStepTextPaint.setTextSize(20);
                mStepTextPaint.setColor(0xffffffff);
                mStepTextPaint.setTextAlign(Paint.Align.CENTER);
                stepArcPaint = new Paint();
                stepArcPaint.setAntiAlias(true);
                stepArcPaint.setStyle(Paint.Style.STROKE);
                stepArcPaint.setStrokeWidth(2);
                stepArcPaint.setColor(0xFF7499BC);

                AssetManager mgr = getAssets();
                Typeface typeface = Typeface.createFromAsset(mgr, "fonts/minijianzhongliang.ttf");
                mDateTextPaint = new Paint();
                mDateTextPaint.setTypeface(typeface);
                mDateTextPaint.setTextSize(14);
                mDateTextPaint.setColor(0xffffffff);
                mDateTextPaint.setTextAlign(Paint.Align.CENTER);

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
                mWeekDrawable.setBounds(bounds);
                mDayDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);

                canvas.save();
                canvas.rotate(WeekRotation[mCalendar.get(Calendar.DAY_OF_WEEK)-1],91,150);
                mWeekDrawable.draw(canvas);
                canvas.restore();

                canvas.save();
                float dayRotation = 340*(mCalendar.get(Calendar.DAY_OF_MONTH)-1)/30f;
                canvas.rotate(dayRotation,228,150);
                mDayDrawable.draw(canvas);
                canvas.restore();
                canvas.drawText(""+mCalendar.get(Calendar.DAY_OF_MONTH), 227, 154, mDateTextPaint);

//R= 86 center = [160, 225]
                float stepRotation = 360*getStepPercents();
                canvas.drawArc(new RectF(116, 183, 202, 269), -90, stepRotation, false, stepArcPaint);
                canvas.drawText(""+getSteps(), 160, 245, mStepTextPaint);

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

                canvas.save();

                canvas.rotate(hoursRotation, mCenterX, mCenterY);
                mHourHand.draw(canvas);

                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                mMinuteHand.draw(canvas);
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                mSecondHand.draw(canvas);
                canvas.restore();
            }
        }
        class AmbientFace extends Face {

            private Drawable mBackgroundDrawable;

            private Drawable mHourHand;

            private Drawable mMinuteHand;

            //private Drawable mWeekDrawable;

            //private Drawable mDayDrawable;

            //private Paint mStepTextPaint;

            //private Paint stepArcPaint;

            //private Paint mDateTextPaint;

            private float mCenterX;

            private float mCenterY;

            public AmbientFace() {
                mBackgroundDrawable = getDrawable(R.drawable.donggan_ambiet_bg);
                mHourHand = getDrawable(R.drawable.donggan_ambiet_hour);
                mMinuteHand = getDrawable(R.drawable.donggan_ambiet_min);
                //mWeekDrawable = getDrawable(R.drawable.donggan_ambient_week);
                //mDayDrawable = getDrawable(R.drawable.donggan_ambient_day);

                //mStepTextPaint = new Paint();
                //mStepTextPaint.setTextSize(20);
                //mStepTextPaint.setColor(0xffffffff);
                //mStepTextPaint.setTextAlign(Paint.Align.CENTER);

                //stepArcPaint = new Paint();
                //stepArcPaint.setAntiAlias(true);
                //stepArcPaint.setStyle(Paint.Style.STROKE);
                //stepArcPaint.setStrokeWidth(2);
                //stepArcPaint.setColor(0xff989898);

                AssetManager mgr = getAssets();
                Typeface typeface = Typeface.createFromAsset(mgr, "fonts/minijianzhongliang.ttf");
                //mDateTextPaint = new Paint();
                //mDateTextPaint.setTypeface(typeface);
                //mDateTextPaint.setTextSize(14);
                //mDateTextPaint.setColor(0xffffffff);
                //mDateTextPaint.setTextAlign(Paint.Align.CENTER);
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
                //mWeekDrawable.setBounds(bounds);
                //mDayDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);

                //canvas.save();
                //canvas.rotate(WeekRotation[mCalendar.get(Calendar.DAY_OF_WEEK)-1],91,150);
                //mWeekDrawable.draw(canvas);
                //canvas.restore();

                //canvas.save();
                //float dayRotation = 340*(mCalendar.get(Calendar.DAY_OF_MONTH)-1)/31f;
                //canvas.rotate(dayRotation,228,150);
                //mDayDrawable.draw(canvas);
                //canvas.restore();
                //canvas.drawText(""+mCalendar.get(Calendar.DAY_OF_MONTH), 227, 154, mDateTextPaint);

                //R= 86 center = [160, 225]
                //float stepRotation = 360*getStepPercents();
                //canvas.drawArc(new RectF(116, 183, 202, 269), -90, stepRotation, false, stepArcPaint);
                //canvas.drawText(""+getSteps(), 160, 245, mStepTextPaint);

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
