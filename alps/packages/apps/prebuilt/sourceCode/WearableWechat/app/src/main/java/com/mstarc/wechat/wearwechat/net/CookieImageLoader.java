package com.mstarc.wechat.wearwechat.net;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;

import java.util.HashMap;
import java.util.Map;

public class CookieImageLoader extends ImageLoader {
    private String cookie;

    public CookieImageLoader(RequestQueue paramRequestQueue, ImageCache paramImageCache, String paramString) {
        super(paramRequestQueue, paramImageCache);
        this.cookie = paramString;
    }

    protected Request<Bitmap> makeImageRequest(String paramString1, int paramInt1, int paramInt2, ImageView.ScaleType paramScaleType, final String paramString2) {
        return new ImageRequest(paramString1, new Response.Listener() {
            @Override
            public void onResponse(Object response) {

                CookieImageLoader.this.onGetImageSuccess(paramString2, (Bitmap) response);
            }

        }
                , paramInt1, paramInt2, Bitmap.Config.RGB_565, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError paramAnonymousVolleyError) {
                CookieImageLoader.this.onGetImageError(paramString2, paramAnonymousVolleyError);
            }
        }) {
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap localHashMap = new HashMap();
                localHashMap.put("Cookie", CookieImageLoader.this.cookie);
                return localHashMap;
            }
        };
    }
}

