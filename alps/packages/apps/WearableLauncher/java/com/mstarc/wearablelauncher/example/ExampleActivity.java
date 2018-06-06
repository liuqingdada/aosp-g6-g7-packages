package com.mstarc.wearablelauncher.example;

import android.app.Activity;
import android.os.Bundle;
import android.transition.Slide;
import android.view.View;
import android.widget.ImageView;

import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.common.RippleLayout;

/**
 * Created by wangxinzhi on 17-3-5.
 */

public class ExampleActivity extends Activity{
    ImageView imageview;
    RippleLayout layout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setupWindowAnimations();
        setContentView(R.layout.example);
        layout = (RippleLayout) findViewById(R.id.ripple_layout);
        imageview = (ImageView) findViewById(R.id.centerImage);
        imageview.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (layout.isRippleAnimationRunning()) {
                    layout.stopRippleAnimation();
                } else {
                    layout.startRippleAnimation();
                }
            }
        });

    }
    private void setupWindowAnimations() {
        Slide slide = new Slide();
        getWindow().setEnterTransition(slide);
        slide.setDuration(1000);
        getWindow().setReturnTransition(slide);
    }
}
