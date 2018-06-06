package com.mstarc.wearablelauncher.view.quicksetting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.ThemeUtils;
import com.mstarc.wearablelauncher.view.quicksetting.weather.Weather;
import com.mstarc.wearablelauncher.view.quicksetting.weather.manager.WeatherManager;

/**
 * Created by wangxinzhi on 17-3-4.
 */

public class QuickSettingWeather extends RelativeLayout implements WeatherManager.DataLoadListener {
    AlarmManager mAlarmManager;
    ConnectivityManager mConnectivityManager;
    static final long WEATHER_DATA_FETCH_INTERVAL = 30 * 60 * 1000; // 0.5 hours
    AlarmBroadcastReceiver mAlarmBroadcastReceiver = new AlarmBroadcastReceiver();
    public static final String INTENT_ALARM = "com.mstarc.wearablelauncher.alarm";
    public QuickSettingWeather(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public QuickSettingWeather(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public QuickSettingWeather(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        WeatherManager.getInstance().init(mContext);
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        IntentFilter alarmIntent = new IntentFilter();
        alarmIntent.addAction(INTENT_ALARM);
        mContext.registerReceiver(mAlarmBroadcastReceiver, alarmIntent);
    }

    Context mContext;
    TextView mLocationCity;
    TextView mLocationTime;
    TextView mLocationWeek;
    TextView mLocationDate;

    ImageView mLocationWeather;
    TextView mLocationTemp;
    TextView mKongqi;
    //TextView mKongqiValue;
    //TextView mKongqiValueDes;

    TextView mWindDirection;
    TextView mWindScale;

    LinearLayout mNoDataLayout;
    ImageView mGetData;
    TextView mNoDataPrompt;
    ProgressBar mProgressBar;


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLocationCity = (TextView) findViewById(R.id.qs3_weather_city);
        mLocationTime = (TextView) findViewById(R.id.qs3_date);
        mLocationWeek = (TextView) findViewById(R.id.qs3_week);
        mLocationDate = (TextView) findViewById(R.id.qs3_jieqie);

        mLocationWeather = (ImageView) findViewById(R.id.qs3_weather_icon);
        mLocationTemp = (TextView) findViewById(R.id.qs3_wendu_text);

        mKongqi = (TextView) findViewById(R.id.qs3_kongqi_layout);

        mWindDirection = (TextView) findViewById(R.id.qs3_fengxiang_text);
        mWindScale = (TextView) findViewById(R.id.qs3_fengjiebie_text);

        mNoDataLayout = (LinearLayout) findViewById(R.id.no_data_layout);
        mGetData = (ImageView) findViewById(R.id.get_data);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mNoDataPrompt = (TextView) findViewById(R.id.no_data_prompt);
        mNoDataPrompt.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mProgressBar.setVisibility(GONE);
        WeatherManager.getInstance().addListerner(this);
        (new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                WeatherManager.getInstance().retreiveData();
                return null;
            }
        }).execute();

