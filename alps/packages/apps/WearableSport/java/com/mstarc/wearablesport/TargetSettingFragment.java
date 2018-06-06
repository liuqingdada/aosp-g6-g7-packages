package com.mstarc.wearablesport;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ZoomButton;

import com.mstarc.watchbase.service.sportservice.bean.SportGoal;
import com.mstarc.watchbase.service.sportservice.bean.SportType;
import com.mstarc.wearablesport.common.VerticalCircleIndicator;
import com.mstarc.wearablesport.common.VerticalViewPager;

import java.util.ArrayList;

/**
 * Created by wangxinzhi on 17-3-13.
 */

public class TargetSettingFragment extends Fragment implements View.OnClickListener {
    VerticalViewPager mPager;
    ArrayList<TargetData> mData = new ArrayList<TargetData>();
    TargetPageAdapter mPagerAdapter;
    TargetAdapter mRecyclerViewAdapter;
    SportType mSportType;
    Button mConfirmButton;
    ProgressBar mRotateView;
    View mGpsCloseView;
    ImageView mGpsSignalView;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Context context = getActivity();
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.target_setting, container, false);
        mData.clear();
        mData.add(new LichengData(
                context, 0));
        mData.add(new TimeData(context, 0));
        mData.add(new ReliangData(context, 0));
        mPagerAdapter = new TargetPageAdapter();
        mPager = (VerticalViewPager) rootView.findViewById(R.id.pagercontainer);
        mPager.setAdapter(mPagerAdapter);
