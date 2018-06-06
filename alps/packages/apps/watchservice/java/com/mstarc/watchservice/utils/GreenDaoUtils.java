package com.mstarc.watchservice.utils;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.mstarc.commonbase.communication.bluetooth.ble.periphery.Advertiser;
import com.mstarc.commonbase.communication.bluetooth.traditional.BluetoothPush;
import com.mstarc.commonbase.communication.message.RequestCode;
import com.mstarc.commonbase.communication.message.transmite.BlackAndWhiteList;
import com.mstarc.commonbase.communication.message.transmite.Contact;
import com.mstarc.commonbase.communication.message.transmite.ScheduleMessage;
import com.mstarc.commonbase.database.DatabaseWizard;
import com.mstarc.commonbase.database.bean.DeviceAddress;
import com.mstarc.commonbase.database.bean.NotificationList;
import com.mstarc.commonbase.database.bean.PhoneType;
import com.mstarc.commonbase.database.bean.Switch;
import com.mstarc.commonbase.database.bean.UserInfo;
import com.mstarc.commonbase.database.bean.WaterClock;
import com.mstarc.commonbase.database.greendao.DaoSession;
import com.mstarc.commonbase.database.greendao.NotificationListDao;
import com.mstarc.commonbase.schedule.Schedule;
import com.mstarc.commonbase.schedule.ScheduleData;
import com.mstarc.watchbase.contact.ContactUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuqing
 * 2017/4/17.
 * Email: 1239604859@qq.com
 */

public class GreenDaoUtils {
    private static GreenDaoUtils sGreenDaoUtils = new GreenDaoUtils();

    private DaoSession mDaoSession;
    private boolean isScheduleInit;
    private Context mContext;
    private ExecutorService asyncThreadPool = Executors.newFixedThreadPool(32);

    private GreenDaoUtils() {
    }

    public static GreenDaoUtils getInstance() {
        return sGreenDaoUtils;
    }

    public void init(Context context) {
        mContext = context;
        // 数据库
        mDaoSession = DatabaseWizard.getInstance()
                                    .getDaoSession();

        // IMEI
        List<DeviceAddress> deviceAddresses = mDaoSession.getDeviceAddressDao()
                                                         .loadAll();
        if (deviceAddresses == null || deviceAddresses.size() == 0) {
            String localMacAddress = BluetoothPush.getInstance()
                                                  .getLocalMacAddress();
            TelephonyManager tm = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);
            String deviceId = tm.getDeviceId();
            String simSerialNumber = tm.getSimSerialNumber();

            DeviceAddress deviceAddress = new DeviceAddress(null, "", localMacAddress, deviceId,
                                                            simSerialNumber, "", "", "");
            mDaoSession.getDeviceAddressDao()
                       .insertInTx(deviceAddress);
        }

        // Switch
        List<Switch> switches = mDaoSession.getSwitchDao()
                                           .loadAll();
        if (switches == null || switches.size() == 0) {
            Switch watchSwitch = new Switch();

            mDaoSession.getSwitchDao()
                       .insertInTx(watchSwitch);
        }

        // WaterClock
        List<WaterClock> waterClocks = mDaoSession.getWaterClockDao()
                                                  .loadAll();
        if (waterClocks == null || waterClocks.size() == 0) {
            WaterClock waterClock = new WaterClock();

            mDaoSession.getWaterClockDao()
                       .insertInTx(waterClock);
        }

