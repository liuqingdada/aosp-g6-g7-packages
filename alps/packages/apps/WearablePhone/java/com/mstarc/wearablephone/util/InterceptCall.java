package com.mstarc.wearablephone.util;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.mstarc.wearablephone.database.DatabaseWizard;
import com.mstarc.wearablephone.database.greendao.InterceptCallDao;

/**
 * Created by liuqing
 * 17-11-7.
 * Email: 1239604859@qq.com
 */

public class InterceptCall {
    private static final String TAG = InterceptCall.class.getSimpleName();
    private static InterceptCall sInterceptCall;
    private TelephonyManager mTelephonyManager;
    private Context mContext;

    public static InterceptCall getInstance() {
        if (sInterceptCall == null) {
            sInterceptCall = new InterceptCall();
        }
        return sInterceptCall;
    }

    public void init(Context context) {
        mContext = context.getApplicationContext();
    }

    //private int lastPhoneState = TelephonyManager.CALL_STATE_IDLE;
    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE: // 空闲
                    Log.w(TAG, "onCallStateChanged: 空闲");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK: // 摘机, 正在通话中
                    Log.w(TAG, "onCallStateChanged: 摘机, 正在通话中");
                    break;
                case TelephonyManager.CALL_STATE_RINGING: // 来电
                    Log.w(TAG, "onCallStateChanged: 来电 --> " + incomingNumber);
                    break;
            }
            //lastPhoneState = state;
        }
    };

    public synchronized void intercept() {
        // 删除通话记录
        // 1.获取内容解析者
        final ContentResolver resolver = mContext.getContentResolver();
        // 2.获取内容提供者地址 call_log calls表的地址:calls
        // 3.获取执行操作路径
        final Uri uri = Uri.parse("content://call_log/calls");
        // 4.删除操作
        // 通过内容观察者观察内容提供者内容,如果变化,就去执行删除操作
        // notifyForDescendents : 匹配规则,true : 精确匹配 false:模糊匹配
        resolver.registerContentObserver(uri, true, new ContentObserver(new Handler()) {
            // 内容提供者内容变化的时候调用
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (currentNumber == null) {
                    return;
                }
                // 删除通话记录
                resolver.delete(uri, "number=?", new String[]{currentNumber});
                // 注销内容观察者
                resolver.unregisterContentObserver(this);
            }
        });

        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) mContext.getSystemService(
                    Service.TELEPHONY_SERVICE);
            mTelephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        mTelephonyManager.endCall();
        // save intercept log to greenDao.
        InterceptCallDao inCallDao = DatabaseWizard.getInstance()
                                                   .getDaoSession()
                                                   .getInterceptCallDao();
        String name = currentName;
        if (TextUtils.isEmpty(name)) {
            name = currentNumber;
        }
        if (currentNumber == null) {
            return;
        }

        inCallDao.insertInTx(
                new com.mstarc.wearablephone.database.bean.InterceptCall(null, name,
                                                                         currentNumber));
        currentName = null;
        currentNumber = null;
    }

    private String currentName;
    private String currentNumber;

    public synchronized boolean isIntercept(String num) {
        String name = Util.getContactName(mContext, num);
        currentName = name;
        currentNumber = num;
        int stop = Settings.System.getInt(mContext.getContentResolver(), "stopstranger", 1);
        boolean isIntercept = stop == 1;

        return isIntercept && TextUtils.isEmpty(name);
    }
}
