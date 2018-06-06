package com.mstarc.wechat.wearwechat.protocol;


import com.mstarc.wechat.wearwechat.model.Contact;

import java.util.ArrayList;

public class ContactResponse {
    public BaseResponse BaseResponse;
    public int MemberCount;
    public ArrayList<Contact> MemberList;
    public int Seq;
}

