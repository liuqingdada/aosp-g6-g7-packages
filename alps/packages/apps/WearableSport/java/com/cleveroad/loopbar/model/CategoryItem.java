package com.cleveroad.loopbar.model;

import android.graphics.drawable.Drawable;

import com.cleveroad.loopbar.adapter.ICategoryItem;

public class CategoryItem implements ICategoryItem {

    public String mName, mValue, mDanwei;
    private boolean isTimeReached;

    public CategoryItem(String mName, String mValue, String mDanwei, boolean isTimeReached) {
        this.mName = mName;
        this.mValue = mValue;
        this.mDanwei = mDanwei;
        this.isTimeReached = isTimeReached;
    }


    @Override
    public String getName() {
        return mName;
    }

    @Override
    public String getValue() {
        return mValue;
    }

    @Override
    public String getDanwei() {
        return mDanwei;
    }

    @Override
    public boolean isTargetReached() {
        return isTimeReached;
    }

    @Override
    public void setTargetReached() {
        isTimeReached = true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CategoryItem && ((CategoryItem) o).mName.equals(mName);
    }
}
