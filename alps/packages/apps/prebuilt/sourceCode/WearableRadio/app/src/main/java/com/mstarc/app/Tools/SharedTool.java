package com.mstarc.app.Tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.Map;

public class SharedTool {
    static SharedTool instance;
    private final String CURR_RADIO_INDEX_KEY = "curr_radio_index";
    private final String SHARED_LOCATION = "radio_other_shared";
    private final String PROVINCE_SHARED_LOCATION = "province_type_shared";
    private final String DISTRICT_SHARED_LOCATION = "district_radio_shared";

    public static SharedTool getInstance() {
        return instance == null ? new SharedTool() : instance;
    }

    /**
     * 取当前频道列表位置
     *
     * @param context
     * @return
     */
    public int getIntSharedInfoByKey(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        return sharedPreferences.getInt(CURR_RADIO_INDEX_KEY, 0);
    }

    /**
     * 存当前频道
     *
     * @param context
     * @param value
     */
    public void editIntSharedInfo(Context context, int value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        Editor editor = sharedPreferences.edit();// 用于写入
        editor.putInt(CURR_RADIO_INDEX_KEY, value);
        editor.commit();
    }

    public Map<String, Integer> getAllProvinceTypeInfo(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PROVINCE_SHARED_LOCATION, 0);
        return (Map<String, Integer>) sharedPreferences.getAll();
    }

    public Map<String, Integer> getAllDIstrictRadioInfo(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(DISTRICT_SHARED_LOCATION, 0);
        return (Map<String, Integer>) sharedPreferences.getAll();
    }

    /**
     * 获取省台分类id
     *
     * @param context
     * @param sharedKey
     * @return
     */
    public int getIntSharedInfoByKey(Context context, String sharedKey) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PROVINCE_SHARED_LOCATION, 0);
        return sharedPreferences.getInt(sharedKey, 0);
    }

    /**
     * 存储省台分类id
     *
     * @param context
     * @param sharedKey
     * @param value
     */
    public void editIntSharedInfo(Context context, String sharedKey, int value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PROVINCE_SHARED_LOCATION, 0);
        Editor editor = sharedPreferences.edit();// 用于写入
        editor.putInt(sharedKey, value);
        editor.commit();
    }

    /**
     * 当位置换了时要清空一下
     *
     * @param context
     */
    public void clearDistrictRadioShared(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(DISTRICT_SHARED_LOCATION, 0);
        Editor editor = sharedPreferences.edit();// 用于写入
        editor.clear();
        editor.commit();
    }
    //
    // /**
    // * 获取区台id
    // *
    // * @param context
    // * @param sharedKey
    // * @return
    // */
    // public int getDistrictRadioSharedInfoByKey(Context context, String
    // sharedKey)
    // {
    // SharedPreferences sharedPreferences =
    // context.getSharedPreferences(DISTRICT_SHARED_LOCATION, 0);
    // return sharedPreferences.getInt(sharedKey, 0);
    // }
    //
    // /**
    // * 存储区台id
    // *
    // * @param context
    // * @param sharedKey
    // * @param value
    // */
    // public void editDistrictRadioSharedInfo(Context context, String
    // sharedKey, int value)
    // {
    // SharedPreferences sharedPreferences =
    // context.getSharedPreferences(DISTRICT_SHARED_LOCATION, 0);
    // Editor editor = sharedPreferences.edit();// 用于写入
    // editor.putInt(sharedKey, value);
    // editor.commit();
    // }

    /**
     * 获取缓存日期/所属地区
     *
     * @param context
     * @param key
     * @return
     */
    public String getSharedDateOrBelongInfo(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        String value = sharedPreferences.getString(key, "");
        return value;
    }

    /**
     * 编辑缓存日期/所属地区
     *
     * @param context
     * @param key
     * @param value
     */
    public void editSharedDateOrBelongInfo(Context context, String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        Editor editor = sharedPreferences.edit();// 用于写入
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * 获取当前播放状态
     *
     * @param context
     * @return
     */
    public String getSharedPlayInfo(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        String value = sharedPreferences.getString("play_info", "");
        return value;
    }

    /**
     * 记录当前播放状态
     *
     * @param context
     * @param value
     */
    public void editSharedPlayInfo(Context context, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        Editor editor = sharedPreferences.edit();// 用于写入
        editor.putString("play_info", value);
        editor.commit();
    }

    public void savePlayState(Context context, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        Editor editor = sharedPreferences.edit();// 用于写入
        editor.putBoolean("play_state", value);
        editor.commit();
    }

    public boolean getPlayState(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        boolean value = sharedPreferences.getBoolean("play_state", true);
        return value;
    }

    public void clearSharedLocationInfo(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_LOCATION, 0);
        Editor editor = sharedPreferences.edit();// 用于写入
        editor.clear();
        editor.commit();
    }

    public void clearAllSharedInfo(Context context) {
        clearDistrictRadioShared(context);
        clearSharedLocationInfo(context);
    }

    /**
     * @param context
     * @param provinceN
     * @return -1没存过获取过；-2台湾，香港，澳门等不包含地区；>0可获取地区
     */
    public int getProvinceIdFromShared(Context context, String provinceN) {
        Map<String, Integer> provinceType = getAllProvinceTypeInfo(context);
        if (provinceType == null)
            return -1;
        for (String provinceName : provinceType.keySet()) {
            if (provinceName.equals(provinceN))
                return provinceType.get(provinceName);
        }
        return -2;
    }
}
