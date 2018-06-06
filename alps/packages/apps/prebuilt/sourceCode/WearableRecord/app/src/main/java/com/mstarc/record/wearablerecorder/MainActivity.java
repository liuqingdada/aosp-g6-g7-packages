package com.mstarc.record.wearablerecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.mstarc.record.wearablerecorder.fragment.RecordFragment;
import com.mstarc.record.wearablerecorder.fragment.RecordListFragment;
import com.mstarc.record.wearablerecorder.view.CircleIndicator;

public class MainActivity extends FragmentActivity {

    ViewPager mPager;
    Fragment mRecordFragment = new RecordFragment();
    Fragment mRecordListFragment = new RecordListFragment();
    private static final int notification_id = 10000;

    private ScreenSlidePagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecordManager.setApplicationContext(getApplicationContext());
        MSessionWizard.getInstance(this).initMediaSession();
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.view_pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(2);
        CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        showNotification();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = mRecordFragment;
                    break;
                case 1:
                    fragment = mRecordListFragment;
                    break;
                default:
                    fragment = mRecordFragment;
                    break;

            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showNotification();
        RecordManager.getInstance().bindServiceConnection(MainActivity.this);
        MSessionWizard.getInstance(this).setMediaSessionActive(true);
        MSessionWizard.getInstance(this).setRecordView(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RecordManager.getInstance().unBindService(MainActivity.this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MSessionWizard.getInstance(this).setRecordView(false);
    }

    @Override
    public void finish() {
        super.finish();
        Log.d("dingyichen", "onFinished!!!");
        rmNotification();
        RecordManager.getInstance().reset();
        RecordManager.getInstance().stopPlay();
        RecordManager.getInstance().releaseRecord();
        MSessionWizard.getInstance(this).finish();

        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
        System.exit(0);
    }

    private void showNotification() {
        // Andy TODO
        rmNotification();
        PendingIntent localPendingIntent = PendingIntent.getActivity(this,
                0, new Intent(this, MainActivity.class), 0);
        Notification localNotification = new NotificationCompat.Builder(this).
                setSmallIcon(R.mipmap.ic_launcher).
                setContentTitle("录音").
                setContentText("录音器").
                setContentIntent(localPendingIntent).
                setAutoCancel(true).build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).
                notify(notification_id, localNotification);
    }

    private void rmNotification() {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notification_id);
    }
}
