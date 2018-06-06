package com.mstarc.wearablesettings.activitys;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.utils.ThemeUtils;

public class ResetWatchActivity extends Activity {

    private ImageView mCancel;
    private ImageView mSure;
    private ImageView mReset;
    private TextView mTitle;
    private TextView mResetSystem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_watch);
        mReset = (ImageView)findViewById(R.id.reset);
        mSure = (ImageView)findViewById(R.id.imagesure);
        mCancel = (ImageView)findViewById(R.id.imagecancel);
        mTitle = (TextView)findViewById(R.id.title);
        mTitle.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mResetSystem = (TextView)findViewById(R.id.resetsystem);
        updateImageView(mReset, R.mipmap.queding);
        updateImageView(mSure, R.mipmap.queding);
        updateImageView(mCancel, R.mipmap.quxiao);
    }

    public void sureClick(View v){
        startActivity(new Intent(ResetWatchActivity.this,LoadingResetActivity.class));
    }

    public void reset(View v){
        mSure.setVisibility(View.VISIBLE);
        mCancel.setVisibility(View.VISIBLE);
        mTitle.setVisibility(View.GONE);
        mReset.setVisibility(View.GONE);
        mResetSystem.setVisibility(View.VISIBLE);
    }

    public void cancelClick(View v){
        finish();
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
