package com.mstarc.wearablemms.data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Created by wangxinzhi on 17-3-9.
 */
public class Contact {
    public Drawable mProfile;
    public String mName;
    public String mPhoneNumber;
    public Bitmap mContactPhoto;
    public String mUri;

    public Contact(Drawable mProfile, String mName, String mPhoneNumber) {
        this.mProfile = mProfile;
        this.mName = mName;
        this.mPhoneNumber = mPhoneNumber;
    }
    public Contact(String uri, String mName, String mPhoneNumber) {
        this.mUri = uri;
        this.mName = mName;
        this.mPhoneNumber = mPhoneNumber;
    }


    public Contact(Bitmap mContactPhoto, String mName, String mPhoneNumber,boolean isContact) {
        this.mContactPhoto = mContactPhoto;
        this.mName = mName;
        this.mPhoneNumber = mPhoneNumber;
    }
}
