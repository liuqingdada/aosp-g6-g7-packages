package com.mstarc.wearablelauncher.poweroff;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import com.android.internal.os.storage.ExternalStorageFormatter;
import com.mstarc.commonbase.communication.aidl.AidlCommunicate;
import com.mstarc.commonbase.communication.message.RequestCode;
import com.mstarc.wearablelauncher.CommonManager;
import com.mstarc.wearablelauncher.R;

/**
 * Created by wangxinzhi on 17-6-3.
 */

public class LongPressOnPowerKeyReceiver extends BroadcastReceiver implements PowerSelectDialog.Listener {

    private static final String TAG = LongPressOnPowerKeyReceiver.class.getSimpleName();

    public static final String ACTION = "com.mstarc.powerkey.longpress";

    PowerSelectDialog mDialog;

    boolean needExitWatchMode = false;

    UiHandler mHandler;

    PowerManager mPowerManger;

    Context mContext;

    RebootProgressDialog mProgressDialog;

    boolean mEraseSdCard = true;

    public LongPressOnPowerKeyReceiver() {
    }

    public LongPressOnPowerKeyReceiver(Context mContext) {

    }

    class UiHandler extends Handler {
        public static final int MSG_REBOOT = 1;
        public static final int MSG_POWEROFF = 2;
        public static final int MSG_FACTORYRESET = 3;
        public static final int MSG_EXITWATCHMODE = 4;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG,"msg: "+msg.what);
            switch (msg.what) {
                case MSG_REBOOT:
                    showProgressDialog();
                    /////////
                    //mPowerManger.reboot(null);
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_REBOOT);
                    intent.putExtra("nowait", 1);
                    intent.putExtra("interval", 1);
                    intent.putExtra("window", 0);
                    mContext.sendBroadcast(intent);
                    break;
                case MSG_POWEROFF:
                    showProgressDialog();
                    //////
                    Intent i = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
                    i.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
                    i.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(i);
                    break;
                case MSG_FACTORYRESET:
                    showProgressDialog();
                    //////////////
                    doMasterClear();
                    break;
                case MSG_EXITWATCHMODE:
                    CommonManager.getInstance(mContext.getApplicationContext()).setmPowerMode(CommonManager.POWERMODE_NORMAL);
                    break;
            }
        }
    }

    private void notify2Phone() {
        AidlCommunicate.getInstance().sendMessageImmediately(RequestCode.UNBOUND_WATCH, "");
    }

    private void doMasterClear() {
        if (mEraseSdCard) {
            Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
            intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
            intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
            mContext.startService(intent);
        } else {
            Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
            mContext.sendBroadcast(intent);
            // Intent handling is asynchronous -- assume it will happen soon.
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new RebootProgressDialog(mContext, R.layout.progress);
        }
        mProgressDialog.show();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive " + intent);

        if(mContext == null) {
            mContext = context.getApplicationContext();
        }
        if(mHandler== null){
            mHandler = new UiHandler();
        }
        if (intent.getAction().equals(ACTION)) {
            mPowerManger = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
            mDialog = new PowerSelectDialog(context, this, R.layout.poweroff);
            needExitWatchMode = false;
            mDialog.show();
        }
    }

    @Override
    public void onSelected(int index) {
        switch (index) {
            case 0:
                mHandler.sendEmptyMessage(UiHandler.MSG_REBOOT);
                break;
            case 1:
                mHandler.sendEmptyMessage(UiHandler.MSG_POWEROFF);
                break;
            case 2:
                if (needExitWatchMode) {
                    mHandler.sendEmptyMessage(UiHandler.MSG_EXITWATCHMODE);
                } else {
                    notify2Phone();
                    mHandler.sendEmptyMessageDelayed(UiHandler.MSG_FACTORYRESET, 200);
                }
                break;
        }
    }
}
