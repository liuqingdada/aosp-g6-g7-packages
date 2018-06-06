package com.mstarc.wearablemms.data;

import android.content.Context;

import java.io.IOException;

/**
 * Created by vista on 2017/7/12.
 */

public class Utils {

    public static boolean isNetworkAvailable(Context context) {
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
