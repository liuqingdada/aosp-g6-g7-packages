package com.mstarc.wearablesport.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.mstarc.wearablesport.R;
import com.mstarc.wearablesport.ThemeUtils;

/**
 * Created by wangxinzhi on 17-3-13.
 */

public class TargetCofirmButtonLayout extends RelativeLayout {
    ProgressBar mProgressBar;
    TargetConfirmButton mButton;

    public TargetCofirmButtonLayout(Context context) {
        super(context);
    }

    public TargetCofirmButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TargetCofirmButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mButton = (TargetConfirmButton) findViewById(R.id.button);
        mButton.setProgressBar(mProgressBar);

        Drawable drawable = mProgressBar.getIndeterminateDrawable();
        int color = ThemeUtils.getCurrentPrimaryColor();
        ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
        drawable.clearColorFilter();
        drawable.mutate().setColorFilter(filter);
        mProgressBar.setIndeterminateDrawable(drawable);

        Drawable drawable1 = ContextCompat.getDrawable(getContext(), R.drawable.selector_circle_g6);
        drawable1.clearColorFilter();
        drawable1.mutate().setColorFilter(filter);
        mButton.setBackground(drawable1);
    }
}
