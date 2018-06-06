package com.mstarc.app.watchface.utils;

import android.util.Log;

/**
 * Created by liuqing
 * 17-8-22.
 * E1239604859@qq.com
 */

public class Logger {
    public static boolean DBG = true;

    public static void v(String TAG, String method, Object info) {
        if (DBG) {
            Log.v(TAG, method + info);
        }
    }

    public static void v(String TAG, String method, Throwable tr) {
        if (DBG) {
            Log.v(TAG, method + Log.getStackTraceString(tr));
        }
    }

    // ------------------------------------------------------------------

    public static void d(String TAG, String method, Object info) {
        if (DBG) {
            Log.d(TAG, method + info);
        }
    }

    public static void d(String TAG, String method, Throwable tr) {
        if (DBG) {
            Log.d(TAG, method + Log.getStackTraceString(tr));
        }
    }

    // ------------------------------------------------------------------

    public static void i(String TAG, String method, Object info) {
        if (DBG) {
            Log.i(TAG, method + info);
        }
    }

    public static void i(String TAG, String method, Throwable tr) {
        if (DBG) {
            Log.i(TAG, method + Log.getStackTraceString(tr));
        }
    }

    // ------------------------------------------------------------------

    public static void e(String TAG, String method, Object info) {
        if (DBG) {
            Log.e(TAG, method + info);
        }
    }

    public static void e(String TAG, String method, Throwable tr) {
        if (DBG) {
            Log.e(TAG, method + Log.getStackTraceString(tr));
        }
    }
}
