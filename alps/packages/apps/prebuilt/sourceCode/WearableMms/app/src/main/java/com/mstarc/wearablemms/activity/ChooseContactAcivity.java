package com.mstarc.wearablemms.activity;

import android.os.Bundle;

public class ChooseContactAcivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFragment(FRAGMENT_CONTACTS_MESSAGE, null);
    }

}
