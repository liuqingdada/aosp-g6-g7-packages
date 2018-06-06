package com.mstarc.wearablesport.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.mstarc.wearablesport.R;

/**
 * Created by wangxinzhi on 17-3-14.
 */

public class TargetSettingIndicator extends LinearLayout {

    public TargetSettingIndicator(Context context) {
        this(context, null);
    }

    public TargetSettingIndicator(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TargetSettingIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

}