        // Schedule
        isScheduleInit = Schedule.getInstance()
                                 .init(context);
    }

    //****************************** DeviceAddress **************************************

    DeviceAddress getDeviceAddress() {
        return mDaoSession.getDeviceAddressDao()
                          .load(1L);
    }

    //****************************** NotificationList **************************************

    BlackAndWhiteList getAllNotificationList() {
        return new BlackAndWhiteList(mDaoSession.getNotificationListDao()
                                                .loadAll());
    }

    void addNotificationList(NotificationList notificationList) {
        NotificationList nl = mDaoSession.getNotificationListDao()
                                         .queryBuilder()
                                         .where(NotificationListDao.Properties.AppPkg.eq(
                                                 notificationList.getAppPkg()))
                                         .build()
                                         .unique();
        if (nl == null) {
            mDaoSession.getNotificationListDao()
                       .insertInTx(notificationList);
        } else {
            if (!nl.getAppPkg()
                   .equals(notificationList.getAppPkg())) {
                mDaoSession.getNotificationListDao()
                           .insertInTx(notificationList);
            }
        }
    }

    void removeNotificationList(String pkgName) {
        List<NotificationList> notificationLists = mDaoSession
                .getNotificationListDao()
                .queryBuilder()
                .where(NotificationListDao.Properties.AppPkg.eq(
                        pkgName))
                .build()
                .list();
        if (notificationLists != null && notificationLists.size() > 0) {
            mDaoSession.getNotificationListDao()
                       .deleteInTx(notificationLists);
        }
    }

    void updateNotificationList(NotificationList notificationList) {
        NotificationList nl = mDaoSession.getNotificationListDao()
                                         .queryBuilder()
                                         .where(NotificationListDao.Properties.AppPkg.eq(
                                                 notificationList.getAppPkg()))
                                         .build()
                                         .unique();
        if (nl != null) {
            nl.setAppName(notificationList.getAppName());
            nl.setAppPkg(notificationList.getAppPkg());
            nl.setIsBlacklist(notificationList.getIsBlacklist());
            mDaoSession.getNotificationListDao()
                       .updateInTx(nl);
        }
    }

    //******************************* Switch *************************************

    List<Switch> getAllSwitch() {
        return mDaoSession.getSwitchDao()
                          .loadAll();
    }

    boolean updateOneSwitch(String req) {
        boolean result = true;

        Switch aSwitch = mDaoSession.getSwitchDao()
                                    .load(1L);
        switch (req) {
            case RequestCode.SMART_REMINDER:
                result = !aSwitch.getSmartReminder();
                aSwitch.setSmartReminder(result);
                break;

            case RequestCode.CONSTANT_LIGHT:
                result = !aSwitch.getConstantLight();
                aSwitch.setConstantLight(result);
                break;

            case RequestCode.CLICK_SCREEN_LIGHT:
                result = !aSwitch.getClickScreenLight();
                aSwitch.setClickScreenLight(result);
                break;

            case RequestCode.MOBILE_SCREEN_ON_NOT_PUSH:
                result = !aSwitch.getMobileScreenOnNotPush();
                aSwitch.setMobileScreenOnNotPush(result);
                break;

            case RequestCode.MESSAGE_PUSH:
                result = !aSwitch.getMessagePush();
                aSwitch.setMessagePush(result);
                break;

            case RequestCode.SCHEDULE:
                result = !aSwitch.getSchedule();
                aSwitch.setSchedule(result);
                break;

            case RequestCode.SEDENTARINESS:
                result = !aSwitch.getSedentariness();
                aSwitch.setSedentariness(result);
                break;

            case RequestCode.SWEET_WORDS:
                result = !aSwitch.getSweetWords();
                aSwitch.setSweetWords(result);
                break;

            case RequestCode.MOBILE_AWAY_BODY_REMIND:
                result = !aSwitch.getMobileAwayBodyRemind();
                aSwitch.setMobileAwayBodyRemind(result);
                break;

            case RequestCode.COMMUNICATION_SERVICES:
                result = !aSwitch.getCommunicationServices();
                aSwitch.setCommunicationServices(result);
                break;

            case RequestCode.MOBILE_DATA:
                result = !aSwitch.getMobileData();
                aSwitch.setMobileData(result);
                break;

            case RequestCode.SERVICE_PASSWORD:
                result = !aSwitch.getServicePassword();
                aSwitch.setServicePassword(result);
                break;

            case RequestCode.SWAY_WRIST_LOCK_SCREEN:
                result = !aSwitch.getSwayWristLockScreen();
                aSwitch.setSwayWristLockScreen(result);
                break;

            default:
                break;
        }

        mDaoSession.getSwitchDao()
                   .updateInTx(aSwitch);
        return result;
    }


    //******************************* WaterClock *************************************

    WaterClock getWaterClock() {
        return mDaoSession.getWaterClockDao()
                          .load(1L);
    }

    void updateWaterClock(WaterClock waterClock) {
        WaterClock wc = mDaoSession.getWaterClockDao()
                                   .load(1L);
        wc.setIsOpen(waterClock.getIsOpen());
        wc.setDrinkWaterInterval(waterClock.getDrinkWaterInterval());
        wc.setRemindTime(waterClock.getRemindTime());

        mDaoSession.getWaterClockDao()
                   .updateInTx(wc);
    }

    //****************************** Schedule **************************************

    void removeAllSchedule() {
        if (isScheduleInit) {
            List<ScheduleData> scheduleDatas = Schedule.getInstance()
                                                       .queryAllEvent(true);
            Schedule.getInstance()
                    .deleteAllEvent(scheduleDatas);
        }
    }

    void addSchedule(final ScheduleMessage scheduleMessage) {
        if (isScheduleInit) {
            asyncThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    List<ScheduleData> scheduleDataList = scheduleMessage.getScheduleDataList();
                    for (ScheduleData scheduleData : scheduleDataList) {
                        Schedule.getInstance()
                                .insertEvent(scheduleData);
                    }
                }
            });
        }
    }

    //******************************* Contact *************************************

    void addContact(final Contact contact) {
        asyncThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                List<Contact.Item> contactList = contact.getContactList();
                for (Contact.Item item : contactList) {
                    ContactUtil.getInstance()
                               .addContact(mContext, item.getName(), item.getPhoneNumber(),
                                           item.getHeadImageBase64());
                }
                Advertiser.getInstance()
                          .sendMessage(RequestCode.FINISHED_SYNC_DATA);
            }
        });
    }

    void removeContact(final Contact contact) {
        asyncThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                List<Contact.Item> contactList = contact.getContactList();
                for (Contact.Item item : contactList) {
                    ContactUtil.getInstance()
                               .deleteContactByName(mContext, item.getName());
                }
            }
        });
    }

    //******************************** UserInfo ************************************

    void updateUserInfo(UserInfo userInfo) {
        List<UserInfo> userInfos = mDaoSession.getUserInfoDao()
                                              .loadAll();
        if (userInfos == null || userInfos.size() == 0) {
            UserInfo ui = new UserInfo(null, userInfo.getName(), userInfo.getSex(),
                                       userInfo.getBirth(), userInfo.getHeight(),
                                       userInfo.getWeight(), userInfo.getAddress());
            mDaoSession.getUserInfoDao()
                       .insertInTx(ui);
        } else {
            UserInfo ui = mDaoSession.getUserInfoDao()
                                     .load(1L);
            ui.setName(userInfo.getName());
            ui.setSex(userInfo.getSex());
            ui.setBirth(userInfo.getBirth());
            ui.setHeight(userInfo.getHeight());
            ui.setWeight(userInfo.getWeight());
            ui.setAddress(userInfo.getAddress());
            mDaoSession.getUserInfoDao()
                       .updateInTx(ui);
        }
    }

    //******************************** PhoneType ************************************

    void updatePhoneType(PhoneType phoneType) {
        List<PhoneType> phoneTypes = mDaoSession.getPhoneTypeDao()
                                                .loadAll();
        if (phoneTypes == null || phoneTypes.size() == 0) {
            PhoneType ph = new PhoneType(null, phoneType.getSystemType(),
                                         phoneType.getSystemVersion());
            mDaoSession.getPhoneTypeDao()
                       .insertInTx(ph);
        } else {
            PhoneType ph = mDaoSession.getPhoneTypeDao()
                                      .load(1L);
            ph.setSystemType(phoneType.getSystemType());
            ph.setSystemVersion(phoneType.getSystemVersion());
            mDaoSession.getPhoneTypeDao()
                       .updateInTx(ph);
        }
    }

    //********************************************************************

    //********************************************************************

    //********************************************************************

    //********************************************************************

    //********************************************************************

    //********************************************************************

    //********************************************************************

}
