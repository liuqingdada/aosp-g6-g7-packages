package com.mstarc.wechat.wearwechat.model;

import android.os.Bundle;
import android.util.Log;

public class Token {
    private static final String TAG = "Token";
    public static final String COOKIE = "Cookie";
    public static final String IS_GRAY_SCALE = "isgrayscale";
    public static final String MESSAGE = "message";
    public static final String PASS_TICKET = "pass_ticket";
    public static final String RET = "ret";
    public static final String SKEY = "skey";
    public static final String WXSID = "wxsid";
    public static final String WXUIN = "wxuin";
    public String cookie;
    private String isGrayScale;
    public String message;
    private String passTicket;
    public int ret;
    private String skey;
    private String wxsid;
    private String wxuin;

    public void fromBundle(Bundle paramBundle) {
        try {
            this.skey = paramBundle.getString(SKEY);
            this.wxsid = paramBundle.getString(WXSID);
            this.wxuin = paramBundle.getString(WXUIN);
            this.passTicket = paramBundle.getString(PASS_TICKET);
            this.isGrayScale = paramBundle.getString(IS_GRAY_SCALE);
            this.cookie = paramBundle.getString(COOKIE);
        } catch (Exception e){
            Log.e(TAG, "fromBundle: ", e);
        }
    }

    public String getIsGrayScale() {
        return this.isGrayScale;
    }

    public String getPassTicket() {
        return this.passTicket;
    }

    public String getSkey() {
        return this.skey;
    }

    public String getWxsid() {
        return this.wxsid;
    }

    public String getWxuin() {
        return this.wxuin;
    }

    public void setIsGrayScale(String paramString) {
        this.isGrayScale = paramString;
    }

    public void setPassTicket(String paramString) {
        this.passTicket = paramString;
    }

    public void setSkey(String paramString) {
        this.skey = paramString;
    }

    public void setWxsid(String paramString) {
        this.wxsid = paramString;
    }

    public void setWxuin(String paramString) {
        this.wxuin = paramString;
    }

    public Bundle toBundle() {
        Bundle localBundle = new Bundle();
        localBundle.putString(SKEY, this.skey);
        localBundle.putString(WXSID, this.wxsid);
        localBundle.putString(WXUIN, this.wxuin);
        localBundle.putString(PASS_TICKET, this.passTicket);
        localBundle.putString(IS_GRAY_SCALE, this.isGrayScale);
        localBundle.putString(COOKIE, this.cookie);
        return localBundle;
    }
}
