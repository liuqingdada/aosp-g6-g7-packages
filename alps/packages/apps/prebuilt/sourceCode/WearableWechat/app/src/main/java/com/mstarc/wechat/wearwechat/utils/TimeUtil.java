package com.mstarc.wechat.wearwechat.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");

    public static String dateToStr(Date paramDate) {
        return simpleDateFormat.format(paramDate);
    }

    public static String getDate() {
        return dateToStr(new Date(System.currentTimeMillis()));
    }

    public static void main(String[] paramArrayOfString) {
        getDate();
    }

    public static String timeToStr(long paramLong) {
        return simpleDateFormat.format(new Date(paramLong));
    }

    public static long toCeilSecondsFromMillis(long paramLong) {
        return (long) Math.ceil(paramLong / 1000.0D);
    }

    public static long toTimeMillis(long paramLong) {
        return 1000L * paramLong;
    }
}

