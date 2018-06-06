package com.mstarc.app.watchface.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by liuqing
 * 17-9-20.
 * Email: 1239604859@qq.com
 */

public abstract class SimpleWallpaperService extends CommonWallpaperService {
    private static final String TAG = "SimpleWallpaperService";

    @Override
    public abstract SimpleWallpaperEngine onCreateEngine();

    protected class SimpleWallpaperEngine extends CommonWallpaperEngine {
        protected static final int MSG_UPDATE_TIME = 0;
        protected boolean mRegisteredTimeZoneReceiver = false;
        protected Calendar mCalendar;
        protected Date mDate;
        protected Face mNormalFace, mAmbientFace;
        //protected SimpleDateFormat mDataFormat = new SimpleDateFormat("d", Locale.getDefault());
        //protected SimpleDateFormat mMouthFormat = new SimpleDateFormat("M", Locale.getDefault());

        protected final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction()
                          .equals(Intent.ACTION_LOCALE_CHANGED)) {
                    mCalendar.setTimeZone(TimeZone.getDefault());
                    invalidate();
                }
            }
        };

        protected boolean shouldTimerBeRunning() {
            return isVisible() && !isAmbientMode();
        }

        /* Handler to update the time once a second in interactive mode. */
        protected final Handler mUpdateTimeHandler = new Handler(Looper.getMainLooper()) {
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
            //mNormalFace = new NormalFace();
            //mAmbientFace = new AmbientFace();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        protected void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            getService().registerReceiver(mTimeZoneReceiver, filter);
        }

        protected void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            getService().unregisterReceiver(mTimeZoneReceiver);
        }

        protected void updateTimer() {
            Log.d(TAG, "updateTimer");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
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
        protected void onDraw(Canvas canvas, Rect bounds) {
            Log.v(TAG, "onDraw: " + bounds);
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            if (isAmbientMode()) {
                mAmbientFace.onDraw(canvas, bounds);
            } else {
                mNormalFace.onDraw(canvas, bounds);
            }
        }

        @Override
        protected void onTimeTick() {
            invalidate();
        }

        @Override
        protected void onAmbientModeChanged(boolean isAmbientMode) {
            Log.d(TAG, "onAmbientModeChanged: " + isAmbientMode);
            invalidate();
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mAmbientFace.onSurfaceChanged(width, height);
            mNormalFace.onSurfaceChanged(width, height);
        }
    }
}
