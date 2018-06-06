package com.mstarc.wechat.wearwechat.protocol;


import com.mstarc.wechat.wearwechat.model.Contact;

import java.util.List;

public class BatchContactRequest {
    public BaseRequest BaseRequest;
    public int Count;
    public List<Contact> List;
}
