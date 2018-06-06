package com.mstarc.app.watchface.wallpaper;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.SurfaceHolder;

import com.mstarc.app.watchface.R;
import com.mstarc.app.watchface.base.SimpleWallpaperService;

import java.util.Calendar;

/**
 * Created by liuqing
 * 17-9-19.
 * Email: 1239604859@qq.com
 */

public class BlueNightWatchFace extends SimpleWallpaperService {
    private static final String TAG = "BlueNightWatchFace";

    @Override
    public SimpleWallpaperEngine onCreateEngine() {
        return new BlueNightEngine();
    }

    private class BlueNightEngine extends SimpleWallpaperEngine {

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            mNormalFace = new NormalFace();
            mAmbientFace = new AmbientFace();
        }

        /**
         * 正常
         */
        private class NormalFace extends Face {
            private Drawable mBackgroundDrawable;
            private Drawable mHourHandle;
            private Drawable mMinuteHandle;
            private Drawable mMonthHandle;
            private Drawable mWeekHandle;
            private Drawable mPowerHandle;

            private float mCenterX;
            private float mCenterY;

            NormalFace() {
                mBackgroundDrawable = getDrawable(R.drawable.blue_night_normal_bg);
                mHourHandle = getDrawable(R.drawable.blue_night_normal_hour_handle);
                mMinuteHandle = getDrawable(R.drawable.blue_night_normal_minute_handle);
                mMonthHandle = getDrawable(R.drawable.blue_night_normal_month_handle);
                mWeekHandle = getDrawable(R.drawable.blue_night_normal_weeks_handle);
                mPowerHandle = getDrawable(R.drawable.blue_night_normal_power_handle);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                mCenterX = width * 0.5f;
                mCenterY = height * 0.5f;
            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {
                mBackgroundDrawable.setBounds(bounds);
                mHourHandle.setBounds(bounds);
                mMinuteHandle.setBounds(bounds);
                mMonthHandle.setBounds(bounds);
                mWeekHandle.setBounds(bounds);
                mPowerHandle.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);

                final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

                final float hourHandleOffset = mCalendar.get(Calendar.MINUTE) * 0.5f;
                final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30f) + hourHandleOffset;
                //
                int dayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK);
                final float weekHandleRotation = mWeeksDegrees[dayOfWeek - 1];
                //
                int dayOfMonth = mCalendar.get(Calendar.DAY_OF_MONTH);
                final float monthHandleRotation = mMonthDegrees[dayOfMonth - 1];
                //
                final float powerHandleRotation = -120f + (getBatteryPercent() * 2.325f);

                canvas.save();
                canvas.rotate(weekHandleRotation, 91f, 124f);
                mWeekHandle.draw(canvas);
                canvas.restore();

                canvas.save();
                canvas.rotate(monthHandleRotation, 229f, 124f);
                mMonthHandle.draw(canvas);
                canvas.restore();

                canvas.save();
                canvas.rotate(powerHandleRotation, 160f, 228f);
                mPowerHandle.draw(canvas);
                canvas.restore();

                //
                canvas.save();
                canvas.rotate(hoursRotation, mCenterX, mCenterY);
                mHourHandle.draw(canvas);
                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                mMinuteHandle.draw(canvas);
                canvas.restore();
            }

            @Override
            public void onDestroy() {

            }
        }

        // 1 2 3 4 5 6 7
        private float[] mWeeksDegrees = {-120.0f, -80.0f, -40.0f, 0.0f, 40.0f, 80.0f, 120.0f};

        // 1 2 3 ... 30 31
        private float[] mMonthDegrees = {-171f, 164f, -154f, -145f, -136f, -128f, -119.5f, -111f,
                                         -101.8f, -93.4f, -82f, -70f, -57f, -46.5f, -33f, -22, -10,
                                         0, 13f, 24f, 36f, 48f, 62f, 73.5f, 88f, 100f, 112f, 125.5f,
                                         139f, 153.5f, 166f};

        /**
         * 微光
         */
        private class AmbientFace extends Face {
            private Drawable mBackgroundDrawable;
            private Drawable mHourHandle;
            private Drawable mMinuteHandle;

            private float mCenterX;
            private float mCenterY;

            AmbientFace() {
                mBackgroundDrawable = getDrawable(R.drawable.blue_night_ambient_bg);
                mHourHandle = getDrawable(R.drawable.blue_night_ambient_hour_handle);
                mMinuteHandle = getDrawable(R.drawable.blue_night_ambient_minute_handle);
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                mCenterX = width * 0.5f;
                mCenterY = height * 0.5f;
            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {
                mBackgroundDrawable.setBounds(bounds);
                mHourHandle.setBounds(bounds);
                mMinuteHandle.setBounds(bounds);

                mBackgroundDrawable.draw(canvas);

                final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

                final float hourHandleOffset = mCalendar.get(Calendar.MINUTE) * 0.5f;
                final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30f) + hourHandleOffset;

                canvas.save();
                canvas.rotate(hoursRotation, mCenterX, mCenterY);
                mHourHandle.draw(canvas);
                canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
                mMinuteHandle.draw(canvas);
                canvas.restore();
            }

            @Override
            public void onDestroy() {

            }
        }
    }
}
