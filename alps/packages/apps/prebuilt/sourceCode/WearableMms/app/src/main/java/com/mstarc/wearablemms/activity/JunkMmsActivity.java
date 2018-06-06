package com.mstarc.wearablemms.activity;

import android.os.Bundle;

public class JunkMmsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFragment(FRAGMENT_JUNK_MESSAGE, null);
    }
}
