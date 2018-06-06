package com.mstarc.weather.weather;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mstarc.weather.weather.Utils.ThemeUtils;
import com.mstarc.weather.weather.manager.WeatherManager;

import java.util.ArrayList;

public class MainActivity extends Activity implements WeatherManager.DataLoadListener {

    ViewPager mViewPager;

    // View list in viewPager
    ArrayList<View> mItemViewList;
    private static final int[] mGuidePics = {R.layout.weather_layout, R.layout.weather_layout};

    // Dot image view list
    private ImageView[] mDotImageViews;

    private LinearLayout mDotViewViewgroup;


    private ImageView mLocation;
    private TextView mLocationName;
    private TextView mLocationDate;
    private TextView mLocationTime;

    private ImageView mCurrentWeatherIcon;
    private TextView mCurrentWeatherTmp;
    private TextView mCurrentQulity;

    private TextView mNextOneWeekDay;
    private ImageView mNextOneIcon;
    private TextView mNextOneTmp;

    private TextView mNextTwoWeekDay;
    private ImageView mNextTwoIcon;
    private TextView mNextTwoTmp;

    private TextView mNextThreeWeekDay;
    private ImageView mNextThreeIcon;
    private TextView mNextThreeTmp;

    private GuideViewPagerAdapter mAdapter;
    LinearLayout mNoDataLayout;
    ImageView mGetData;
    TextView mNoDataPrompt;
    ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("dingyichen", "================onCreate");
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mDotViewViewgroup = (LinearLayout) findViewById(R.id.view_page_indicator);

        mNoDataLayout = (LinearLayout) findViewById(R.id.no_data_layout);
        mGetData = (ImageView) findViewById(R.id.get_data);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mNoDataPrompt = (TextView) findViewById(R.id.no_data_prompt);
        mNoDataPrompt.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mProgressBar.setVisibility(View.VISIBLE);

        mGetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshData();
                mProgressBar.setVisibility(View.VISIBLE);
                mNoDataPrompt.setVisibility(View.INVISIBLE);
            }
        });

        WeatherManager.getInstance().init(this);
        WeatherManager.getInstance().addListerner(this);
        WeatherManager.getInstance().retreiveData();
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d("dingyichen", "================onStop");
        onDestroy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("dingyichen", "================onDestroy");
        WeatherManager.getInstance().removeListener();
    }

	@Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    private void refreshData() {
        WeatherManager.getInstance().removeListener();
        WeatherManager.getInstance().addListerner(this);
        WeatherManager.getInstance().retreiveData();
    }

    // Init view pager
    private void initViewPager() {
        mItemViewList = new ArrayList<>();
        for (int i = 0; i < mGuidePics.length; i++) {
            View view = LayoutInflater.from(this).inflate(mGuidePics[i], null);
            mItemViewList.add(view);
        }

        mAdapter = new GuideViewPagerAdapter();
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < mGuidePics.length; i++) {
                    if (i == position) {
                        mDotImageViews[i].setBackgroundResource(R.mipmap.icon_indicater_now);
                    } else {
                        mDotImageViews[i].setBackgroundResource(R.mipmap.icon_indication_grey);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    // Init bottom dot views
    private void initDotViews() {
        mDotImageViews = new ImageView[mItemViewList.size()];
        for (int i = 0; i < mItemViewList.size(); i++) {
            ImageView view = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dip2px(this, 10), dip2px(this, 10));
            params.setMargins(dip2px(this, 20), 0, dip2px(this, 20), 0);
            view.setLayoutParams(params);

            mDotImageViews[i] = view;
            if (i == 0) {
                mDotImageViews[i].setBackgroundResource(R.mipmap.icon_indicater_now);
            } else {
                mDotImageViews[i].setBackgroundResource(R.mipmap.icon_indication_grey);
            }
            mDotViewViewgroup.addView(mDotImageViews[i]);
        }
    }

    class GuideViewPagerAdapter extends PagerAdapter {
        @Override
        public void destroyItem(View v, int position, Object arg2) {
            ((ViewPager) v).removeView(mItemViewList.get(position));
        }

        @Override
        public void finishUpdate(View arg0) {
        }

        @Override
        public int getCount() {
            if (bOnlyHomePosition) {
                return 1;
            } else {
                return mItemViewList.size();
            }
        }

        @Override
        public Object instantiateItem(View v, int position) {
            View view = LayoutInflater.from(v.getContext()).inflate(R.layout.weather_layout, null);
            mLocation = (ImageView) view.findViewById(R.id.weather_location);
            mLocationName = (TextView) view.findViewById(R.id.weather_location_name);
            mLocationDate = (TextView) view.findViewById(R.id.weather_date);
            mLocationTime = (TextView) view.findViewById(R.id.weather_time);

            mCurrentWeatherIcon = (ImageView) view.findViewById(R.id.weather_current_icon);
            mCurrentWeatherTmp = (TextView) view.findViewById(R.id.weather_current_tmp);
            mCurrentQulity = (TextView) view.findViewById(R.id.weather_current_quality);

            mNextOneWeekDay = (TextView) view.findViewById(R.id.weather_next_one_week_day);
            mNextOneIcon = (ImageView) view.findViewById(R.id.weather_next_one_icon);
            mNextOneTmp = (TextView) view.findViewById(R.id.weather_next_one_tmp);

            mNextTwoWeekDay = (TextView) view.findViewById(R.id.weather_next_two_week_day);
            mNextTwoIcon = (ImageView) view.findViewById(R.id.weather_next_two_icon);
            mNextTwoTmp = (TextView) view.findViewById(R.id.weather_next_two_tmp);

            mNextThreeWeekDay = (TextView) view.findViewById(R.id.weather_next_three_week_day);
            mNextThreeIcon = (ImageView) view.findViewById(R.id.weather_next_three_icon);
            mNextThreeTmp = (TextView) view.findViewById(R.id.weather_next_three_tmp);
            Weather weather = null;
            if (position == 0) {
                weather = WeatherManager.getInstance().getCurrentLocationWeather();
            } else if (position == 1) {
                weather = WeatherManager.getInstance().getHomeLocationWeather();
            }
            if (weather != null) {
                if (weather.getTemperature() == null) {
                    mNoDataLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            //mNoDataLayout.setVisibility(View.VISIBLE);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            //imNoDataPrompt.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    mLocation.setImageResource(WeatherManager.getInstance().getLocationIcon(position));
                    mLocationName.setText(weather.getLocationName());
                    mLocationDate.setText(weather.getLocationDate());
                    mLocationTime.setText(weather.getLocationTime());
                    updateImageView(mCurrentWeatherIcon, WeatherManager.getInstance().getWeatherBasedonType(weather.getWeatherType(), true));
                    mCurrentWeatherTmp.setText(weather.getTemperature() + "°");
                    String quality = weather.getWeatherQuality() + "  " +
                            WeatherManager.getInstance().getDescriptionBasedOnQuality(weather.getWeatherQuality());
                    mCurrentQulity.setText(quality);
                    mCurrentQulity.setBackgroundResource(WeatherManager.getInstance().getIconBasedOnQuality(weather.getWeatherQuality()));

                    mNextOneWeekDay.setText(weather.getNextOneDayWeekDay());
                    mNextOneTmp.setText(weather.getNextOneDayTmp() + "°");
                    updateImageView(mNextOneIcon, WeatherManager.getInstance().getWeatherBasedonType(weather.getNextOneDayWeatherType(), false));

                    mNextTwoWeekDay.setText(weather.getNextTwoDayWeekDay());
                    mNextTwoTmp.setText(weather.getNextTwoDayTmp() + "°");
                    updateImageView(mNextTwoIcon, WeatherManager.getInstance().getWeatherBasedonType(weather.getNextTwoDayWeatherType(), false));

                    mNextThreeWeekDay.setText(weather.getNextThreeDayWeekDay());
                    mNextThreeTmp.setText(weather.getNextThreeDayTmp() + "°");
                    updateImageView(mNextThreeIcon, WeatherManager.getInstance().getWeatherBasedonType(weather.getNextThreeDayWeatherType(), false));
                }
            }

            ((ViewPager) v).addView(view);
            return view;
        }

        @Override
        public boolean isViewFromObject(View v, Object arg1) {
            return v == arg1;
        }


        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(MainActivity.this, resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(final Activity activity, final float dpValue) {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final float scale = metrics.density;
        return (int) ((dpValue * scale) + 0.5f);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dp2px(Context context, float dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((dp * scale) + 0.5f);
    }

    @Override
    public void onDateLoadCompleted() {
        Log.d("dingyichen", "onDateLoadCompleted !!");
        mNoDataLayout.post(new Runnable() {
            @Override
            public void run() {
                mHandle.removeMessages(MSG_REFRESH_ERROR);
                Log.d("dingyichen", "onDateLoadCompleted home  weather = " + WeatherManager.getInstance().getHomeLocationWeather());
                if (TextUtils.isEmpty(WeatherManager.getInstance().getHomeLocationWeather().getLocationName())) {
                    bOnlyHomePosition = true;
                    mDotViewViewgroup.setVisibility(View.GONE);
                } else {
                    bOnlyHomePosition = false;
                    mDotViewViewgroup.setVisibility(View.VISIBLE);
                }
                mNoDataLayout.setVisibility(View.INVISIBLE);
                initViewPager();
                initDotViews();
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onLoadFailed() {
        Log.d("dingyichen", "onLoadFailed !!");
        mHandle.sendEmptyMessageDelayed(MSG_REFRESH_ERROR, 2000);
    }

    private static final int MSG_REFRESH_DATA = 0;
    private static final int MSG_REFRESH_ERROR = 1;

    private int mRetryNum = 0;
    private boolean bOnlyHomePosition;

    Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_REFRESH_DATA:
                    if (mRetryNum > 3) {
                        sendEmptyMessage(MSG_REFRESH_ERROR);
                    } else {
                        mProgressBar.setVisibility(View.VISIBLE);
                        sendEmptyMessageDelayed(MSG_REFRESH_DATA, 2000);
                    }
                    mRetryNum++;
                    break;
                case MSG_REFRESH_ERROR:
                    // showErrorLayout();
                    mNoDataLayout.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                   // mNoDataPrompt.setVisibility(View.VISIBLE); //wyg
                    break;
            }
        }
    };

    private void showErrorLayout() {
        Log.d("dingyichen", "showErrorLayout !!");
        mNoDataLayout.post(new Runnable() {
            @Override
            public void run() {
                mNoDataLayout.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
               // mNoDataPrompt.setVisibility(View.VISIBLE); //wyg
            }
        });
    }
}

