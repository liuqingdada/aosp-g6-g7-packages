package com.mstarc.wearablelauncher.view.adpter;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.common.FixLinearSnapHelper;

/**
 * Created by wangxinzhi on 17-3-12.
 */

public class NolinearMapLayoutManager extends LinearLayoutManager {
    private static final String TAG = NolinearMapLayoutManager.class.getSimpleName();
    Nolinear mNolinear;
    int mParentHeight;
    Context mContext;
    RecyclerView mRecyclerView;
    LinearSnapHelper pagerSnapHelper;
    UiHandler mHandler;


    class UiHandler extends Handler {
        static final int MSG_UPDATE_POSITION = 1;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_POSITION) {
                View targetView = getChildAt(1);
                int targetOffset = ((ViewGroup) targetView.getParent()).getWidth() / 2 -
                        targetView.getWidth() / 2;
                scrollToPositionWithOffset(1, targetOffset);
            }
        }

        void updatePosition() {
            removeMessages(MSG_UPDATE_POSITION);
            sendEmptyMessageDelayed(MSG_UPDATE_POSITION, 20);
        }
    }

    public NolinearMapLayoutManager(Context context, RecyclerView recyclerView) {
        super(context, LinearLayoutManager.HORIZONTAL, false);
        mContext = context;
        mRecyclerView = recyclerView;
        pagerSnapHelper = new FixLinearSnapHelper();
        mHandler = new UiHandler();

        pagerSnapHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerView.addOnScrollListener(new RecyclerViewListener());
    }


    private void mapPosition() {
        boolean isG7 = mContext.getResources()
                               .getBoolean(R.bool.g7_target);
        int count = getChildCount();
        View childView;
        float targetScale;
        int centerX;
        int parentWidth = getWidth();
        if (mParentHeight != parentWidth || mNolinear == null) {
            mParentHeight = parentWidth;
            float scale;
            if (isG7) {
                scale = 1.25f;
            } else {
                scale = 1.45f;
            }
            mNolinear = new Nolinear(mParentHeight, scale);
            Log.d(TAG, "parentHeight: " + parentWidth);
        }
        for (int i = 0; i < count; i++) {
            ViewGroup childGroup = (ViewGroup) getChildAt(i);
            childView = childGroup.getChildAt(0);
            centerX = childGroup.getLeft() + childGroup.getWidth() / 2;
            targetScale = mNolinear.toScale(centerX);
            Log.d(TAG,
                  "child " + count + " --  " + i + " originalCenterY: " + centerX + " scale: " +
                          targetScale);
            childView.setScaleX(targetScale);
            childView.setScaleY(targetScale);
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        mapPosition();
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                                    RecyclerView.State state) {
        boolean isG7 = mContext.getResources()
                               .getBoolean(R.bool.g7_target);
        int distance = super.scrollHorizontallyBy(dx, recycler, state);
        if (distance != 0) {
            mapPosition();
        } else {
            if (dx < 0) {
                mHandler.updatePosition();
            }
        }
        if (isG7) {
            locate2Last();
        }
        return distance;
    }

    private void locate2Last() {
        View snapView = pagerSnapHelper.findSnapView(this);
        int childPosition = mRecyclerView.getChildAdapterPosition(snapView);
        int itemCount = mRecyclerView.getAdapter()
                                     .getItemCount();
        if (childPosition == itemCount - 1) {
            move(childPosition - 1, true);
        }
    }

    private boolean move;
    private int mIndex;

    private void move(int n, boolean isSmooth) {
        if (n < 0 || n >= mRecyclerView.getAdapter()
                                       .getItemCount()) {
            return;
        }
        mIndex = n;
        mRecyclerView.stopScroll();

        if (isSmooth) {
            smoothMoveToPosition(n);
        } else {
            moveToPosition(n);
        }
    }

    private void smoothMoveToPosition(int n) {

        int firstItem = findFirstVisibleItemPosition();
        int lastItem = findLastVisibleItemPosition();
        if (n <= firstItem) {
            mRecyclerView.smoothScrollToPosition(n);
        } else if (n <= lastItem) {
            int top = mRecyclerView.getChildAt(n - firstItem)
                                   .getTop();
            mRecyclerView.smoothScrollBy(0, top);
        } else {
            mRecyclerView.smoothScrollToPosition(n);
            move = true;
        }

    }

    private void moveToPosition(int n) {
        //先从RecyclerView的LayoutManager中获取第一项和最后一项的Position
        int firstItem = findFirstVisibleItemPosition();
        int lastItem = findLastVisibleItemPosition();
        //然后区分情况
        if (n <= firstItem) {
            //当要置顶的项在当前显示的第一个项的前面时
            mRecyclerView.scrollToPosition(n);
        } else if (n <= lastItem) {
            //当要置顶的项已经在屏幕上显示时
            int top = mRecyclerView.getChildAt(n - firstItem)
                                   .getTop();
            mRecyclerView.scrollBy(0, top);
        } else {
            //当要置顶的项在当前显示的最后一项的后面时
            mRecyclerView.scrollToPosition(n);
            //这里这个变量是用在RecyclerView滚动监听里面的
            move = true;
        }
    }

    class RecyclerViewListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            //在这里进行第二次滚动（最后的100米！）
            if (move) {
                move = false;
                //获取要置顶的项在当前屏幕的位置，mIndex是记录的要置顶项在RecyclerView中的位置
                int n = mIndex - findFirstVisibleItemPosition();
                if (0 <= n && n < mRecyclerView.getChildCount()) {
                    //获取要置顶的项顶部离RecyclerView顶部的距离
                    int top = mRecyclerView.getChildAt(n)
                                           .getTop();
                    //最后的移动
                    mRecyclerView.scrollBy(0, top);
                }
            }
        }
    }
}
