package com.mstarc.wearablelauncher.view.settings;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mstarc.wearablelauncher.CommonManager;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.example.ExampleActivity;
import com.mstarc.wearablelauncher.view.adpter.SettingListAdapter;
import com.mstarc.wearablelauncher.view.common.DecorationSettingItem;
import com.mstarc.wearablelauncher.view.common.DepthPageTransformer;
import com.mstarc.wearablelauncher.view.common.GravitySnapHelper;
import com.mstarc.wearablelauncher.view.common.RecyclerViewItemTouchListener;

import java.util.ArrayList;

/**
 * Created by wangxinzhi on 17-2-12.
 */

public class SettingFragment extends Fragment implements RecyclerViewItemTouchListener.OnItemClickEventListener, CommonManager.IIosBoundListener {
    static final String TAG = SettingFragment.class.getSimpleName();
    public RecyclerView mSettingListView;
    private static SettingFragment INSTANCE;
    RecyclerViewItemTouchListener mRecyclerViewItemTouchListener;
    ArrayList<String> mComponentNames, mActivitesNames;
    boolean isG7Target = false;
    boolean bInited = false;
    FlightModeDialog mFlightModeDialog;
    ArrayList<String> mFlightModeDisabledComponents;
    ArrayList<SettingListAdapter.AppItem> mAppList = new ArrayList<>();
    SettingListAdapter.AppItem mMusicItem;
    int mMusicIndex;
    SettingListAdapter mAdapter;
    boolean isIOSbound = false;

    public SettingFragment() {
        mComponentNames = new ArrayList<>();
        mActivitesNames = new ArrayList<>();
        mFlightModeDisabledComponents = new ArrayList<>();
    }

    private static final int[] IconArrary = {

            R.drawable.ic_rightboard_1_phone,
            R.drawable.ic_rightboard_11_message,
            R.drawable.ic_rightboard_3_webmessage,
            R.drawable.ic_rightboard_2_sport,
            R.drawable.ic_rightboard_5_alipay,
            R.drawable.ic_rightboard_8_navi,
            R.drawable.ic_rightboard_16_record,
            R.drawable.ic_rightboard_17_radio,
            R.drawable.ic_rightboard_15_music,
            R.drawable.ic_rightboard_10_weather,
            R.drawable.ic_rightboard_12_calendar,
            R.drawable.ic_rightboard_6_heartbeat,
            R.drawable.ic_rightboard_13_setting
    };

    private static final int[] IconArraryG7 = {

            R.drawable.ic_rightboard_1_phone,
            R.drawable.ic_rightboard_11_message,
            R.drawable.ic_rightboard_3_webmessage,
            R.drawable.ic_rightboard_2_sport,
            R.drawable.ic_rightboard_5_alipay,
            R.drawable.ic_rightboard_8_navi,
            R.drawable.ic_rightboard_14_message,
            R.drawable.ic_rightboard_16_record,
            R.drawable.ic_rightboard_17_radio,
            R.drawable.ic_rightboard_15_music,
            R.drawable.ic_rightboard_10_weather,
            R.drawable.ic_rightboard_13_setting

    };

    private static final boolean[] bG7IconThemeChanged = {

            false,
            false,
            false,
            true,
            false,
            false,
            true,
            false,
            false,
            true,
            false,
            false
    };

