package com.mstarc.music.wearablemusic.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RadioButton;

import com.mstarc.music.ThemeUtils;
import com.mstarc.music.wearablemusic.R;

/**
 * description
 * <p/>
 * Created by andyding on 2017/5/29.
 */

public class MyRadioButton extends RadioButton {

    private int mDrawableSizeX;// xml文件中设置的大小
    private int mDrawableSizeY;// xml文件中设置的大小
    Drawable drawableLeft = null, drawableTop = null, drawableRight = null, drawableBottom = null;

    public MyRadioButton(Context context) {
        this(context, null, 0);
    }

    public MyRadioButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyRadioButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MyRadioButton);

        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            Log.i("MyRadioButton", "attr:" + attr);
            switch (attr) {
                case R.styleable.MyRadioButton_drawableSizeX:
                    mDrawableSizeX = a.getDimensionPixelSize(R.styleable.MyRadioButton_drawableSizeX, 50);
                    Log.i("MyRadioButton", "mDrawableSizeX:" + mDrawableSizeX);
                    break;
                case R.styleable.MyRadioButton_drawableSizeY:
                    mDrawableSizeY = a.getDimensionPixelSize(R.styleable.MyRadioButton_drawableSizeY, 50);
                    Log.i("MyRadioButton", "mDrawableSizeX:" + mDrawableSizeY);
                    break;
                case R.styleable.MyRadioButton_drawableTop:
                    drawableTop = a.getDrawable(attr);
                    break;
                case R.styleable.MyRadioButton_drawableBottom:
                    drawableRight = a.getDrawable(attr);
                    break;
                case R.styleable.MyRadioButton_drawableRight:
                    drawableBottom = a.getDrawable(attr);
                    break;
                case R.styleable.MyRadioButton_drawableLeft:
                    drawableLeft = a.getDrawable(attr);
                    break;
                default :
                    break;
            }
        }
        a.recycle();
        setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (checked) {
            if (drawableTop != null) {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                drawableTop.clearColorFilter();
                drawableTop.mutate().setColorFilter(filter);
                setTextColor(color);
            }
            //setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
        } else {
            setTextColor(Color.WHITE);
            if (drawableTop != null) {
                drawableTop.clearColorFilter();
            }
            //setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
        }
    }

    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left,
                                                        Drawable top, Drawable right, Drawable bottom) {

        if (left != null) {
            left.setBounds(0, 0, mDrawableSizeX, mDrawableSizeY);
        }
        if (right != null) {
            right.setBounds(0, 0, mDrawableSizeX, mDrawableSizeY);
        }
        if (top != null) {
            top.setBounds(0, 0, mDrawableSizeX, mDrawableSizeY);
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, mDrawableSizeX, mDrawableSizeY);
        }
        setCompoundDrawables(left, top, right, bottom);
    }
}
