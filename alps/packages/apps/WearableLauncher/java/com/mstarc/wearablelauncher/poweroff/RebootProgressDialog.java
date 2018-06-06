package com.mstarc.wearablelauncher.poweroff;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;

import com.mstarc.wearablelauncher.R;


/**
 * Created by wangxinzhi on 17-6-4.
 */

public class RebootProgressDialog extends Dialog {
    private int mLayoutId;
    private ObjectAnimator mAnimator;

    public RebootProgressDialog(@NonNull Context context, int layoutId) {
        super(context, R.style.Dialog);
        mLayoutId = layoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), mLayoutId, null);
        setContentView(view);
        setCanceledOnTouchOutside(false);

        Window win = getWindow();

        WindowManager.LayoutParams lp = win.getAttributes();
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        lp.height = dm.heightPixels;
        lp.width = dm.widthPixels;
        win.setAttributes(lp);
        win.setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        win.setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        View rotationview = findViewById(R.id.rotation);
        mAnimator = ObjectAnimator.ofFloat(rotationview, "rotation", 0, 360);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setDuration(1000).setRepeatCount(ObjectAnimator.INFINITE);
    }

    @Override
    public void show() {
        super.show();
        Intent i = new Intent();
        i.setAction("com.mstart.launcher.poweroff.keepFOREGROUND");
        getContext().sendBroadcast(i);
        mAnimator.start();
    }

    @Override
    public void hide() {
        super.hide();
        mAnimator.end();
    }
}
