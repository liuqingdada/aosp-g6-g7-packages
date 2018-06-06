package com.mstarc.app.watchface.wallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.SurfaceHolder;

import com.mstarc.app.watchface.R;
import com.mstarc.app.watchface.base.SimpleWallpaperService;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuqing
 * 17-9-20.
 * Email: 1239604859@qq.com
 */

public class GoldKnightWatchFace extends SimpleWallpaperService {
    private static final String TAG = "GoldKnightWatchFace";

    @Override
    public SimpleWallpaperEngine onCreateEngine() {
        return new GoldKnightEngine();
    }

    private class GoldKnightEngine extends SimpleWallpaperEngine {
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            mNormalFace = new NormalFace();
            mAmbientFace = new AmbientFace();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mNormalFace.onDestroy();
            mAmbientFace.onDestroy();
        }

        /**
         * 正常
         */
        private class NormalFace extends Face {
            private ExecutorService initThread = Executors.newSingleThreadExecutor();
            private boolean isInit;
            private Drawable mBackgroundDrawable;
            private Drawable mSecondHandleDrawable;
            private Drawable mMinuteHandleDrawable;
            private Drawable mHourHandleDrawable;
            private Drawable mPowerHandleDrawable;
            private Drawable mWeekHandleDrawable;
            private Drawable mMonthHandleDrawable;

            private float mCenterX;
            private float mCenterY;

            private Paint mTextPaint;

            NormalFace() {
                initThread.execute(initRunnable);

                mBackgroundDrawable = getDrawable(R.drawable.gold_knight_normal_bg);
                mSecondHandleDrawable = getDrawable(R.drawable.gold_knight_normal_second_handle);
                mMinuteHandleDrawable = getDrawable(R.drawable.gold_knight_normal_minute_handle);
                mHourHandleDrawable = getDrawable(R.drawable.gold_knight_normal_hour_handle);
                mPowerHandleDrawable = getDrawable(R.drawable.gold_knight_normal_power_handle);
                mWeekHandleDrawable = getDrawable(R.drawable.gold_knight_normal_week_handle);
                mMonthHandleDrawable = getDrawable(R.drawable.gold_knight_normal_month_handle);
            }

