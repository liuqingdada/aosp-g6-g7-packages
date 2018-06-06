package com.cleveroad.loopbar.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

interface IOrientationState {

    LinearLayoutManager getLayoutManager(Context context, int focusedIndex);

    int getLayoutId();

    /**
     * Check if all items of recyclerView fit on screen
     *
     * @param container recyclerView with items
     * @param itemsSize count of items
     */
    boolean isItemsFitOnScreen(RecyclerView container, int itemsSize);

    @Orientation
    int getOrientation();

}
