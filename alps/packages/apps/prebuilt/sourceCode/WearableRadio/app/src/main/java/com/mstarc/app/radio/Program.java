package com.mstarc.app.radio;

/**
 * Created by hp on 2016/9/14.
 */
public class Program {
    int mediainfo_id;
    String start_time;
    String end_time;

    public Program() {
    }

    public Program(int mediainfo_id, String start_time, String end_time) {
        this.mediainfo_id = mediainfo_id;
        this.start_time = start_time;
        this.end_time = end_time;
    }

    public int getMediainfo_id() {
        return mediainfo_id;
    }

    public void setMediainfo_id(int mediainfo_id) {
        this.mediainfo_id = mediainfo_id;
    }

    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    public String getEnd_time() {
        return end_time;
    }

    public void setEnd_time(String end_time) {
        this.end_time = end_time;
    }
}
