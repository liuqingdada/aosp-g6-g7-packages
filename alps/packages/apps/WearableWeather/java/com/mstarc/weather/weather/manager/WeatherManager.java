package com.mstarc.weather.weather.manager;

import android.content.Context;
import android.util.Log;

import com.mstarc.watchbase.weather.IWeather;
import com.mstarc.weather.weather.R;
import com.mstarc.weather.weather.Utils.WeatherUtils;
import com.mstarc.weather.weather.Weather;

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


    public int getLocationIcon(int locationProperty) {
        switch (locationProperty) {
            case WeatherUtils.LOCATION_CURRENT:
                return R.mipmap.icon_weizhi;
            case WeatherUtils.LOCATION_HOME:
                return R.mipmap.icon_changzhudi;
        }
        return -1;
    }

    /**
     * return resid based on weather type
     */
    public int getWeatherBasedonType(int type, boolean big) {
        switch (type) {
            case 0:
                if (big) {
                    return R.mipmap.icon_weather_0;
                } else {
                    return R.mipmap.icon_weather_small_0;
                }
            case 1:
                if (big) {
                    return R.mipmap.icon_weather_1;
                } else {
                    return R.mipmap.icon_weather_small_1;
                }
            case 2:
                if (big) {
                    return R.mipmap.icon_weather_2;
                } else {
                    return R.mipmap.icon_weather_small_2;
                }
            case 3:
                if (big) {
                    return R.mipmap.icon_weather_3;
                } else {
                    return R.mipmap.icon_weather_small_3;
                }
            case 4:
                //case WeatherUtils.TYPE_WEATHER_QIANGSHACHENBAO:
                if (big) {
                    return R.mipmap.icon_weather_4;
                } else {
                    return R.mipmap.icon_weather_small_4;
                }
            case 5:
                if (big) {
                    return R.mipmap.icon_weather_5;
                } else {
                    return R.mipmap.icon_weather_small_5;
                }
            case 6:
                if (big) {
                    return R.mipmap.icon_weather_6;
                } else {
                    return R.mipmap.icon_weather_small_6;
                }
            case 7:
                //case WeatherUtils.TYPE_WEATHER_DAXUEBAOXUE:
                //case WeatherUtils.TYPE_WEATHER_BAOXUE:
                if (big) {
                    return R.mipmap.icon_weather_7;
                } else {
                    return R.mipmap.icon_weather_small_7;
                }
            case 8:
                //case WeatherUtils.TYPE_WEATHER_ZHONGXUEDAXUE:
                if (big) {
                    return R.mipmap.icon_weather_8;
                } else {
                    return R.mipmap.icon_weather_small_8;
                }
            case 9:
                //case WeatherUtils.TYPE_WEATHER_ZHENXUE:
                //case WeatherUtils.TYPE_WEATHER_XIAOXUEZHONGXUE:
                if (big) {
                    return R.mipmap.icon_weather_9;
                } else {
                    return R.mipmap.icon_weather_small_9;
                }
            case 10:
                //case WeatherUtils.TYPE_WEATHER_LEIZHENYUBINGBAO:
                if (big) {
                    return R.mipmap.icon_weather_10;
                } else {
                    return R.mipmap.icon_weather_small_10;
                }
            case 11:
                if (big) {
                    return R.mipmap.icon_weather_11;
                } else {
                    return R.mipmap.icon_weather_small_11;
                }
            case 12:
                //case WeatherUtils.TYPE_WEATHER_DABAOYU:
                //case WeatherUtils.TYPE_WEATHER_TEDABAOYU:
                //case WeatherUtils.TYPE_WEATHER_BAOYUDABAOYU:
                //case WeatherUtils.TYPE_WEATHER_DABAOYUTEDABAOYU:
                if (big) {
                    return R.mipmap.icon_weather_12;
                } else {
                    return R.mipmap.icon_weather_small_12;
                }
            case 13:
                //case WeatherUtils.TYPE_WEATHER_DAYUBAOYU:
                if (big) {
                    return R.mipmap.icon_weather_13;
                } else {
                    return R.mipmap.icon_weather_small_13;
                }
            case 14:
                //case WeatherUtils.TYPE_WEATHER_ZHONGYUDAYU:
                if (big) {
                    return R.mipmap.icon_weather_14;
                } else {
                    return R.mipmap.icon_weather_small_14;
                }
            case 15:
                //case WeatherUtils.TYPE_WEATHER_XIAOYUZHONGYU:
                if (big) {
                    return R.mipmap.icon_weather_15;
                } else {
                    return R.mipmap.icon_weather_small_15;
                }
            case 16:
                if (big) {
                    return R.mipmap.icon_weather_16;
                } else {
                    return R.mipmap.icon_weather_small_16;
                }
            case 17:
                if (big) {
                    return R.mipmap.icon_weather_17;
                } else {
                    return R.mipmap.icon_weather_small_17;
                }
            case 18:
                if (big) {
                    return R.mipmap.icon_weather_18;
                } else {
                    return R.mipmap.icon_weather_small_18;
                }
            case 19:
                if (big) {
                    return R.mipmap.icon_weather_19;
                } else {
                    return R.mipmap.icon_weather_small_19;
                }
            case 20:
                if (big) {
                    return R.mipmap.icon_weather_20;
                } else {
                    return R.mipmap.icon_weather_small_20;
                }
            case 21:
                if (big) {
                    return R.mipmap.icon_weather_21;
                } else {
                    return R.mipmap.icon_weather_small_21;
                }
            case 22:
                if (big) {
                    return R.mipmap.icon_weather_22;
                } else {
                    return R.mipmap.icon_weather_small_22;
                }
            case 23:
                if (big) {
                    return R.mipmap.icon_weather_23;
                } else {
                    return R.mipmap.icon_weather_small_23;
                }
            case 24:
                if (big) {
                    return R.mipmap.icon_weather_24;
                } else {
                    return R.mipmap.icon_weather_small_24;
                }
            case 25:
                if (big) {
                    return R.mipmap.icon_weather_25;
                } else {
                    return R.mipmap.icon_weather_small_25;
                }
            case 26:
                if (big) {
                    return R.mipmap.icon_weather_26;
                } else {
                    return R.mipmap.icon_weather_small_26;
                }
            case 27:
                if (big) {
                    return R.mipmap.icon_weather_27;
                } else {
                    return R.mipmap.icon_weather_small_27;
                }
            case 28:
                if (big) {
                    return R.mipmap.icon_weather_28;
                } else {
                    return R.mipmap.icon_weather_small_28;
                }

            case 29:
                if (big) {
                    return R.mipmap.icon_weather_29;
                } else {
                    return R.mipmap.icon_weather_small_29;
                }
            case 30:
                if (big) {
                    return R.mipmap.icon_weather_30;
                } else {
                    return R.mipmap.icon_weather_small_30;
                }
            case 31:
                if (big) {
                    return R.mipmap.icon_weather_31;
                } else {
                    return R.mipmap.icon_weather_small_31;
                }
            case 32:
                if (big) {
                    return R.mipmap.icon_weather_32;
                } else {
                    return R.mipmap.icon_weather_small_32;
                }


            case 49:
                if (big) {
                    return R.mipmap.icon_weather_49;
                } else {
                    return R.mipmap.icon_weather_small_49;
                }
            case 53:
                if (big) {
                    return R.mipmap.icon_weather_53;
                } else {
                    return R.mipmap.icon_weather_small_53;
                }
            case 54:
                if (big) {
                    return R.mipmap.icon_weather_54;
                } else {
                    return R.mipmap.icon_weather_small_54;
                }
            case 55:
                if (big) {
                    return R.mipmap.icon_weather_55;
                } else {
                    return R.mipmap.icon_weather_small_55;
                }
            case 56:
                if (big) {
                    return R.mipmap.icon_weather_56;
                } else {
                    return R.mipmap.icon_weather_small_56;
                }
            case 57:
                if (big) {
                    return R.mipmap.icon_weather_57;
                } else {
                    return R.mipmap.icon_weather_small_57;
                }

            case 58:
                if (big) {
                    return R.mipmap.icon_weather_58;
                } else {
                    return R.mipmap.icon_weather_small_58;
                }
            case 99:
                if (big) {
                    return R.mipmap.icon_weather_99;
                } else {
                    return R.mipmap.icon_weather_small_99;
                }
            case 301:
                if (big) {
                    return R.mipmap.icon_weather_301;
                } else {
                    return R.mipmap.icon_weather_small_301;
                }

            case 302:
                if (big) {
                    return R.mipmap.icon_weather_302;
                } else {
                    return R.mipmap.icon_weather_small_302;
                }

            default:
                return -1;
        }
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

    public int getIconBasedOnQuality(int weatherQuality) {
        if (weatherQuality > 0 && weatherQuality <= 50) {
            return R.mipmap.icon_kongqi_you;
        } else if (weatherQuality > 50 && weatherQuality <= 100) {
            return R.mipmap.icon_kongqi_liang;
        } else if (weatherQuality > 100 && weatherQuality <= 150) {
            return R.mipmap.icon_kongqi_qingdu;
        } else if (weatherQuality > 150 && weatherQuality <= 200) {
            return R.mipmap.icon_kongqi_zhongdu_small;
        } else if (weatherQuality > 200 && weatherQuality <= 300) {
            return R.mipmap.icon_kongqi_zhongdu_big;
        } else if (weatherQuality > 300) {
            return R.mipmap.icon_kongqi_yanzhong;
        }
        return R.mipmap.icon_kongqi_you;
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
