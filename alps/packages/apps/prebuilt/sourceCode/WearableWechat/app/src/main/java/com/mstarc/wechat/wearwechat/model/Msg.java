package com.mstarc.wechat.wearwechat.model;

import android.os.Bundle;

public class Msg {
    private static final String CONTENT = "Content";
    private static final String CREATE_TIME = "CreateTime";
    private static final String FROM_MEMBER_NICK_NAME = "fromMemberNickName";
    private static final String FROM_MEMBER_USER_NAME = "fromMemberUserName";
    private static final String FROM_NICK_NAME = "fromNickName";
    private static final String FROM_USER_NAME = "FromUserName";
    private static final String MSG_ID = "MsgId";
    private static final String Msg_TYPE = "MsgType";
    private static final String TO_NICK_NAME = "toNickName";
    private static final String TO_USER_NAME = "ToUserName";
    private static final String TYPE = "Type";
    private static final String VOICE_LENGTH = "VoiceLength";
    public long ClientMsgId;
    public String Content;
    public long CreateTime;
    public String FromUserName;
    public long LocalID;
    public String MsgId;
    public int MsgType;
    public String ToUserName;
    public int Type;
    public long VoiceLength;
    public String fromMemberNickName = "";
    public String fromMemberUserName = "";
    public String fromNickName;
    public String toNickName;

    public void fromBundle(Bundle paramBundle) {
        this.Type = paramBundle.getInt("Type");
        this.Content = paramBundle.getString("Content");
        this.FromUserName = paramBundle.getString("FromUserName");
        this.ToUserName = paramBundle.getString("ToUserName");
        this.MsgId = paramBundle.getString("MsgId");
        this.MsgType = paramBundle.getInt("MsgType");
        this.fromNickName = paramBundle.getString("fromNickName");
        this.toNickName = paramBundle.getString("toNickName");
        this.fromMemberUserName = paramBundle.getString("fromMemberUserName");
        this.fromMemberNickName = paramBundle.getString("fromMemberNickName");
        this.VoiceLength = paramBundle.getLong("VoiceLength");
        this.CreateTime = paramBundle.getLong("CreateTime");
    }

    public Bundle toBundle() {
        Bundle localBundle = new Bundle();
        localBundle.putInt("Type", this.Type);
        localBundle.putString("Content", this.Content);
        localBundle.putString("FromUserName", this.FromUserName);
        localBundle.putString("ToUserName", this.ToUserName);
        localBundle.putString("MsgId", this.MsgId);
        localBundle.putInt("MsgType", this.MsgType);
        localBundle.putString("fromNickName", this.fromNickName);
        localBundle.putString("toNickName", this.toNickName);
        localBundle.putString("fromMemberUserName", this.fromMemberUserName);
        localBundle.putString("fromMemberNickName", this.fromMemberNickName);
        localBundle.putLong("VoiceLength", this.VoiceLength);
        localBundle.putLong("CreateTime", this.CreateTime);
        return localBundle;
    }

    @Override
    public String toString() {
        return "Msg{" +
                "ClientMsgId=" + ClientMsgId +
                ", Content='" + Content + '\'' +
                ", CreateTime=" + CreateTime +
                ", FromUserName='" + FromUserName + '\'' +
                ", LocalID=" + LocalID +
                ", MsgId='" + MsgId + '\'' +
                ", MsgType=" + MsgType +
                ", ToUserName='" + ToUserName + '\'' +
                ", Type=" + Type +
                ", VoiceLength=" + VoiceLength +
                ", fromMemberNickName='" + fromMemberNickName + '\'' +
                ", fromMemberUserName='" + fromMemberUserName + '\'' +
                ", fromNickName='" + fromNickName + '\'' +
                ", toNickName='" + toNickName + '\'' +
                '}';
    }
}

