package com.mstarc.lib.watchbase.weather.manager;

import android.content.Context;
import android.util.Log;

import com.mstarc.lib.watchbase.weather.Weather;
import com.mstarc.watchbase.weather.IWeather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * description
 * <p>
 * Created by andyding on 2017/4/5.
 */
public class WeatherManager implements IWeather.OnGetWeatherListener {
    private static final String TAG = WeatherManager.class.getSimpleName();
    private static WeatherManager sInstance = null;
    private Context mContext;

    public static WeatherManager getInstance() {
        if (sInstance == null) {
            sInstance = new WeatherManager();
        }
        return sInstance;
    }

    public void init(Context context) {
        mContext = context;
    }

    private WeatherManager() {
    }

    private Weather mCurrentLocationWeather = new Weather();
    private Weather mHomeLocationWeather = new Weather();
    private DataLoadListener mDataLoadListener;

    public void retreiveData() {
        com.mstarc.watchbase.weather.Weather.getInstance().init(mContext);
        com.mstarc.watchbase.weather.Weather.getInstance().setOnGetWeatherListener(this);
    }

    public void addListerner(DataLoadListener listener) {
        mDataLoadListener = listener;
    }

    public void removeListener() {
        mDataLoadListener = null;
    }

    public interface DataLoadListener {
        void onDateLoadCompleted();

        void onLoadFailed();
    }

    public Weather getCurrentLocationWeather() {
        return mCurrentLocationWeather;
    }

    public Weather getHomeLocationWeather() {
        return mHomeLocationWeather;
    }

    private void parseRawData(String data) {
        try {
            JSONObject rawdata = new JSONObject(data);
            JSONArray jsonArray = rawdata.getJSONArray("datas");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = new JSONObject(jsonArray.get(i).toString());
                JSONArray tianqi = json.getJSONArray("tianqiinfo");
                if (json.getInt("type") == 1) {
                    mCurrentLocationWeather.setLocationType(1);
                    buildWeather(mCurrentLocationWeather, tianqi);
                } else if (json.getInt("type") == 0) {
                    mHomeLocationWeather.setLocationType(0);
                    buildWeather(mHomeLocationWeather, tianqi);
                }
            }
        } catch (JSONException e) {
           Log.d(TAG, "parseRawData error!!!");
           mDataLoadListener.onLoadFailed();
        }
    }

    private void buildWeather(Weather weather, JSONArray array) {
        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject jsonObject = new JSONObject(array.get(i).toString());
                if (i == 0) {
                    weather.setLocationName(jsonObject.getString("citymc"));
                    weather.setWeatherQuality(jsonObject.getInt("pm25"));
                    weather.setWeatherName(jsonObject.getString("tianqi"));
                    weather.setWeatherType(jsonObject.getInt("tianqiid"));
                    weather.setTemperature(jsonObject.getString("wendu"));
                    weather.setWindDirection(jsonObject.getString("fengxiang"));
                    weather.setWindScale(jsonObject.getString("fengli"));
                    weather.setLunarDate(jsonObject.getString("nongli"));
                    weather.setLunarJieqi(jsonObject.getString("jieqi"));
                    Date date = getDate(jsonObject.getString("gxrqtq"));
                    Log.d("dingyichen", "data : " + date);
                    Calendar mCalendar = Calendar.getInstance();
                    if (date != null) {
                        mCalendar.setTime(date);
                        String month = String.valueOf(mCalendar.get(Calendar.MONTH)+1);
                        if (month.length() == 1) {
                            month = 0 + month;
                        }
                        String day = String.valueOf(mCalendar.get(Calendar.DAY_OF_MONTH));
                        if (day.length() == 1) {
                            day = 0 + day;
                        }
                        String hour = String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY));
                        if (hour.length() == 1) {
                            hour = 0 + hour;
                        }
                        String minute = String.valueOf(mCalendar.get(Calendar.MINUTE));
                        if (minute.length() == 1) {
                            minute = 0 + minute;
                        }
                        Log.d(TAG, "month : " + month + " day : " + day + "hour: " + hour + " minute : " + minute);
                        String currentdate = String.format("%s-%s", month, day);
                        String currenttime = String.format("%s:%s", hour, minute);
                        weather.setLocationTime(currenttime);
                        weather.setLocationDate(currentdate);
                    }
                } else if (i == 1) {
                    weather.setNextOneDayTmp(jsonObject.getString("wendu"));
                    weather.setWeatherName(jsonObject.getString("tianqi"));
                    weather.setNextOneDayWeatherType(jsonObject.getInt("tianqiid"));
                    weather.setNextOneDayWeekDay(getWeekDayByString(jsonObject.getString("updatedon")));
                } else if (i == 2) {
                    weather.setNextTwoDayTmp(jsonObject.getString("wendu"));
                    weather.setWeatherName(jsonObject.getString("tianqi"));
                    weather.setNextTwoDayWeatherType(jsonObject.getInt("tianqiid"));
                    weather.setNextTwoDayWeekDay(getWeekDayByString(jsonObject.getString("updatedon")));
                } else if (i == 3) {
                    weather.setNextThreeDayTmp(jsonObject.getString("wendu"));
                    weather.setWeatherName(jsonObject.getString("tianqi"));
                    weather.setNextThreeDayWeatherType(jsonObject.getInt("tianqiid"));
                    weather.setNextThreeDayWeekDay(getWeekDayByString(jsonObject.getString("updatedon")));
                }

            }
        } catch (JSONException e) {

        }
    }

    private String getWeekDayByString(String createon) {
        Date date = getDate(createon);
        if (date != null) {
            return getWeekOfDate(date);
        }
        return "";
    }

    private Date getDate(String createon) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try {
            Date date = format.parse(createon);
            return date;
        } catch (ParseException e) {
            return null;
        }
    }


    public static String getWeekOfDate(Date dt) {
        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        return weekDays[w];
    }

    public String getDescriptionBasedOnQuality(int mWeatherQuality) {
        String qualityString = null;
        if (mWeatherQuality > 0 && mWeatherQuality <= 50) {
            qualityString = "优";
        } else if (mWeatherQuality > 50 && mWeatherQuality <= 100) {
            qualityString = "良";
        } else if (mWeatherQuality > 100 && mWeatherQuality <= 150) {
            qualityString = "轻度";
        } else if (mWeatherQuality > 150 && mWeatherQuality <= 200) {
            qualityString = "中度";
        } else if (mWeatherQuality > 200 && mWeatherQuality <= 300) {
            qualityString = "重度";
        } else if (mWeatherQuality > 300) {
            qualityString = "严重";
        }
        return qualityString;
    }

    @Override
    public void onSuccess(String s) {
        Log.d(TAG, "get weather data onSuccess, data : " + s);
        parseRawData(s);
        if (mDataLoadListener != null) {
            mDataLoadListener.onDateLoadCompleted();
        }
    }

    @Override
    public void onFailure(String s) {
        Log.d(TAG, "get weather data onFailure !!!!!!");
        if (mDataLoadListener != null) {
            mDataLoadListener.onLoadFailed();
        }
    }
}
