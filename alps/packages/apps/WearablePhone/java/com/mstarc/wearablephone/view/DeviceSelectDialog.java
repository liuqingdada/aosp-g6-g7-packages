package com.mstarc.wearablephone.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import com.mstarc.wearablephone.R;
import com.mstarc.wearablephone.bluetooth.BTCallManager;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
/**
 * Created by wangxinzhi on 17-3-4.
 */

public class DeviceSelectDialog extends Dialog {
    Context mContext;
    UiHandler mHandler;
    ProgressBar mRotateView;
    interface Listener {
        void byPhone();

        void byWatch();
    }

    Listener mListener;
    private static final String TAG = DeviceSelectDialog.class.getSimpleName();

    public DeviceSelectDialog(Context context, Listener listener) {
        super(context, R.style.Dialog);
        mListener = listener;
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.dial_select_dialog, null);
        setContentView(view);
        mRotateView = (ProgressBar) view.findViewById(R.id.progressBar);

        int resId;
        switch (ThemeUtils.getCurrentProduct()) {
            case ThemeUtils.PRODUCT_COLOR_ROSE_GOLDEN:
                resId = R.drawable.rotation_arc_g7_rose;
                break;
            case ThemeUtils.PRODUCT_COLOR_HIGH_BLACK:
                resId = R.drawable.rotation_arc_g7_golden;
                break;
            case ThemeUtils.PRODUCT_COLOR_APPLE_GREEN:
                resId = R.drawable.rotation_arc_g7_green;
                break;
            default:
                resId = R.drawable.rotation_arc_g6;
                break;
        }
        Drawable drawable = ContextCompat.getDrawable(getContext(), resId);
        mRotateView.setIndeterminateDrawable(drawable);

        setCanceledOnTouchOutside(false);
//
        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        boolean g7 = mContext.getResources().getBoolean(R.bool.g7_target);
        if (g7) {
            lp.height = 340;
            lp.width = 272;
        } else {
            lp.height = 320;
            lp.width = 320;
        }
        win.setAttributes(lp);

        view.findViewById(R.id.dial_by_phone).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (mListener != null) {
                    mListener.byPhone();
                }
                Log.d(TAG, "by phone");

            }
        });
        view.findViewById(R.id.dial_by_watch).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (mListener != null) {
                    mListener.byWatch();
                }
                Log.d(TAG, "by watch");
            }
        });
        mHandler = new UiHandler();
        mHandler.sendEmptyMessageDelayed(UiHandler.MSG_DISMISS,5000);
    }

    @Override
    public void dismiss() {
        mHandler.removeMessages(UiHandler.MSG_DISMISS);
        super.dismiss();
    }

    class UiHandler extends Handler {
        public final static int MSG_DISMISS = 1;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DISMISS:
                    if (mListener != null) {
                        if (BTCallManager.getInstance(getContext()).isBTPhoneEnnable()) {
                            mListener.byPhone();
                        } else {
                            mListener.byWatch();
                        }
                    }
                    dismiss();
                    Log.d(TAG, "by watch");
                    break;
            }
        }
    }
}
