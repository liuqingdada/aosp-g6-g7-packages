package com.mstarc.app.radio;

/**
 * Created by hp on 2016/9/14.
 */
public class Radio {
    int id;
    String title;
    String update_time;

    public Radio() {
    }

    public Radio(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public Radio(int id, String title, String update_time) {
        this.id = id;
        this.title = title;
        this.update_time = update_time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(String update_time) {
        this.update_time = update_time;
    }
}
