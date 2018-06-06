package com.mstarc.wechat.wearwechat.protocol;


import com.mstarc.wechat.wearwechat.model.Contact;
import com.mstarc.wechat.wearwechat.model.Msg;
import com.mstarc.wechat.wearwechat.model.SyncKey;

import java.util.List;

public class MsgSyncResponse {
    public int AddMsgCount;
    public List<Msg> AddMsgList;
    public BaseResponse BaseResponse;
    public int ModContactCount;
    public List<Contact> ModContactList;
    public SyncKey SyncKey;
}

