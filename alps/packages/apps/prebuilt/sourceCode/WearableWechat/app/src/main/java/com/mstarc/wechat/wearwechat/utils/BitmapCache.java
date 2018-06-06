package com.mstarc.wechat.wearwechat.utils;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class BitmapCache implements ImageCache {

    private int max = 10485760;
    private LruCache<String, Bitmap> lruCache = new LruCache(this.max) {
        protected int sizeOf(String paramAnonymousString, Bitmap paramAnonymousBitmap) {
            return paramAnonymousBitmap.getRowBytes() * paramAnonymousBitmap.getHeight();
        }
    };


    public Bitmap getBitmap(String paramString) {
        return this.lruCache.get(paramString);
    }

    public void putBitmap(String paramString, Bitmap paramBitmap) {
        this.lruCache.put(paramString, paramBitmap);
    }
}

