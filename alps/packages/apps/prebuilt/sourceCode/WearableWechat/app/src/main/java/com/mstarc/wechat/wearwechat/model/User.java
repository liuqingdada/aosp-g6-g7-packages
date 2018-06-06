package com.mstarc.wechat.wearwechat.model;

import android.os.Bundle;
import android.util.Log;

public class User {
    private static final String TAG = "User";
    private static final String CONTACTFLAG = "ContactFlag";
    private static final String HEADIMGURL = "HeadImgUrl";
    private static final String NICKNAME = "NickName";
    private static final String USERNAME = "UserName";
    public int ContactFlag;
    public String HeadImgUrl;
    public String NickName;
    public String UserName;

    public void fromBundle(Bundle paramBundle) {
        try {
            this.UserName = paramBundle.getString("UserName");
            this.NickName = paramBundle.getString("NickName");
            this.HeadImgUrl = paramBundle.getString("HeadImgUrl");
            this.ContactFlag = paramBundle.getInt("ContactFlag");
        } catch (Exception e){
            Log.e(TAG, "fromBundle: ", e);
        }
    }

    public Bundle toBundle() {
        Bundle localBundle = new Bundle();
        localBundle.putString("UserName", this.UserName);
        localBundle.putString("NickName", this.NickName);
        localBundle.putString("HeadImgUrl", this.HeadImgUrl);
        localBundle.putInt("ContactFlag", this.ContactFlag);
        return localBundle;
    }
}
