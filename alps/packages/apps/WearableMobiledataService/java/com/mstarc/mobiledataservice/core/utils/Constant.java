package com.mstarc.mobiledataservice.core.utils;

/**
 * Created by liuqing
 * 2017/4/18.
 * Email: 1239604859@qq.com
 */

public interface Constant {
    // 开关等
    String SETTINGS_ACTION = "com.mstarc.watchservice.SETTINGS";
    String SETTING = "setting";
    String SWITCH = "switch";

    String WATER_CLOCK = "water_clock";

    // OTA
    String OTA_ACTION = "com.mstarc.watchbase.otaclock";

    // 通知
    String NOTIFICATION_ACTION = "com.mstarc.watch.action.notification";
    String FUNCTION = "function";
    String CONTENT = "content";
    String SEDENTARINESS = "久坐";
    String FROM_THE_WRIST = "离腕";
    String DRINK_WATER = "喝水";
    String SCHEDULE = "日程";
    String SWEET_WORDS = "，蜜语";
    String RAISE_HAND = "抬手";
    String OBTAIN_HANDS_DOWN = "垂手";
    String STEP = "计步";

    // 第一次配对连接
    String FTE = "step";
    String PAIR_ACTION = "com.mstarc.fte.step";
    String PWD_OK_ACTION = "com.mstarc.fte.pw.finish";

    // userinfo
    String USERINFO_ADDRESS = "userinfo_address";

    // 电量
    String BATTERY = "watch_battery";
    String STEP_NUM = "step_number";

    // 蓝牙定位
    String BT_LOCATION_KEY = "phone_location_ok";
    String BT_LOCATION = "com.mstarc.bt.location"; // 腕表应用发心跳
    String BT_LOCATION_OK = "com.mstarc.bt.location.ok"; // 得到手机的定位

    // 时间
    String TIME_KEY = "watch_time";
    String WATCH_TIME = "com.mstarc.watch.time";
}
