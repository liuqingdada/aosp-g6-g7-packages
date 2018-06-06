package com.mstarc.wearablesport.common;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by wangxinzhi on 17-3-7.
 */

public class ClickHelper {
    public static void addOnClickListenersRecursive(ViewGroup vg, View.OnClickListener listener) {
        vg.setOnClickListener(listener);
        for (int i = 0; i < vg.getChildCount(); ++i) {
            View nextChild = vg.getChildAt(i);
            if (nextChild instanceof ViewGroup) addOnClickListenersRecursive((ViewGroup) nextChild, listener);
            else nextChild.setOnClickListener(listener);
        }
    }
}
