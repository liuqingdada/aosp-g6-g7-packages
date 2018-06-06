package com.mstarc.app.radio;

/**
 * Created by hp on 2016/11/11.
 */
public class PlayInfo {
    int index;
    int playstate;
    String url;

    public PlayInfo() {
    }

    /**
     * @param index     currentRadioIndex
     * @param playstate 0.否1.播
     * @param url       播放地址
     */
    public PlayInfo(int index, int playstate, String url) {
        this.index = index;
        this.playstate = playstate;
        this.url = url;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getPlaystate() {
        return playstate;
    }

    public PlayInfo setPlaystate(int playstate) {
        this.playstate = playstate;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "{ index:" + index + ",playstate:" + playstate + ",url:'" + url + "'}";
    }
}