//        mPager.setCurrentItem(0, false);
        VerticalCircleIndicator indicator = (VerticalCircleIndicator) rootView.findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        mPager.setOffscreenPageLimit(3);
        mRecyclerViewAdapter = new TargetAdapter(getContext());
        mSportType = ((MainActivity) getActivity()).getSportType();
        mConfirmButton = (Button) rootView.findViewById(R.id.button);
        mGpsSignalView = (ImageView) rootView.findViewById(R.id.gps_signal);
        mGpsCloseView = rootView.findViewById(R.id.gps_close);
        mRotateView = (ProgressBar) rootView.findViewById(R.id.progressBar);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SportGoal goal = new SportGoal(((LichengData) mData.get(0)).mValue,
                        ((TimeData) mData.get(1)).mValue,
                        ((ReliangData) mData.get(2)).mValue);
                ((MainActivity) getActivity()).mSportService.setGoal(goal);
                ((MainActivity) getActivity()).showFragment(MainActivity.FRAGMENT_INDEX_PROGRESS, null);
                Settings.System.putLong(getActivity().getContentResolver(), "navigation", System.currentTimeMillis());
            }
        });
        if (mSportType == SportType.RUN_INDOOR) {
            mGpsSignalView.setVisibility(View.GONE);
            mGpsCloseView.setVisibility(View.VISIBLE);
        } else {
            mGpsSignalView.setVisibility(View.VISIBLE);
            mGpsCloseView.setVisibility(View.GONE);
        }
        int resId;
        switch (ThemeUtils.getCurrentProduct()) {
            case ThemeUtils.PRODUCT_COLOR_ROSE_GOLDEN:
                mConfirmButton.setBackgroundResource(R.drawable.selector_circle_g7_rose);
                resId = R.drawable.rotation_arc_g7_rose;
                break;
            case ThemeUtils.PRODUCT_COLOR_HIGH_BLACK:
                mConfirmButton.setBackgroundResource(R.drawable.selector_circle_g7_black);
                resId = R.drawable.rotation_arc_g7_golden;
                break;
            case ThemeUtils.PRODUCT_COLOR_APPLE_GREEN:
                mConfirmButton.setBackgroundResource(R.drawable.selector_circle_g7_apple);
                resId = R.drawable.rotation_arc_g7_green;
                break;
            default:
                mConfirmButton.setBackgroundResource(R.drawable.selector_circle_g6);
                resId = R.drawable.rotation_arc_g6;
                break;
        }
        Drawable drawable = ContextCompat.getDrawable(getContext(), resId);
        mRotateView.setIndeterminateDrawable(drawable);
        mConfirmButton.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        return rootView;
    }

    class TargetPageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object view) {
            container.removeView((View) view);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TargetViewHolder viewHolder = mRecyclerViewAdapter.onCreateViewHolder(container, 0);
            mRecyclerViewAdapter.onBindViewHolder(viewHolder, position);
            container.addView(viewHolder.itemView);
            return viewHolder.itemView;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    @Override
    public void onClick(View v) {
    }

    abstract class TargetData {
        String mName;
        Context mContext;

        public TargetData(Context context) {
            mContext = context;
        }

        public String getName() {
            return mName;
        }

        public abstract void inc(int value);

        public abstract void dec(int value);

        public abstract String getValue();
    }

    class LichengData extends TargetData {
        float mValue;

        public LichengData(Context context, int value) {
            super(context);
            mName = mContext.getString(R.string.sport_target_lichengqianmi);
            mValue = value;
        }

        @Override
        public void inc(int value) {
            mValue += value / 2f;
        }

        @Override
        public void dec(int value) {
            mValue -= value / 2f;
            if (mValue < 0) mValue = 0;
        }

        @Override
        public String getValue() {
            return String.valueOf(mValue);
        }
    }

    class TimeData extends TargetData {
        int mValue;

        public TimeData(Context context, int value) {
            super(context);
            mName = mContext.getString(R.string.sport_target_shijianfen);
            mValue = value;
        }

        @Override
        public void inc(int value) {
            mValue += value;
        }

        @Override
        public void dec(int value) {
            mValue -= value;
            if (mValue < 0) mValue = 0;
        }

        @Override
        public String getValue() {
            return String.valueOf(mValue);
        }
    }

    class ReliangData extends TargetData {
        int mValue;

        public ReliangData(Context context, int value) {
            super(context);
            mName = mContext.getString(R.string.sport_target_reliangqianka);
            mValue = value;
        }

        @Override
        public void inc(int value) {
            mValue += value * 10;
        }

        @Override
        public void dec(int value) {
            mValue -= value * 10;
            if (mValue < 0) mValue = 0;
        }

        @Override
        public String getValue() {
            return String.valueOf(mValue);
        }
    }

    class TargetAdapter extends RecyclerView.Adapter<TargetViewHolder> {
        private final LayoutInflater mInflater;
        private Context mContext;

        TargetAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public TargetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TargetViewHolder(mInflater.inflate(R.layout.target_setting_item, null));
        }

        @Override
        public void onBindViewHolder(TargetViewHolder holder, int position) {
            final TargetData data = mData.get(position);
            final TargetViewHolder itemView = holder;
            itemView.mTargetName.setText(data.mName);
            itemView.mValue.setText(data.getValue());
            itemView.mAddButton.setZoomSpeed(200);
            itemView.mDecButton.setZoomSpeed(200);
            itemView.mAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.inc(1);
                    itemView.mValue.setText(data.getValue());
                }
            });

            itemView.mDecButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.dec(1);
                    itemView.mValue.setText(data.getValue());
                }
            });

        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

    }

    class TargetViewHolder extends RecyclerView.ViewHolder {
        TextView mTargetName;
        TextView mValue;
        ZoomButton mAddButton;
        ZoomButton mDecButton;

        public TargetViewHolder(View itemView) {
            super(itemView);
            mTargetName = (TextView) itemView.findViewById(R.id.item);
            mValue = (TextView) itemView.findViewById(R.id.value);
            mAddButton = (ZoomButton) itemView.findViewById(R.id.add);
            mDecButton = (ZoomButton) itemView.findViewById(R.id.dec);
            mGpsCloseView = itemView.findViewById(R.id.gps_close);

            switch (ThemeUtils.getCurrentProduct()) {
                case ThemeUtils.PRODUCT_COLOR_ROSE_GOLDEN:
                    mDecButton.setBackgroundResource(R.drawable.selector_circle_g7_rose);
                    mAddButton.setBackgroundResource(R.drawable.selector_circle_g7_rose);
                    break;
                case ThemeUtils.PRODUCT_COLOR_HIGH_BLACK:
                    mDecButton.setBackgroundResource(R.drawable.selector_circle_g7_black);
                    mAddButton.setBackgroundResource(R.drawable.selector_circle_g7_black);
                    break;
                case ThemeUtils.PRODUCT_COLOR_APPLE_GREEN:
                    mDecButton.setBackgroundResource(R.drawable.selector_circle_g7_apple);
                    mAddButton.setBackgroundResource(R.drawable.selector_circle_g7_apple);
                    break;
                default:
                    mDecButton.setBackgroundResource(R.drawable.selector_circle_g6);
                    mAddButton.setBackgroundResource(R.drawable.selector_circle_g6);
                    break;
            }
            //updateImageView();
        }

    }

    private void updateImageView(final View view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageDrawable(drawable);
                } else {
                    view.setBackground(drawable);
                }
            }
        });
    }
}

