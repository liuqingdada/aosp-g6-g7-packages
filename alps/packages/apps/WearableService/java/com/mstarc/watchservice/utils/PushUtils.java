package com.mstarc.watchservice.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.mstarc.commonbase.application.SystemTimeUtils;
import com.mstarc.commonbase.communication.bluetooth.ble.periphery.Advertiser;
import com.mstarc.commonbase.communication.bluetooth.utils.BtUtils;
import com.mstarc.commonbase.communication.bluetooth.utils.FileUtils;
import com.mstarc.commonbase.communication.jpush.utils.NetWorkUtils;
import com.mstarc.commonbase.communication.message.RequestCode;
import com.mstarc.commonbase.communication.message.transmite.BlackAndWhiteList;
import com.mstarc.commonbase.communication.message.transmite.Contact;
import com.mstarc.commonbase.communication.message.transmite.NetworkType;
import com.mstarc.commonbase.communication.message.transmite.PowerStep;
import com.mstarc.commonbase.communication.message.transmite.ScheduleMessage;
import com.mstarc.commonbase.communication.utils.Constant;
import com.mstarc.commonbase.database.bean.NotificationList;
import com.mstarc.commonbase.database.bean.PhoneType;
import com.mstarc.commonbase.database.bean.Switch;
import com.mstarc.commonbase.database.bean.UserInfo;
import com.mstarc.commonbase.database.bean.WaterClock;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import mstarc_os_api.mstarc_os_api_msg;

/**
 * Created by liuqing
 * 2017/4/17.
 * Email: 1239604859@qq.com
 */

public class PushUtils {
    private final String TAG = "PushUtils";
    private Context mContext;

    private mstarc_os_api_msg m_api_msg;
    private boolean isApiConnect;

    private final File mPath = new File(Environment.getExternalStorageDirectory(),
                                        "/Android/data/bleFile");

    public PushUtils(Context context) {
        mContext = context.getApplicationContext();
        m_api_msg = new mstarc_os_api_msg(mContext) {
            @Override
            public void onServiceConnected() {
                super.onServiceConnected();
                isApiConnect = true;
            }
        };
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

        Log.v(TAG, "analyse - String: " + requestCode);
        switch (requestCode) {
            case RequestCode.IMEI:
                Log.v(TAG, GreenDaoUtils.getInstance()
                                        .getDeviceAddress()
                                        .toString());
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
                String step = BtUtils.getString(mContext, Constant.STEP_NUM, "0");
                String powerPercent = BtUtils.getString(mContext, Constant.BATTERY, "0%");
                Advertiser.getInstance()
                          .sendMessage(new PowerStep(powerPercent, step));
                break;

            case RequestCode.NETWORK_TYPE:
                NetworkType networkType = getNetworkType();
                Advertiser.getInstance()
                          .sendMessage(networkType);
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
                break;

            case RequestCode.FINISHED_SYNC_DATA:
                pairIntent.putExtra("step", 4);
                mContext.sendBroadcast(pairIntent);
                break;
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

        Intent intent = new Intent();
        intent.setAction(Constant.SETTINGS_ACTION);
        intent.putExtra(Constant.USERINFO_ADDRESS, userInfo.getAddress());
        mContext.sendBroadcast(intent);

        if (userInfo.getCurrentTimeMillis() != 0) {
            SystemTimeUtils.setAutoDateTime(mContext, 1);
            SystemTimeUtils.setAutoTimeZone(mContext, 1);

            SystemTimeUtils.setSysTime(mContext, userInfo.getCurrentTimeMillis());
        }

        FileUtils.createFile(userInfo.getUserId()
                                     .getBytes(), mPath.getAbsolutePath(), "userid.file");

        if (isApiConnect) {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int b = Integer.valueOf(userInfo.getBirth()
                                            .substring(0, 4));
            char age = (char) (year - b);

            char gender;
            if (userInfo.getSex()
                        .equals("男")) {
                gender = 'M';
            } else {
                gender = 'F';
            }

            m_api_msg.mstarc_api_set_sensorhub_jb((float) userInfo.getHeight(),
                                                  (float) userInfo.getWeight(), age, gender, 'L');
        }
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
        Intent intent = new Intent();
        intent.putExtra("phonepower", powerStep.getWatchPower());
        intent.setAction("com.mstarc.notificationwizard.phonepower");
        mContext.sendBroadcast(intent);
    }

    /**
     * 0  无网络
     * 1  流量
     * 2  WiFi
     */
    private NetworkType getNetworkType() {
        NetworkType networkType = new NetworkType();

        if (NetWorkUtils.isNetworkConnected(mContext)) {

            if (NetWorkUtils.isWifiConnected(mContext)) {
                networkType.setType(2);

            } else if (NetWorkUtils.isMobileConnected(mContext)) {
                networkType.setType(1);
            }

        } else {
            networkType.setType(0);
        }
        return networkType;
    }

    public void onDestroy() {
        if (m_api_msg != null) {
            m_api_msg.unbindService();
            m_api_msg = null;
        }
    }
}