        mGetData.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshData();
                mProgressBar.setVisibility(VISIBLE);
                mNoDataPrompt.setVisibility(INVISIBLE);
            }
        });

    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            refreshData();
            Log.d("weather", "weather VISIBLE");

        } else if (visibility == INVISIBLE || visibility == GONE) {


            Log.d("weather", "weather INVISIBLE");

        }
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancleWeatherDataFetch();
    }

    private void updateView() {
        final Weather weather = WeatherManager.getInstance().getCurrentLocationWeather();
        final boolean isG7Target = mContext.getResources().getBoolean(R.bool.g7_target);
        mLocationCity.post(new Runnable() {
            @Override
            public void run() {
                mLocationCity.setText(weather.getLocationName());
                mLocationTime.setText(weather.getLocationDate());
                if ("六".equals(weather.getNextOneDayWeekDay()) ||
                        "日".equals(weather.getNextOneDayWeekDay())) {
                    mLocationWeek.setTextColor(ThemeUtils.getCurrentPrimaryColor());
                    mLocationDate.setTextColor(ThemeUtils.getCurrentPrimaryColor());
                } else {
                    mLocationWeek.setTextColor(Color.WHITE);
                    mLocationDate.setTextColor(Color.WHITE);
                }
                mLocationWeek.setText("周" + weather.getNextOneDayWeekDay());
                if (weather.getLunarJieqi().isEmpty()) {
                    mLocationDate.setText(weather.getLunarDate());
                    mLocationWeek.setTextColor(Color.WHITE);
                    mLocationDate.setTextColor(Color.WHITE);
                } else {
                    mLocationDate.setText(weather.getLunarJieqi());
                    mLocationWeek.setTextColor(ThemeUtils.getCurrentPrimaryColor());
                    mLocationDate.setTextColor(ThemeUtils.getCurrentPrimaryColor());
                }
//                mLocationWeather.setImageResource(WeatherManager.getInstance().
//                        getWeatherBasedonType(weather.getWeatherType(), true));
                updateImageView(mLocationWeather, WeatherManager.getInstance().
                        getWeatherBasedonType(weather.getWeatherType(), true));
                mLocationTemp.setText(weather.getTemperature() + "°");

                String quality = weather.getWeatherQuality() + "  " +
                        WeatherManager.getInstance().getDescriptionBasedOnQuality(weather.getWeatherQuality());
                mKongqi.setBackgroundResource(WeatherManager.getInstance()
                        .getIconBasedOnQuality(weather.getWeatherQuality()));
                mKongqi.setText(quality);
                mWindDirection.setText(weather.getWindDirection());
                mWindScale.setText(weather.getWindScale());

            }
        });
    }




    @Override
    public void onDateLoadCompleted() {
        mNoDataLayout.post(new Runnable() {
            @Override
            public void run() {
                mNoDataLayout.setVisibility(INVISIBLE);
            }
        });
        updateView();
        mHandler.sendEmptyMessageDelayed(MSG_RETREIVE, TIME_REFRESH);
        schedulWeatherDataFetch();
    }

    @Override
    public void onLoadFailed() {
        mNoDataLayout.post(new Runnable() {
            @Override
            public void run() {
                mNoDataLayout.setVisibility(VISIBLE);
                mProgressBar.setVisibility(INVISIBLE);
               // mNoDataPrompt.setVisibility(VISIBLE); //wyg
            }
        });
        schedulWeatherDataFetch();
    }

    private void refreshData() {
        WeatherManager.getInstance().removeListener();
        WeatherManager.getInstance().addListerner(this);
        WeatherManager.getInstance().retreiveData();
    }

    private final static int MSG_RETREIVE = 0;
    private final static int TIME_REFRESH = 3 * 60 * 60 * 1000;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RETREIVE:
                    refreshData();
                    break;
            }
        }
    };

    public void featchWeatherAsync() {
        mHandler.removeMessages(MSG_RETREIVE);
        mHandler.sendEmptyMessage(MSG_RETREIVE);
    }

    private void updateImageView(final View view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                try {
                    int color = ThemeUtils.getCurrentPrimaryColor();
                    ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                    Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                    drawable.clearColorFilter();
                    drawable.mutate().setColorFilter(filter);
                    view.setBackground(drawable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void schedulWeatherDataFetch() {
        if (isNetWorkConnected()) {
            Intent intent = new Intent(INTENT_ALARM);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            long triggerAtTime = SystemClock.elapsedRealtime() + WEATHER_DATA_FETCH_INTERVAL;
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
        }
    }

    private void cancleWeatherDataFetch() {
        Intent intent = new Intent(INTENT_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        mAlarmManager.cancel(pendingIntent);
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

    private final class AlarmBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("dingyichen", "AlarmBroadcastReceiver");
            featchWeatherAsync();
        }
    }
}
