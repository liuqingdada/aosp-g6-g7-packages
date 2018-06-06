package com.mstarc.wechat.wearwechat.utils;

import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String AUDIO_DIRECTORY = "/weixinQingliao/";
    public static final int UNSYNC_LIMIT = 2;
    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/51.0.2704.79 Chrome/51.0.2704.79 Safari/537.36";

    // 特殊用户 须过滤
    public static final List<String> FILTER_USERS = Arrays.asList("newsapp", "fmessage", "filehelper", "weibo", "qqmail",
            "fmessage", "tmessage", "qmessage", "qqsync", "floatbottle", "lbsapp", "shakeapp", "medianote", "qqfriend",
            "readerapp", "blogapp", "facebookapp", "masssendapp", "meishiapp", "feedsapp", "voip", "blogappweixin",
            "weixin", "brandsessionholder", "weixinreminder", "wxid_novlwrv3lqwv11", "gh_22b87fa7cb3c", "officialaccounts",
            "notification_messages", "wxid_novlwrv3lqwv11", "gh_22b87fa7cb3c", "wxitil", "userexperience_alarm",
            "notification_messages");

    public static final class Action {
        public static final String CLOSE_APP = "com.riyuxihe.weixinqingliao.CLOSEAPP";
        public static final String NEW_MSG = "com.riyuxihe.weixinqingliao.MSG";
        public static final String RESCHEDULE = "com.riyuxihe.weixinqingliao.RESCHEDULE";
    }

    public static final class ContactFlag {
        public static final int DEFAULT = 0;
        public static final int FRIEND = 3;
        public static final int GROUP = 2;
        public static final int PUBLIC = 1;
    }

    public static class LoginCode {
        public static final String INIT = "408";
        public static final String LOGIN = "200";
        public static final String SCANNED = "201";
    }

    public static final class MsgType {
        public static final int TEXT = 1;
        public static final int VOICE = 34;
    }

    public static final class MyRetry {
        public static final float BACKOFF_MULT = 2.0F;
        public static final int RETRIES = 1;
        public static final int TIMEOUT_MS = 2500;
    }

    public static final class Period {
        public static final long HOME_STANDARD = 60000L;
        public static final long LOGIN = 7000L;
    }

    public static final class SyncCheckCode {
        public static final String ERROR = "1101";
        public static final String FAIL = "1100";
        public static final String SUCCESS = "0";
    }
}

