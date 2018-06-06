package com.mstarc.wechat.wearwechat.protocol;


import com.mstarc.wechat.wearwechat.model.Contact;

import java.util.List;

public class BatchContactResponse {
    public BaseResponse BaseResponse;
    public List<Contact> ContactList;
    public int Count;
}
