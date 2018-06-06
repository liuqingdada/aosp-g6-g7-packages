package com.mstarc.wechat.wearwechat.net;

import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.mstarc.wechat.wearwechat.MyApplication;
import com.mstarc.wechat.wearwechat.utils.BitmapCache;


public class VolleySingleton {
    private static VolleySingleton mInstance = null;
    private String cookie;
    private ImageLoader imageLoader;
    private RequestQueue mRequestQueue = Volley.newRequestQueue(MyApplication.getAppContext());

    public static VolleySingleton getInstance() {
        if (mInstance == null)
            mInstance = new VolleySingleton();
        return mInstance;
    }

    public ImageLoader getImageLoader(String paramString) {
        if (paramString == null)
            Log.e("VolleySingleton", "getImageLoader:cookie should not be null");
        else if ((this.imageLoader == null) || (!paramString.equals(this.cookie))) {
            this.cookie = paramString;
            this.imageLoader = new CookieImageLoader(this.mRequestQueue, new BitmapCache(), paramString);
        }
        return this.imageLoader;
    }

    public RequestQueue getRequestQueue() {
        return this.mRequestQueue;
    }
}

