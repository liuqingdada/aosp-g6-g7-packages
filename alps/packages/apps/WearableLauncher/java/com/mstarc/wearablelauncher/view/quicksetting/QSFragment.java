package com.mstarc.wearablelauncher.view.quicksetting;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mstarc.commonbase.communication.aidl.AidlCommunicate;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.clock.IdleFragment;
import com.mstarc.wearablelauncher.view.common.CircleIndicator;
import com.mstarc.wearablelauncher.view.common.DepthPageTransformer;
import com.mstarc.wearablelauncher.view.common.HorizontalViewPager;

import java.util.ArrayList;

/**
 * Created by wangxinzhi on 17-2-19.
 */

public class QSFragment extends Fragment implements IdleFragment.PageListener{

    private static final String TAG = QSFragment.class.getSimpleName();

    /**
     * QuickSetting Fragment Definition.
     */
    public QSFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.quicksetting_subfragment_layout, container,
                false);
        ViewPager page = (ViewPager) view.findViewById(R.id.pager);
        page.setAdapter(new PageAdapter(getFragmentManager()));
        view.setTag(DepthPageTransformer.ITEM_LEFT_OR_TOP);
        page.setOffscreenPageLimit(2);
        CircleIndicator indicator = (CircleIndicator) view.findViewById(R.id.indicator);
        indicator.setViewPager(page);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        resetPagePosition();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment parentFragment = getParentFragment();
        if(parentFragment!=null && parentFragment instanceof  IdleFragment){
            ((IdleFragment)parentFragment).setPageListener(this);
        }
    }

    private void resetPagePosition() {
        View rootView = getView();
        try {
            if (rootView != null && rootView instanceof ViewGroup) {
                View childView = ((ViewGroup) rootView).getChildAt(0);
                if (childView != null && childView instanceof ViewPager) {
                    ((ViewPager) childView).setCurrentItem(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onPageSelected(int position) {
        if(position!=0){
            Log.d(TAG, "on Idle fragment page change to "+position+" reset QSFragment page position");
            resetPagePosition();
        }
    }

    ArrayList<Fragment> mFragments = new ArrayList<>();

    public QSPower getQSPower(){
        return (QSPower) mFragments.get(0);
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(FragmentManager fm) {
            super(fm);
            mFragments.add(new QSPower());
            mFragments.add(new QSMode());
            mFragments.add(new QSWeather());
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }
    }


    public static class QSPower extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.quicksetting1, container,
                    false);
          return view;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }
    }

    public static class QSMode extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.quicksetting2, container,
                    false);
            return view;
        }
    }

    public static class QSWeather extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.quicksetting3, container,
                    false);
            return view;
        }
    }
}