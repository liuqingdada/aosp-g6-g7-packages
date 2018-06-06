package com.mstarc.wearablemms.database.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by liuqing
 * 17-11-22.
 * Email: 1239604859@qq.com
 */

@Entity
public class JunkMMS {
    @Id
    private Long id;

    private String snippet;
    private int thread_id;
    private String msg_count;
    private String address;
    private long date;

    @Generated(hash = 1009348849)
    public JunkMMS(Long id, String snippet, int thread_id, String msg_count,
                   String address, long date) {
        this.id = id;
        this.snippet = snippet;
        this.thread_id = thread_id;
        this.msg_count = msg_count;
        this.address = address;
        this.date = date;
    }

    @Generated(hash = 1601476429)
    public JunkMMS() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSnippet() {
        return this.snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public int getThread_id() {
        return this.thread_id;
    }

    public void setThread_id(int thread_id) {
        this.thread_id = thread_id;
    }

    public String getMsg_count() {
        return this.msg_count;
    }

    public void setMsg_count(String msg_count) {
        this.msg_count = msg_count;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getDate() {
        return this.date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "JunkMMS{" +
                "id=" + id +
                ", snippet='" + snippet + '\'' +
                ", thread_id=" + thread_id +
                ", msg_count='" + msg_count + '\'' +
                ", address='" + address + '\'' +
                ", date=" + date +
                '}';
    }
}
