package com.mstarc.watchservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mstarc.commonbase.communication.bluetooth.ble.periphery.Advertiser;
import com.mstarc.commonbase.communication.bluetooth.utils.BtUtils;
import com.mstarc.commonbase.communication.message.RequestCode;
import com.mstarc.commonbase.communication.message.WatchCode;
import com.mstarc.commonbase.communication.utils.Constant;
import com.mstarc.commonbase.database.bean.NotificationBean;
import com.mstarc.commonbase.notification.NotificationWizard;

public class CommunicateReceiver extends BroadcastReceiver {

    private final String TAG = "CommunicateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()
                  .equals(Constant.NOTIFICATION_ACTION)) {
            Log.i(TAG, "onReceive: NOTIFICATION_ACTION");
            Bundle bundle = intent.getExtras();
            String func = bundle.getString(Constant.FUNCTION);
            String content = bundle.getString(Constant.CONTENT);

            NotificationBean notificationBean = new NotificationBean(null, WatchCode.SEDENTARINESS,
                                                                     "久坐提醒", "您已经坐很久了！", "",
                                                                     System.currentTimeMillis(), "",
                                                                     "", true, true);
            if (content != null) {
                notificationBean.setContent(content);
            }

            if (func != null) {
                notificationBean.setTitle(func);
                switch (func) {
                    case Constant.SEDENTARINESS: // 久坐
                        notificationBean.setType(WatchCode.SEDENTARINESS);
                        NotificationWizard.getInstance()
                                          .sendNotification(notificationBean);
                        break;

                    /*
                    case Constant.FROM_THE_WRIST: // 离腕
                        notificationBean.setType(WatchCode.FROM_THE_WRIST);
                        NotificationWizard.getInstance()
                                          .sendNotification(notificationBean);
                        break;
                    */

                    case Constant.DRINK_WATER: // 喝水
                        notificationBean.setType(WatchCode.WATER_CLOCK);
                        NotificationWizard.getInstance()
                                          .sendNotification(notificationBean);
                        break;

                    case Constant.SCHEDULE: // 日程
                        notificationBean.setType(WatchCode.SCHEDULE);
                        NotificationWizard.getInstance()
                                          .sendNotification(notificationBean);
                        break;

                    case Constant.SWEET_WORDS: // 蜜语
                        notificationBean.setType(WatchCode.SWEET_WORDS);
                        notificationBean.setIconAdress(bundle.getString("type"));
                        NotificationWizard.getInstance()
                                          .sendNotification(notificationBean);

                    /*
                    case Constant.RAISE_HAND: // 抬手
                        notificationBean.setType(WatchCode.RAISE_HAND);
                        NotificationWizard.getInstance()
                                          .sendNotification(notificationBean);
                        break;

                    case Constant.OBTAIN_HANDS_DOWN: // 垂手
                        notificationBean.setType(WatchCode.OBTAIN_HANDS_DOWN);
                        NotificationWizard.getInstance()
                                          .sendNotification(notificationBean);
                        break;
                    */

                    case Constant.STEP: // 计步
                        BtUtils.putString(context, Constant.STEP_NUM, content);
                        break;

                    default:
                        break;
                }
            }
        }

        // --------------------------------------------------------------------------

        if (intent.getAction()
                  .equals(Constant.PWD_OK_ACTION)) {
            Log.i(TAG, "onReceive: PWD_OK_ACTION");
            if (Advertiser.getInstance()
                          .isConnected()) {
                Advertiser.getInstance()
                          .sendMessage(RequestCode.OK_PWD);
            }
        }

        // --------------------------------------------------------------------------
    }
}
