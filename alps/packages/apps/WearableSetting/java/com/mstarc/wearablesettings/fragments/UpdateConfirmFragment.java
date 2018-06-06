package com.mstarc.wearablesettings.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.activitys.DownloadActivity;
import com.mstarc.wearablesettings.activitys.SystemUpdateActivity;
import com.mstarc.fakewatch.settings.Settings;
import com.mstarc.fakewatch.ota.api.bean.OTAUpdate;
import com.mstarc.wearablesettings.utils.ThemeUtils;

import java.util.List;

public class UpdateConfirmFragment extends Fragment implements OnClickListener{
    private TextView mContent;
    private ImageView mSure;
    private ImageView mCancel;
    private ScrollView mContentScroll;
    private ImageView mStartUpdate;
    private TextView mTip1;
    private TextView mTip2;
    private TextView mVerison;
    private BatteryReceiver mBatteryReceiver;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_update_confirm, container, false);
        Settings settings = Settings.getInstance();
        String curVersion =  settings.getProductVersion();
        mVerison = (TextView)rootView.findViewById(R.id.version);
        mVerison.setText(curVersion);
        mContentScroll = (ScrollView)rootView.findViewById(R.id.scrollcontent);
        mSure = (ImageView) rootView.findViewById(R.id.imagesure);
        mCancel = (ImageView)rootView.findViewById(R.id.imagecancel) ;
        mContent = (TextView)rootView.findViewById(R.id.content);
        mTip1 = (TextView)rootView.findViewById(R.id.tip1);
        mTip2 = (TextView)rootView.findViewById(R.id.tip2);
        mStartUpdate = (ImageView)rootView.findViewById(R.id.startupdate);
        updateImageView(mSure, R.mipmap.queding);
        updateImageView(mCancel, R.mipmap.quxiao);
        updateImageView(mStartUpdate, R.mipmap.queding);

        mSure.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        mStartUpdate.setOnClickListener(this);
        OTAUpdate otaUpdate = ((SystemUpdateActivity)getActivity()).getOTAUpdate();
        List<OTAUpdate.DatasEntity> datas = otaUpdate.getDatas();
        if(datas != null) {
            StringBuffer s = new StringBuffer();
            for (OTAUpdate.DatasEntity data : datas) {
                s.append(data.getMoreinfo());
                s.append("\n");
            }
            mContent.setText(s.toString());
            if(datas.size() == 1 && datas.get(0).getUpdatetype() == 0) {
                mVerison.setText(datas.get(0).getSoftversion());
            }
        }

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        mBatteryReceiver = new BatteryReceiver();
        getActivity().registerReceiver(mBatteryReceiver, intentFilter);
        return rootView;
    }

    @Override
    public void onStop() {
        if(mBatteryReceiver != null) {
            getActivity().unregisterReceiver(mBatteryReceiver);
        }
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.imagesure:
                mContentScroll.setVisibility(View.GONE);
                mVerison.setVisibility(View.GONE);
                mSure.setVisibility(View.GONE);
                mCancel.setVisibility(View.GONE);
                mTip1.setVisibility(View.VISIBLE);
                mTip2.setVisibility(View.VISIBLE);
                mStartUpdate.setVisibility(View.VISIBLE);
                break;
            case R.id.imagecancel:
                getActivity().finish();
                break;
            case R.id.startupdate:
                startActivity(new Intent(getActivity(), DownloadActivity.class));
                getActivity().finish();
                break;
            default:
                break;
        }
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            //判断它是否是为电量变化的Broadcast Action
            if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                //获取当前电量
                int level = intent.getIntExtra("level", 0);
                //电量的总刻度
                int scale = intent.getIntExtra("scale", 100);
                if((level*100)/scale < 50) {
                    Toast.makeText(context,context.getString(R.string.power_bit_low),Toast.LENGTH_LONG).show();
                }
            }
        }

    }
}
