package com.mstarc.wearablelauncher.view.clock;

/**
 * Created by wangxinzhi on 17-2-19.
 */

/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mstarc.wearablelauncher.MainActivity;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.common.DepthPageTransformer;
import com.mstarc.wearablelauncher.view.common.VerticalViewPager;
import com.mstarc.wearablelauncher.view.notification.NotificationFragment;
import com.mstarc.wearablelauncher.view.quicksetting.QSFragment;

import java.util.ArrayList;


public class IdleFragment extends Fragment {
    private static final String TAG = IdleFragment.class.getSimpleName();

    private static final boolean DEBUG = true;

    private static final int NUM_PAGES = 3;

    private static final int QUICKSETTING_PAGE_INDEX = 0;

    private static final int CLOCK_PAGE_INDEX = 1;

    private static final int NOTIFICATION_PAGE_INDEX = 2;


    private VerticalViewPager mPager;

    private PagerAdapter mPagerAdapter;

    private Handler mHandlerTime = new Handler();

    private ArrayList<Fragment> mFragments;

    private QSFragment mQSFragment = new QSFragment();

//    private ClockFragment mClockFragment = new ClockFragment();
    private BlackFragment mBlackFragment = new BlackFragment();

    private NotificationFragment mNotificationFragment = new NotificationFragment();

    private final Runnable mTimerRun = new Runnable() {
        public void run() {
            backToClock();
        }
    };

    public void backToClock() {
        if (mPager != null && mPager.getCurrentItem() != CLOCK_PAGE_INDEX) {
            mPager.setCurrentItem(CLOCK_PAGE_INDEX, false);
        }
    }

    public IdleFragment() {
        mFragments = new ArrayList<Fragment>();
        mFragments.add(mQSFragment);
        mFragments.add(mBlackFragment);
    }

    public void setPageListener(PageListener pageListener) {
        this.mPageListener = pageListener;
    }

    private PageListener mPageListener;


    public interface PageListener {
        void onPageSelected(int position);
    }

    public int getCurrentPage() {
        if(mPager != null) {
            return mPager.getCurrentItem();
        }else{
            return -1;
        }
    }

    public void setCurrentItem(int item){
        if(mPager!=null) {
            mPager.setCurrentItem(item, false);
            Log.d(TAG ,"setCurrentItem: "+item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        setCurrentItem(1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view =  inflater.inflate(R.layout.example_colock_codinator,container,false);
//        view.setTag(DepthPageTransformer.ITEM_CENTER);
//        return view;
        mFragments.add(mNotificationFragment);
        View view = inflater.inflate(R.layout.idle_fragment_layout, container, false);

        mPager = (VerticalViewPager) view.findViewById(R.id.vpager);
        mPagerAdapter = new IdlePagerAdapter(this.getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(NUM_PAGES);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected: " + position+" mPageListener: "+mPageListener);
                ((MainActivity) getActivity()).onPageSelected(position);
                if (mPageListener != null) {
                    mPageListener.onPageSelected(position);
                }
            }
        });

        mPager.setCurrentItem(CLOCK_PAGE_INDEX, false);
        mPager.setPageTransformer(true, new DepthPageTransformer(DepthPageTransformer.VERTICAL));
        view.setTag(DepthPageTransformer.ITEM_CENTER);
        return view;
    }

    public QSFragment getQSFragment(){
        return mQSFragment;
    }

    private class IdlePagerAdapter extends FragmentStatePagerAdapter {

        public IdlePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getItemPosition(Object object) {
            int adapterPosition = mFragments.indexOf(object);
            if (adapterPosition != -1) {
                return adapterPosition;
            }
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            try {
                super.finishUpdate(container);
            } catch (NullPointerException nullPointerException) {
                Log.e(TAG,"Catch the NullPointerException in FragmentPagerAdapter.finishUpdate");
            }
        }
    }


}

