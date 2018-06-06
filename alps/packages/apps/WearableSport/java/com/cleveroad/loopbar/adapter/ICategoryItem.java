package com.cleveroad.loopbar.adapter;

import android.graphics.drawable.Drawable;

/**
 * Interface for item in LoopBar
 */
public interface ICategoryItem {

    String getName();
    String getValue();
    String getDanwei();
    boolean isTargetReached();
    void setTargetReached();
}
