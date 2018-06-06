package com.mstarc.music.wearablemusic.data;

/**
 * description
 * <p/>
 * Created by andyding on 2017/6/5.
 */

public class MusicData {

    private String musicId;
    private String mMusicFileName;
    private String mMusicName;
    private String mMusicSinger;


    public MusicData(String id, String file, String name, String singer) {
        musicId = id;
        mMusicFileName = file;
        mMusicName = name;
        mMusicSinger = singer;
    }

    public String getMusicPath() {
        return mMusicFileName;
    }

    public String getMusicName() {
        return mMusicName + " - " + mMusicSinger;
    }

    public String getMusicId() {
        return musicId;
    }
}
