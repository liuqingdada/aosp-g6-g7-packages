package com.mstarc.wechat.wearwechat.model;

public class VoiceInfo {
    private String content;
    private String name;
    private long voiceLength;

    public String getContent() {
        return this.content;
    }

    public String getName() {
        return this.name;
    }

    public long getVoiceLength() {
        return this.voiceLength;
    }

    public void setContent(String paramString) {
        this.content = paramString;
    }

    public void setName(String paramString) {
        this.name = paramString;
    }

    public void setVoiceLength(long paramLong) {
        this.voiceLength = paramLong;
    }
}

