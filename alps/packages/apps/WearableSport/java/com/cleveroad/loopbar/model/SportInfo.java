package com.cleveroad.loopbar.model;

/**
 * Created by wangxinzhi on 17-4-15.
 */

public class SportInfo extends  CategoryItem{
    public final static int ID_SPEED = 0;
    public final static int ID_STEP = 1;
    public final static int ID_DISTANCE = 2;
    public final static int ID_CAL = 3;
    public boolean hasVibrated = false;
    public int ID;

    public SportInfo(String mName, String mValue, String mDanwei, boolean isTimeReached, int ID) {
        super(mName, mValue, mDanwei, isTimeReached);
        this.ID = ID;
    }

    public SportInfo(String mName, String mValue, String mDanwei, boolean isTimeReached) {
        super(mName, mValue, mDanwei, isTimeReached);
    }
}
