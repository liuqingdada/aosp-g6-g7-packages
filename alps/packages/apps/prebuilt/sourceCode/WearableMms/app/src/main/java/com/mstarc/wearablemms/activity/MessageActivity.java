package com.mstarc.wearablemms.activity;

import android.os.Bundle;

public class MessageActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFragment(FRAGMENT_INDEX_CHAT_MESSAGE,getIntent().getExtras());
    }
}
