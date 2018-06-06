package com.mstarc.wearablelauncher;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.telecom.TelecomManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import mstarc_os_api.mstarc_os_api_msg;
import com.mstarc.commonbase.communication.aidl.AidlCommunicate;
import com.mstarc.commonbase.communication.listener.ConnectionStateListener;
import com.mstarc.fakewatch.bind.BindWizard;
import com.mstarc.mobiledataservice.IMobileDataAidl;
import com.mstarc.wearablelauncher.service.LauncherService;
import com.mstarc.wearablelauncher.view.alipay.AlipayWigitFragment;
import com.mstarc.wearablelauncher.view.clock.IdleFragment;
import com.mstarc.wearablelauncher.view.common.HorizontalViewPager;
import com.mstarc.wearablelauncher.view.fte.FteViewGroup;
import com.mstarc.wearablelauncher.view.quicksetting.MobileServiceClient;
import com.mstarc.wearablelauncher.view.settings.SettingFragment;

import android.os.SystemProperties;

public class MainActivity extends FragmentActivity implements IdleFragment.PageListener, View.OnClickListener, FteViewGroup.IFteListener, CommonManager.OnPowerModeChangeListener {

    private static final int NUM_PAGES = 3;


    public static final int ALIPAY_PAGE_INDEX = 0;

    public static final int IDLE_PAGE_INDEX = 1;

    public static final int SETTING_PAGE_INDEX = 2;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String ACTION_FTE_STEP = "com.mstarc.fte.step";
    public static final String INTENT_AMBIENT_ON = "com.mstarc.ambient.on";
    public static final String INTENT_AMBIENT_OFF = "com.mstarc.ambient.off";
    public static final String ACTION_HOME_SCROLL = "ACTION_HOME_SCROLL";
    BindWizard mBindWizard;
    public HorizontalViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private IdleFragment mIdleFragment;
    private SettingFragment mSettingFragment;
    private AlipayWigitFragment mAlipayFragment;
    FteViewGroup mFteView;
    FteBroadcastReceiver fteBroadcastReceiver;
    private int mPowerMode;
    private final boolean bSkipFTE = SystemProperties.getBoolean("persist.mstarc.skipfte", false);
    AmbientModeReceiver mAmbientModeReceiver;
    PowerManager.WakeLock mWakeLock;
    AlipaySlideListener mAlipaySlideListener;
    Object mLock = new Object();
    private SendHandler mHandler;
    private mstarc_os_api_msg m_api_msg;
    public interface AlipaySlideListener{
        void onUpdate();

        void onShow();

        void onHide();
    }

    public void setAlipaySlideListener(AlipaySlideListener listener) {
        synchronized (mLock) {
            mAlipaySlideListener = listener;
        }
    }

    @Override
    public void OnPowerModeChange(int powermode) {
        if (powermode != mPowerMode) {
            //exit watchmode
            if (mPowerMode == CommonManager.POWERMODE_WATCH) {
                Log.d(TAG, "exit WatchMode powermode " + powermode);
                mPowerMode = powermode;
                Log.e(TAG, "OnPowerModeChange " + powermode);
                finish();
            }
            //enter watchmode
            if (powermode == CommonManager.POWERMODE_WATCH) {
                Log.d(TAG, "enter WatchMode");
                mPowerMode = powermode;
                mIdleFragment = null;
                mSettingFragment = null;
                mAlipayFragment = null;
                Log.e(TAG, "OnPowerModeChange " + powermode);
                finish();
            }
        }

    }

