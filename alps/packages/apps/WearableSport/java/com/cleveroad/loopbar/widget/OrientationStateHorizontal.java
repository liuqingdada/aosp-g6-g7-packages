package com.cleveroad.loopbar.widget;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.mstarc.wearablesport.R;


class OrientationStateHorizontal implements IOrientationState {

    private Integer itemWidth;

    OrientationStateHorizontal() {
    }

    @Override
    public LinearLayoutManager getLayoutManager(Context context, int focusedIndex) {
        return new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
    }

    @Override
    public int getLayoutId() {
        return R.layout.enls_view_categories_navigation_horizontal;
    }

    @Override
    public boolean isItemsFitOnScreen(RecyclerView recyclerView, int itemsSize) {
        calcItemWidth(recyclerView);
        int itemsWidth = itemWidth * (itemsSize);
        int containerWidth = recyclerView.getMeasuredWidth();
        return containerWidth >= itemsWidth;
    }


    @Override
    public int getOrientation() {
        return Orientation.ORIENTATION_HORIZONTAL;
    }

    //very big duct tape
    private int calcItemWidth(RecyclerView rvCategories) {
        if (itemWidth == null || itemWidth == 0) {
            for (int i = 0; i < rvCategories.getChildCount(); i++) {
                itemWidth = rvCategories.getChildAt(i).getWidth();
                if (itemWidth != 0) {
                    break;
                }
            }
        }
        // in case of call before view was created
        if (itemWidth == null) {
            itemWidth = 0;
        }
        return itemWidth;
    }
}
