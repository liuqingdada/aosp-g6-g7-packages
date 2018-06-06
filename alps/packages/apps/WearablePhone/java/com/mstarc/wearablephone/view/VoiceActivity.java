package com.mstarc.wearablephone.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.mstarc.wearablephone.PhoneApplication;
import com.mstarc.wearablephone.R;

/**
 * Created by wangxinzhi on 17-5-16.
 */

public class VoiceActivity extends FragmentActivity {
    private static final String TAG = VoiceActivity.class.getSimpleName();
    Fragment mVoiceFragment, mVoiceResultFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyG7Theme();
        setContentView(R.layout.voice_recognize);
        mVoiceFragment = new VoiceFragment();
        mVoiceResultFragment = new VoiceResultFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_container, mVoiceFragment);
        transaction.commit();
    }

    protected void showVoiceResult(String result) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Bundle arg = new Bundle();
        arg.putString("RESULT", result);
        mVoiceResultFragment.setArguments(arg);
        transaction.replace(R.id.frame_container, mVoiceResultFragment);
        transaction.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    void applyG7Theme() {
        int theme = ((PhoneApplication)getApplication()).getThemeStyle();
        if (theme != 0) {
            setTheme(theme);
        }
    }
}
