package com.mstarc.wearablesport;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.mstarc.watchbase.service.sportservice.IService;
import com.mstarc.watchbase.service.sportservice.SportsService;
import com.mstarc.watchbase.service.sportservice.bean.SportType;

import java.util.ArrayList;

/**
 * Created by wangxinzhi on 17-3-16.
 */

public class MainActivity extends FragmentActivity implements View.OnClickListener {
    public static final int FRAGMENT_INDEX_MODE = 0;
    public static final int FRAGMENT_INDEX_TARGET = 1;
    public static final int FRAGMENT_INDEX_PROGRESS = 2;
    public static final int FRAGMENT_INDEX_RESULT = 3;
    public static final String SPORT_TYPE = "sport_type";
    private static final String TAG = MainActivity.class.getSimpleName();
    ArrayList<Fragment> mFragments;
    private Intent serviceIntent;
    Connection mServiceConnection;
    public IService mSportService;
    boolean isServiceReady = false;
    private boolean mIsServiceBinded = false;

    public MainActivity() {
        mFragments = new ArrayList<Fragment>();
        mFragments.add(new ModelSelectFragment());
        mFragments.add(new TargetSettingFragment());
        mFragments.add(new ProgressFragment());
        mFragments.add(new ResultFragment());
        mServiceConnection = new Connection();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent ");
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        showFragment(FRAGMENT_INDEX_MODE, null);
        serviceIntent = new Intent(this, SportsService.class);
        if (!mIsServiceBinded) {
            startService(serviceIntent);
            Log.d(TAG, "bindService " + serviceIntent);
            mIsServiceBinded = bindService(serviceIntent, mServiceConnection, BIND_AUTO_CREATE);
        }
    }

    class Connection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG, "onServiceConnected");
            mSportService = ((SportsService.SportsBinder) service).getService();
            isServiceReady = true;
            resumeSportProgress();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mSportService = null;
            isServiceReady = false;
        }
    }

    private void resumeSportProgress() {
        /**
         * 返回运动状态,即上次操作
         * -1.无操作 1.开始 2.暂停 3.继续 4.停止
         */
        int sportcondition = mSportService.getSportCondition();
        Log.e(TAG, "sportcondition: " + sportcondition);
        if (sportcondition == 1 || sportcondition == 2 || sportcondition == 3) {
            showFragment(FRAGMENT_INDEX_PROGRESS, null);
        }else{
            showFragment(FRAGMENT_INDEX_MODE, null);
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart ");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart ");
        super.onRestart();

    }
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume ");
        super.onResume();
        if (isServiceReady) {
            resumeSportProgress();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause ");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy ");
        if (mIsServiceBinded) {
            unbindService(mServiceConnection);
            isServiceReady = false;
        }
        super.onDestroy();

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void showFragment(int index, Bundle bundle) {
        Fragment fragment = mFragments.get(index);
        Log.d(TAG, "try to show "+fragment);
        if (!fragment.isResumed()) {
            if (bundle != null) {
                fragment.setArguments(bundle);
            }
            FragmentManager fragmentManager = this.getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_right_in, R.anim.fade_out);
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
            Log.d(TAG, "show "+fragment);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.resultconfirm) {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right_out);
    }

    SportType getSportType() {
        int sportint = mSportService.getMode();
        switch (sportint) {
            case 1:
                return SportType.RUN_INDOOR;
            case 3:
                return SportType.RUN_OUTDOOR;
            case 5:
                return SportType.RIDE;
            default:
                return SportType.RUN_OUTDOOR;
        }
    }
}