    private void init(boolean g7) {
        if (bInited) return;
        if (g7) {

            mComponentNames.add("com.mstarc.wearablephone");
            mComponentNames.add("com.mstarc.wearablemms");
            mComponentNames.add("com.mstarc.wechat.wearwechat");
            mComponentNames.add("com.mstarc.wearablesport");
            mComponentNames.add("com.eg.android.AlipayGphone");//alipay
            mComponentNames.add("com.mstarc.plus.route_navigation");//navy
            mComponentNames.add("com.mstarc.sweat.wearablesweat");//miyu
            mComponentNames.add("com.mstarc.record.wearablerecorder");
            mComponentNames.add("com.mstarc.app.radio");
            mComponentNames.add("com.mstarc.music.wearablemusic");
            mComponentNames.add("com.mstarc.weather.weather");
            mComponentNames.add("com.mstarc.wearablesettings");


            mActivitesNames.add("PhoneActivity");
            mActivitesNames.add("MainActivity");//mms
            mActivitesNames.add("LaunchActivity");//wechat
            mActivitesNames.add("MainActivity");//sport
            mActivitesNames.add("com.alipay.watch.ui.MainActivity");//alipay
            mActivitesNames.add("MainActivity");//navi
            mActivitesNames.add("LaunchActivity");//sweet
            mActivitesNames.add("MainActivity");//Recoder
            mActivitesNames.add("RadioActivity");//radio
            mActivitesNames.add("MainActivity");//Music
            mActivitesNames.add("MainActivity");//weather
            mActivitesNames.add("activitys.SettingsActivity");//set


            mFlightModeDisabledComponents.add("com.mstarc.wearablephone");
            mFlightModeDisabledComponents.add("com.mstarc.wearablesport");
            mFlightModeDisabledComponents.add("com.mstarc.wechat.wearwechat");
            mFlightModeDisabledComponents.add("com.eg.android.AlipayGphone");//alipay
            mFlightModeDisabledComponents.add("com.mstarc.plus.route_navigation");//navy
            mFlightModeDisabledComponents.add("com.mstarc.weather.weather");
            mFlightModeDisabledComponents.add("com.mstarc.wearablemms");
            mFlightModeDisabledComponents.add("com.mstarc.sweat.wearablesweat");// miyu
            mFlightModeDisabledComponents.add("com.mstarc.app.radio");
        } else {

            mComponentNames.add("com.mstarc.wearablephone");
            mComponentNames.add("com.mstarc.wearablemms");
            mComponentNames.add("com.mstarc.wechat.wearwechat");
            mComponentNames.add("com.mstarc.wearablesport");
            mComponentNames.add("com.eg.android.AlipayGphone");//alipay
            mComponentNames.add("com.mstarc.plus.route_navigation");// navy
            mComponentNames.add("com.mstarc.record.wearablerecorder");
            mComponentNames.add("com.mstarc.app.radio");
            mComponentNames.add("com.mstarc.music.wearablemusic");
            mComponentNames.add("com.mstarc.weather.weather");
            mComponentNames.add("com.mstarc.mstarc.calendar");
            mComponentNames.add("com.mstarc.heartrate.heartrate");
            mComponentNames.add("com.mstarc.wearablesettings");

            mActivitesNames.add("PhoneActivity");
            mActivitesNames.add("MainActivity");//mms
            mActivitesNames.add("LaunchActivity");//wechat
            mActivitesNames.add("MainActivity");//sport
            mActivitesNames.add("com.alipay.watch.ui.MainActivity");//alipay
            mActivitesNames.add("MainActivity");//navi
            mActivitesNames.add("MainActivity");//Recoder
            mActivitesNames.add("RadioActivity");//radio
            mActivitesNames.add("MainActivity");//Music
            mActivitesNames.add("MainActivity");//weather
            mActivitesNames.add("FullscreenActivity");//calendar
            mActivitesNames.add("MainActivity");//heartrate
            mActivitesNames.add("activitys.SettingsActivity");//set

            mFlightModeDisabledComponents.add("com.mstarc.wearablephone");
            mFlightModeDisabledComponents.add("com.mstarc.wearablesport");
            mFlightModeDisabledComponents.add("com.mstarc.wechat.wearwechat");
            mFlightModeDisabledComponents.add("com.eg.android.AlipayGphone");//alipay
            mFlightModeDisabledComponents.add("com.mstarc.plus.route_navigation");// navy
            mFlightModeDisabledComponents.add("com.mstarc.weather.weather");
            mFlightModeDisabledComponents.add("com.mstarc.wearablemms");
            mFlightModeDisabledComponents.add("com.mstarc.app.radio");
        }
        int appNumber = mComponentNames.size();
        String[] lables = getResources().getStringArray(R.array.settinglist);
        mAppList.clear();
        for (int i = 0; i < appNumber; i++) {
            boolean isdisabledinIos = mComponentNames.get(i).equals("com.mstarc.music.wearablemusic");
            if (isG7Target) {


                  if(i==4)
                    {
                   SettingListAdapter.AppItem item = new SettingListAdapter.AppItem(new ComponentName(mComponentNames.get(i), mActivitesNames.get(i)),
                        IconArraryG7[i],
                        lables[i],
                        mFlightModeDisabledComponents.contains(mComponentNames.get(i)),
                        isdisabledinIos,
                        bG7IconThemeChanged[i]);
                mAppList.add(item);
                if(isdisabledinIos) {
                    mMusicItem = item;
                    mMusicIndex = i;
                }
                Log.d(TAG, "add "+item.mComponentName.getClassName());
                    }else {
                SettingListAdapter.AppItem item = new SettingListAdapter.AppItem(new ComponentName(mComponentNames.get(i), mComponentNames.get(i) + "." + mActivitesNames.get(i)),
                        IconArraryG7[i],
                        lables[i],
                        mFlightModeDisabledComponents.contains(mComponentNames.get(i)),
                        isdisabledinIos,
                        bG7IconThemeChanged[i]);
                mAppList.add(item);
                if(isdisabledinIos) {
                    mMusicItem = item;
                    mMusicIndex = i;
                }
                Log.d(TAG, "add "+item.mComponentName.getClassName());
                     }
            } else {


                  if(i==4)
                   {
                SettingListAdapter.AppItem item = new SettingListAdapter.AppItem(new ComponentName(mComponentNames.get(i), mActivitesNames.get(i)),
                        IconArrary[i],
                        lables[i],
                        mFlightModeDisabledComponents.contains(mComponentNames.get(i)),
                        isdisabledinIos, false);
                mAppList.add(item);
                if(isdisabledinIos) {
                    mMusicItem = item;
                    mMusicIndex = i;
                }
                Log.d(TAG, "add "+item.mComponentName.getClassName());
                  }else{
                SettingListAdapter.AppItem item = new SettingListAdapter.AppItem(new ComponentName(mComponentNames.get(i), mComponentNames.get(i) + "." + mActivitesNames.get(i)),
                        IconArrary[i],
                        lables[i],
                        mFlightModeDisabledComponents.contains(mComponentNames.get(i)),
                        isdisabledinIos, false);
                mAppList.add(item);
                if(isdisabledinIos) {
                    mMusicItem = item;
                    mMusicIndex = i;
                }
                Log.d(TAG, "add "+item.mComponentName.getClassName());
                   }
            }
        }
        boundTypeChanged(isIOSbound);

        bInited = true;
    }

