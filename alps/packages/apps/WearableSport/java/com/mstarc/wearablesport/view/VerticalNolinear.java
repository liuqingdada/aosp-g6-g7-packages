
package com.mstarc.wearablesport.view;

import android.graphics.Rect;
import android.util.Log;

/**
 * Created by wangxinzhi on 2015-7-29.
 */
public class VerticalNolinear {
    private static final String TAG = "Nolinear";
    public Rect mNolinearRect;
    public float mRatio = 1.187f;
    public static final float mMaxScale = 1.25f;
    public static final float MAX_Z = 10;
    public static final float MIN_Z = 5;

    VerticalNolinear(Rect rect) {
        mNolinearRect = rect;
        mRatio = Math.round(-rect.height() / (float) (rect.width()));
    }

    public int getFocusCenter() {
        return mNolinearRect.centerX();
    }

    public int toY(int x) {
        int y;
        if (x <= mNolinearRect.left) {
            y = x;
        } else if (x >= mNolinearRect.right) {
            y = mNolinearRect.top + (x - mNolinearRect.right);
        } else {
            y = Math.round(mNolinearRect.bottom + (x - mNolinearRect.left) * mRatio);
        }
        return y;
    }

    public float toScale(int x) {
        float scale = 1f;
        if (x > mNolinearRect.left && x < mNolinearRect.centerX()) {
            scale = (x - mNolinearRect.left) * (mMaxScale - 1f)
                    / (mNolinearRect.centerX() - mNolinearRect.left) + 1f;

        } else if (x > mNolinearRect.centerX() && x < mNolinearRect.right) {
            scale = (x - mNolinearRect.right) * (1f - mMaxScale)
                    / (mNolinearRect.right - mNolinearRect.centerX()) + 1f;
        } else if (x == mNolinearRect.centerX() && mNolinearRect.width() != 0) {
            scale = mMaxScale;
        }
        // Log.d(TAG,"toScale x:"+x+" scale:"+scale);
        return scale;
    }

    public float toTranslationZ(int x) {
        float z = MIN_Z;
        if (x > mNolinearRect.left && x < mNolinearRect.centerX()) {
            z = (x - mNolinearRect.left) * (MAX_Z - MIN_Z)
                    / (mNolinearRect.centerX() - mNolinearRect.left) + MIN_Z;

        } else if (x > mNolinearRect.centerX() && x < mNolinearRect.right) {
            z = (x - mNolinearRect.right) * (MIN_Z - MAX_Z)
                    / (mNolinearRect.right - mNolinearRect.centerX()) + MIN_Z;
        } else if (x == mNolinearRect.centerX() && mNolinearRect.width() != 0) {
            z = MAX_Z;
        }
        return z;
    }
}
