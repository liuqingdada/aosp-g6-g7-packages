package com.mstarc.wechat.wearwechat.common;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Rany Albeg Wein on 07/11/2015.
 * Since we currently don't have a LongItemClick listener for the {@link RecyclerView}
 * this is an implementation that uses a {@link GestureDetector} to notify us for long click events on one of
 * {@link RecyclerView}'s child views. It also handles single-tap-up ( a click ) and a double-click on a child view.
 */
public class RecyclerViewItemTouchListener extends RecyclerView.SimpleOnItemTouchListener {

    /**
     * A listener that will be invoked on item click events.
     */
    private OnItemClickEventListener mOnItemClickListener;
    /**
     * A gesture detector to detect and capture click events.
     */
    private GestureDetector mGestureDetector;
    /**
     * The child on which a click event happened.
     */
    private View mChildView;
    /**
     * The position of #mChildView in the adapter.
     */
    private int mChildViewAdapterPosition;

    public RecyclerViewItemTouchListener(Context context, OnItemClickEventListener listener) {
        mGestureDetector = new GestureDetector(context, new GestureDelegator());
        mOnItemClickListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();

        mChildView = recyclerView.findChildViewUnder(x, y);

        if (mChildView != null) {
            int pos = recyclerView.getChildAdapterPosition(mChildView);
            mChildViewAdapterPosition = pos;
            mGestureDetector.onTouchEvent(motionEvent);
        }
        return false;
    }

    /**
     * A listener for {@link RecyclerView} click events.
     */
    public interface OnItemClickEventListener {
        /**
         * Called when an item is long clicked.
         * @param longClickedView The long clicked view.
         * @param adapterPosition The position of the long clicked view in the adapter.
         */
        void onItemLongClick(View longClickedView, int adapterPosition);

        /**
         * Called when an item is clicked.
         * @param clickedView     The clicked view.
         * @param adapterPosition The position of the clicked view in the adapter.
         */
        void onItemClick(View clickedView, int adapterPosition);

        /**
         * Called when an item is double clicked.
         * @param doubleClickedView The clicked view.
         * @param adapterPosition   The position of the clicked view in the adapter.
         */
        void onItemDoubleClick(View doubleClickedView, int adapterPosition);
    }

    /**
     * An implementation of a {@link GestureDetector.SimpleOnGestureListener} that will handle
     * {@link GestureDetector.SimpleOnGestureListener#onLongPress(MotionEvent)}
     * {@link GestureDetector.SimpleOnGestureListener#onSingleTapConfirmed(MotionEvent)}, and
     * {@link GestureDetector.SimpleOnGestureListener#onDoubleTap(MotionEvent)}
     * methods and will invoke
     * {@link OnItemClickEventListener#onItemLongClick(View, int)}
     * {@link OnItemClickEventListener#onItemClick(View, int)}, and
     * {@link OnItemClickEventListener#onItemDoubleClick(View, int)}
     * accordingly.
     */
    private class GestureDelegator extends GestureDetector.SimpleOnGestureListener {

        @Override
        public void onLongPress(MotionEvent e) {
            if (mOnItemClickListener != null) {
                if (mChildView != null) {
                    mOnItemClickListener.onItemLongClick(mChildView, mChildViewAdapterPosition);
                }
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mOnItemClickListener != null) {
                if (mChildView != null) {
                    mOnItemClickListener.onItemDoubleClick(mChildView, mChildViewAdapterPosition);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            /**
             * Unlike OnGestureListener.onSingleTapUp(MotionEvent), this will only be called after the detector
             * is confident that the user's first tap is not followed by a second tap leading to a double-tap gesture.
             */
            if (mOnItemClickListener != null) {
                if (mChildView != null) {
                    mOnItemClickListener.onItemClick(mChildView, mChildViewAdapterPosition);
                    return true;
                }
            }
            return false;
        }
    }
}