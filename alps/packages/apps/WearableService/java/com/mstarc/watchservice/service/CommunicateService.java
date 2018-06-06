package com.mstarc.watchservice.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mstarc.commonbase.communication.ITransmitter;
import com.mstarc.commonbase.communication.IntelligentPush;
import com.mstarc.commonbase.communication.bluetooth.ble.periphery.Advertiser;
import com.mstarc.commonbase.communication.bluetooth.ble.utils.Compatable;
import com.mstarc.commonbase.communication.bluetooth.utils.ByteUtils;
import com.mstarc.commonbase.communication.exception.PushException;
import com.mstarc.commonbase.communication.jpush.JPush;
import com.mstarc.commonbase.communication.listener.CommonTransmitListener;
import com.mstarc.commonbase.communication.listener.ConnectionStateListener;
import com.mstarc.commonbase.communication.message.WatchCode;
import com.mstarc.commonbase.communication.message.transmite.Contact;
import com.mstarc.commonbase.communication.message.transmite.LocationMessage;
import com.mstarc.commonbase.communication.message.transmite.Music;
import com.mstarc.commonbase.communication.message.transmite.PowerStep;
import com.mstarc.commonbase.communication.message.transmite.Record;
import com.mstarc.commonbase.communication.message.transmite.ScheduleMessage;
import com.mstarc.commonbase.communication.message.transmite.SportHealth;
import com.mstarc.commonbase.communication.message.transmite.WatchFace;
import com.mstarc.commonbase.communication.message.transmite.Weather;
import com.mstarc.commonbase.communication.utils.GsonUtil;
import com.mstarc.commonbase.database.DatabaseWizard;
import com.mstarc.commonbase.database.bean.DeviceAddress;
import com.mstarc.commonbase.database.bean.NotificationBean;
import com.mstarc.commonbase.database.bean.NotificationList;
import com.mstarc.commonbase.database.bean.PhoneType;
import com.mstarc.commonbase.database.bean.Switch;
import com.mstarc.commonbase.database.bean.UserInfo;
import com.mstarc.commonbase.database.bean.WaterClock;
import com.mstarc.commonbase.database.greendao.DaoSession;
import com.mstarc.commonbase.notification.NotificationWizard;
import com.mstarc.commonbase.notification.listener.OnReceiveNotificationListener;
import com.mstarc.commonbase.schedule.Schedule;
import com.mstarc.commonbase.schedule.ScheduleData;
import com.mstarc.watchservice.ICommunicateAidl;
import com.mstarc.watchservice.R;
import com.mstarc.watchservice.common.IConnectStateListener;
import com.mstarc.watchservice.common.IStringTransmitListener;
import com.mstarc.watchservice.notification.OnDeleteNotificationListener;
import com.mstarc.watchservice.notification.OnReceiveNotification;
import com.mstarc.watchservice.notification.OnUpdataNotificationListener;
import com.mstarc.watchservice.receiver.BatteryReceiver;
import com.mstarc.watchservice.receiver.ScreenStatusReceiver;
import com.mstarc.watchservice.utils.GreenDaoUtils;
import com.mstarc.watchservice.utils.PushUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mstarc_os_api.mstarc_os_api_msg;

/**
 * Created by liuqing
 * 2017/4/18.
 * Email: 1239604859@qq.com
 */
public class CommunicateService extends Service {
    private static final String TAG = "CommunicateService";
    public static boolean bleFlag = false;

    private AdvertiserBinder mAdvertiserBinder = new AdvertiserBinder();
    private ExecutorService advertiserService = Executors.newSingleThreadExecutor();

    private IntelligentPush mIntelligentPush;

    private Advertiser mAdvertiser;
    private JPush mJPush;

    private NotificationWizard mNotificationWizard;

    private PushUtils mPushUtils;
    private Handler mHandler = new Handler();

    private static CommunicateService sCommunicateService;
    private ScreenStatusReceiver mScreenStatusReceiver;
    private BatteryReceiver mBatteryReceiver;

    // ************************ Jpush BLE Status *************************

    private Runnable disRunnable = new Runnable() {
        @Override
        public void run() {
            NotificationBean notificationBean =
                    new NotificationBean(null, WatchCode.MOBILE_AWAY_BODY_REMIND,
                                         "手机离身", "请连接您的手机", "",
                                         System.currentTimeMillis(), "", "", false, true);
            mNotificationWizard.sendNotification(notificationBean);
        }
    };

