package com.mstarc.wearablelauncher.view.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.transition.Slide;

import com.mstarc.wearablelauncher.R;

/**
 * Created by wangxinzhi on 17-2-16.
 */

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setupWindowAnimations();
    }
    private void setupWindowAnimations() {
        Slide slide = new Slide();
        getWindow().setEnterTransition(slide);

        slide.setDuration(1000);
        getWindow().setReturnTransition(slide);
    }
}
