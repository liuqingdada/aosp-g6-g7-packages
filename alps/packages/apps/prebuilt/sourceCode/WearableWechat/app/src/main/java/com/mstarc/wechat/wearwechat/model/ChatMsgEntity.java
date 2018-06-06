package com.mstarc.wechat.wearwechat.model;

public class ChatMsgEntity {
    private static final String TAG = ChatMsgEntity.class.getSimpleName();
    private String date;
    private boolean isComMeg = true;
    private String memberNickName;
    private String memberUserName;
    private String nickName;
    private String text;
    private String time;
    private String userName;

    public ChatMsgEntity() {
    }

    public String getDate() {
        return this.date;
    }

    public String getMemberNickName() {
        return this.memberNickName;
    }

    public String getMemberUserName() {
        return this.memberUserName;
    }

    public boolean getMsgType() {
        return this.isComMeg;
    }

    public String getNickName() {
        return this.nickName;
    }

    public String getText() {
        return this.text;
    }

    public String getTime() {
        return this.time;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setDate(String paramString) {
        this.date = paramString;
    }

    public void setMemberNickName(String paramString) {
        this.memberNickName = paramString;
    }

    public void setMemberUserName(String paramString) {
        this.memberUserName = paramString;
    }

    public void setMsgType(boolean paramBoolean) {
        this.isComMeg = paramBoolean;
    }

    public void setNickName(String paramString) {
        this.nickName = paramString;
    }

    public void setText(String paramString) {
        this.text = paramString;
    }

    public void setTime(String paramString) {
        this.time = paramString;
    }

    public void setUserName(String paramString) {
        this.userName = paramString;
    }


    @Override
    public String toString() {
        return "ChatMsgEntity{" +
                "date='" + date + '\'' +
                ", isComMeg=" + isComMeg +
                ", memberNickName='" + memberNickName + '\'' +
                ", memberUserName='" + memberUserName + '\'' +
                ", nickName='" + nickName + '\'' +
                ", text='" + text + '\'' +
                ", time='" + time + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}
