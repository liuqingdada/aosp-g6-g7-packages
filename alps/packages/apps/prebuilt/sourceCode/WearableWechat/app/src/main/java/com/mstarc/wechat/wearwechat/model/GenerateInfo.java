package com.mstarc.wechat.wearwechat.model;

import android.graphics.Bitmap;

public class GenerateInfo {
    private Bitmap bitmap;
    private String uuid;

    public Bitmap getBitmap() {
        return this.bitmap;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setBitmap(Bitmap paramBitmap) {
        this.bitmap = paramBitmap;
    }

    public void setUuid(String paramString) {
        this.uuid = paramString;
    }
}

