package com.mstarc.wearablesport.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mstarc.wearablesport.R;
import com.mstarc.wearablesport.ThemeUtils;

/**
 * Created by wangxinzhi on 17-3-12.
 */

public class SportItemLayout extends RelativeLayout {
    TextView mTextName, mTextValue, mTextDanwei;

    public SportItemLayout(Context context) {
        super(context);
    }

    public SportItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SportItemLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTextName = (TextView) findViewById(R.id.sport_progress_item_name);
        mTextValue = (TextView) findViewById(R.id.sport_progress_item_value);
        mTextDanwei = (TextView) findViewById(R.id.sport_progress_item_danwei);
    }

    @Override
    public void setScaleX(float scaleX) {
        mTextName.setScaleX(scaleX);
        if(scaleX>1.01f){
//            mTextValue.setTextColor(getResources().getColor(R.color.main_highlight));
            mTextValue.setTextColor(ThemeUtils.getCurrentPrimaryColor());
            mTextValue.setScaleX(1+(scaleX-1)*4);
        }else{
            mTextValue.setTextColor(Color.WHITE);
            mTextValue.setScaleX(scaleX);
        }

    }

    @Override
    public void setScaleY(float scaleY) {
        mTextName.setScaleY(scaleY);
        if(scaleY>1.01f){
            mTextValue.setScaleY(1+(scaleY-1)*4);
        }else{
            mTextValue.setScaleY(scaleY);
        }
    }
}
