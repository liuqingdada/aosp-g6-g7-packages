package com.mstarc.wearablesport.view;

import android.app.Dialog;
import android.content.Context;
//import android.support.design.widget.BottomSheetDialog;
import android.graphics.Rect;
import android.support.v4.view.LayoutInflaterFactory;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.view.GestureDetector;

import com.mstarc.wearablesport.R;


/**
 * Created by wangxinzhi on 17-3-15.
 */

public class ProgressView extends RelativeLayout {
    GestureDetector mGestureDetector;
//    BottomSheetDialog mBottomSheetDialog;
    Dialog mBottomSheetDialog;
    View mStopView;

    public ProgressView(Context context) {
        super(context);
        init();
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mStopView = findViewById(R.id.pause_stop);
    }

    void init() {
        mGestureDetector = new GestureDetector(this.getContext(), new GestureDelegator());
//
////        mBottomSheetDialog = new BottomSheetDialog(getContext());
//        mBottomSheetDialog = new Dialog(getContext());
//
//        LayoutInflater inflater = LayoutInflater.from(getContext());
//        View sheetView = inflater.inflate(R.layout.start_pause_dialog, null);
//        mBottomSheetDialog.setContentView(sheetView);

    }

    class GestureDelegator extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
//            if (mBottomSheetDialog.isShowing()) {
//                mBottomSheetDialog.hide();
//            } else {
//                mBottomSheetDialog.show();
//            }
            TransitionManager.beginDelayedTransition(ProgressView.this);

            if(mStopView.getVisibility()!=View.VISIBLE){
                mStopView.setVisibility(View.VISIBLE);
            }else{
                Rect rect = new Rect();
                        mStopView.getGlobalVisibleRect(rect);
                if(!rect.contains((int)e.getX(),(int)e.getY())){
                    mStopView.setVisibility(View.GONE);
                }
            }
            return super.onSingleTapUp(e);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mGestureDetector.onTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }
}
