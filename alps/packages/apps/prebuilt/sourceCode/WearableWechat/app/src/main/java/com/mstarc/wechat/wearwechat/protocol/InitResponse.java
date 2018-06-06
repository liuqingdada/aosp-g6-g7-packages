package com.mstarc.wechat.wearwechat.protocol;


import com.mstarc.wechat.wearwechat.model.Contact;
import com.mstarc.wechat.wearwechat.model.SyncKey;
import com.mstarc.wechat.wearwechat.model.User;

import java.util.ArrayList;

//有做修改,注意;
public class InitResponse {
    public BaseResponse BaseResponse;
    public String ChatSet;
    public ArrayList<Contact> ContactList;
    public int Count;
    public int GrayScale;
    public String Skey;
    public SyncKey SyncKey;
    public long SystemTime;
    public User User;
}
