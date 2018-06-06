package com.mstarc.wearablesport.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

/**
 * Created by wangxinzhi on 17-3-13.
 */

public class TargetConfirmButton extends Button {
    public void setProgressBar(ProgressBar progressBar) {
        this.mProgressBar = progressBar;
    }

    ProgressBar mProgressBar;

    public TargetConfirmButton(Context context) {
        super(context);
    }

    public TargetConfirmButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TargetConfirmButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (mProgressBar != null) {
            if (pressed) {
                mProgressBar.setVisibility(View.INVISIBLE);
            } else {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }
    }
}
