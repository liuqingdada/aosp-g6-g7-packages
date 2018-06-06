package com.mstarc.watchservice.utils;

import android.content.Context;
import android.content.Intent;

import com.mstarc.commonbase.communication.bluetooth.ble.periphery.Advertiser;
import com.mstarc.commonbase.communication.message.RequestCode;
import com.mstarc.commonbase.communication.message.transmite.BlackAndWhiteList;
import com.mstarc.commonbase.communication.message.transmite.Contact;
import com.mstarc.commonbase.communication.message.transmite.PowerStep;
import com.mstarc.commonbase.communication.message.transmite.ScheduleMessage;
import com.mstarc.commonbase.communication.message.transmite.SportHealth;
import com.mstarc.commonbase.communication.message.transmite.Weather;
import com.mstarc.commonbase.communication.utils.Constant;
import com.mstarc.commonbase.database.bean.NotificationList;
import com.mstarc.commonbase.database.bean.PhoneType;
import com.mstarc.commonbase.database.bean.Switch;
import com.mstarc.commonbase.database.bean.UserInfo;
import com.mstarc.commonbase.database.bean.WaterClock;

import java.util.List;

/**
 * Created by liuqing
 * 2017/4/17.
 * Email: 1239604859@qq.com
 */

public class PushUtils {
    private Context mContext;


    public PushUtils(Context context) {
        mContext = context;
    }

    /**
     * 分析String请求
     *
     * @param requestCode {@link RequestCode}
     */
    public void analyse(String requestCode) {
        Intent intent = new Intent(Constant.SETTINGS_ACTION);
        intent.setFlags(Context.BIND_NOT_FOREGROUND);
        Intent pairIntent = new Intent(Constant.PAIR_ACTION);
        pairIntent.setFlags(Context.BIND_NOT_FOREGROUND);
        boolean oneSwitch;

        switch (requestCode) {
            case RequestCode.IMEI:
                Advertiser.getInstance()
                          .sendMessage(GreenDaoUtils.getInstance()
                                                    .getDeviceAddress());
                break;

            case RequestCode.GETALL_NOTIFICATION_LIST:
                BlackAndWhiteList blackAndWhiteList = GreenDaoUtils.getInstance()
                                                                   .getAllNotificationList();
                Advertiser.getInstance()
                          .sendMessage(blackAndWhiteList);
                break;

            case RequestCode.WATCH_POWER_STEP:
                intent.putExtra(Constant.SETTING, RequestCode.WATCH_POWER_STEP);
                mContext.sendBroadcast(intent);
                break;

            // ***************************************************************************

            case RequestCode.SWITCHES_ALL:
                List<Switch> switchList = GreenDaoUtils.getInstance()
                                                       .getAllSwitch();
                for (Switch aSwitch : switchList) {
                    Advertiser.getInstance()
                              .sendMessage(aSwitch);
                }
                break;

            case RequestCode.SMART_REMINDER:
                oneSwitch = updateOneSwitch(requestCode);
                intent.putExtra(Constant.SETTING, RequestCode.SMART_REMINDER);
                intent.putExtra(Constant.SWITCH, oneSwitch);
                mContext.sendBroadcast(intent);

                break;

            case RequestCode.CONSTANT_LIGHT:
                updateOneSwitch(requestCode);
                break;

            case RequestCode.CLICK_SCREEN_LIGHT:
                updateOneSwitch(requestCode);
                break;

            case RequestCode.MOBILE_SCREEN_ON_NOT_PUSH:
                updateOneSwitch(requestCode);
                break;

            case RequestCode.MESSAGE_PUSH:
                updateOneSwitch(requestCode);
                break;

            case RequestCode.SMART_PUSH:
                updateOneSwitch(requestCode);
                break;

            case RequestCode.SCHEDULE: // 本地提醒
                oneSwitch = updateOneSwitch(requestCode);
                intent.putExtra(Constant.SETTING, RequestCode.SCHEDULE);
                intent.putExtra(Constant.SWITCH, oneSwitch);
                mContext.sendBroadcast(intent);
                break;

            case RequestCode.SEDENTARINESS: // 本地提醒
                oneSwitch = updateOneSwitch(requestCode);
                intent.putExtra(Constant.SETTING, RequestCode.SEDENTARINESS);
                intent.putExtra(Constant.SWITCH, oneSwitch);
                mContext.sendBroadcast(intent);
                break;

            case RequestCode.SWEET_WORDS: // 本地提醒
                oneSwitch = updateOneSwitch(requestCode);
                intent.putExtra(Constant.SETTING, RequestCode.SWEET_WORDS);
                intent.putExtra(Constant.SWITCH, oneSwitch);
                mContext.sendBroadcast(intent);
                break;

            case RequestCode.MOBILE_AWAY_BODY_REMIND: // 本地提醒 (蓝牙断开10分钟提醒一次)
                updateOneSwitch(requestCode);

                break;

            case RequestCode.COMMUNICATION_SERVICES:
                updateOneSwitch(requestCode);
                break;

            case RequestCode.MOBILE_DATA:
                updateOneSwitch(requestCode);
                break;

            case RequestCode.SERVICE_PASSWORD:
                updateOneSwitch(requestCode);
                break;

            case RequestCode.SWAY_WRIST_LOCK_SCREEN:
                updateOneSwitch(requestCode);
                break;

            // ***************************************************************************

            case RequestCode.WATER_CLOCK:
                Advertiser.getInstance()
                          .sendMessage(GreenDaoUtils.getInstance()
                                                    .getWaterClock());
                break;

            case RequestCode.REMOVEALL_SCHEDULE:
                GreenDaoUtils.getInstance()
                             .removeAllSchedule();
                break;

            default: break;

            // ***************************************************************************

            case RequestCode.FIRST_CONNECTED:
                pairIntent.putExtra("step", 1);
                mContext.sendBroadcast(pairIntent);
                break;

            case RequestCode.PASSWORD:
                pairIntent.putExtra("step", 2);
                mContext.sendBroadcast(pairIntent);
                break;

            case RequestCode.START_SYNC_DATA:
                pairIntent.putExtra("step", 3);
                mContext.sendBroadcast(pairIntent);

            case RequestCode.FINISHED_SYNC_DATA:
                pairIntent.putExtra("step", 4);
                mContext.sendBroadcast(pairIntent);
        }
    }

