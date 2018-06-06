package com.mstarc.wearablelauncher.view.common;

/**
 * Created by wangxinzhi on 17-2-27.
 */

class MathUtils {

    static int constrain(int amount, int low, int high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

    static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }

}
