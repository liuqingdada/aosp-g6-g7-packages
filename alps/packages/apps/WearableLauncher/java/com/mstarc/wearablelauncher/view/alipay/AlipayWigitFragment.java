package com.mstarc.wearablelauncher.view.alipay;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.SharedPreferencesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.mstarc.wearablelauncher.MainActivity;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.common.DepthPageTransformer;

/**
 * Created by wangxinzhi on 17-8-3.
 */

public class AlipayWigitFragment extends Fragment implements MainActivity.AlipaySlideListener {
    private static final String TAG = AlipayWigitFragment.class.getSimpleName();
    AppWidgetManager mAppWidgetManager;
    AppWidgetHost mHost;
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    static final int HOST_ID = 1234;
    final static ComponentName mTestApp0 = new ComponentName("com.android.deskclock", "com.android.alarmclock.DigitalAppWidgetProvider");
    final static ComponentName mTestApp1 = new ComponentName("com.android.deskclock", "com.android.alarmclock.AnalogAppWidgetProvider");
    final static ComponentName mTestApp2 = new ComponentName("com.mstarc.widgettest", "com.mstarc.widgettest.Widget");
    final static ComponentName mAlipayApp = new ComponentName("com.eg.android.AlipayGphone", "com.alipay.quickcard.BarcodeWidgetProvider");
    private final boolean bTest = SystemProperties.getBoolean("persist.mstarc.widget.test", false);
    private final int testIndex = SystemProperties.getInt("persist.mstarc.widget.index", 0);
    ComponentName mTargetApp;
    AppWidgetProviderInfo mAppWidgetProviderInfo;
    AppWidgetHostView mAppWidgetHostView;
    PowerManager.WakeLock mWakeLock;
    UiHandler mHandler;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppWidgetManager = AppWidgetManager.getInstance(getContext());
        mHost = new AppWidgetHost(getContext(), HOST_ID);
        mHost.deleteHost();
        mAppWidgetId = mHost.allocateAppWidgetId();
        PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Launcher_Alipay");
        mHandler = new UiHandler();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView this:" + this);
        View view = inflater.inflate(R.layout.alipaywiget, container, false);
        view.setTag(DepthPageTransformer.ITEM_LEFT_OR_TOP);
        Log.d(TAG, "onCreateView result:" + view);
        return view;
    }


    @Override
    public void onDestroy() {
        mHost.deleteAppWidgetId(mAppWidgetId);
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        mHost.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHost.stopListening();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause " + this);
        if(mHandler!=null) {
            mHandler.releaseScreen();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume " + this);
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.setAlipaySlideListener(this);
        }
    }

    public void rebindWidget() {
        ViewGroup rootView = (ViewGroup) getView();
        if (rootView != null) {
            rootView.removeAllViews();
        } else {
            Log.e(TAG, "skip rebindWidget for mContainerView null");
            return;
        }

        Log.d(TAG, "mAppWidgetId: " + mAppWidgetId);
        if (bTest) {
            if (testIndex == 0) {
                mTargetApp = mTestApp0;
            }
            if (testIndex == 1) {
                mTargetApp = mTestApp1;
            }
            if (testIndex == 2) {
                mTargetApp = mTestApp2;
            }
        } else {
            mTargetApp = mAlipayApp;
        }
        mAppWidgetManager.bindAppWidgetIdIfAllowed(mAppWidgetId, mTargetApp);
        mAppWidgetProviderInfo = mAppWidgetManager.getAppWidgetInfo(mAppWidgetId);

        mAppWidgetHostView = mHost.createView(getContext(), mAppWidgetId, mAppWidgetProviderInfo);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        rootView.addView(mAppWidgetHostView, layoutParams);
        sendUpdateIntent();
    }

    public void sendUpdateIntent() {
        if (mAppWidgetProviderInfo == null) {
            Log.e(TAG, "skip sendUpdateIntent: " + null);
            return;
        }
        int[] appWidgetIds = {mAppWidgetId};
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.setComponent(mAppWidgetProviderInfo.provider);
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        getContext().sendBroadcast(intent);
        Log.d(TAG, "sendUpdateIntent : " + intent);
    }

    @Override
    public void onUpdate() {
        rebindWidget();
    }

    @Override
    public void onShow() {
        if (mHandler != null) {
            mHandler.lightScreen();
        }
    }

    @Override
    public void onHide() {
        if (mHandler != null) {
            mHandler.releaseScreen();
        }
    }


    class UiHandler extends Handler {
        public static final int MSG_DISALBE_CHUIWAN_SCREEN_OFF = 1;
        boolean bDisableScreenOff = false;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DISALBE_CHUIWAN_SCREEN_OFF:
                    if (bDisableScreenOff) {
                        Settings.System.putLong(getContext().getContentResolver(), "navigation", System.currentTimeMillis());
                        removeMessages(MSG_DISALBE_CHUIWAN_SCREEN_OFF);
                        sendEmptyMessage(MSG_DISALBE_CHUIWAN_SCREEN_OFF);
                        Log.d(TAG, "update Settings.System navigation");
                    }
                    break;
            }
        }

        public void lightScreen() {
            if (!mWakeLock.isHeld()) {
                Log.d(TAG, "aquire wakelock");
                mWakeLock.acquire();
            }
            Settings.System.putLong(getContext().getContentResolver(), "navigation", System.currentTimeMillis());
            Log.d(TAG, "update Settings.System navigation");
            removeMessages(MSG_DISALBE_CHUIWAN_SCREEN_OFF);
            sendEmptyMessageDelayed(MSG_DISALBE_CHUIWAN_SCREEN_OFF, 5000);
            bDisableScreenOff = true;
        }

        public void releaseScreen() {
            if (mWakeLock.isHeld()) {
                Log.d(TAG, "release wakelock");
                mWakeLock.release();
            }
            removeMessages(MSG_DISALBE_CHUIWAN_SCREEN_OFF);
            bDisableScreenOff = false;
        }
    }
}