    public IdleFragment getIdleFragment(){
        return mIdleFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iniBle();
        mBindWizard = new BindWizard(getApplicationContext());
        CommonManager.getInstance(this.getApplicationContext()).addPowerModeListener(this);
        mPowerMode = CommonManager.getInstance(this.getApplicationContext()).getmPowerMode();
        mHandler = new SendHandler();
        if (!mBindWizard.isBind() && !bSkipFTE) {
            setContentView(R.layout.fte);
            mFteView = (FteViewGroup) findViewById(R.id.fte);
            mFteView.setListener(this);
            IntentFilter intentFilter = new IntentFilter(ACTION_FTE_STEP);
            fteBroadcastReceiver = new FteBroadcastReceiver();
            try {
                registerReceiver(fteBroadcastReceiver, intentFilter);
            } catch (Exception e) {
                Log.e(TAG, "onCreate: ", e);
            }
            // PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            // mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "");
            //mWakeLock.acquire();
            Log.d(TAG, "onCreate with fte");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Log.d(TAG, "gln launcher开启常亮--------------");
        } else if (CommonManager.getInstance(this.getApplicationContext()).getmPowerMode() == CommonManager.POWERMODE_WATCH) {
            setContentView(R.layout.black);
            Log.d(TAG, "onCreate with WatchMode");
        } else {
            setContentView(R.layout.activity_main);
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int w_screen = dm.widthPixels;
            int h_screen = dm.heightPixels;
            Log.i(TAG, "Scren px = [ " + w_screen + " , " + h_screen + " ] density = " + dm.densityDpi);
            // Instantiate a ViewPager and a PagerAdapter.
            mIdleFragment = new IdleFragment();
            mSettingFragment = new SettingFragment();
            mAlipayFragment = new AlipayWigitFragment();
            mPager = (HorizontalViewPager) findViewById(R.id.pager);
            mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);
            mPager.setOffscreenPageLimit(2);
            mPager.setCurrentItem(IDLE_PAGE_INDEX, true);
            mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {

                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    handleSendBoradcast();
                    if (state == ViewPager.SCROLL_STATE_IDLE) {
                        if (mPager.getCurrentItem() == 0) {
                            synchronized (mLock) {
                                if (mAlipaySlideListener != null) {
                                    mAlipaySlideListener.onUpdate();
                                }
                            }
                            if (mAlipaySlideListener != null) {
                                mAlipaySlideListener.onShow();
                            }
                        } else {
                            if (mAlipaySlideListener != null) {
                                mAlipaySlideListener.onHide();
                            }
                        }
                    }
                }
            });
            mAmbientModeReceiver = new AmbientModeReceiver();
            Log.d(TAG, "onCreate with normal");
/*
            m_api_msg = new mstarc_os_api_msg(this) {
                @Override
                public void onServiceConnected() {
                    super.onServiceConnected();
                    m_api_msg.mstarc_api_adb(MainActivity.this,"echo 41 > /sys/class/lwq/lwq/val");
                }
            };
*/
        }
