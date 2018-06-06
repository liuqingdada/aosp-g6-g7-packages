package com.mstarc.app.watchfaceg7.wallpaper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;

import com.mstarc.app.watchfaceg7.R;
import com.mstarc.app.watchfaceg7.base.SimpleWallpaperService;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuqing
 * 17-9-20.
 * Email: 1239604859@qq.com
 */
public class ModernCityWatchFace extends SimpleWallpaperService {
    private static final String TAG = "ModernCityWatchFace";

    @Override
    public SimpleWallpaperEngine onCreateEngine() {
        return new ModernCityEngine();
    }

    private class ModernCityEngine extends SimpleWallpaperEngine {

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
            private Drawable mHourHandleDrawable;
            private Drawable mMinuteHandleDrawable;
            private Drawable mSecondHandleDrawable;

            private float mCenterX;
            private float mCenterY;
            private Paint mPowerStepPaint;
            private Paint mTextPaint;

            private RectF mPowerRectF;
            private RectF mStepRectF;

            NormalFace() {
                initThread.execute(initRunnable);

                mBackgroundDrawable = getDrawable(R.drawable.modern_city_normal_bg);
                mHourHandleDrawable = getDrawable(R.drawable.modern_city_normal_hour_handle);
                mMinuteHandleDrawable = getDrawable(R.drawable.modern_city_normal_minute_handle);
                mSecondHandleDrawable = getDrawable(R.drawable.modern_city_normal_second_handle);

                mPowerStepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPowerStepPaint.setColor(ContextCompat.getColor(
                        getService(), R.color.modern_city_normal_powerstep_color));
                mPowerStepPaint.setStrokeWidth(4f);
                mPowerStepPaint.setStyle(Paint.Style.STROKE);

                mPowerRectF = new RectF(98.5f, 49.5f, 172.5f, 123.5f);
                mStepRectF = new RectF(98.5f, 227.5f, 172.5f, 301.5f);
            }

            private Runnable initRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mTextPaint == null) {
                        isInit = false;

                        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        mTextPaint.setTypeface(Typeface.createFromAsset(
                                getAssets(), "fonts/source_han_sans_cn_bold.otf"));
                        mTextPaint.setTextAlign(Paint.Align.CENTER);
                        mTextPaint.setColor(Color.BLACK);

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
                mHourHandleDrawable.setBounds(bounds);
                mMinuteHandleDrawable.setBounds(bounds);
                mSecondHandleDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);

                final float secondDegrees = mCalendar.get(Calendar.SECOND) * 6f;
                final float minuteDegrees = mCalendar.get(Calendar.MINUTE) * 6f;
                final float hourDegrees = mCalendar.get(Calendar.HOUR) * 30f +
                        mCalendar.get(Calendar.MINUTE) * 0.5f;

                String dayOfMonth = String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH));
                if (dayOfMonth.length() == 1) {
                    dayOfMonth = 0 + dayOfMonth;
                }
                String month = String.valueOf(mCalendar.get(Calendar.MONTH) + 1);
                if (month.length() == 1) {
                    month = 0 + month;
                }

                if (isInit) {
                    mTextPaint.setTextSize(33f);
                    canvas.drawText(dayOfMonth.substring(0, 1), 47, 153f, mTextPaint);
                    canvas.drawText(dayOfMonth.substring(1, 2), 74f, 153f, mTextPaint);
                    canvas.drawText(month.substring(0, 1), 197f, 153f, mTextPaint);
                    canvas.drawText(month.substring(1, 2), 225f, 153f, mTextPaint);
                    //
                    mTextPaint.setTextSize(20f);
                    canvas.drawText(String.valueOf((int) getBatteryPercent()) + "%", mCenterX, 100f,
                                    mTextPaint);
                    canvas.drawText(String.valueOf(getSteps()), mCenterX, 280f, mTextPaint);
                }

                //
                canvas.drawArc(mPowerRectF, -90f, getBatteryPercent() * 3.6f, false,
                               mPowerStepPaint);
                canvas.drawArc(mStepRectF, -90f, getStepPercents() * 360f, false, mPowerStepPaint);

                //
                canvas.save();
                canvas.rotate(hourDegrees, mCenterX, mCenterY);
                mHourHandleDrawable.draw(canvas);
                canvas.rotate(minuteDegrees - hourDegrees, mCenterX, mCenterY);
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
            private Drawable mHourHandleDrawable;
            private Drawable mMinuteHandleDrawable;

            private float mCenterX;
            private float mCenterY;
            private Paint mPowerStepPaint;
            private Paint mTextPaint;

            private RectF mPowerRectF;

            AmbientFace() {
                initThread.execute(initRunnable);

                mBackgroundDrawable = getDrawable(R.drawable.modern_city_ambient_bg);
                mHourHandleDrawable = getDrawable(R.drawable.modern_city_ambient_hour_handle);
                mMinuteHandleDrawable = getDrawable(R.drawable.modern_city_ambient_minute_handle);

                mPowerStepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mPowerStepPaint.setColor(ContextCompat.getColor(
                        getService(), R.color.modern_city_ambient_powerstep_color));
                mPowerStepPaint.setStrokeWidth(5f);
                mPowerStepPaint.setStyle(Paint.Style.STROKE);

                mPowerRectF = new RectF(123f, 39f, 197f, 113f);
            }

            private Runnable initRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mTextPaint == null) {
                        isInit = false;

                        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        mTextPaint.setTypeface(Typeface.createFromAsset(
                                getAssets(), "fonts/source_han_sans_cn_bold.otf"));
                        mTextPaint.setTextAlign(Paint.Align.CENTER);
                        mTextPaint.setColor(Color.WHITE);

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
                mHourHandleDrawable.setBounds(bounds);
                mMinuteHandleDrawable.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);

                final float minuteDegrees = mCalendar.get(Calendar.MINUTE) * 6f;
                final float hourDegrees = mCalendar.get(Calendar.HOUR) * 30f +
                        mCalendar.get(Calendar.MINUTE) * 0.5f;

                //
                String dayOfMonth = String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH));
                if (dayOfMonth.length() == 1) {
                    dayOfMonth = 0 + dayOfMonth;
                }
                String month = String.valueOf(mCalendar.get(Calendar.MONTH) + 1);
                if (month.length() == 1) {
                    month = 0 + month;
                }

                if (isInit) {
                    mTextPaint.setTextSize(33f);
                    canvas.drawText(dayOfMonth.substring(0, 1), 52f, 128f, mTextPaint);
                    canvas.drawText(dayOfMonth.substring(1, 2), 78f, 128f, mTextPaint);
                    canvas.drawText(month.substring(0, 1), 243f, 128f, mTextPaint);
                    canvas.drawText(month.substring(1, 2), 269f, 128f, mTextPaint);
                    //
                    mTextPaint.setTextSize(20f);
                    canvas.drawText(String.valueOf((int) getBatteryPercent()) + "%", 160f, 94f,
                                    mTextPaint);
                }

                //
                canvas.drawArc(mPowerRectF, -90f, getBatteryPercent() * 3.6f, false,
                               mPowerStepPaint);
                //
                canvas.save();
                canvas.rotate(hourDegrees, mCenterX, mCenterY);
                mHourHandleDrawable.draw(canvas);
                canvas.rotate(minuteDegrees - hourDegrees, mCenterX, mCenterY);
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
