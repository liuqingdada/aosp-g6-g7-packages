package com.mstarc.music.wearablemusic;

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

import com.mstarc.music.wearablemusic.fragment.MusciFragment;
import com.mstarc.music.wearablemusic.fragment.MusicListFragment;
import com.mstarc.music.wearablemusic.view.CircleIndicator;

public class MainActivity extends FragmentActivity{

    ViewPager mPager;
    Fragment mMusicFragment = new MusciFragment();
    Fragment mMusicListFragment = new MusicListFragment();
    private static final int notification_id = 10000;

    private ScreenSlidePagerAdapter mPagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MusicManager.getInstance().setApplicationContext(getApplicationContext());

        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.view_pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(2);
        CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        showNotification();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.getInstance().bindServiceConnection(MainActivity.this);
        MusicManager.getInstance().setMediaSessionActive(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.getInstance().unBindService(MainActivity.this);
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
                    fragment = mMusicFragment;
                    break;
                case 1:
                    fragment = mMusicListFragment;
                    break;
                default:
                    fragment = mMusicFragment;
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
    public void finish() {
        super.finish();
        Log.d("dingyichen", "onFinished!!!");
        rmNotification();
        MusicManager.getInstance().stopPlay();
        MusicManager.getInstance().reset();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
        System.exit(0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        rmNotification();
        Log.d("dingyichen", "onBackPressed!!!");
    }

    private void showNotification() {
        // Andy TODO
        rmNotification();
        PendingIntent localPendingIntent = PendingIntent.getActivity(this,
                0, new Intent(this, MainActivity.class), 0);
        Notification localNotification = new NotificationCompat.Builder(this).
                setSmallIcon(R.mipmap.ic_launcher).
                setContentTitle("音乐播放").
                setContentText("音乐播放").
                setContentIntent(localPendingIntent).
                setAutoCancel(true).build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).
                notify(notification_id, localNotification);
    }

    private void rmNotification() {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notification_id);
    }
}
