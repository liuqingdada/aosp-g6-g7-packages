package com.mstarc.wearablesettings.utils;

/**
 * @author: wyg
 * created at: 2018/1/3 0003  13:42
 * QQ:  865976769
 */

public class WifiBean {
    public String capabilities;
    public String SSID;
    public int level;
    public boolean showPro;

    @Override
    public String toString() {
        return "WifiBean{" +
                "capabilities='" + capabilities + '\'' +
                ", SSID='" + SSID + '\'' +
                ", level=" + level +
                ", showPro=" + showPro +
                '}';
    }
}
