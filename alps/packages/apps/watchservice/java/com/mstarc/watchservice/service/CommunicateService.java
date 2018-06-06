package com.mstarc.watchservice.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;

import com.mstarc.commonbase.communication.ITransmitter;
import com.mstarc.commonbase.communication.IntelligentPush;
import com.mstarc.commonbase.communication.bluetooth.ble.periphery.Advertiser;
import com.mstarc.commonbase.communication.bluetooth.ble.utils.Compatable;
import com.mstarc.commonbase.communication.bluetooth.utils.ByteUtils;
import com.mstarc.commonbase.communication.bluetooth.utils.ClsUtils;
import com.mstarc.commonbase.communication.exception.PushException;
import com.mstarc.commonbase.communication.listener.CommonTransmitListener;
import com.mstarc.commonbase.communication.listener.ConnectionStateListener;
import com.mstarc.commonbase.communication.message.WatchCode;
import com.mstarc.commonbase.communication.message.transmite.Contact;
import com.mstarc.commonbase.communication.message.transmite.PowerStep;
import com.mstarc.commonbase.communication.message.transmite.ScheduleMessage;
import com.mstarc.commonbase.communication.message.transmite.SportHealth;
import com.mstarc.commonbase.communication.message.transmite.Weather;
import com.mstarc.commonbase.communication.utils.Constant;
import com.mstarc.commonbase.communication.utils.GsonUtil;
import com.mstarc.commonbase.database.DatabaseWizard;
import com.mstarc.commonbase.database.bean.NotificationBean;
import com.mstarc.commonbase.database.bean.NotificationList;
import com.mstarc.commonbase.database.bean.PhoneType;
import com.mstarc.commonbase.database.bean.Switch;
import com.mstarc.commonbase.database.bean.UserInfo;
import com.mstarc.commonbase.database.bean.WaterClock;
import com.mstarc.commonbase.database.greendao.DaoSession;
import com.mstarc.commonbase.notification.NotificationWizard;
import com.mstarc.commonbase.notification.listener.OnReceiveNotificationListener;
import com.mstarc.commonbase.notification.utils.ID;
import com.mstarc.commonbase.schedule.ScheduleData;
import com.mstarc.watchservice.ICommunicateAidl;
import com.mstarc.watchservice.common.IStringTransmitListener;
import com.mstarc.watchservice.notification.OnDeleteNotificationListener;
import com.mstarc.watchservice.notification.OnReceiveNotification;
import com.mstarc.watchservice.notification.OnUpdataNotificationListener;
import com.mstarc.watchservice.receiver.CommunicateReceiver;
import com.mstarc.watchservice.utils.GreenDaoUtils;
import com.mstarc.watchservice.utils.PushUtils;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommunicateService extends Service {
    private AdvertiserBinder mAdvertiserBinder = new AdvertiserBinder();

    private IntelligentPush mIntelligentPush;

    private Advertiser mAdvertiser;

    private NotificationWizard mNotificationWizard;

    private PushUtils mPushUtils = new PushUtils(this);
    private ScheduledExecutorService asyncThreadPool = Executors.newScheduledThreadPool(32);

    private CommunicateReceiver mCommunicateReceiver = new CommunicateReceiver();

    private ConnectionStateListener mConnectionStateListener = new ConnectionStateListener() {
        @Override
        public void onConnected(String state) {
            System.out.println(state);

            asyncThreadPool.shutdownNow();
        }

        @Override
        public void onDisconnected() {
            DaoSession daoSession = DatabaseWizard.getInstance()
                                                  .getDaoSession();
            Switch aSwitch = daoSession.getSwitchDao()
                                       .load(1L);
            if (aSwitch.getMobileAwayBodyRemind()) {
                asyncThreadPool.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        NotificationBean notificationBean =
                                new NotificationBean(null, ID.nextId(),
                                                     WatchCode.MOBILE_AWAY_BODY_REMIND, "手机离身",
                                                     "请连接您的手机", "", "", "", "", false, true);
                        mNotificationWizard.sendNotification(notificationBean);
                    }
                }, 0, 10, TimeUnit.MINUTES);
            }
        }
    };

    private CommonTransmitListener stringListener = new CommonTransmitListener<String>() {
        @Override
        public void onReadData(String requestCode, String filePath) throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            requestCode.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(requestCode));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            System.out.println(requestCode);
            mPushUtils.analyse(requestCode);
        }
    };

    private CommonTransmitListener notificationListListener = new
            CommonTransmitListener<NotificationList>() {
                @Override
                public void onReadData(NotificationList notificationList, String filePath)
                        throws PushException {
                    if (mIStringTransmitListener != null) {
                        try {
                            Compatable.TypeAndCounter dataType = Compatable.getDataType(
                                    notificationList.getClass());
                            if (dataType != null) {
                                short type = dataType.getType();
                                String shortConvert = ByteUtils.shortConvert(type);

                                mIStringTransmitListener.onReadData(
                                        shortConvert + GsonUtil.toJson(notificationList));
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            throw new PushException(ITransmitter.AIDL_ERROR);
                        }
                    }

                    mPushUtils.analyse(notificationList);
                }
            };

    private CommonTransmitListener waterClockListener = new CommonTransmitListener<WaterClock>() {
        @Override
        public void onReadData(WaterClock waterClock, String filePath) throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            waterClock.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(waterClock));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            mPushUtils.analyse(waterClock);
        }
    };

    private CommonTransmitListener scheduleListener = new CommonTransmitListener<ScheduleMessage>
            () {
        @Override
        public void onReadData(ScheduleMessage scheduleMessage, String filePath)
                throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            scheduleMessage.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(scheduleMessage));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            mPushUtils.analyse(scheduleMessage);
        }
    };

    private CommonTransmitListener contactListener = new CommonTransmitListener<Contact>() {

        @Override
        public void onReadData(Contact contact, String filePath) throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            contact.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(contact));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            mPushUtils.analyse(contact);
        }
    };

    private CommonTransmitListener userInfoListener = new CommonTransmitListener<UserInfo>() {
        @Override
        public void onReadData(UserInfo userInfo, String filePath) throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            userInfo.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(userInfo));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            mPushUtils.analyse(userInfo);
        }
    };

    private CommonTransmitListener phoneTypeListener = new CommonTransmitListener<PhoneType>() {
        @Override
        public void onReadData(PhoneType phoneType, String filePath) throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            phoneType.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(phoneType));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            mPushUtils.analyse(phoneType);
        }
    };

    private CommonTransmitListener powerStepListener = new CommonTransmitListener<PowerStep>() {
        @Override
        public void onReadData(PowerStep powerStep, String filePath) throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            powerStep.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(powerStep));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            mPushUtils.analyse(powerStep);
        }
    };

    private CommonTransmitListener sportHealthListener = new CommonTransmitListener<SportHealth>() {

        @Override
        public void onReadData(SportHealth sportHealth, String filePath) throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            sportHealth.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(sportHealth));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            mPushUtils.analyse(sportHealth);
        }
    };

    private CommonTransmitListener weatherListener = new CommonTransmitListener<Weather>() {

        @Override
        public void onReadData(Weather weather, String filePath) throws PushException {
            if (mIStringTransmitListener != null) {
                try {
                    Compatable.TypeAndCounter dataType = Compatable.getDataType(
                            weather.getClass());
                    if (dataType != null) {
                        short type = dataType.getType();
                        String shortConvert = ByteUtils.shortConvert(type);

                        mIStringTransmitListener.onReadData(
                                shortConvert + GsonUtil.toJson(weather));
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new PushException(ITransmitter.AIDL_ERROR);
                }
            }

            mPushUtils.analyse(weather);
        }
    };

    @Override
    public void onCreate() {
        // 注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.PAIR_ACTION);
        intentFilter.addAction(Constant.SETTINGS_ACTION);
        intentFilter.addAction(Constant.NOTIFICATION_ACTION);
        registerReceiver(mCommunicateReceiver, intentFilter);

        System.out.println(ClsUtils.getBtAddressViaReflection());
        // init commondata in database
        GreenDaoUtils.getInstance()
                     .init(this);

        super.onCreate();
        // init intelligent push
        mIntelligentPush = IntelligentPush.getInstance();
        mIntelligentPush.setRole(IntelligentPush.WATCH);
        mIntelligentPush.getDefaultConnection();
        mIntelligentPush.setAlias("860023810000124");

        mAdvertiser = Advertiser.getInstance();
        mNotificationWizard = NotificationWizard.getInstance();
        mNotificationWizard.initNotificationWizard(this);

        mIntelligentPush.onConnectionStateChanged(mConnectionStateListener);

        mNotificationWizard.setOnReceiveNotification(new OnReceiveNotificationListener() {
            @Override
            public void onReceiveNotification(NotificationBean notification) {
                if (mOnReceiveNotification != null) {
                    try {
                        mOnReceiveNotification.onReceiveNotification(GsonUtil.toJson(notification));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mNotificationWizard.setOnDeleteNotificationListener(
                new com.mstarc.commonbase.notification.listener.OnDeleteNotificationListener() {
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
                });

        mNotificationWizard.setOnUpdataNotificationListener(
                new com.mstarc.commonbase.notification.listener.OnUpdataNotificationListener() {
                    @Override
                    public void onUpdataNotification(NotificationBean notification) {
                        try {
                            mOnUpdataNotificationListener.onUpdataNotification(GsonUtil.toJson
                                    (notification));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });

        mAdvertiser.setOnReceiveMessageListener(stringListener, String.class);

        mAdvertiser.setOnReceiveMessageListener(notificationListListener, NotificationList.class);

        mAdvertiser.setOnReceiveMessageListener(waterClockListener, WaterClock.class);

        mAdvertiser.setOnReceiveMessageListener(scheduleListener, ScheduleData.class);

        mAdvertiser.setOnReceiveMessageListener(contactListener, Contact.class);

        mAdvertiser.setOnReceiveMessageListener(userInfoListener, UserInfo.class);

        mAdvertiser.setOnReceiveMessageListener(phoneTypeListener, PhoneType.class);

        mAdvertiser.setOnReceiveMessageListener(powerStepListener, PowerStep.class);

        mAdvertiser.setOnReceiveMessageListener(sportHealthListener, SportHealth.class);

        mAdvertiser.setOnReceiveMessageListener(weatherListener, Weather.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAdvertiserBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mCommunicateReceiver);
        mIntelligentPush.removeConnectionStateChanged(mConnectionStateListener);
        mIntelligentPush.removeReceiveMessageListener(stringListener, notificationListListener,
                                                      waterClockListener, scheduleListener,
                                                      contactListener, userInfoListener,
                                                      phoneTypeListener, powerStepListener,
                                                      sportHealthListener, weatherListener);
        asyncThreadPool.shutdownNow();
        mAdvertiser.stopAdvertise();
        mNotificationWizard.onDestroy();
        mIntelligentPush.onDestroy();
    }

    /******************************************************************
     * AIDL listener interface
     */

    private IStringTransmitListener mIStringTransmitListener;
    private OnReceiveNotification mOnReceiveNotification;
    private OnDeleteNotificationListener mOnDeleteNotificationListener;
    private OnUpdataNotificationListener mOnUpdataNotificationListener;

    private class AdvertiserBinder extends ICommunicateAidl.Stub {

        @Override
        public void setOnReceiveMessageListener(IStringTransmitListener istl)
                throws RemoteException {
            mIStringTransmitListener = istl;
        }

        @Override
        public void sendMessage(String json, String filePath) throws RemoteException {
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
        public String getAll() throws RemoteException {
            List<NotificationBean> all = mNotificationWizard.getAll();
            return GsonUtil.toJson(all);
        }

        @Override
        public void remove(long id) throws RemoteException {
            mNotificationWizard.remove(id);
        }

        @Override
        public void removeAll() throws RemoteException {
            mNotificationWizard.removeAll();
        }

        @Override
        public void update(long id, String title, String content) throws RemoteException {
            mNotificationWizard.update(id, title, content);
        }

        @Override
        public void setRead(long id) throws RemoteException {
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

    }
}
