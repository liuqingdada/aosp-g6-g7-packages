package com.mstarc.wearablesettings.activitys;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.mstarc.wearablesettings.R;

public class LoadingResetActivity extends BaseActivity {

    private static final String TAG = LoadingResetActivity.class.getSimpleName();
    private ImageView mLoding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_loading_watch);
        mLoding = (ImageView)findViewById(R.id.loading);
        reset();
    }

    private void reset() {
        Animation mAnimation = AnimationUtils.loadAnimation(this, R.anim.img_animation);
        LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
        mAnimation.setInterpolator(lin);
        mLoding.startAnimation(mAnimation);
        Intent intentErase = new Intent("com.android.internal.os.storage.FORMAT_AND_FACTORY_RESET");
        ComponentName COMPONENT_NAME
                = new ComponentName("android", "com.android.internal.os.storage.ExternalStorageFormatter");
        intentErase.setComponent(COMPONENT_NAME);
        startService(intentErase);
//        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
//       intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
//        sendBroadcast(intent);
    }
}
