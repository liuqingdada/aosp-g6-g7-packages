package com.mstarc.wearablelauncher.view.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import com.mstarc.wearablelauncher.R;

/**
 * Created by wangxinzhi on 17-2-16.
 */

public class MstarcPreferenceHead extends Preference {


    public MstarcPreferenceHead(Context context) {
        this(context,null);
    }

    public MstarcPreferenceHead(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.setting_detail_header);
    }



    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
    }
}
