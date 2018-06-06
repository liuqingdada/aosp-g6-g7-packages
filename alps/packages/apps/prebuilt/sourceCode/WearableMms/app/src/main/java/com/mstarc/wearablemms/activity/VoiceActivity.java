package com.mstarc.wearablemms.activity;

import android.os.Bundle;

public class VoiceActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFragment(FRAGMENT_INDEX_MESSAGE_VOICE,getIntent().getExtras());
    }
}
