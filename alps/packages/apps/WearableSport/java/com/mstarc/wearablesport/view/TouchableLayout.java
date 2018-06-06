package com.mstarc.wearablesport.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * Created by wangxinzhi on 17-3-19.
 */

public class TouchableLayout extends RelativeLayout {
    public TouchableLayout(Context context) {
        super(context);
    }

    public TouchableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        for(int i =0 ;i< getChildCount();i++){
            getChildAt(i).setPressed(pressed);
        }
    }
}
