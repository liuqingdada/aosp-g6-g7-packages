package com.mstarc.g7.watchface;

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
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;

import com.mstarc.g7.watchface.weather.Weather;
import com.mstarc.g7.watchface.weather.manager.WeatherManager;

import java.text.NumberFormat;

/**
 * Created by hawking on 17-4-21.
 */

abstract public class CanvasWatchFaceService extends WallpaperService {
    private static final String TAG = CanvasWatchFaceService.class.getSimpleName();
    private boolean mRegisteredCommonReceiver = false;
    protected boolean mWeatherEnabled = false;

    public CanvasWatchFaceService() {
    }

    public abstract class Engine extends WallpaperService.Engine implements WeatherManager.DataLoadListener {
        private static final int MSG_INVALIDATE = 1;
        private static final int MSG_UPDATE_AMBIENTMODE = 2;
        public static final String INTENT_AMBIENT_ON = "com.mstarc.ambient.on";
        public static final String INTENT_AMBIENT_OFF = "com.mstarc.ambient.off";
        public static final String INTENT_SPORT_STEPS = "com.mstarc.action.totalsteps";
        public static final String SETTINGS_SPORT_STEPS_TARGET = "userInfo_stepGoal";
        public static final String SETTINGS_SPORT_STEPS_CURRENT = "totalsteps";
        public static final String SETTINGS_SPORT_STEPS_SPORTSTEPS = "sportSteps";
        public static final String SETTINGS_SPORT_STEPS_ISSPORT = "isSport";
        public static final int SETTINGS_SPORT_STEPS_TARGET_DEFAULT = 10000;
        public static final String INTENT_ALARM = "com.mstarc.watchface.weatherfeatch.alarm";
        private boolean mDrawRequested;
        private boolean mDestroyed;
        private boolean mAmbient;
        int mWifiLevel = 0;
        int mSimLevel = 0;
        float mBatteryLevel = 0f;
        WifiBroadcastReceiver mWifiBroadcastReceiver = new WifiBroadcastReceiver();
        BatteryBroadcastReceiver mBatteryBroadcastReceiver = new BatteryBroadcastReceiver();
        NetWorkStateReceiver mNetWorkStateReceiver = new NetWorkStateReceiver();
        AlarmBroadcastReceiver mAlarmBroadcastReceiver = new AlarmBroadcastReceiver();
        boolean isNetworkConnected = false;
        BluetoothAdapter mBlueadapter;
        WifiManager mWifiManager;
        TelephonyManager mTelephonyManager;
        AlarmManager mAlarmManager;
        ConnectivityManager mConnectivityManager;
        SimOnSubscriptionsChangedListener simOnSubscriptionsChangedListener;
        private SubscriptionManager mSubscriptionManager;
        boolean hasSimeCard = false;
        Weather mWeather;
        int mSteps = 0; // 一天的行走步数
        int mTargetSteps = 0; // 一天的行走目标
        int mSportStep = 0; // 运动中的步数
        boolean isSport; // 是否处于运动状态
        Drawable mWeathCommonDrawable;
        HandlerThread mWorkThread;
        WorkHandler mWorkHandler;
        SettingsContentObserver mSettingsContentObserver;
        static final long WEATHER_DATA_FETCH_INTERVAL = 2 * 60 * 60 * 1000; // 2 hours
        //        static final long WEATHER_DATA_FETCH_INTERVAL = 10 * 1000; // 2 hours
        private SubscriptionInfo mSir = null;
        private PhoneStateListener mPhoneStateListener;
        public static final int WIFI_ICON_LEVELS = 5;
        public static final int SIM_SIGNAL_ICON_LEVELS = 3;
        public static final int BATTERY_ICON_LEVELS = 5;
        private final Choreographer mChoreographer = Choreographer.getInstance();
        private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
            public void doFrame(long frameTimeNs) {
                if (!Engine.this.mDestroyed) {
                    if (Engine.this.mDrawRequested) {
                        Engine.this.draw(Engine.this.getSurfaceHolder());
                    }

                }
            }
        };