    public static SettingFragment getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SettingFragment();
        }
        return INSTANCE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isG7Target = getActivity().getResources().getBoolean(R.bool.g7_target);
        mAdapter = new SettingListAdapter(getActivity(), mAppList);
        isIOSbound = CommonManager.getInstance(getContext().getApplicationContext()).isIOSbound();
        init(isG7Target);
        for(SettingListAdapter.AppItem item: mAppList){
            Log.d(TAG,""+item.mComponentName);
        }
        Log.d(TAG, "onCreateView.this:" + this);
        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.setting, container, false);
        mSettingListView = (RecyclerView)
                rootView.findViewById(R.id.setting_list_view);
        mSettingListView.setAdapter(mAdapter);
        mSettingListView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mSettingListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.setting_divider));
        mRecyclerViewItemTouchListener = new RecyclerViewItemTouchListener(getActivity(), this);
        mSettingListView.addOnItemTouchListener(mRecyclerViewItemTouchListener);
        rootView.setTag(DepthPageTransformer.ITEM_RIGHT_OR_BOTTOM);
        mFlightModeDialog = new FlightModeDialog(getContext());
        GravitySnapHelper gravitySnapHelper = new GravitySnapHelper(Gravity.TOP);
        gravitySnapHelper.attachToRecyclerView(mSettingListView);
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean iosbound = CommonManager.getInstance(getContext().getApplicationContext()).isIOSbound();
        if(isIOSbound != iosbound){
            isIOSbound = iosbound;
            boundTypeChanged(isIOSbound);
        }
        View rootView = getView();
        try {
            if (rootView != null && rootView instanceof RecyclerView) {
                ((RecyclerView)rootView).scrollToPosition(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {
        Log.d(TAG, "onItemLongClick " + adapterPosition);
    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Log.d(TAG, "onItemClick " + adapterPosition);
        if (mComponentNames.get(adapterPosition) == "todo") return;
        if (CommonManager.getInstance(getContext().getApplicationContext()).getmPowerMode() == CommonManager.POWERMODE_FLIGHT
                && mAppList.get(adapterPosition).disabledInflightMode) {
            if (mFlightModeDialog.isShowing()) {
                mFlightModeDialog.dismiss();
            }
            mFlightModeDialog.show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(mAppList.get(adapterPosition).mComponentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        Bundle translateBundle =
                ActivityOptions.makeCustomAnimation(getActivity(),
                        R.anim.slide_in_left, R.anim.slide_out_left).toBundle();
        getActivity().startActivity(intent, translateBundle);
//        startActivity(intent);
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {
        Log.d(TAG, "onItemDoubleClick " + adapterPosition);

    }

    @Override
    public void boundTypeChanged(boolean isios) {
        Log.d(TAG, "isios: "+isios);
        if (isios) {
            if (mAppList.contains(mMusicItem)) {
                mAppList.remove(mMusicItem);
                Log.d(TAG, "remove music " +mAppList.size());
                mAdapter.notifyDataSetChanged();
            }
        } else {
            if (!mAppList.contains(mMusicItem)) {
                mAppList.add(mMusicIndex, mMusicItem);
                Log.d(TAG, "add music " +mAppList.size());
                mAdapter.notifyDataSetChanged();
            }
        }

    }
}
