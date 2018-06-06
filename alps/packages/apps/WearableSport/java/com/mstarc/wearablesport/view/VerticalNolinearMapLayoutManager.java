package com.mstarc.wearablesport.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by wangxinzhi on 17-3-12.
 */

public class VerticalNolinearMapLayoutManager extends LinearLayoutManager {
    private static final String TAG = VerticalNolinearMapLayoutManager.class.getSimpleName();
    VerticalNolinear mNolinear;
    int mParentHeight;
    int mScaleIndex;

    public VerticalNolinearMapLayoutManager(Context context) {
        super(context);
    }

    public VerticalNolinearMapLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public VerticalNolinearMapLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public VerticalNolinearMapLayoutManager(Context context, int orientation, boolean reverseLayout, int focusedIndex) {
        super(context, orientation, reverseLayout);
        mScaleIndex = focusedIndex;
    }


    private void mapPosition() {
        int count = getChildCount();
        View childView;
        int targetTransY, originalCenterY;
        float targetScale;
        int parentHeight = getHeight();
        if (mParentHeight != parentHeight || mNolinear == null) {
            mParentHeight = parentHeight;
            int itemHeight = getChildAt(0).getHeight();
            //    public Rect(int left, int top, int right, int bottom) {

            Rect rect;
            if(mScaleIndex == 0){
                rect = new Rect(0, itemHeight * 2, itemHeight, 0);
            }else{
                rect = new Rect(itemHeight, itemHeight * 3, itemHeight * 2, itemHeight);
            }
            mNolinear = new VerticalNolinear(rect);
            Log.d(TAG, "parentHeight: " + parentHeight);
        }
        for (int i = 0; i < count; i++) {
            childView = getChildAt(i);
            originalCenterY = childView.getTop() + childView.getHeight() / 2;
            targetTransY = mNolinear.toY(originalCenterY) - originalCenterY;
            targetScale = mNolinear.toScale(originalCenterY);
            Log.d(TAG, "child " + count + " --  " + i + " originalCenterY: " + originalCenterY + " transY: " + targetTransY + " scale: " + targetScale);

            childView.setTranslationY(targetTransY);
            childView.setScaleX(targetScale);
            childView.setScaleY(targetScale);
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //removeAllViews();
        super.onLayoutChildren(recycler, state);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        mapPosition();
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int distance = super.scrollVerticallyBy(dy, recycler, state);
        if (distance != 0) {
            mapPosition();
        }
        return distance;
    }
}
