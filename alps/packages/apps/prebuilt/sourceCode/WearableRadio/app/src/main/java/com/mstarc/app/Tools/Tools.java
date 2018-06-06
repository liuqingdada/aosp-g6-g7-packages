package com.mstarc.app.Tools;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import java.io.IOException;

/**
 * Created by Administrator on 2016/8/5 0005.
 */

public class Tools {
    public static boolean classIsRunningForeground(Context context, String className) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentClassName = cn.getClassName();
        if (currentClassName != null && currentClassName.equals(className)) {
            return true;
        }
        return false;
    }

    public static boolean isRunningForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (currentPackageName != null && currentPackageName.equals(context.getPackageName())) {
            return true;
        }
        return false;
    }

    public static boolean isNetworkAvailable() {
        try {
            String ip = "www.baidu.com";
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 " + ip);
            int status = p.waitFor();
            if (status == 0) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}
