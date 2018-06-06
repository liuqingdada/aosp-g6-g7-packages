package com.cleveroad.loopbar.widget;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.mstarc.wearablesport.view.VerticalNolinearMapLayoutManager;
import com.mstarc.wearablesport.R;


class OrientationStateVertical implements IOrientationState {

    private Integer itemHeight;

    OrientationStateVertical() {
    }

    @Override
    public LinearLayoutManager getLayoutManager(Context context, int focusedIndex) {
        return new VerticalNolinearMapLayoutManager(context, LinearLayoutManager.VERTICAL, false, focusedIndex);
//        return new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
    }

    @Override
    public int getLayoutId() {
        return R.layout.enls_view_categories_navigation_vertical;
    }

    @Override
    public boolean isItemsFitOnScreen(RecyclerView rvCategories, int itemsSize) {
        calcItemHeight(rvCategories);
        int itemsHeight = itemHeight * (itemsSize);
        int containerHeight = rvCategories.getHeight();
        return containerHeight >= itemsHeight;
    }


    private int calcItemHeight(RecyclerView rvCategories) {
        if (itemHeight == null || itemHeight == 0) {
            for (int i = 0; i < rvCategories.getChildCount(); i++) {
                itemHeight = rvCategories.getChildAt(i).getHeight();
                if (itemHeight != 0) {
                    break;
                }
            }
        }
        // in case of call before view was created
        if (itemHeight == null) {
            itemHeight = 0;
        }
        return itemHeight;
    }

    @Override
    public int getOrientation() {
        return Orientation.ORIENTATION_VERTICAL;
    }

}