            private Runnable initRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mTextPaint == null) {
                        isInit = false;

                        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        mTextPaint.setTypeface(Typeface.createFromAsset(
                                getAssets(), "fonts/microsoft_accor_black_bold.ttc"));
                        mTextPaint.setColor(Color.BLACK);
                        mTextPaint.setTextSize(25f);
                        mTextPaint.setTextAlign(Paint.Align.CENTER);

                        isInit = true;
                    }
                }
            };

            @Override
            public void onSurfaceChanged(int width, int height) {
                mCenterX = width * 0.5f;
                mCenterY = height * 0.5f;
            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {
                mBackgroundDrawable.setBounds(bounds);
                mSecondHandleDrawable.setBounds(bounds);
                mMinuteHandleDrawable.setBounds(bounds);
                mHourHandleDrawable.setBounds(bounds);
                mPowerHandleDrawable.setBounds(bounds);
                mWeekHandleDrawable.setBounds(bounds);
                mMonthHandleDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);

                final float secondDegrees = mCalendar.get(Calendar.SECOND) * 6f;
                final float minuteDegrees = mCalendar.get(Calendar.MINUTE) * 6f;
                final float hour24Degrees = mCalendar.get(Calendar.HOUR_OF_DAY) * 15f +
                        mCalendar.get(Calendar.MINUTE) * 0.25f;
                final float powerDegrees = getBatteryPercent() * 3.6f;
                final float weekDegrees = (mCalendar.get(Calendar.DAY_OF_WEEK) - 1) * (360f / 7f);
                final float monthDegrees = (mCalendar.get(Calendar.MONTH) + 1) * 30f;

                canvas.save();
                canvas.rotate(powerDegrees, 90f, 151f);
                mPowerHandleDrawable.draw(canvas);
                canvas.restore();

                canvas.save();
                canvas.rotate(weekDegrees, 233f, 151f);
                mWeekHandleDrawable.draw(canvas);
                canvas.restore();

                canvas.save();
                canvas.rotate(monthDegrees, 160f, 233f);
                mMonthHandleDrawable.draw(canvas);
                canvas.restore();

                if (isInit) {
                    canvas.drawText(String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH)), 220f,
                                    237f, mTextPaint);
                }

                canvas.save();
                canvas.rotate(hour24Degrees, mCenterX, mCenterY);
                mHourHandleDrawable.draw(canvas);
                canvas.rotate(minuteDegrees - hour24Degrees, mCenterX, mCenterY);
                mMinuteHandleDrawable.draw(canvas);
                canvas.rotate(secondDegrees - minuteDegrees, mCenterX, mCenterY);
                mSecondHandleDrawable.draw(canvas);
                canvas.restore();
            }

            @Override
            public void onDestroy() {
                initThread.shutdownNow();
            }
        }

        /**
         * 微光
         */
        private class AmbientFace extends Face {
            private ExecutorService initThread = Executors.newSingleThreadExecutor();
            private boolean isInit;
            private Drawable mBackgroundDrawable;
            private Drawable mMinuteHandleDrawable;
            private Drawable mHourHandleDrawable;
            private Drawable mPowerHandleDrawable;
            private Drawable mWeekHandleDrawable;
            private Drawable mMonthHandleDrawable;

            private float mCenterX;
            private float mCenterY;

            private Paint mTextPaint;

            AmbientFace() {
                initThread.execute(initRunnable);

                mBackgroundDrawable = getDrawable(R.drawable.gold_knight_ambient_bg);
                mMinuteHandleDrawable = getDrawable(R.drawable.gold_knight_ambient_minute_handle);
                mHourHandleDrawable = getDrawable(R.drawable.gold_knight_ambient_hour_handle);
                mPowerHandleDrawable = getDrawable(R.drawable.gold_knight_ambient_power_handle);
                mWeekHandleDrawable = getDrawable(R.drawable.gold_knight_ambient_week_handle);
                mMonthHandleDrawable = getDrawable(R.drawable.gold_knight_ambient_month_handle);
            }

            private Runnable initRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mTextPaint == null) {
                        isInit = false;

                        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        mTextPaint.setTypeface(Typeface.createFromAsset(
                                getAssets(), "fonts/microsoft_accor_black_bold.ttc"));
                        mTextPaint.setColor(Color.WHITE);
                        mTextPaint.setTextSize(25f);
                        mTextPaint.setTextAlign(Paint.Align.CENTER);

                        isInit = true;
                    }
                }
            };

            @Override
            public void onSurfaceChanged(int width, int height) {
                mCenterX = width * 0.5f;
                mCenterY = height * 0.5f;
            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {
                mBackgroundDrawable.setBounds(bounds);
                mMinuteHandleDrawable.setBounds(bounds);
                mHourHandleDrawable.setBounds(bounds);
                mPowerHandleDrawable.setBounds(bounds);
                mWeekHandleDrawable.setBounds(bounds);
                mMonthHandleDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);

                final float minuteDegrees = mCalendar.get(Calendar.MINUTE) * 6f;
                final float hour24Degrees = mCalendar.get(Calendar.HOUR_OF_DAY) * 15f +
                        mCalendar.get(Calendar.MINUTE) * 0.25f;
                final float powerDegrees = getBatteryPercent() * 3.6f;
                final float weekDegrees = (mCalendar.get(Calendar.DAY_OF_WEEK) - 1) * (360f / 7f);
                final float monthDegrees = (mCalendar.get(Calendar.MONTH) + 1) * 30f;

                canvas.save();
                canvas.rotate(powerDegrees, 90f, 151f);
                mPowerHandleDrawable.draw(canvas);
                canvas.restore();

                canvas.save();
                canvas.rotate(weekDegrees, 233f, 151f);
                mWeekHandleDrawable.draw(canvas);
                canvas.restore();

                canvas.save();
                canvas.rotate(monthDegrees, 160f, 233f);
                mMonthHandleDrawable.draw(canvas);
                canvas.restore();

                if (isInit) {
                    canvas.drawText(String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH)), 220f,
                                    237f, mTextPaint);
                }

                canvas.save();
                canvas.rotate(hour24Degrees, mCenterX, mCenterY);
                mHourHandleDrawable.draw(canvas);
                canvas.rotate(minuteDegrees - hour24Degrees, mCenterX, mCenterY);
                mMinuteHandleDrawable.draw(canvas);
                canvas.restore();
            }

            @Override
            public void onDestroy() {
                initThread.shutdownNow();
            }
        }
    }
}
