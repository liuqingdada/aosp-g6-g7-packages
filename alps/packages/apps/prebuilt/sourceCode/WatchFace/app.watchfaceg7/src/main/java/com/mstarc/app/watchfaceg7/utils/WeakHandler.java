package com.mstarc.app.watchfaceg7.utils;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

/**
 * Created by liuqing
 * 2017/2/17.
 * Email: 1239604859@qq.com
 */

public abstract class WeakHandler<T> extends Handler {
    private WeakReference<T> mOwner;

    public WeakHandler(T owner) {
        mOwner = new WeakReference<>(owner);
    }

    public WeakHandler(T owner, Looper looper) {
        super(looper);
        mOwner = new WeakReference<>(owner);
    }

    public T getOwner() {
        return mOwner.get();
    }
}
