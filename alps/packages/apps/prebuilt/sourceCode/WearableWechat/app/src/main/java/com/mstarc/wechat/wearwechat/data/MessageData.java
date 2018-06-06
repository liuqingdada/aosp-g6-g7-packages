package com.mstarc.wechat.wearwechat.data;

/**
 * Created by wangxinzhi on 17-3-9.
 */
public class MessageData {
    public MessageData(Contact mContact, String mContent, String mDate) {
        this.mContact = mContact;
        this.mContent = mContent;
        this.mDate = mDate;
    }

    public Contact mContact;
    public String mContent;
    public String mDate;

}
