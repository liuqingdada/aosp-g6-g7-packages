
package com.mstarc.wearablelauncher.view.adpter;

import android.graphics.Rect;

/**
 * Created by wangxinzhi on 2015-7-29.
 */
public class Nolinear {
    private static final String TAG = "Nolinear";
    float mParentWidth;
    float mScale = 1f;
    Nolinear(int parentWidth, float scale) {
        mParentWidth = parentWidth;
        mScale = scale;
    }


    public float toScale(int x) {
        float scale = 1f;
        if(x>mParentWidth/4 && x< mParentWidth*3/4){
            scale = 1f + (mParentWidth/4 - Math.abs(x-mParentWidth/2))/(mParentWidth/4)*(mScale - 1f);
        }
        return scale;
    }
}
