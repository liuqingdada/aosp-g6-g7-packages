package com.mstarc.g6.watchface.weather;

/**
 * description
 * <p>
 * Created by andyding on 2017/3/10.
 */
public class Weather {

    // locationName
    private String mLocationName;

    // current location Or home
    private int mLocationProperty;

    // time of location
    private String mLocationTime;

    // date of location
    private String mLocationDate;

    // Temperature of location
    private String mTemperature;

    // type of current weather
    private int mWeatherType;

    // name of current weather
    private String mWeatherName;

    // type of next day weather
    private int mNextOneDayWeatherType;
    // type of next two weather
    private int mNextTwoDayWeatherType;
    // type of next three weather
    private int mNextThreeDayWeatherType;

    // Temperature range of next day
    private String mNextOneDayTemperature;
    // Temperature range of next two day
    private String mNextTwoDayTemperature;
    // Temperature range of next three day
    private String mNextThreeDayTemperature;

    // week day of next day
    private String mNextOneDayWeekDay;
    // week day of next two day
    private String mNextTwoDayWeekDay;
    // week day  of next three day
    private String mNextThreeDayWeekDaye;

    // quality of weather
    private int mWeatherQuality;

    private String mWindDirection;
    private String mWindScale;
    private String mLunarDate;
    private String mLunarJieqi = "";

    public void setLocationType(int type) {
        mLocationProperty = type;
    }

    public void setWindDirection(String windDirection) {
        mWindDirection = windDirection;
    }

    public String getWindDirection() {
        return  mWindDirection;
    }
    public void setWindScale(String windScale) {
        mWindScale = windScale;
    }

    public String getWindScale() {
        return  mWindScale;
    }
    public void setLunarDate(String lunarDate) {
        mLunarDate = lunarDate;
    }

    public String getLunarDate() {
        return  mLunarDate;
    }
    public void setLunarJieqi(String lunarJieqi) {
        mLunarJieqi = lunarJieqi;
    }

    public String getLunarJieqi() {
        return  mLunarJieqi;
    }

    public int getLocationType() {
        return mLocationProperty;
    }

    public void setLocationName(String name) {
        mLocationName = name;
    }

    public String getLocationName() {
        return mLocationName;
    }

    public void setLocationTime(String time) {
        mLocationTime = time;
    }

    public String getLocationTime() {
        return  mLocationTime;
    }

    public void setLocationDate(String time) {
        mLocationDate = time;
    }

    public String getLocationDate() {
        return  mLocationDate;
    }

    public void setTemperature(String tmp) {
        mTemperature = tmp;
    }

    public String getTemperature() {
        return mTemperature;
    }

    public void setWeatherName(String name) {
        mWeatherName = name;
    }

    public String getWeatherName() {
        return mWeatherName;
    }

    public void setWeatherType(int type) {
        mWeatherType = type;
    }

    public int getWeatherType() {
        return mWeatherType;
    }

    public void setNextOneDayWeatherType(int type) {
        mNextOneDayWeatherType = type;
    }

    public int getNextOneDayWeatherType() {
        return mNextOneDayWeatherType;
    }

    public void setNextTwoDayWeatherType(int type) {
        mNextTwoDayWeatherType = type;
    }

    public int getNextTwoDayWeatherType() {
        return mNextTwoDayWeatherType;
    }

    public void setNextThreeDayWeatherType(int type) {
        mNextThreeDayWeatherType = type;
    }

    public int getNextThreeDayWeatherType() {
        return mNextThreeDayWeatherType;
    }

    public void setNextOneDayTmp(String tmp) {
        mNextOneDayTemperature = tmp;
    }

    public String getNextOneDayTmp() {
        return mNextOneDayTemperature;
    }

    public void setNextTwoDayTmp(String tmp) {
        mNextTwoDayTemperature = tmp;
    }

    public String getNextTwoDayTmp() {
        return mNextTwoDayTemperature;
    }

    public void setNextThreeDayTmp(String tmp) {
        mNextThreeDayTemperature = tmp;
    }

    public String getNextThreeDayTmp() {
        return mNextThreeDayTemperature;
    }

    public void setWeatherQuality(int quality) {
        mWeatherQuality = quality;
    }

    public int getWeatherQuality() {
        return mWeatherQuality;
    }

    public void setNextOneDayWeekDay(String day) {
        mNextOneDayWeekDay = day;
    }

    public String getNextOneDayWeekDay() {
        return mNextOneDayWeekDay;
    }

    public void setNextTwoDayWeekDay(String day) {
        mNextTwoDayWeekDay = day;
    }

    public String getNextTwoDayWeekDay() {
        return mNextTwoDayWeekDay;
    }

    public void setNextThreeDayWeekDay(String day) {
        mNextThreeDayWeekDaye = day;
    }

    public String getNextThreeDayWeekDay() {
        return mNextThreeDayWeekDaye;
    }
}