        class SimOnSubscriptionsChangedListener extends SubscriptionManager.OnSubscriptionsChangedListener {
            public void onSubscriptionsChanged() {
                updateSimState();
            }
        }
        private void updateSimState() {
            int simState = mTelephonyManager.getSimState();
            Log.d(TAG, "SimState: " + simState);
            if (TelephonyManager.SIM_STATE_READY == simState) {
                hasSimeCard = true;
                updateSignalStrength(0);
            } else {
                hasSimeCard = false;
                updateSignalStrength(0);
            }
        }
        void updateSignalStrength(int signal) {
            Log.d(TAG, "hasSimeCard: " + hasSimeCard + " updateSignalStrength: " + signal);
            if (!hasSimeCard) {
                mSimLevel = 0;
            } else {
                if (-1 == signal || signal == 99) {
                    signal = 0;
                }
                int level = signal * SIM_SIGNAL_ICON_LEVELS / 32;
                if (level > SIM_SIGNAL_ICON_LEVELS) level = SIM_SIGNAL_ICON_LEVELS;
                Log.d(TAG, "Sim signalDbm:" + signal + " level:" + level + 1);
                mSimLevel = level + 1;
            }
        }
        final Handler mHandler = new Handler() {
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_INVALIDATE:
                        Engine.this.invalidate();
                        break;
                    case MSG_UPDATE_AMBIENTMODE:
                        onAmbientModeChanged(mAmbient);
                        break;
                    default:
                        break;
                }
                super.handleMessage(message);
            }
        };

        class WorkHandler extends Handler {
            public final static int MSG_UPDATE_FEATCHWEATHER= 1;

            public WorkHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_UPDATE_FEATCHWEATHER:
                        featchWeatherData();
                        break;
                }
            }

            public void featchWeatherAsync() {
                removeMessages(MSG_UPDATE_FEATCHWEATHER);
                sendEmptyMessage(MSG_UPDATE_FEATCHWEATHER);
            }
        }


        public Engine() {
        }

        public void onDestroy() {
            Log.d(TAG, "onDestroy");
            this.mDestroyed = true;
            this.mHandler.removeMessages(0);
            this.mChoreographer.removeFrameCallback(this.mFrameCallback);
            WeatherManager.getInstance().removeListener();
            cancleWeatherDataFetch();
            unregisterReceiver();
            mWorkHandler.removeMessages(mWorkHandler.MSG_UPDATE_FEATCHWEATHER);
            mWorkThread.quit();
            getContentResolver().unregisterContentObserver(mSettingsContentObserver);
            mSubscriptionManager.removeOnSubscriptionsChangedListener(simOnSubscriptionsChangedListener);
            super.onDestroy();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceChanged");
            }

            super.onSurfaceChanged(holder, format, width, height);
            this.invalidate();
        }

        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceRedrawNeeded");
            }

            super.onSurfaceRedrawNeeded(holder);
            this.draw(holder);
        }

        public void onSurfaceCreated(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceCreated");
            }

            super.onSurfaceCreated(holder);
            this.invalidate();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            Log.d(TAG, "onCreate");
            super.onCreate(surfaceHolder);
            /**
             * POWERMODE_WATCH = 1;
             * POWERMODE_NORMAL = 2;
             * POWERMODE_FLIGHT = 3;
             **/
            int powermode = 2;
            try {
                powermode = android.provider.Settings.System.getInt(getContentResolver(), "launcher_settings_powermode");
            } catch (android.provider.Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            mAmbient = (powermode == 1);

            mWorkThread = new HandlerThread("handler-thread");
            mWorkThread.start();
            mWorkHandler = new WorkHandler(mWorkThread.getLooper());
            mTelephonyManager = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mSubscriptionManager = SubscriptionManager.from(getApplicationContext());

            mBlueadapter = BluetoothAdapter.getDefaultAdapter();
            mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            isNetworkConnected = isNetWorkConnected();
            if (isNetworkConnected) {
                mWorkHandler.featchWeatherAsync();
            }
            registerReceiver();
            mWeathCommonDrawable = getDrawable(R.drawable.weather_common_99);
            mSettingsContentObserver = new SettingsContentObserver();
            try {
                mTargetSteps = android.provider.Settings.System.getInt(getContentResolver(), SETTINGS_SPORT_STEPS_TARGET);
            } catch (android.provider.Settings.SettingNotFoundException e) {
                e.printStackTrace();
                mTargetSteps = SETTINGS_SPORT_STEPS_TARGET_DEFAULT;
                Log.e(TAG,"No target step set, use default  "+SETTINGS_SPORT_STEPS_TARGET_DEFAULT);
            }
            try {
                mSteps = android.provider.Settings.System.getInt(getContentResolver(), SETTINGS_SPORT_STEPS_CURRENT);
            } catch (android.provider.Settings.SettingNotFoundException e) {
                e.printStackTrace();
                mSteps = 0;
                Log.e(TAG,"No current step set, use default  "+0);
            }
            mSportStep = android.provider.Settings.System.getInt(getContentResolver(), SETTINGS_SPORT_STEPS_SPORTSTEPS, 0);
            isSport = android.provider.Settings.System.getInt(getContentResolver(), SETTINGS_SPORT_STEPS_ISSPORT, 0) == 1;

            getContentResolver().registerContentObserver(
                    android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_TARGET),
                    true,
                    mSettingsContentObserver);
            getContentResolver().registerContentObserver(
                    android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_CURRENT),
                    true,
                    mSettingsContentObserver);
            getContentResolver().registerContentObserver(
                    android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_SPORTSTEPS),
                    true,
                    mSettingsContentObserver);
            getContentResolver().registerContentObserver(
                    android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_ISSPORT),
                    true,
                    mSettingsContentObserver);

            simOnSubscriptionsChangedListener = new SimOnSubscriptionsChangedListener();
            mSubscriptionManager.addOnSubscriptionsChangedListener(simOnSubscriptionsChangedListener);
            updateSimState();
        }


        class SettingsContentObserver extends ContentObserver {

            public SettingsContentObserver() {
                super(new Handler(getMainLooper()));
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if(selfChange){
                    Log.d(TAG, "ignore seflChange for " + uri);
                    return;
                }
                if(uri.equals(android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_TARGET))){
                    try {
                        mTargetSteps = android.provider.Settings.System.getInt(getContentResolver(), SETTINGS_SPORT_STEPS_TARGET);
                        Log.d(TAG,"steps target changed to "+mTargetSteps);
                    } catch (android.provider.Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                else if(uri.equals(android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_TARGET))){
                    try {
                        mSteps = android.provider.Settings.System.getInt(getContentResolver(), SETTINGS_SPORT_STEPS_CURRENT);
                        Log.d(TAG,"current steps changed to "+mSteps);
                    } catch (android.provider.Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                }else if(uri.equals(android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_SPORTSTEPS))){
                    mSportStep = android.provider.Settings.System.getInt(getContentResolver(), SETTINGS_SPORT_STEPS_SPORTSTEPS, 0);
                    Log.d(TAG, "onChange: mSportStep changed to " + mSportStep);
                }else if(uri.equals(android.provider.Settings.System.getUriFor(SETTINGS_SPORT_STEPS_ISSPORT))){
                    isSport = android.provider.Settings.System.getInt(getContentResolver(), SETTINGS_SPORT_STEPS_ISSPORT, 0) == 1;
                    Log.d(TAG, "onChange: isSport changed to " + isSport);
                }
            }
        }

        public void invalidate() {
            if (!this.mDrawRequested) {
                this.mDrawRequested = true;
                this.mChoreographer.postFrameCallback(this.mFrameCallback);
            }

        }

        public void postInvalidate() {
            this.mHandler.sendEmptyMessage(0);
        }

        public void onDraw(Canvas canvas, Rect bounds) {
        }

        private void draw(SurfaceHolder holder) {
            this.mDrawRequested = false;
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    canvas.drawColor(Color.BLACK);
                    this.onDraw(canvas, holder.getSurfaceFrame());
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }

            }
        }

        private boolean isNetWorkConnected() {
            Network[] networks = mConnectivityManager.getAllNetworks();
            for (int i = 0; i < networks.length; i++) {
                NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(networks[i]);
                if (networkInfo != null && networkInfo.isConnected()) {
                    return true;
                }
            }
            return false;
        }

        private void featchWeatherData() {
            Log.d(TAG,"featchWeatherData mWeatherEnabled: "+mWeatherEnabled);
            if (mWeatherEnabled) {
                WeatherManager.getInstance().init(getApplicationContext());
                WeatherManager.getInstance().addListerner(this);
                WeatherManager.getInstance().retreiveData();
            }
        }

        private final class AlarmBroadcastReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "AlarmBroadcastReceiver");
                mWorkHandler.featchWeatherAsync();
            }
        }

        private final class NetWorkStateReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ConnectivityManager.CONNECTIVITY_ACTION == intent.getAction()) {
                    boolean connected = isNetWorkConnected();
                    if (connected != isNetworkConnected) {
                        isNetworkConnected = connected;
                        if (connected) {
                            mWorkHandler.featchWeatherAsync();
                        }
                    }
                }
            }
        }

        private final BroadcastReceiver mCommonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, intent.getAction());
                if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                    onTimeTick();
                } else if (intent.getAction().equals(INTENT_AMBIENT_ON)) {
                    mAmbient = true;
                    mHandler.sendEmptyMessage(MSG_UPDATE_AMBIENTMODE);
                } else if (intent.getAction().equals(INTENT_AMBIENT_OFF)) {
                    mAmbient = false;
                    mHandler.sendEmptyMessage(MSG_UPDATE_AMBIENTMODE);
                } else if (intent.getAction().equals(INTENT_SPORT_STEPS)) {
                    mSteps = intent.getIntExtra("totalsteps", 0);
                }
            }
        };

        private void registerReceiver() {
            if (mRegisteredCommonReceiver) {
                return;
            }
            mRegisteredCommonReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
            filter.addAction(INTENT_AMBIENT_ON);
            filter.addAction(INTENT_AMBIENT_OFF);
            filter.addAction(INTENT_SPORT_STEPS);
            getApplicationContext().registerReceiver(mCommonReceiver, filter);

            IntentFilter filterWifi = new IntentFilter();
            filterWifi.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filterWifi.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filterWifi.addAction(WifiManager.RSSI_CHANGED_ACTION);
            Intent intent = getApplicationContext().registerReceiver(mWifiBroadcastReceiver, filterWifi);
            mWifiLevel = getWifiLevel(intent);

            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                            | PhoneStateListener.LISTEN_SERVICE_STATE);
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    updateSignalStrength(signalStrength.getGsmSignalStrength());
                }
            };

            IntentFilter batteryfilter = new IntentFilter();
            batteryfilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            intent = getApplicationContext().registerReceiver(mBatteryBroadcastReceiver, batteryfilter);
            updateBatteryLevel(intent);
            Log.d(TAG, "Register broadcast receivers");

            IntentFilter netStatIntendFilter = new IntentFilter();
            netStatIntendFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getApplicationContext().registerReceiver(mNetWorkStateReceiver, netStatIntendFilter);

            IntentFilter alarmIntent = new IntentFilter();
            alarmIntent.addAction(INTENT_ALARM);
            getApplicationContext().registerReceiver(mAlarmBroadcastReceiver, alarmIntent);

        }

        private void unregisterReceiver() {
            if (!mRegisteredCommonReceiver) {
                return;
            }
            mRegisteredCommonReceiver = false;
            try {
                getApplicationContext().unregisterReceiver(mCommonReceiver);
                getApplicationContext().unregisterReceiver(mWifiBroadcastReceiver);
                getApplicationContext().unregisterReceiver(mBatteryBroadcastReceiver);
                getApplicationContext().unregisterReceiver(mNetWorkStateReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "unRegister broadcast receivers");
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
        }

        public int getWifiSignalLeve() {
            return mWifiLevel;
        }

        public int getSimSingalLevel() {
            return mSimLevel;
        }

        public boolean isBtConnected() {
            return mBlueadapter != null
                    && mBlueadapter.isEnabled()
                    && mBlueadapter.getState() == BluetoothAdapter.STATE_ON;
        }

        public String getBatteryLevelString() {
            NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(2);
            return percentFormat.format(mBatteryLevel);
        }

        public int getBatteryLevel() {
            return (int) (BATTERY_ICON_LEVELS * mBatteryLevel);
        }

        public float getBatteryPercent(){
            return mBatteryLevel;
        }

        private void updateBatteryLevel(Intent intent) {
            if (intent.getAction() == Intent.ACTION_BATTERY_CHANGED) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                mBatteryLevel = level / (float) scale;
                Log.d(TAG, "Battery EXTRA_LEVEL: " + level + " EXTRA_SCALE: " + scale + " mBatteryLevel: " + mBatteryLevel);
            }
        }

        class BatteryBroadcastReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                updateBatteryLevel(intent);
            }
        }

        class WifiBroadcastReceiver extends BroadcastReceiver {

            @Override
            public void onReceive(Context context, Intent intent) {
                mWifiLevel = getWifiLevel(intent);
                Log.d(TAG, "Wifi onReceive: " + mWifiLevel);
            }
        }

        int getWifiLevel(Intent intent) {
            if (intent == null) {
                if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                    int rssi = mWifiManager.getConnectionInfo().getRssi();
                    return WifiManager.calculateSignalLevel(
                            rssi, WIFI_ICON_LEVELS);
                } else {
                    return 0;
                }
            } else {

                String action = intent.getAction();
                if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    boolean enabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;
                    if (!enabled) {
                        return 0;
                    } else {
                        int rssi = mWifiManager.getConnectionInfo().getRssi();
                        return WifiManager.calculateSignalLevel(
                                rssi, WIFI_ICON_LEVELS);
                    }
                } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    String ssid;
                    final NetworkInfo networkInfo = (NetworkInfo)
                            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    boolean connected = networkInfo != null && networkInfo.isConnected();
                    // If Connected grab the signal strength and ssid.
                    if (!connected) {
                        return 0;
                    }
                } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
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

        abstract public void onAmbientModeChanged(boolean inAmbientMode);

        abstract public void onTimeTick();

        public final boolean isInAmbientMode() {
            return mAmbient;
        }

        public abstract class Face {

            public abstract void onSurfaceChanged(int width, int height);

            public abstract void onDraw(Canvas canvas, Rect bounds);
        }

        private void schedulWeatherDataFetch() {
            if (isNetworkConnected) {
                Intent intent = new Intent(INTENT_ALARM);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
                long triggerAtTime = SystemClock.elapsedRealtime() + WEATHER_DATA_FETCH_INTERVAL;
                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
            }
        }

        private void cancleWeatherDataFetch() {
            Intent intent = new Intent(INTENT_ALARM);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
            mAlarmManager.cancel(pendingIntent);
        }

        @Override
        public void onDateLoadCompleted() {
            mWeather = WeatherManager.getInstance().getCurrentLocationWeather();
            Log.d(TAG, "onDateLoadCompleted : " + mWeather.getLocationName() + " Temperature: " + mWeather.getTemperature() + " WeatherType: " + mWeather.getWeatherType());
            mWeathCommonDrawable = getWeatherCommonDrawableInternal();
            schedulWeatherDataFetch();
        }

        @Override
        public void onLoadFailed() {
            schedulWeatherDataFetch();
        }

        Weather getWeather() {
            return mWeather;
        }

        Drawable getWeatherCommonDrawable() {
            return mWeathCommonDrawable;
        }
        Drawable getWeatherCommonDrawableInternal() {
            int resId;
            Weather weather = getWeather();
            if (weather == null) {
                return getDrawable(R.drawable.weather_common_99);
            }
//            switch (getWeather().getWeatherType()) {
//                case WeatherUtils.TYPE_WEATHER_WUMAI:  resId = R.drawable.weather_common_53;break;
//                case WeatherUtils.TYPE_WEATHER_WU:  resId = R.drawable.weather_common_18;break;
//                case WeatherUtils.TYPE_WEATHER_SHACHENBAO:  resId = R.drawable.weather_common_20;break;
//                case WeatherUtils.TYPE_WEATHER_FUCHEN  :  resId = R.drawable.weather_common_29;break;
//                case WeatherUtils.TYPE_WEATHER_YANGSHA:  resId = R.drawable.weather_common_30;break;
//                case WeatherUtils.TYPE_WEATHER_DONGYU:  resId = R.drawable.weather_common_19;break;
//                case WeatherUtils.TYPE_WEATHER_YUJIAXUE:  resId = R.drawable.weather_common_6;break;
//                case WeatherUtils.TYPE_WEATHER_DAXUE:  resId = R.drawable.weather_common_16;break;
//                case WeatherUtils.TYPE_WEATHER_ZHONGXUE:  resId = R.drawable.weather_common_15;break;
//                case WeatherUtils.TYPE_WEATHER_XIAOXUE:  resId = R.drawable.weather_common_14;break;
//                case WeatherUtils.TYPE_WEATHER_LEIZHENYU:  resId = R.drawable.weather_common_4;break;
//                case WeatherUtils.TYPE_WEATHER_ZHENYU:  resId = R.drawable.weather_common_3;break;
//                case WeatherUtils.TYPE_WEATHER_BAOYU:  resId = R.drawable.weather_common_10;break;
//                case WeatherUtils.TYPE_WEATHER_DAYU:  resId = R.drawable.weather_common_9;break;
//                case WeatherUtils.TYPE_WEATHER_ZHONGYU:  resId = R.drawable.weather_common_8;break;
//                case WeatherUtils.TYPE_WEATHER_XIAOYU:  resId = R.drawable.weather_common_7;break;
//                case WeatherUtils.TYPE_WEATHER_YIN:  resId = R.drawable.weather_common_2;break;
//                case WeatherUtils.TYPE_WEATHER_QING:  resId = R.drawable.weather_common_0;break;
//                case WeatherUtils.TYPE_WEATHER_DUOYUN:  resId = R.drawable.weather_common_1;break;
//                case WeatherUtils.TYPE_WEATHER_LEIZHENYUBINGBAO:  resId = R.drawable.weather_common_4;break;
//                case WeatherUtils.TYPE_WEATHER_DABAOYU:  resId = R.drawable.weather_common_10;break;
//                case WeatherUtils.TYPE_WEATHER_TEDABAOYU:  resId = R.drawable.weather_common_10;break;
//                case WeatherUtils.TYPE_WEATHER_ZHENXUE:  resId = R.drawable.weather_common_14;break;
//                case WeatherUtils.TYPE_WEATHER_BAOXUE:  resId = R.drawable.weather_common_16;break;
//                case WeatherUtils.TYPE_WEATHER_XIAOYUZHONGYU:  resId = R.drawable.weather_common_7;break;
//                case WeatherUtils.TYPE_WEATHER_ZHONGYUDAYU:  resId = R.drawable.weather_common_8;break;
//                case WeatherUtils.TYPE_WEATHER_DAYUBAOYU:  resId = R.drawable.weather_common_9;break;
//                case WeatherUtils.TYPE_WEATHER_BAOYUDABAOYU:  resId = R.drawable.weather_common_10;break;
//                case WeatherUtils.TYPE_WEATHER_DABAOYUTEDABAOYU:  resId = R.drawable.weather_common_10;break;
//                case WeatherUtils.TYPE_WEATHER_XIAOXUEZHONGXUE:  resId = R.drawable.weather_common_14;break;
//                case WeatherUtils.TYPE_WEATHER_ZHONGXUEDAXUE:  resId = R.drawable.weather_common_15;break;
//                case WeatherUtils.TYPE_WEATHER_DAXUEBAOXUE:  resId = R.drawable.weather_common_16;break;
//                case WeatherUtils.TYPE_WEATHER_QIANGSHACHENBAO:  resId = R.drawable.weather_common_30;break;
//                case WeatherUtils.TYPE_WEATHER_NONGWU:  resId = R.drawable.weather_common_32;break;
//                case WeatherUtils.TYPE_WEATHER_QIANGNONGWU:  resId = R.drawable.weather_common_49;break;
//                case WeatherUtils.TYPE_WEATHER_ZHONGDUMAI:  resId = R.drawable.weather_common_54;break;
//                case WeatherUtils.TYPE_WEATHER_ZHONGDUBIGMAI:  resId = R.drawable.weather_common_55;break;
//                case WeatherUtils.TYPE_WEATHER_YANZHONGMAI:  resId = R.drawable.weather_common_56;break;
//                case WeatherUtils.TYPE_WEATHER_DAWU:  resId = R.drawable.weather_common_57;break;
//                case WeatherUtils.TYPE_WEATHER_TEQIANGNONGWU:  resId = R.drawable.weather_common_58;break;
//                case WeatherUtils.TYPE_WEATHER_NONE:  resId = R.drawable.weather_common_99;break;
//                case WeatherUtils.TYPE_WEATHER_YU:  resId = R.drawable.weather_common_301;break;
//                case WeatherUtils.TYPE_WEATHER_XUE:  resId = R.drawable.weather_common_302;break;
//                 default:   resId = R.drawable.weather_common_99; break;
//            }
            switch (getWeather().getWeatherType()){
                case 0   :  resId = R.drawable.weather_common_0   ;break;
                case 1   :  resId = R.drawable.weather_common_1   ;break;
                case 2   :  resId = R.drawable.weather_common_2   ;break;
                case 3   :  resId = R.drawable.weather_common_3   ;break;
                case 4   :  resId = R.drawable.weather_common_4   ;break;
                case 5   :  resId = R.drawable.weather_common_5   ;break;
                case 6   :  resId = R.drawable.weather_common_6   ;break;
                case 7   :  resId = R.drawable.weather_common_7   ;break;
                case 8   :  resId = R.drawable.weather_common_8   ;break;
                case 9   :  resId = R.drawable.weather_common_9   ;break;
                case 10  :  resId = R.drawable.weather_common_10  ;break;
                case 11  :  resId = R.drawable.weather_common_11  ;break;
                case 12  :  resId = R.drawable.weather_common_12  ;break;
                case 13  :  resId = R.drawable.weather_common_13  ;break;
                case 14  :  resId = R.drawable.weather_common_14  ;break;
                case 15  :  resId = R.drawable.weather_common_15  ;break;
                case 16  :  resId = R.drawable.weather_common_16  ;break;
                case 17  :  resId = R.drawable.weather_common_17  ;break;
                case 18  :  resId = R.drawable.weather_common_18  ;break;
                case 19  :  resId = R.drawable.weather_common_19  ;break;
                case 20  :  resId = R.drawable.weather_common_20  ;break;
                case 21  :  resId = R.drawable.weather_common_21  ;break;
                case 22  :  resId = R.drawable.weather_common_22  ;break;
                case 23  :  resId = R.drawable.weather_common_23  ;break;
                case 24  :  resId = R.drawable.weather_common_24  ;break;
                case 25  :  resId = R.drawable.weather_common_25  ;break;
                case 26  :  resId = R.drawable.weather_common_26  ;break;
                case 27  :  resId = R.drawable.weather_common_27  ;break;
                case 28  :  resId = R.drawable.weather_common_28  ;break;
                case 29  :  resId = R.drawable.weather_common_29  ;break;
                case 30  :  resId = R.drawable.weather_common_30  ;break;
                case 31  :  resId = R.drawable.weather_common_31  ;break;
                case 32  :  resId = R.drawable.weather_common_32  ;break;
                case 49  :  resId = R.drawable.weather_common_49  ;break;
                case 53  :  resId = R.drawable.weather_common_53  ;break;
                case 54  :  resId = R.drawable.weather_common_54  ;break;
                case 55  :  resId = R.drawable.weather_common_55  ;break;
                case 56  :  resId = R.drawable.weather_common_56  ;break;
                case 57  :  resId = R.drawable.weather_common_57  ;break;
                case 58  :  resId = R.drawable.weather_common_58  ;break;
                case 99  :  resId = R.drawable.weather_common_99  ;break;
                case 301 :  resId = R.drawable.weather_common_301;break;
                case 302 :  resId = R.drawable.weather_common_302;break;
                default:
                    resId = 0;
            }
            return getDrawable(resId);
        }

        /*
         * 运动状态
         *      0 表示不在运动中
         *          每天走的步数:mStep
         *      1 表示在运动中
         *          运动的步数:  Settings.System.putInt(getContentResolver(), "sportSteps", (int) data.getStep());
         * Settings.System.putInt(getContentResolver(), "isSport", 1);
         */
        int getSteps() {
            if (isSport) {
                return mSportStep;
            } else {
                return mSteps;
            }
        }

        int getTargetSteps() {
            return mTargetSteps;
        }

        float getStepPercents(){
            if(mTargetSteps == 0){
                return 1f;
            }
            float percent = mSteps/(float)mTargetSteps;
            if(percent<0 ) percent = 0;
            else if(percent>1f ) percent = 1f;
            return percent;
        }

    }
}
