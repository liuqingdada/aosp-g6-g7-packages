package com.mstarc.watchservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.mstarc.watchservice.service.CommunicateService;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //startService(new Intent(this, CommunicateService.class));
    }
}