    private ConnectionStateListener mConnectionStateListener = new ConnectionStateListener() {
        @Override
        public void onConnected(String state) {
            if (mIConnectStateListener != null) {
                try {
                    mIConnectStateListener.bleOrJpushConnect(state);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            if (state.equals(ConnectionStateListener.BLE_ADVERTISE)) {
                mHandler.removeCallbacks(disRunnable);
            }
        }

        @Override
        public void onDisconnected() {
            // TODO BLE disconnect
            if (mIConnectStateListener != null) {
                try {
                    mIConnectStateListener.bleDisconnect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            DaoSession daoSession = DatabaseWizard.getInstance()
                                                  .getDaoSession();
            Switch aSwitch = daoSession.getSwitchDao()
                                       .load(1L);
            if (aSwitch.getMobileAwayBodyRemind()) {
                mHandler.postDelayed(disRunnable, 1000 * 60 * 10);
            }
        }
    };

    // ************************ BLE Listener *************************

    private CommonTransmitListener<String> stringListener = new CommonTransmitListener<String>() {
        @Override
        public void onReadData(String requestCode, String filePath) throws PushException {
            Log.i(TAG, requestCode);
            mPushUtils.analyse(requestCode);
            callBackReadData(requestCode.getClass(), GsonUtil.toJson(requestCode));
        }
    };

    private CommonTransmitListener<NotificationList> notificationListListener = new
            CommonTransmitListener<NotificationList>() {
                @Override
                public void onReadData(NotificationList notificationList, String filePath)
                        throws PushException {
                    Log.i(TAG, notificationList.toString());
                    mPushUtils.analyse(notificationList);
                    callBackReadData(notificationList.getClass(),
                                     GsonUtil.toJson(notificationList));
                }
            };

    private CommonTransmitListener<WaterClock> waterClockListener = new
            CommonTransmitListener<WaterClock>() {
                @Override
                public void onReadData(WaterClock waterClock, String filePath)
                        throws PushException {
                    Log.i(TAG, waterClock.toString());
                    mPushUtils.analyse(waterClock);
                    callBackReadData(waterClock.getClass(), GsonUtil.toJson(waterClock));
                }
            };

    private CommonTransmitListener<ScheduleMessage> scheduleListener = new
            CommonTransmitListener<ScheduleMessage>() {
                @Override
                public void onReadData(ScheduleMessage scheduleMessage, String filePath)
                        throws PushException {
                    Log.i(TAG, scheduleMessage.toString());
                    mPushUtils.analyse(scheduleMessage);
                    callBackReadData(scheduleMessage.getClass(), GsonUtil.toJson(scheduleMessage));
                }
            };

    private CommonTransmitListener<Contact> contactListener = new CommonTransmitListener<Contact>
            () {
        @Override
        public void onReadData(Contact contact, String filePath) throws PushException {
            Log.i(TAG, contact.toString());
            mPushUtils.analyse(contact);
            callBackReadData(contact.getClass(), GsonUtil.toJson(contact));
        }
    };

    private CommonTransmitListener<UserInfo> userInfoListener = new
            CommonTransmitListener<UserInfo>() {
                @Override
                public void onReadData(UserInfo userInfo, String filePath) throws PushException {
                    Log.i(TAG, userInfo.toString());
                    mPushUtils.analyse(userInfo);
                    callBackReadData(userInfo.getClass(), GsonUtil.toJson(userInfo));
                }
            };

    private CommonTransmitListener<PhoneType> phoneTypeListener = new
            CommonTransmitListener<PhoneType>() {
                @Override
                public void onReadData(PhoneType phoneType, String filePath) throws PushException {
                    Log.i(TAG, phoneType.toString());
                    mPushUtils.analyse(phoneType);
                    callBackReadData(phoneType.getClass(), GsonUtil.toJson(phoneType));
                }
            };

    private CommonTransmitListener<PowerStep> powerStepListener = new
            CommonTransmitListener<PowerStep>() {
                @Override
                public void onReadData(PowerStep powerStep, String filePath) throws PushException {
                    Log.i(TAG, powerStep.toString());
                    mPushUtils.analyse(powerStep);
                    callBackReadData(powerStep.getClass(), GsonUtil.toJson(powerStep));
                }
            };

    private CommonTransmitListener<SportHealth> sportHealthListener = new
            CommonTransmitListener<SportHealth>() {

                @Override
                public void onReadData(SportHealth sportHealth, String filePath)
                        throws PushException {
                    Log.i(TAG, sportHealth.toString());
                    callBackReadData(sportHealth.getClass(), GsonUtil.toJson(sportHealth));
                }
            };

    private CommonTransmitListener<Weather> weatherListener = new CommonTransmitListener<Weather>
            () {
        @Override
        public void onReadData(Weather weather, String filePath) throws PushException {
            Log.i(TAG, weather.toString());
            callBackReadData(weather.getClass(), GsonUtil.toJson(weather));
        }
    };

    private CommonTransmitListener<LocationMessage> locationMessageListener = new
            CommonTransmitListener<LocationMessage>() {
                @Override
                public void onReadData(LocationMessage locationMessage, String filePath)
                        throws PushException {
                    Log.i(TAG, locationMessage.toString());
                    callBackReadData(locationMessage.getClass(), GsonUtil.toJson(locationMessage));
                }
            };

    private CommonTransmitListener<Music> musicListener = new CommonTransmitListener<Music>() {
        @Override
        public void onReadData(Music music, String filePath) throws PushException {
            Log.i(TAG, music.toString());
            callBackReadData(music.getClass(), GsonUtil.toJson(music));
        }
    };

    private CommonTransmitListener<Record> recordListener = new CommonTransmitListener<Record>() {
        @Override
        public void onReadData(Record record, String filePath) throws PushException {
            Log.i(TAG, record.toString());
            callBackReadData(record.getClass(), GsonUtil.toJson(record));
        }
    };

    private CommonTransmitListener<WatchFace> watchFaceListener = new
            CommonTransmitListener<WatchFace>() {
                @Override
                public void onReadData(WatchFace watchFace, String filePath) throws PushException {
                    Log.i(TAG, watchFace.toString());
                    callBackReadData(watchFace.getClass(), GsonUtil.toJson(watchFace));
                }
            };

    // ************************ Jpush Listener *************************

    // 蜜语推送
    private CommonTransmitListener<ScheduleData> sweetWordsListener = new CommonTransmitListener
            <ScheduleData>() {
        @Override
        public void onReadData(ScheduleData scheduleData, String filePath) throws PushException {
            Log.i(TAG, "Jpush: " + scheduleData.toString());
            Schedule.getInstance()
                    .insertSweetWord(scheduleData);
        }
    };

    /**
     * 各种类型回调监听的实现方法的封装复用
     **/
    private void callBackReadData(Class<?> clazz, String json) throws PushException {
        if (mIStringTransmitListener != null) {
            try {
                Compatable.TypeAndCounter dataType = Compatable.getDataType(clazz);
                if (dataType != null) {
                    short type = dataType.getType();
                    String shortConvert = ByteUtils.shortConvert(type);

                    mIStringTransmitListener.onReadData(shortConvert + json);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                throw new PushException(ITransmitter.AIDL_ERROR);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        m_api_msg = new mstarc_os_api_msg(this) {
            @Override
            public void onServiceConnected() {
                super.onServiceConnected();

                initDatabaseAndReceiver();

                setIntelligentPush();

                setNotificationWizard();

                setAdvertiser();
            }
        };
    }


    private void initDatabaseAndReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mBatteryReceiver = new BatteryReceiver();
        registerReceiver(mBatteryReceiver, filter);

        mScreenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter screenStatusIF = new IntentFilter();
        screenStatusIF.addAction(Intent.ACTION_SCREEN_ON);
        screenStatusIF.addAction(Intent.ACTION_SCREEN_OFF);
        screenStatusIF.addAction("com.mstarc.modechanged");
        registerReceiver(mScreenStatusReceiver, screenStatusIF);

        sCommunicateService = this;
        mPushUtils = new PushUtils(this);
        GreenDaoUtils.getInstance()
                     .init(this);
    }

    private void setIntelligentPush() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        while (imei == null || imei.equals("")) {
            imei = tm.getDeviceId();
        }
        mIntelligentPush = IntelligentPush.getInstance();
        mIntelligentPush.setRole(IntelligentPush.WATCH);
        mIntelligentPush.getDefaultConnection();
        mIntelligentPush.setAlias(imei);
        mIntelligentPush.onConnectionStateChanged(mConnectionStateListener);
    }

    private void setNotificationWizard() {
        mNotificationWizard = NotificationWizard.getInstance();
        mNotificationWizard.setOnReceiveNotification(onReceiveNotificationListener);
        mNotificationWizard.setOnDeleteNotificationListener(onDeleteNotificationListener);
        mNotificationWizard.setOnUpdataNotificationListener(onUpdateNotificationListener);
        mNotificationWizard.initNotificationWizard(this);
    }

    private void setAdvertiser() {
        mAdvertiser = Advertiser.getInstance();
        mJPush = JPush.getInstance();

        // Advertiser
        mAdvertiser.setOnReceiveMessageListener(stringListener, String.class);
        mAdvertiser.setOnReceiveMessageListener(notificationListListener, NotificationList.class);
        mAdvertiser.setOnReceiveMessageListener(waterClockListener, WaterClock.class);
        mAdvertiser.setOnReceiveMessageListener(scheduleListener, ScheduleMessage.class);
        mAdvertiser.setOnReceiveMessageListener(contactListener, Contact.class);
        mAdvertiser.setOnReceiveMessageListener(userInfoListener, UserInfo.class);
        mAdvertiser.setOnReceiveMessageListener(phoneTypeListener, PhoneType.class);
        mAdvertiser.setOnReceiveMessageListener(powerStepListener, PowerStep.class);
        mAdvertiser.setOnReceiveMessageListener(sportHealthListener, SportHealth.class);
        mAdvertiser.setOnReceiveMessageListener(weatherListener, Weather.class);
        mAdvertiser.setOnReceiveMessageListener(locationMessageListener, LocationMessage.class);
        mAdvertiser.setOnReceiveMessageListener(musicListener, Music.class);
        mAdvertiser.setOnReceiveMessageListener(recordListener, Record.class);
        mAdvertiser.setOnReceiveMessageListener(watchFaceListener, WatchFace.class);

        // Jpush
        mJPush.setOnReceiveMessageListener(sweetWordsListener, ScheduleData.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");

        if (bleFlag) { // controled by BluetoothStatusReceiver
            advertiserService.execute(advertiserRunnable);
        }

        startForegroound();
        return START_STICKY;
    }

    private Runnable advertiserRunnable = new Runnable() {
        @Override
        public void run() {
            mAdvertiser.startAdvertise(CommunicateService.this);
        }
    };

    private void startForegroound() {
        Intent notificationIntent = new Intent(getApplicationContext(), CommunicateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1,
                                                               notificationIntent,
                                                               PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                CommunicateService.this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("蓝牙服务")
                .setContentText("")
                .setContentIntent(pendingIntent);
        Notification notification = mNotifyBuilder.build();
        startForeground(0, notification); // 0 不在通知栏显示    1 显示
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAdvertiserBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBatteryReceiver);
        unregisterReceiver(mScreenStatusReceiver);
        mIntelligentPush.removeConnectionStateChanged(mConnectionStateListener);
        mIntelligentPush.removeReceiveMessageListener(stringListener, notificationListListener,
                                                      waterClockListener, scheduleListener,
                                                      contactListener, userInfoListener,
                                                      phoneTypeListener, powerStepListener,
                                                      sportHealthListener, weatherListener,
                                                      locationMessageListener, musicListener,
                                                      recordListener, watchFaceListener);
        mAdvertiser.stopAdvertise();
        mJPush.onDestroy();
        mNotificationWizard.onDestroy();
        mIntelligentPush.onDestroy();
        mPushUtils.onDestroy();

        if (m_api_msg != null) {
            m_api_msg.unbindService();
            m_api_msg = null;
        }

        // 销毁时重新启动Service
        Intent localIntent = new Intent(this, CommunicateService.class);
        startService(localIntent);
    }

    // -->
    // Commonbase NotificationWizard Listener:
    private OnReceiveNotificationListener onReceiveNotificationListener = new
            OnReceiveNotificationListener() {
                @Override
                public void onReceiveNotification(NotificationBean notification) {
                    Log.i(TAG, "onReadData NotificationBean: " + notification.toString());
                    if (mOnReceiveNotification != null) {
                        try {
                            mOnReceiveNotification.onReceiveNotification(
                                    GsonUtil.toJson(notification));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
    private com.mstarc.commonbase.notification.listener.OnDeleteNotificationListener
            onDeleteNotificationListener
            = new com.mstarc.commonbase.notification.listener.OnDeleteNotificationListener() {
        @Override
        public void OnDeleteOneNotification(long id) {
            try {
                mOnDeleteNotificationListener.OnDeleteOneNotification(id);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void OnDeleteAllNotification() {
            try {
                mOnDeleteNotificationListener.OnDeleteAllNotification();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };
    private com.mstarc.commonbase.notification.listener.OnUpdataNotificationListener
            onUpdateNotificationListener
            = new com.mstarc.commonbase.notification.listener.OnUpdataNotificationListener() {
        @Override
        public void onUpdataNotification(NotificationBean notification) {
            try {
                mOnUpdataNotificationListener.onUpdataNotification(GsonUtil.toJson(notification));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };
    // <--

    /******************************************************************
     * AIDL listener interface
     */

    private IStringTransmitListener mIStringTransmitListener;
    private IConnectStateListener mIConnectStateListener;
    private OnReceiveNotification mOnReceiveNotification;
    private OnDeleteNotificationListener mOnDeleteNotificationListener;
    private OnUpdataNotificationListener mOnUpdataNotificationListener;

    private class AdvertiserBinder extends ICommunicateAidl.Stub {
        /*
         ***********************************************************************************
         ************************************ BLE 接口 **************************************
         ***********************************************************************************
         */

        @Override
        public void setOnReceiveMessageListener(IStringTransmitListener istl)
                throws RemoteException {
            mIStringTransmitListener = istl;
        }

        @Override
        public void setBleConnectStateListener(IConnectStateListener icsl) throws RemoteException {
            mIConnectStateListener = icsl;
        }

        @Override
        public void sendMessage(String json, @NonNull String filePath) throws RemoteException {
            if (mAdvertiser.isConnected()) {
                String dataType = json.substring(0, 6); // 数据类型
                short type = ByteUtils.stringConvert(dataType);

                Class<?> clazz = Compatable.getDataType(type);

                if (clazz != null) {
                    Object obj = GsonUtil.parseJson(json.substring(6, json.length()), clazz);
                    mAdvertiser.sendMsg(obj, filePath);
                }
            }
        }

        @Override
        public boolean isBleConnect() throws RemoteException {
            return Advertiser.getInstance()
                             .isConnected();
        }

        /*
         ***********************************************************************************
         ************************************ 腕表开关接口 ***********************************
         ***********************************************************************************
         */

        @Override
        public void setPowerReminder(boolean b) throws RemoteException {
            Switch aSwitch = getSwitch();
            aSwitch.setPowerReminder(b);
            setSwitch(aSwitch);
        }

        @Override
        public boolean getPowerReminder() throws RemoteException {
            return getSwitch().getPowerReminder();
        }

        @Override
        public void setScheduleReminder(boolean b) throws RemoteException {
            Switch aSwitch = getSwitch();
            aSwitch.setSchedule(b);
            setSwitch(aSwitch);
        }

        @Override
        public boolean getScheduleReminder() throws RemoteException {
            return getSwitch().getSchedule();
        }

        @Override
        public void setSweetWordsReminder(boolean b) throws RemoteException {
            Switch aSwitch = getSwitch();
            aSwitch.setSweetWords(b);
            setSwitch(aSwitch);
        }

        @Override
        public boolean getSweetWordsReminder() throws RemoteException {
            return getSwitch().getSweetWords();
        }

        @Override
        public void setSedentarinessReminder(boolean b) throws RemoteException {
            Switch aSwitch = getSwitch();
            aSwitch.setSedentariness(b);
            setSwitch(aSwitch);
        }

        @Override
        public boolean getSedentarinessReminder() throws RemoteException {
            return getSwitch().getSedentariness();
        }

        @Override
        public void setMobileAwayBodyRemind(boolean b) throws RemoteException {
            Switch aSwitch = getSwitch();
            aSwitch.setMobileAwayBodyRemind(b);
            setSwitch(aSwitch);
        }

        @Override
        public boolean getMobileAwayBodyRemind() throws RemoteException {
            return getSwitch().getMobileAwayBodyRemind();
        }

        /*
         ***********************************************************************************
         ********************************* Notification 相 关 接 口 *************************
         ***********************************************************************************
         */

        @Override
        public String getAll() throws RemoteException {
            if (mNotificationWizard == null) {
                return null;
            }

            List<NotificationBean> all = mNotificationWizard.getAll();

            if (all == null || all.size() == 0) {
                return null;
            }

            return GsonUtil.toJson(all);
        }

        @Override
        public void remove(long id) throws RemoteException {
            if (mNotificationWizard == null) {
                return;
            }
            mNotificationWizard.remove(id);
        }

        @Override
        public void removeAll() throws RemoteException {
            if (mNotificationWizard == null) {
                return;
            }
            mNotificationWizard.removeAll();
        }

        @Override
        public void update(long id, String title, String content) throws RemoteException {
            if (mNotificationWizard == null) {
                return;
            }
            mNotificationWizard.update(id, title, content);
        }

        @Override
        public void setRead(long id) throws RemoteException {
            if (mNotificationWizard == null) {
                return;
            }
            mNotificationWizard.setRead(id);
        }

        @Override
        public void setOnReceiveNotification(OnReceiveNotification ornl) throws RemoteException {
            mOnReceiveNotification = ornl;
        }

        @Override
        public void setOnDeleteNotificationListener(OnDeleteNotificationListener odnl)
                throws RemoteException {
            mOnDeleteNotificationListener = odnl;
        }

        @Override
        public void setOnUpdataNotificationListener(OnUpdataNotificationListener ounl)
                throws RemoteException {
            mOnUpdataNotificationListener = ounl;
        }

        @Override
        public void add2Black(String appPkg) throws RemoteException {
            if (mNotificationWizard == null) {
                return;
            }
            mNotificationWizard.add2BlackList(appPkg);
        }

        /*
         ***********************************************************************************
         ***********************************************************************************
         ***********************************************************************************
         */

        @Override
        public String getAdvertiseData() throws RemoteException {
            return CommunicateService.this.getAdvertiseData();
        }
    }

    /*
     ***********************************************************************************
     ************************************* 数据库接口 ************************************
     ***********************************************************************************
     */

    /**
     * 更新/设置 开关状态
     */
    private synchronized void setSwitch(Switch aSwitch) {
        DatabaseWizard.getInstance()
                      .getDaoSession()
                      .getSwitchDao()
                      .updateInTx(aSwitch);
    }

    /**
     * 获取开关状态
     */
    private synchronized Switch getSwitch() {
        return DatabaseWizard.getInstance()
                             .getDaoSession()
                             .getSwitchDao()
                             .load(1L);
    }

    /**
     * 蓝牙广播数据
     */
    private synchronized String getAdvertiseData() {
        DeviceAddress deviceAddress = DatabaseWizard.getInstance()
                                                    .getDaoSession()
                                                    .getDeviceAddressDao()
                                                    .load(1L);
        if (deviceAddress == null) {
            return null;
        }
        return deviceAddress.getWatchMacAddress();
    }

    /*
     *******************************************************************************
     ************************************ 开关状态和设置 *****************************
     *******************************************************************************
     */

    private mstarc_os_api_msg m_api_msg;

    // 常量微光
    public boolean isConstantLight() { // 0 关  1 开
        int state = m_api_msg.mstarc_api_getlcmevent_modem(this);
        return state != 0;
    }

    public void setConstantLight(boolean constantLight) {
        m_api_msg.mstarc_api_setlcmevent_modem(this, constantLight);
    }

    // 点击亮屏
    public boolean isClickScreenLight() {
        int state = m_api_msg.mstarc_api_gettpevent_modem(this);
        return state != 0;
    }

    public void setClickScreenLight(boolean clickScreenLight) {
        m_api_msg.mstarc_api_settpevent_modem(this, clickScreenLight);
    }

    public mstarc_os_api_msg getM_api_msg() {
        return m_api_msg;
    }

    public static CommunicateService getInstance() {
        return sCommunicateService;
    }
}
