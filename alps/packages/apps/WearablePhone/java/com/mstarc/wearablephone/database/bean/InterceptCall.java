package com.mstarc.wearablephone.database.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

/**
 * Created by liuqing
 * 17-11-27.
 * Email: 1239604859@qq.com
 */

@Entity
public class InterceptCall {
    @Id
    private Long id;
    private String name;
    private String number;

    @Generated(hash = 278151929)
    public InterceptCall(Long id, String name, String number) {
        this.id = id;
        this.name = name;
        this.number = number;
    }

    @Generated(hash = 1240368826)
    public InterceptCall() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "InterceptCall{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", number='" + number + '\'' +
                '}';
    }
}
