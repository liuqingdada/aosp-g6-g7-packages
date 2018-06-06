package com.mstarc.wearablemms.activity;

import android.os.Bundle;

public class InputNumAcivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFragment(FRAGMENT_INPUT_NUM_MESSAGE, null);
    }

}
