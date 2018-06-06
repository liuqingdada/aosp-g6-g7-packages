package com.mstarc.wearablelauncher.battery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mstarc.wearablelauncher.LauncherApplication;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.service.LauncherService;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Charge extends Activity {
    BatterydReceiver mPowerDisconnectedReceiver;
    int mBatteryLevel;
    ImageView mLevelImageView;
    TextView mLevelTextView;
    TextView mTimeTextView;
    ImageView mBatteryIcon;
    UiHandler mHandler = new UiHandler();
    PowerManager.WakeLock mWakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charge);
        mPowerDisconnectedReceiver = new BatterydReceiver();

        mLevelImageView = (ImageView) findViewById(R.id.progress);
        mBatteryIcon = (ImageView) findViewById(R.id.icon);
        mLevelTextView = (TextView) findViewById(R.id.battery_level_text);
        mTimeTextView = (TextView) findViewById(R.id.battery_time);

    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(mPowerDisconnectedReceiver);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Receiver not registered")) {
            } else {
                throw e;
            }
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        registerReceiver(mPowerDisconnectedReceiver, filter);
        mHandler.removeMessages(UiHandler.MSG_UPDATE_TIMER);
        mHandler.sendEmptyMessage(UiHandler.MSG_UPDATE_TIMER);
        if (mWakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "LauncherChargeUI");
        }
        if (!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        LauncherService service = ((LauncherApplication) getApplicationContext()).mLauncherService;
        if (service != null) {
            service.setChargeShowing(true);
        }
    }


    class BatterydReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                overridePendingTransition(0, 0);
                finish();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                if (level != mBatteryLevel) {
                    mBatteryLevel = level;
                    mHandler.sendEmptyMessage(UiHandler.MSG_UPDATE_BATTERY_LEVEL);
                }
            } else if (Intent.ACTION_BATTERY_OKAY.equals(action)) {
                mHandler.sendEmptyMessage(UiHandler.MSG_UPDATE_BATTERY_OKAY);
            }
        }
    }

    @Override
    protected void onPause() {
        LauncherService service = ((LauncherApplication) getApplicationContext()).mLauncherService;
        if (service != null) {
            service.setChargeShowing(false);
        }
        mHandler.removeMessages(UiHandler.MSG_UPDATE_TIMER);
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        super.onPause();
    }


    @Override
    public void onWindowDismissed() {
        sendBroadcast(new Intent("QUIT_CHARGE_SCREEN"));
        finish();
    }

    class UiHandler extends Handler {
        public final static int MSG_UPDATE_BATTERY_LEVEL = 1;
        public final static int MSG_UPDATE_BATTERY_OKAY = 2;
        public final static int MSG_UPDATE_TIMER = 3;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_BATTERY_LEVEL:
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mLevelImageView.getLayoutParams();
                    float percent = mBatteryLevel / 100f;
                    int parentWidth = ((ViewGroup) mLevelImageView.getParent()).getWidth();
                    int margin = (int) (parentWidth * ((float) (1 - percent)) / 2f);
                    layoutParams.setMarginStart(margin);
                    layoutParams.setMarginEnd(margin);
                    mLevelImageView.setLayoutParams(layoutParams);

                    mLevelTextView.setText("" + mBatteryLevel + "%");
                    if (mBatteryLevel == 100) {
                        mBatteryIcon.setImageResource(R.drawable.charge_finished);
                    } else {
                        mBatteryIcon.setImageResource(R.drawable.chargring);
                    }
                    break;
                case MSG_UPDATE_BATTERY_OKAY:
                    mBatteryIcon.setImageResource(R.drawable.charge_finished);
                    break;
                case MSG_UPDATE_TIMER:
                    SimpleDateFormat sdformat;
                    sdformat = new SimpleDateFormat("HH:mm");
                    mTimeTextView.setText(sdformat.format(new Date()));
                    sendEmptyMessageDelayed(UiHandler.MSG_UPDATE_TIMER, 1000);
                    break;

            }
        }
    }
}
