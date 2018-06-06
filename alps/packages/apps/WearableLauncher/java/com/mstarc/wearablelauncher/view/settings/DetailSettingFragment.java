package com.mstarc.wearablelauncher.view.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.mstarc.wearablelauncher.R;

/**
 * Created by wangxinzhi on 17-2-16.
 */

public class DetailSettingFragment  extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
