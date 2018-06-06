package com.mstarc.wearablephone;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.mstarc.wearablephone.view.ContractsFragment;
import com.mstarc.wearablephone.view.DialFragment;
import com.mstarc.wearablephone.view.InterceptPhoneFragment;
import com.mstarc.wearablephone.view.RecordFragment;
import com.mstarc.wearablephone.view.common.CircleIndicator;

/**
 * Created by wangxinzhi on 17-3-6.
 */

public class PhoneActivity extends FragmentActivity {
    ViewPager mPager;
    Fragment mRecordFragment = new RecordFragment();
    Fragment mContractsFragment = new ContractsFragment();
    Fragment mDialFragment = new DialFragment();

    private ScreenSlidePagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyG7Theme();
        setContentView(R.layout.main);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.vp);
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(2);
        CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        mPager.setCurrentItem(1);
        Intent intent = getIntent();
        if (intent != null) {
            String type = intent.getType();
            if (type != null && type.equals(CallLog.Calls.CONTENT_TYPE))
                try {
                    mPager.setCurrentItem(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            String type = intent.getType();
            if (type != null && type.equals(CallLog.Calls.CONTENT_TYPE))
                try {
                    mPager.setCurrentItem(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentPagerAdapter {

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = mDialFragment;

                    break;
                case 1:
                    fragment = mContractsFragment;

                    break;
                case 2:
                    fragment = mRecordFragment;
                    break;
                case 3:
                    fragment = new InterceptPhoneFragment();
                    break;
                default:
                    fragment = mDialFragment;
                    break;

            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 4;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    void applyG7Theme() {
        int theme = ((PhoneApplication) getApplication()).getThemeStyle();
        if (theme != 0) {
            setTheme(theme);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mPager.setCurrentItem(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        try {
            mPager.setCurrentItem(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
    }

}
