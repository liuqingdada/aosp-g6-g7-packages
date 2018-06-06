package com.mstarc.wearablesettings.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.fakewatch.ota.JobSchedulerService;
import com.mstarc.fakewatch.ota.OTAWizard;
import com.mstarc.fakewatch.ota.api.bean.OTAUpdate;
import com.mstarc.fakewatch.settings.Settings;
import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.activitys.SystemUpdateActivity;
import com.mstarc.wearablesettings.utils.ThemeUtils;


public class VersionFragment extends Fragment implements View.OnClickListener {
    private TextView mCurrentVeison;
    private TextView mStatus;
    private ImageView mDownload;
    private ImageView mLoding;
    private ImageView mLodingBg;
    private TextView mUpdateCheck;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_version, container, false);
        mUpdateCheck = (TextView) rootView.findViewById(R.id.update_check);
        mUpdateCheck.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mLodingBg = (ImageView)rootView.findViewById(R.id.loading_bg);
        mLoding = (ImageView)rootView.findViewById(R.id.loading);
        Animation mAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.img_animation);
        LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
        mAnimation.setInterpolator(lin);
        mLoding.startAnimation(mAnimation);
        OTAWizard.getInstance().setOTAUpdateListener(new JobSchedulerService.OTAUpdateListener() {
            @Override
            public void onOTAUpdate(OTAUpdate otaUpdate) {
                mUpdateCheck.setVisibility(View.GONE);
                mLoding.clearAnimation();
                mLoding.setVisibility(View.GONE);
                mLodingBg.setVisibility(View.GONE);
                ((SystemUpdateActivity)getActivity()).setOTAUpdate(otaUpdate);
                if (!TextUtils.isEmpty(otaUpdate.getType()) && otaUpdate.getType().equals("list")) {
                    mStatus.setVisibility(View.GONE);
                    mDownload.setVisibility(View.VISIBLE);
                } else {
                    mStatus.setVisibility(View.VISIBLE);
                    mDownload.setVisibility(View.GONE);
                }
            }

            @Override
            public void onOTAError() {
                mUpdateCheck.setVisibility(View.GONE);
                mLoding.clearAnimation();
                mLoding.setVisibility(View.GONE);
                mLodingBg.setVisibility(View.GONE);
                mStatus.setVisibility(View.VISIBLE);
                mDownload.setVisibility(View.GONE);
            }
        });
        OTAWizard.getInstance()
                 .checkOTA(getActivity());
        Settings settings = Settings.getInstance();
        String currentVersion =  settings.getProductVersion();
        mCurrentVeison = (TextView)rootView.findViewById(R.id.version);
        mStatus = (TextView)rootView.findViewById(R.id.status);
        mDownload = (ImageView)rootView.findViewById(R.id.download);
        updateImageView(mDownload, R.mipmap.xiazai);
        mStatus.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mCurrentVeison.setText(currentVersion);
        mDownload.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.download) {
            ((SystemUpdateActivity)getActivity()).showFragment(SystemUpdateActivity.FRAGMENT_INDEX_CONFIRM,null);
//            startActivity(new Intent(getActivity(),DownloadActivity.class));
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
}
