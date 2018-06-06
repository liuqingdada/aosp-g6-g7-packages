package com.mstarc.app.watchfaceg7.base;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.wallpaper.WallpaperService;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;

import com.mstarc.app.watchfaceg7.R;
import com.mstarc.app.watchfaceg7.utils.IntenetUtil;
import com.mstarc.app.watchfaceg7.utils.Logger;
import com.mstarc.app.watchfaceg7.utils.WeakHandler;
import com.mstarc.lib.watchbase.weather.Weather;
import com.mstarc.lib.watchbase.weather.manager.WeatherManager;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

public abstract class CommonWallpaperService extends WallpaperService {
    private final String TAG = CommonWallpaperService.class.getSimpleName();
    private boolean isRegisteredCommonReceiver;
    protected boolean mWeatherEnabled;

    protected static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF,
                                                                    Typeface.BOLD);
    protected static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF,
                                                                      Typeface.NORMAL);
    protected static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    protected CommonWallpaperService getService() {
        return this;
    }

    @Override
    public abstract CommonWallpaperEngine onCreateEngine();

    protected abstract class CommonWallpaperEngine extends Engine
            implements WeatherManager.DataLoadListener {
        private boolean mDrawFlag;
        protected final String INTENT_AMBIENT_ON = "com.mstarc.ambient.on"; // 微光
        protected final String INTENT_AMBIENT_OFF = "com.mstarc.ambient.off";
        protected final String INTENT_SPORT_STEPS = "com.mstarc.action.totalsteps";
        protected final String INTENT_ALARM = "com.mstarc.watchface.weatherfeatch.alarm";
        protected final long WEATHER_DATA_FETCH_INTERVAL = 2 * 60 * 60 * 1000; // 2 hours
        protected final String SETTINGS_SPORT_STEPS_TARGET = "userInfo_stepGoal";
        protected final String SETTINGS_SPORT_STEPS_CURRENT = "totalsteps";
        protected final String SETTINGS_SPORT_STEPS_SPORTSTEPS = "sportSteps";
        protected final String SETTINGS_SPORT_STEPS_ISSPORT = "isSport";
        protected final int SETTINGS_SPORT_STEPS_TARGET_DEFAULT = 3000;
        protected final int MSG_POST_INVALIDATE = 0;
        protected final int MSG_INVALIDATE = 1;
        protected final int MSG_UPDATE_AMBIENTMODE = 2;

        protected final int WIFI_ICON_LEVELS = 5;
        protected final int SIM_SIGNAL_ICON_LEVELS = 3;
        protected final int BATTERY_ICON_LEVELS = 5;

        protected final int MSG_UPDATE_FEATCHEWATHER = 1;

        protected WifiBroadcastReceiver mWifiBroadcastReceiver;
        protected BatteryBroadcastReceiver mBatteryBroadcastReceiver;
        protected NetWorkStateReceiver mNetWorkStateReceiver;
        protected AlarmBroadcastReceiver mAlarmBroadcastReceiver;

        protected HandlerThread mWorkThread;
        protected WorkHandler mWorkHandler;

        protected TelephonyManager mTelephonyManager;
        protected PhoneStateListener mPhoneStateListener;
        protected WifiManager mWifiManager;
        protected SubscriptionManager mSubscriptionManager;
        protected BluetoothAdapter mBluetoothAdapter;
        protected AlarmManager mAlarmManager;
        protected ConnectivityManager mConnectivityManager;

        protected boolean mDrawRequested;
        protected boolean mDestroyed;
        protected boolean mAmbient; // flag of 手环模式
        protected boolean isNetworkConnected;
        protected boolean hasSimeCard;
        protected int mSteps = 0; // 一天的行走步数
        protected int mTargetSteps = 0; // 一天的行走目标
        protected int mSportStep = 0; // 运动中的步数
        protected boolean isSport; // 是否处于运动状态
        protected int mWifiLevel = 0;
        protected int mSimLevel = 0;
        protected float mBatteryLevel = 0f;
        protected int mPercent;

        protected Weather mWeather;
        protected Drawable mWeatherCommonDrawable;
        protected SettingsContentObserver mSettingsContentObserver;
        protected SimOnSubscriptionsChangedListener mSimOnSubscriptionsChangedListener;

        protected CommonWallpaperEngine() {
            baseTextureHandlerThread.start();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            synchronized (this) {
                mDrawFlag = true;
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceCreated: ");
            }
            this.invalidate();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceChanged: ");
            }
            this.invalidate();
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            super.onSurfaceRedrawNeeded(holder);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceRedrawNeeded: ");
            }

            Log.w(TAG, "onSurfaceRedrawNeeded: draw");
            this.draw(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            synchronized (this) {
                mDrawFlag = false;
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceDestroyed: ");
            }
            baseTextureHandlerThread.quitSafely();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.v(TAG, "onVisibilityChanged: visible = " + visible);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            Log.i(TAG, "onCreate: " + surfaceHolder);
            Log.d(TAG, "init: mChoreographer = " + mChoreographer);
            while (mChoreographer == null) {
                SystemClock.sleep(20);
            }
            Log.d(TAG, "init: mChoreographer = " + mChoreographer);

            /*
             * POWERMODE_WATCH = 1;
             * POWERMODE_NORMAL = 2;
             * POWERMODE_FLIGHT = 3;
             */
            int powermode = 2;
            try {
                powermode = Settings.System.getInt(getContentResolver(),
                                                   "launcher_settings_powermode");
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            mAmbient = (powermode == 1);
            mWorkThread = new HandlerThread("handler_thread_work");
            mWorkThread.start();
            mWorkHandler = new WorkHandler(this, mWorkThread.getLooper());

            mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            mSubscriptionManager = SubscriptionManager.from(getService());
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

            isNetworkConnected = isNetWorkConnected();
            if (isNetworkConnected) {
                mWorkHandler.fetchWeatherAsync();
            }

            registerReceiver();

            mWeatherCommonDrawable = getDrawable(R.drawable.weather_common_99);

            mSettingsContentObserver = new SettingsContentObserver();

            mTargetSteps = Settings.System.getInt(getContentResolver(),
                                                  SETTINGS_SPORT_STEPS_TARGET,
                                                  SETTINGS_SPORT_STEPS_TARGET_DEFAULT);
            mSteps = Settings.System.getInt(getContentResolver(),
                                            SETTINGS_SPORT_STEPS_CURRENT,
                                            0);
            Log.i(TAG, "onCreate: mSteps = " + mSteps + ", mTargetSteps = " + mTargetSteps);
            mSportStep = android.provider.Settings.System.getInt(getContentResolver(),
                                                                 SETTINGS_SPORT_STEPS_SPORTSTEPS,
                                                                 0);
            isSport = android.provider.Settings.System.getInt(getContentResolver(),
                                                              SETTINGS_SPORT_STEPS_ISSPORT, 0) == 1;
            Log.i(TAG, "onCreate: mSportStep = " + mSportStep + ", isSport = " + isSport);

            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(SETTINGS_SPORT_STEPS_TARGET), true,
                    mSettingsContentObserver);
            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(SETTINGS_SPORT_STEPS_CURRENT), true,
                    mSettingsContentObserver);
            getContentResolver().registerContentObserver(
                    android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_SPORTSTEPS),
                    true,
                    mSettingsContentObserver);
            getContentResolver().registerContentObserver(
                    android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_ISSPORT),
                    true,
                    mSettingsContentObserver);

            mSimOnSubscriptionsChangedListener = new SimOnSubscriptionsChangedListener();
            mSubscriptionManager.addOnSubscriptionsChangedListener(
                    mSimOnSubscriptionsChangedListener);
            updateSimState();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.w(TAG, "onDestroy: ");

            mDestroyed = true;
            mHandler.removeMessages(MSG_POST_INVALIDATE);
            mChoreographer.removeFrameCallback(mFrameCallback);
            WeatherManager.getInstance()
                          .removeListener();
            cancelWeatherDataFetch();
            unregisterReceiver();
            mWorkHandler.removeMessages(MSG_UPDATE_FEATCHEWATHER);
            mWorkThread.quitSafely();
            getContentResolver().unregisterContentObserver(mSettingsContentObserver);
            mSubscriptionManager.removeOnSubscriptionsChangedListener(
                    mSimOnSubscriptionsChangedListener);
        }

        private boolean isNetWorkConnected() {
            int networkState = IntenetUtil.getNetworkState(getService());
            return networkState != 0;
        }

        private void registerReceiver() {
            if (isRegisteredCommonReceiver) {
                return;
            }
            isRegisteredCommonReceiver = true;
            //
            IntentFilter commonFilter = new IntentFilter(Intent.ACTION_TIME_TICK);
            commonFilter.addAction(INTENT_AMBIENT_ON);
            commonFilter.addAction(INTENT_AMBIENT_OFF);
            commonFilter.addAction(INTENT_SPORT_STEPS);
            getService().registerReceiver(mCommonReceiver, commonFilter);

            //
            mWifiBroadcastReceiver = new WifiBroadcastReceiver();
            IntentFilter wifiFilter = new IntentFilter();
            wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            wifiFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
            Intent intent = getService().registerReceiver(mWifiBroadcastReceiver,
                                                          wifiFilter);
            mWifiLevel = getWifiLevel(intent);

            //
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    updateSignalStrength(signalStrength.getGsmSignalStrength());
                }
            };
            mTelephonyManager.listen(mPhoneStateListener,
                                     PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                                             PhoneStateListener.LISTEN_SERVICE_STATE);
            //
            mBatteryBroadcastReceiver = new BatteryBroadcastReceiver();
            IntentFilter batteryFilter = new IntentFilter();
            batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            intent = getService().registerReceiver(mBatteryBroadcastReceiver,
                                                   batteryFilter);
            updateBatteryLevel(intent);

            //
            mNetWorkStateReceiver = new NetWorkStateReceiver();
            IntentFilter networkFilter = new IntentFilter();
            networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getService().registerReceiver(mNetWorkStateReceiver, networkFilter);

            //
            mAlarmBroadcastReceiver = new AlarmBroadcastReceiver();
            IntentFilter alarmFilter = new IntentFilter();
            alarmFilter.addAction(INTENT_ALARM);
            getService().registerReceiver(mAlarmBroadcastReceiver, alarmFilter);
            Log.d(TAG, "registerReceiver: ");
        }

        private void unregisterReceiver() {
            if (!isRegisteredCommonReceiver) {
                return;
            }
            isRegisteredCommonReceiver = false;
            getService().unregisterReceiver(mCommonReceiver);
            getService().unregisterReceiver(mAlarmBroadcastReceiver);
            getService().unregisterReceiver(mWifiBroadcastReceiver);
            getService().unregisterReceiver(mBatteryBroadcastReceiver);
            getService().unregisterReceiver(mNetWorkStateReceiver);

            Log.d(TAG, "unregisterReceiver: ");
        }

        protected HandlerThread baseTextureHandlerThread = new HandlerThread("CWSHandlerThread") {
            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                mChoreographer = Choreographer.getInstance();
            }
        };

        private Choreographer mChoreographer;

        private final Choreographer.FrameCallback mFrameCallback = new Choreographer
                .FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (!CommonWallpaperEngine.this.mDestroyed) {
                    if (CommonWallpaperEngine.this.mDrawRequested) {

                        Log.w(TAG, "doFrame: engin draw");
                        CommonWallpaperEngine.this.draw(
                                CommonWallpaperEngine.this.getSurfaceHolder());
                    }
                }
            }
        };

        protected abstract void onDraw(Canvas canvas, Rect bounds);

        private void draw(SurfaceHolder surfaceHolder) {
            synchronized (this) {
                if (mDrawFlag) {
                    this.mDrawRequested = false;
                    Canvas canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        try {
                            canvas.drawColor(Color.BLACK);
                            this.onDraw(canvas, surfaceHolder.getSurfaceFrame());
                        } catch (Exception e) {
                            Logger.e(TAG, "draw: ", e);
                        } finally {
                            try {
                                surfaceHolder.unlockCanvasAndPost(canvas);
                            } catch (Exception e) {
                                Logger.e(TAG, "draw: ", e);
                            }
                        }
                    }
                }
            }
        }

        private class WorkHandler extends WeakHandler<CommonWallpaperEngine> {
            WorkHandler(CommonWallpaperEngine owner, Looper looper) {
                super(owner, looper);
            }

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case MSG_UPDATE_FEATCHEWATHER:
                        fetchWeatherData();
                        break;
                }
            }

            void fetchWeatherAsync() {
                removeMessages(MSG_UPDATE_FEATCHEWATHER);
                sendEmptyMessage(MSG_UPDATE_FEATCHEWATHER);
            }
        }

        private void fetchWeatherData() {
            Log.d(TAG, "fetchWeatherData: " + mWeatherEnabled);
            if (mWeatherEnabled) {
                WeatherManager.getInstance()
                              .init(getService());
                WeatherManager.getInstance()
                              .addListerner(this);
                WeatherManager.getInstance()
                              .retreiveData();
            }
        }

        @Override
        public void onDateLoadCompleted() {
            mWeather = WeatherManager.getInstance()
                                     .getCurrentLocationWeather();
            Log.d(TAG,
                  "onDateLoadCompleted : " + mWeather.getLocationName() + " Temperature: " +
                          mWeather.getTemperature() + " WeatherType: " + mWeather.getWeatherType());
            schedulWeatherDataFetch();
        }

        @Override
        public void onLoadFailed() {
            schedulWeatherDataFetch();
        }

        private void schedulWeatherDataFetch() {
            if (isNetworkConnected) {
                Intent intent = new Intent(INTENT_ALARM);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        getService(), 0, intent, 0);
                long triggerAtTime = SystemClock.elapsedRealtime() + WEATHER_DATA_FETCH_INTERVAL;
                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime,
                                  pendingIntent);
            }
        }

        private void cancelWeatherDataFetch() {
            Intent intent = new Intent(INTENT_ALARM);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getService(), 0,
                                                                     intent, 0);
            mAlarmManager.cancel(pendingIntent);
        }

        private final Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_INVALIDATE:
                        CommonWallpaperEngine.this.invalidate();
                        break;

                    case MSG_UPDATE_AMBIENTMODE:
                        onAmbientModeChanged(mAmbient);
                        break;
                }
            }
        };

        protected void invalidate() {
            if (!this.mDrawRequested) {
                this.mDrawRequested = true;
                this.mChoreographer.postFrameCallback(mFrameCallback);
            }
        }

        public void postIncalidate() {
            mHandler.sendEmptyMessage(MSG_POST_INVALIDATE);
        }

        private class SettingsContentObserver extends ContentObserver {
            SettingsContentObserver() {
                super(new Handler(Looper.getMainLooper()));
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);

                if (selfChange) {
                    Log.d(TAG, "onChange: ignore selfChange for" + uri);
                }

                if (uri.equals(Settings.System.getUriFor(SETTINGS_SPORT_STEPS_TARGET))) {
                    mTargetSteps = Settings.System.getInt(getContentResolver(),
                                                          SETTINGS_SPORT_STEPS_TARGET,
                                                          SETTINGS_SPORT_STEPS_TARGET_DEFAULT);
                    Log.i(TAG, "onChange: steps target changed to -- " + mTargetSteps);

                } else if (uri.equals(Settings.System.getUriFor(SETTINGS_SPORT_STEPS_CURRENT))) {
                    mSteps = Settings.System.getInt(getContentResolver(),
                                                    SETTINGS_SPORT_STEPS_CURRENT,
                                                    0);
                    Log.i(TAG, "onChange: current steps changed to --" + mSteps);

                } else if (uri.equals(Settings.System.getUriFor(SETTINGS_SPORT_STEPS_SPORTSTEPS))) {
                    mSportStep = Settings.System.getInt(getContentResolver(),
                                                        SETTINGS_SPORT_STEPS_SPORTSTEPS,
                                                        0);
                    Log.d(TAG, "onChange: mSportStep changed to " + mSportStep);

                } else if (uri.equals(Settings.System.getUriFor(SETTINGS_SPORT_STEPS_ISSPORT))) {
                    isSport = Settings.System.getInt(getContentResolver(),
                                                     SETTINGS_SPORT_STEPS_ISSPORT,
                                                     0) == 1;
                    Log.d(TAG, "onChange: isSport changed to " + isSport);
                }
            }
        }

        private class WifiBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                mWifiLevel = getWifiLevel(intent);
                Log.d(TAG, "onReceive: wifi = " + mWifiLevel);
            }
        }

        private class BatteryBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateBatteryLevel(intent);
            }
        }

        private class AlarmBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: AlarmBroadcastReceiver: " + intent);
                mWorkHandler.fetchWeatherAsync();
            }
        }

        private class NetWorkStateReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction()
                          .equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    boolean networkConnected = isNetWorkConnected();
                    if (networkConnected != isNetworkConnected) {
                        isNetworkConnected = networkConnected;
                        if (networkConnected) {
                            mWorkHandler.fetchWeatherAsync();
                        }
                    }
                }
            }
        }

        private final BroadcastReceiver mCommonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: " + intent);

                if (intent.getAction()
                          .equals(Intent.ACTION_TIME_TICK)) {
                    onTimeTick();
                }

                if (intent.getAction()
                          .equals(INTENT_AMBIENT_ON)) {
                    mAmbient = true;
                    mHandler.sendEmptyMessage(MSG_UPDATE_AMBIENTMODE);
                }

                if (intent.getAction()
                          .equals(INTENT_AMBIENT_OFF)) {
                    mAmbient = false;
                    mHandler.sendEmptyMessage(MSG_UPDATE_AMBIENTMODE);
                }

                if (intent.getAction()
                          .equals(INTENT_SPORT_STEPS)) {
                    mSteps = intent.getIntExtra("totalsteps", 0);
                }
            }
        };

        private class SimOnSubscriptionsChangedListener
                extends SubscriptionManager.OnSubscriptionsChangedListener {
            @Override
            public void onSubscriptionsChanged() {
                super.onSubscriptionsChanged();
                updateSimState();
            }
        }

        private void updateSimState() {
            int simState = mTelephonyManager.getSimState();
            Log.d(TAG, "simState: " + simState);
            if (simState == TelephonyManager.SIM_STATE_READY) {
                hasSimeCard = true;
                updateSignalStrength(0);
            } else {
                hasSimeCard = false;
                updateSignalStrength(0);
            }
        }

        private void updateSignalStrength(int gsmSignalStrength) {
            Log.d(TAG,
                  "updateSignalStrength = " + gsmSignalStrength + ", hasSIMCard = " + hasSimeCard);

            if (!hasSimeCard) {
                mSimLevel = 0;
            } else {
                if (-1 == gsmSignalStrength || 99 == gsmSignalStrength) {
                    gsmSignalStrength = 0;
                }
                int level = gsmSignalStrength * SIM_SIGNAL_ICON_LEVELS / 32;
                if (level > SIM_SIGNAL_ICON_LEVELS) {
                    level = SIM_SIGNAL_ICON_LEVELS;
                }
                mSimLevel = level + 1;
                Log.d(TAG, "SIM signal dbm: " + gsmSignalStrength + ", level: " + mSimLevel);
            }
        }

        private void updateBatteryLevel(Intent intent) {
            if (intent.getAction()
                      .equals(Intent.ACTION_BATTERY_CHANGED)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                mBatteryLevel = level / (float) scale;
                mPercent = level * 100 / scale;
                Log.d(TAG,
                      "udateBatteryLevel: level = " + level + ", scale = " + scale + ", " +
                              "mBatteryLevel = " + mBatteryLevel + ", mPercent = " + mPercent);
            }
        }

        private int getWifiLevel(Intent intent) {
            if (intent == null) {
                if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                    int rssi = mWifiManager.getConnectionInfo()
                                           .getRssi();
                    return WifiManager.calculateSignalLevel(
                            rssi, WIFI_ICON_LEVELS);
                } else {
                    return 0;
                }
            } else {
                String action = intent.getAction();
                switch (action) {
                    case WifiManager.WIFI_STATE_CHANGED_ACTION:
                        boolean enabled = intent.getIntExtra(
                                WifiManager.EXTRA_WIFI_STATE,
                                WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;
                        if (!enabled) {
                            return 0;
                        } else {
                            int rssi = mWifiManager.getConnectionInfo()
                                                   .getRssi();
                            return WifiManager.calculateSignalLevel(
                                    rssi, WIFI_ICON_LEVELS);
                        }
                    case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                        final NetworkInfo networkInfo = intent.getParcelableExtra(
                                WifiManager.EXTRA_NETWORK_INFO);
                        boolean connected = networkInfo != null && networkInfo.isConnected();
                        // If Connected grab the signal strength and ssid.
                        if (!connected) {
                            return 0;
                        }
                        break;
                    case WifiManager.RSSI_CHANGED_ACTION:
                        // Default to -200 as its below WifiManager.MIN_RSSI.
                        int rssi, level;
                        rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
                        level = WifiManager.calculateSignalLevel(
                                rssi, WIFI_ICON_LEVELS);
                        return level;
                }
                return 0;
            }
        }

        protected boolean isAmbientMode() {
            return mAmbient;
        }

        protected Weather getWeather() {
            return mWeather;
        }

        protected int getSteps() {
            return mSteps;
        }

        protected int getTargetSteps() {
            return mTargetSteps;
        }

        protected float getStepPercents() {
            if (mSteps == 0) {
                return 0f;
            }
            float percent = mSteps / (float) mTargetSteps;

            if (percent < 0) {
                percent = 0f;
            } else if (percent > 1f) {
                percent = 1f;
            }
            return percent;
        }

        protected int getWifiSignalLeve() {
            return mWifiLevel;
        }

        protected int getSimSingalLevel() {
            return mSimLevel;
        }

        /*************************************************************************
         * 此做法不是判断是否连接ble设备, 而是判断蓝牙处于开启状态哦
         */
        protected boolean isBtConnected() {
            return mBluetoothAdapter != null
                    && mBluetoothAdapter.isEnabled()
                    && mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON;
        }

        protected String getBatteryLevelString() {
            NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(2);
            return percentFormat.format(mBatteryLevel);
        }

        protected int getBatteryLevel() {
            return (int) (BATTERY_ICON_LEVELS * mBatteryLevel);
        }

        protected float getBatteryPercent() {
            return mPercent;
        }

        protected Drawable getWeatherCommonDrawableInternal() {
            int resId;
            Weather weather = getWeather();

            if (weather == null) {
                return getDrawable(R.drawable.weather_common_99);
            }

            switch (getWeather().getWeatherType()) {
                case 0: resId = R.drawable.weather_common_0; break;
                case 1: resId = R.drawable.weather_common_1; break;
                case 2: resId = R.drawable.weather_common_2; break;
                case 3: resId = R.drawable.weather_common_3; break;
                case 4: resId = R.drawable.weather_common_4; break;
                case 5: resId = R.drawable.weather_common_5; break;
                case 6: resId = R.drawable.weather_common_6; break;
                case 7: resId = R.drawable.weather_common_7; break;
                case 8: resId = R.drawable.weather_common_8; break;
                case 9: resId = R.drawable.weather_common_9; break;
                case 10: resId = R.drawable.weather_common_10; break;
                case 11: resId = R.drawable.weather_common_11; break;
                case 12: resId = R.drawable.weather_common_12; break;
                case 13: resId = R.drawable.weather_common_13; break;
                case 14: resId = R.drawable.weather_common_14; break;
                case 15: resId = R.drawable.weather_common_15; break;
                case 16: resId = R.drawable.weather_common_16; break;
                case 17: resId = R.drawable.weather_common_17; break;
                case 18: resId = R.drawable.weather_common_18; break;
                case 19: resId = R.drawable.weather_common_19; break;
                case 20: resId = R.drawable.weather_common_20; break;
                case 21: resId = R.drawable.weather_common_21; break;
                case 22: resId = R.drawable.weather_common_22; break;
                case 23: resId = R.drawable.weather_common_23; break;
                case 24: resId = R.drawable.weather_common_24; break;
                case 25: resId = R.drawable.weather_common_25; break;
                case 26: resId = R.drawable.weather_common_26; break;
                case 27: resId = R.drawable.weather_common_27; break;
                case 28: resId = R.drawable.weather_common_28; break;
                case 29: resId = R.drawable.weather_common_29; break;
                case 30: resId = R.drawable.weather_common_30; break;
                case 31: resId = R.drawable.weather_common_31; break;
                case 32: resId = R.drawable.weather_common_32; break;
                case 49: resId = R.drawable.weather_common_49; break;
                case 53: resId = R.drawable.weather_common_53; break;
                case 54: resId = R.drawable.weather_common_54; break;
                case 55: resId = R.drawable.weather_common_55; break;
                case 56: resId = R.drawable.weather_common_56; break;
                case 57: resId = R.drawable.weather_common_57; break;
                case 58: resId = R.drawable.weather_common_58; break;
                case 99: resId = R.drawable.weather_common_99; break;
                case 301: resId = R.drawable.weather_common_301; break;
                case 302: resId = R.drawable.weather_common_302; break;
                default:
                    resId = R.drawable.weather_common_99;
            }
            return getDrawable(resId);
        }

        protected abstract void onTimeTick();

        protected abstract void onAmbientModeChanged(boolean isAmbientMode);

        protected abstract class Face {
            public abstract void onSurfaceChanged(int width, int height);

            public abstract void onDraw(Canvas canvas, Rect bounds);

            public abstract void onDestroy();
        }
    }
}
