package com.mstarc.wechat.wearwechat.utils;

public class StringUtil {
    public static String filterHtml(String paramString) {
        if ((paramString == null) || (paramString.isEmpty()))
            return "";
        return paramString.replaceAll("\\&[a-zA-Z]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "");
    }

    public static boolean isNullOrEmpty(String paramString) {
        return (paramString == null) || (paramString.isEmpty());
    }

    public static boolean notNullOrEmpty(String paramString) {
        return (paramString != null) && (!paramString.isEmpty());
    }
}
