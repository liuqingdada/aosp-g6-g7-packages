package com.mstarc.wearablemms.data;

/**
 * Created by wangxinzhi on 17-3-9.
 */
public class MessageData {
    public MessageData(Contact mContact, String mContent, String mDate,int mRead) {
        this.mContact = mContact;
        this.mContent = mContent;
        this.mDate = mDate;
        this.mRead = mRead;
    }

    public Contact mContact;
    public String mContent;
    public String mDate;
    public int mRead;

}
