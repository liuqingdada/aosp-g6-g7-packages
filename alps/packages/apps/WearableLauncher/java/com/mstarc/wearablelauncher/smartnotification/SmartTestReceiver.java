package com.mstarc.wearablelauncher.smartnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.notification.NotificationFragment;


/**
 * Created by wangxinzhi on 17-6-4.
 * adb shell am broadcast -a com.mstarc.smartnotification.test --ei id 1
 */

public class SmartTestReceiver extends BroadcastReceiver {
    public static final String ACTION_TEST_SMART_NOTIFICATION = "com.mstarc.smartnotification.test";
    private static final String TAG = SmartTestReceiver.class.getSimpleName();
    SmartNotificationDialog mDialog;
    int mType = 0;
    Listener mListener = new Listener();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_TEST_SMART_NOTIFICATION)) {
            int id = intent.getExtras().getInt("id");
            String iconAddress = intent.getExtras().getString("icon");
            Log.d(TAG, "onReceive " + id+" iconAddress: "+iconAddress);
            mType = id;
            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }
            switch (id) {
                case 1:
                    mDialog = new SmartNotificationDialog(context,
                            SmartNotificationDialog.BEHAVIAR_TYPE_SMART_NOTIFICATION,
                            mListener,
                            R.layout.smart_notify, null, null, null);
                    break;
                case 2:
                    mDialog = new SmartNotificationDialog(context,
                            SmartNotificationDialog.BEHAVIAR_TYPE_PAY,
                            mListener,
                            R.layout.smart_pay, null, null, null);
                    break;
                case 3:
                    mDialog = new SmartNotificationDialog(context,
                            SmartNotificationDialog.BEHAVIAR_TYPE_FORGET,
                            mListener,
                            R.layout.smart_forget, null, null, null);
                    break;
                case 4:
                    mDialog = new SmartNotificationDialog(context,
                            SmartNotificationDialog.BEHAVIAR_TYPE_JIURZUO,
                            mListener,
                            R.layout.smart_jiuzuo, null, null, null);
                    break;
                case 5: {
                    int iconResId = 0;
                    Log.d(TAG,"111 "+iconAddress);
                    if (iconAddress != null) {
                        Log.d(TAG,"222 "+iconAddress);
                        iconResId = NotificationFragment.getSmartMiyuIcon(iconAddress);
                    }
                    mDialog = new SmartNotificationDialog(context,
                            SmartNotificationDialog.BEHAVIAR_TYPE_MIYU,
                            mListener,
                            R.layout.smart_miyu, null, null, null, iconResId);
                }
                break;
            }
            if(mDialog!=null){
                mDialog.show();
            }
        }
    }

    class Listener implements SmartNotificationDialog.Listener {

        @Override
        public void onSetting() {
            Log.d(TAG, "onSetting " + mType);
        }

        @Override
        public void onConfirm() {
            Log.d(TAG, "onConfirm " + mType);
        }
    }
}
