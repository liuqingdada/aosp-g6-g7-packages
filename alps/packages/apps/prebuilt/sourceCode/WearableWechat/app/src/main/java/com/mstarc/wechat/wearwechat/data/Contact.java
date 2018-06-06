package com.mstarc.wechat.wearwechat.data;

import android.graphics.drawable.Drawable;

/**
 * Created by wangxinzhi on 17-3-9.
 */
public class Contact {
    public Drawable mProfile;
    public String mName;
    public String mPhoneNumber;

    public Contact(Drawable mProfile, String mName, String mPhoneNumber) {
        this.mProfile = mProfile;
        this.mName = mName;
        this.mPhoneNumber = mPhoneNumber;
    }
}
