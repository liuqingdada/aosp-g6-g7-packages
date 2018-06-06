package com.mstarc.watchservice.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.mstarc.commonbase.communication.bluetooth.ble.periphery.Advertiser;
import com.mstarc.commonbase.communication.message.RequestCode;
import com.mstarc.commonbase.communication.message.WatchCode;
import com.mstarc.commonbase.communication.utils.Constant;
import com.mstarc.commonbase.database.bean.NotificationBean;
import com.mstarc.commonbase.notification.NotificationWizard;
import com.mstarc.commonbase.notification.utils.ID;

public class CommunicateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()
                  .equals(Constant.SETTINGS_ACTION)) {

            String resp = intent.getStringExtra(Constant.SETTING);

            switch (resp) {
                default: break;
            }
        }

        if (intent.getAction()
                  .equals(Constant.NOTIFICATION_ACTION)) {
            Bundle bundle = intent.getExtras();
            String func = bundle.getString(Constant.FUNCTION);
            String content = bundle.getString(Constant.CONTENT);

            NotificationBean notificationBean = new NotificationBean(null, ID.nextId(),
                                                                     WatchCode.SEDENTARINESS,
                                                                     "久坐提醒", "您已经坐很久了！", "",
                                                                     "", "", "", true, true);
            if (content != null) {
                notificationBean.setContent(content);
            }

            if (func != null) {
                switch (func) {
                    case Constant.SEDENTARINESS: // 久坐
                        notificationBean.setType(WatchCode.SEDENTARINESS);
                        break;

                    case Constant.FROM_THE_WRIST: // 离腕
                        notificationBean.setType(WatchCode.FROM_THE_WRIST);
                        break;

                    case Constant.DRINK_WATER: // 喝水
                        notificationBean.setType(WatchCode.WATER_CLOCK);
                        break;

                    case Constant.SCHEDULE: // 日程
                        notificationBean.setType(WatchCode.SCHEDULE);
                        break;

                    case Constant.RAISE_HAND: // 抬手
                        notificationBean.setType(WatchCode.RAISE_HAND);
                        break;

                    case Constant.OBTAIN_HANDS_DOWN: // 垂手
                        notificationBean.setType(WatchCode.OBTAIN_HANDS_DOWN);
                        break;

                    default:
                        break;
                }
            }

            NotificationWizard.getInstance()
                              .sendNotification(notificationBean);
        }

        if (intent.getAction()
                  .equals(Constant.PWD_OK_ACTION)) {
            Advertiser.getInstance()
                      .sendMessage(RequestCode.OK_PWD);
        }
    }
}