    /**
     * 分析消息白名单
     *
     * @param notificationList {@link NotificationList}
     */
    public void analyse(NotificationList notificationList) {
        switch (notificationList.getType()) {
            case 0: // add
                NotificationList nl = new NotificationList(null, notificationList.getAppName(),
                                                           notificationList.getAppPkg(),
                                                           notificationList.getIsBlacklist());
                GreenDaoUtils.getInstance()
                             .addNotificationList(nl);
                break;

            case 1: // remove
                GreenDaoUtils.getInstance()
                             .removeNotificationList(notificationList.getAppPkg());
                break;

            case 2: // update
                GreenDaoUtils.getInstance()
                             .updateNotificationList(notificationList);
                break;

            default: break;
        }
    }

    private boolean updateOneSwitch(String requestCode) {
        boolean result = GreenDaoUtils.getInstance()
                                      .updateOneSwitch(requestCode);
        Advertiser.getInstance()
                  .sendMessage(RequestCode.OK + requestCode);
        return result;
    }

    /**
     * 分析喝水提醒
     *
     * @param waterClock {@link WaterClock}
     */
    public void analyse(WaterClock waterClock) { // 本地提醒
        GreenDaoUtils.getInstance()
                     .updateWaterClock(waterClock);

        Intent intent = new Intent(Constant.SETTINGS_ACTION);
        intent.putExtra(Constant.SETTING, RequestCode.WATER_CLOCK);
        intent.putExtra(Constant.WATER_CLOCK, waterClock);
        mContext.sendBroadcast(intent);
    }

    /**
     * 分析日程数据
     */
    public void analyse(ScheduleMessage scheduleMessage) {
        GreenDaoUtils.getInstance()
                     .addSchedule(scheduleMessage);
    }

    /**
     * 分析联系人
     *
     * @param contact {@link Contact}
     */
    public void analyse(Contact contact) {
        switch (contact.getType()) {
            case 0: // add
                GreenDaoUtils.getInstance()
                             .addContact(contact);
                break;

            case 1: // remove
                GreenDaoUtils.getInstance()
                             .removeContact(contact);
                break;

            default: break;
        }
    }

    /**
     * @param userInfo {@link UserInfo}
     */
    public void analyse(UserInfo userInfo) {
        GreenDaoUtils.getInstance()
                     .updateUserInfo(userInfo);
    }

    /**
     * @param phoneType {@link PhoneType}
     */
    public void analyse(PhoneType phoneType) {
        GreenDaoUtils.getInstance()
                     .updatePhoneType(phoneType);

        if (phoneType.getSystemType()
                     .equals(PhoneType.iPhone)) {
            Advertiser.getInstance()
                      .startANCS();
        }
    }

    public void analyse(PowerStep powerStep) {

    }

    public void analyse(SportHealth sportHealth) {

    }

    public void analyse(Weather weather) {

    }
}
