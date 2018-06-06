package com.mstarc.wechat.wearwechat.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.mstarc.wechat.wearwechat.R;


/**
 * Created by wangxinzhi on 17-8-13.
 */

public class CustomScrollbarRecyclerView extends RecyclerView {
    int mCustomMarginLeft;
    int mCustomMarginTop;
    int mCustomMarginRight;
    int mCustomMarginBottom;

    public CustomScrollbarRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public CustomScrollbarRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    protected void onDrawVerticalScrollBar(Canvas canvas, Drawable scrollBar,
                                           int l, int t, int r, int b) {
        scrollBar.setBounds(l + mCustomMarginLeft, t + mCustomMarginTop, r - mCustomMarginRight, b - mCustomMarginBottom);
        scrollBar.draw(canvas);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null && context != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.CustomScrollbarRecyclerView,
                    0, 0
            );
            mCustomMarginLeft = a.getDimensionPixelSize(R.styleable.CustomScrollbarRecyclerView_marginleft, 0);
            mCustomMarginTop = a.getDimensionPixelSize(R.styleable.CustomScrollbarRecyclerView_margintop, 0);
            mCustomMarginRight = a.getDimensionPixelSize(R.styleable.CustomScrollbarRecyclerView_marginright, 0);
            mCustomMarginBottom = a.getDimensionPixelSize(R.styleable.CustomScrollbarRecyclerView_marginbottom, 0);
            a.recycle();
        }
    }
}
