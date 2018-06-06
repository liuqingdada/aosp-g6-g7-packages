package com.mstarc.app.watchfaceg7.wallpaper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceHolder;

import com.mstarc.app.watchfaceg7.R;
import com.mstarc.app.watchfaceg7.base.SimpleWallpaperService;
import com.mstarc.app.watchfaceg7.utils.Logger;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuqing
 * 17-9-22.
 * Email: 1239604859@qq.com
 */
public class ColourfulBallWatchFace extends SimpleWallpaperService {
    private static final String TAG = "ColourfulBallWatchFace";

    @Override
    public SimpleWallpaperEngine onCreateEngine() {
        return new ColourfulBallEngine();
    }

    private class ColourfulBallEngine extends SimpleWallpaperEngine {
        private int mNewSmsCount;
        private int mMissCallCount;

        ////////////////////////////////////////////////////
        private ContentObserver newMmsContentObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                mNewSmsCount = getNewSmsCount() + getNewMmsCount();
            }
        };

        private int getNewSmsCount() {
            int result = 0;
            Cursor csr = getContentResolver().query(Uri.parse("content://sms"), null,
                                                    "type = 1 and read = 0", null, null);
            if (csr != null) {
                result = csr.getCount();
                csr.close();
            }
            return result;
        }

        private int getNewMmsCount() {
            int result = 0;
            Cursor csr = getContentResolver().query(Uri.parse("content://mms/inbox"),
                                                    null, "read = 0", null, null);
            if (csr != null) {
                result = csr.getCount();
                csr.close();
            }
            return result;
        }

        private void registerObserver() {
            unregisterObserver();
            getContentResolver().registerContentObserver(Uri.parse("content://sms"), true,
                                                         newMmsContentObserver);
            getContentResolver().registerContentObserver(Telephony.MmsSms.CONTENT_URI, true,
                                                         newMmsContentObserver);
        }

        private synchronized void unregisterObserver() {
            try {
                if (newMmsContentObserver != null) {
                    getContentResolver().unregisterContentObserver(newMmsContentObserver);
                }
                if (newMmsContentObserver != null) {
                    getContentResolver().unregisterContentObserver(newMmsContentObserver);
                }
            } catch (Exception e) {
                Log.e(TAG, "unregisterObserver fail");
            }
        }
        ////////////////////////////////////////////////////

        /**
         * 电话状态: 结束通话调用一次
         */
        private int readMissCall() {
            int result = 0;
            if (ActivityCompat.checkSelfPermission(getService(), Manifest.permission.READ_CALL_LOG)
                    != PackageManager.PERMISSION_GRANTED) {
                return result;
            }

            Cursor cursor = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.TYPE}, " type=? and new=?",
                    new String[]{CallLog.Calls.MISSED_TYPE + "", "1"}, "date desc");

            if (cursor != null) {
                result = cursor.getCount();
                cursor.close();
            }
            return result;
        }

        private PhoneStateListener mTelephonyListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_IDLE: // 空闲
                        mMissCallCount = readMissCall();
                        Log.w(TAG, "onCallStateChanged: 空闲");
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK: // 摘机, 正在通话中
                        Log.w(TAG, "onCallStateChanged: 摘机, 正在通话中");
                        break;
                    case TelephonyManager.CALL_STATE_RINGING: // 来电
                        Log.w(TAG, "onCallStateChanged: 来电 --> " + incomingNumber);
                        break;
                    default:
                        mMissCallCount = readMissCall();
                        Log.w(TAG, "onCallStateChanged: default");
                        break;
                }
            }
        };

        //private void missCallReceiver() {
        //    final IntentFilter filter = new IntentFilter();
        //    filter.addAction("com.android.phone.NotificationMgr.MissedCall_intent");
        //    final Application application = getApplication();
        //    application.registerReceiver(new BroadcastReceiver() {
        //        @Override
        //        public void onReceive(Context context, Intent intent) {
        //            String action = intent.getAction();
        //            if (action != null && "com.android.phone.NotificationMgr.MissedCall_intent"
        //                    .equals(action)) {
        //                int mMissCallCount = intent.getExtras()
        //                                           .getInt("MissedCallNumber");
        //            }
        //        }
        //    }, filter);
        //}

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            setTouchEventsEnabled(true);
            super.onCreate(surfaceHolder);
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(
                    Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                try {
                    // 注册来电监听
                    telephonyManager.listen(mTelephonyListener,
                                            PhoneStateListener.LISTEN_CALL_STATE);
                } catch (Exception e) {
                    // 异常捕捉
                    Logger.e(TAG, "onCreate: ", e);
                }
            }

            registerObserver();
            //
            mNewSmsCount = getNewSmsCount() + getNewMmsCount();
            mMissCallCount = readMissCall();
            //
            mNormalFace = new NormalFace();
            mAmbientFace = new AmbientFace();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            unregisterObserver();
            mNormalFace.onDestroy();
            mAmbientFace.onDestroy();
        }

        /**
         * 正常
         */
        private class NormalFace extends Face {
            private ExecutorService initThread = Executors.newSingleThreadExecutor();
            private boolean isInit;
            private float mCenterX;
            private float mCenterY;

            private Paint pmzdPaint;
            private Paint hykPaint;

            private Drawable mBackgroundDrawable;

            NormalFace() {
                initThread.execute(initRunnable);
                mBackgroundDrawable = getDrawable(R.drawable.colorful_ball_normal_bg);
            }

            private Runnable initRunnable = new Runnable() {
                @Override
                public void run() {
                    if (pmzdPaint == null || hykPaint == null) {
                        isInit = false;

                        pmzdPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        pmzdPaint.setTypeface(Typeface.createFromAsset(
                                getAssets(), "fonts/pang_men_zheng_dao.ttf"));
                        pmzdPaint.setTextAlign(Paint.Align.CENTER);

                        hykPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        hykPaint.setTypeface(
                                Typeface.createFromAsset(getAssets(), "fonts/hyk1gj.ttf"));
                        hykPaint.setColor(Color.WHITE);
                        hykPaint.setTextAlign(Paint.Align.CENTER);

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
                mBackgroundDrawable.draw(canvas);

                String hours = String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY));
                if (hours.length() == 1) {
                    hours = 0 + hours;
                }
                String minutes = String.valueOf(mCalendar.get(Calendar.MINUTE));
                if (minutes.length() == 1) {
                    minutes = 0 + minutes;
                }
                String months = String.valueOf(mCalendar.get(Calendar.MONTH) + 1);
                if (months.length() == 1) {
                    months = 0 + months;
                }
                String days = String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH));
                if (days.length() == 1) {
                    days = 0 + days;
                }

                if (isInit) {
                    pmzdPaint.setColor(ContextCompat.getColor(
                            getService(), R.color.colorful_ball_normal_time_color));
                    pmzdPaint.setTextSize(32f);
                    canvas.drawText(hours + ":" + minutes, mCenterX, 145f, pmzdPaint);
                    pmzdPaint.setColor(ContextCompat.getColor(
                            getService(), R.color.colorful_ball_normal_date_color));
                    pmzdPaint.setTextSize(16f);
                    canvas.drawText(months + "/" + days, mCenterX, 178f, pmzdPaint);

                    hykPaint.setTextSize(26f);
                    canvas.drawText(String.valueOf(6), 216f, 269f, hykPaint);
                }
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

            @Override
            public void onSurfaceChanged(int width, int height) {

            }

            @Override
            public void onDraw(Canvas canvas, Rect bounds) {

            }

            @Override
            public void onDestroy() {

            }
        }
    }
}