//        mPager.setPageTransformer(true, new DepthPageTransformer(DepthPageTransformer.HORIZONTAL));
    }

    private void iniBle() {
        AidlCommunicate.getInstance().initAIDL(this);
        MobileServiceClient.getInstance().initMobileService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBindWizard.isBind() && !bSkipFTE) {
        } else if (CommonManager.getInstance(this.getApplicationContext()).getmPowerMode() == CommonManager.POWERMODE_WATCH) {
        } else {
            mPager.setCurrentItem(1, false);
            mIdleFragment.setCurrentItem(1);
            IntentFilter filter = new IntentFilter(INTENT_AMBIENT_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            try{
                registerReceiver(mAmbientModeReceiver, filter);
            } catch (Exception e) {
                Log.e(TAG, "onResume: ", e);
            }
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            telecomManager.endCall();
            LauncherService service = ((LauncherApplication) getApplicationContext()).mLauncherService;
            if (service != null) {
                service.setLauncherShowing(true);
                if (service.isPowerPlugged() && (service.isAmbientOn() || !service.isScreenOn())) {
                    service.startChargeUI();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        LauncherService service = ((LauncherApplication) getApplicationContext()).mLauncherService;
        if (service != null) {
            service.setLauncherShowing(false);
        }
        try {
            unregisterReceiver(mAmbientModeReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent " + intent);
    }

    class AmbientModeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(INTENT_AMBIENT_ON)
                    || action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (mPager != null) {
                    mPager.setCurrentItem(1, false);
                }
                if (mIdleFragment != null) {
                    mIdleFragment.setCurrentItem(1);
                }
                Log.d(TAG, "onReceive: " + action);
            }
        }
    }

    @Override
    public void onPageSelected(int position) {
        Log.d(TAG, "onPageSelected:" + position);
        handleSendBoradcast();
        if (mPager != null) {
            if (CommonManager.getInstance(this.getApplicationContext()).getmPowerMode() == CommonManager.POWERMODE_WATCH) {
                mPager.setSwipeEnabled(false);
            } else {
                mPager.setSwipeEnabled(position == 1 ? true : false);
            }
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "ScreenSlidePagerAdapter getItem:" + position);
            if (position == IDLE_PAGE_INDEX) {
                return mIdleFragment;
            } else if (position == SETTING_PAGE_INDEX) {
                return mSettingFragment;
            } else if (position == ALIPAY_PAGE_INDEX) {
                return mAlipayFragment;
            } else {
                return null;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_bound) {
            ComponentName componentName = new ComponentName("com.mstarc.alipay", "com.mstarc.alipay.ui.WatchDemoMainActivity");
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(componentName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            Bundle translateBundle =
                    ActivityOptions.makeCustomAnimation(this,
                            R.anim.slide_in_left, R.anim.slide_out_left).toBundle();
            startActivity(intent, translateBundle);
        }
    }

    @Override
    public void onFteFinished() {
        Log.d(TAG, "Fte finished");
        mBindWizard.completeBind();
        CommonManager.getInstance(this.getApplicationContext()).setFteFinished();
        try {
            unregisterReceiver(fteBroadcastReceiver);
        } catch (Exception e) {
            Log.e(TAG, "onFteFinished: ", e);
        }
        // mWakeLock.release();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "gln launcher关闭常亮-----------");
        startFteApplication();
        recreate();
    }

    private void startFteApplication() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        ComponentName cn = new ComponentName("com.mstarc.fte.wearablefte", "com.mstarc.fte.wearablefte.MainActivity");
        intent.setComponent(cn);
        startActivity(intent);
    }

    @Override
    public void onFteStepSelected(int step) {
        if (step == 5) {
            Intent intent = new Intent("com.mstarc.fte.pw.finish");
            Log.d(TAG, "send pass word finished broadcast");
            intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            sendBroadcast(intent);
        }
    }

    class FteBroadcastReceiver extends BroadcastReceiver {
        int mFteServiceStep = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_FTE_STEP)) {
                int serviceStep = intent.getIntExtra("step", -1);
                Log.d(TAG, "got fte service step： " + serviceStep);
                if (serviceStep != -1 && mFteServiceStep != serviceStep) {
                    mFteServiceStep = serviceStep;
                    int viewStep = -1;
                    switch (serviceStep) {
                        case 1:
                            viewStep = 2; //QR scan finished
                            break;
                        case 2:
                            viewStep = 3; //enter password setting
                            break;
                        case 3:
                            viewStep = 7; //enter data sync
                            break;
                        case 4:
                            viewStep = 8;
                            onFteFinished();// data sync finished;
                            break;
                    }
                    if (viewStep != -1) {
                        Log.d(TAG, "switch to view step ： " + viewStep);
                        mFteView.biginSyncPhone();
                        mFteView.setStep(viewStep);
                    }
                }

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommonManager.getInstance(getApplicationContext()).removePowerModeListener(this);
        Log.d(TAG, "onDestroy removePowerModeListener");
        AidlCommunicate.getInstance().onDestroy();
        MobileServiceClient.getInstance().unbind(this);
    }

    private void handleSendBoradcast() {
        mHandler.removeMessages(SendHandler.MSG_SEND_BROADCAST);
        mHandler.sendEmptyMessageDelayed(SendHandler.MSG_SEND_BROADCAST, 200);
    }

    class SendHandler extends Handler {
        public final static int MSG_SEND_BROADCAST = 1;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SEND_BROADCAST:
                    Intent intent = new Intent();
                    intent.setAction(ACTION_HOME_SCROLL);
                    sendBroadcast(intent);
                    break;
            }
        }
    }
}
