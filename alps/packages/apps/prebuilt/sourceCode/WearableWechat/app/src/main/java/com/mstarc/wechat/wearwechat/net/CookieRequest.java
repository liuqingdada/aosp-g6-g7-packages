package com.mstarc.wechat.wearwechat.net;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CookieRequest extends JsonObjectRequest {
    private Map<String, String> mHeaders = new HashMap();

    public CookieRequest(int paramInt, String paramString, Response.Listener<JSONObject> paramListener, Response.ErrorListener paramErrorListener) {
        super(paramInt, paramString, paramListener, paramErrorListener);
    }

    public CookieRequest(int paramInt, String paramString1, String paramString2, Response.Listener<JSONObject> paramListener, Response.ErrorListener paramErrorListener) {
        super(paramInt, paramString1, paramString2, paramListener, paramErrorListener);
    }

    public CookieRequest(int paramInt, String paramString, JSONObject paramJSONObject, Response.Listener<JSONObject> paramListener, Response.ErrorListener paramErrorListener) {
        super(paramInt, paramString, paramJSONObject, paramListener, paramErrorListener);
    }

    public CookieRequest(String paramString, JSONObject paramJSONObject, Response.Listener<JSONObject> paramListener, Response.ErrorListener paramErrorListener) {
        super(paramString, paramJSONObject, paramListener, paramErrorListener);
    }

    public Map<String, String> getHeaders()
            throws AuthFailureError {
        this.mHeaders.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/51.0.2704.79 Chrome/51.0.2704.79 Safari/537.36");
        this.mHeaders.put("Referer", "https://wx.qq.com/");
        return this.mHeaders;
    }

    public void setCookie(String paramString) {
        this.mHeaders.put("Cookie", paramString);
    }
}

