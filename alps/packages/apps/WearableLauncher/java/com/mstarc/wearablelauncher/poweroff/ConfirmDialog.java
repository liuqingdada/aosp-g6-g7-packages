package com.mstarc.wearablelauncher.poweroff;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.ThemeUtils;

/**
 * Created by wangxinzhi on 17-3-4.
 */

public class ConfirmDialog extends Dialog {
    int mLayoutId;
    DialogHandler mHandle;
    String mContent;

    public interface Listener {
        void onConfirm();

        void onCancel();
    }

    Listener mListener;
    private static final String TAG = ConfirmDialog.class.getSimpleName();

    public ConfirmDialog(Context context, Listener listener, String text) {
        super(context, R.style.Dialog);
        mListener = listener;
        mLayoutId = R.layout.power_confirm_dialog;
        mContent = text;
        mHandle = new DialogHandler();
    }


    @Override
    public void show() {
        super.show();
        getContext().getMainLooper();
        mHandle.sendEmptyMessageDelayed(mHandle.AUTO_DISMISS, 5000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), mLayoutId, null);
        if (mContent != null) {
            TextView textView = (TextView) view.findViewById(R.id.power_pormotion);
            textView.setText(mContent);
        }
        setContentView(view);

        setCanceledOnTouchOutside(false);
        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();

        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        lp.height = dm.heightPixels;
        lp.width = dm.widthPixels;
        win.setAttributes(lp);
        win.setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        win.setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ImageView confirm = (ImageView) view.findViewById(R.id.confirm);
        ImageView cancel = (ImageView) view.findViewById(R.id.cancel);
        updateImageView(confirm, R.drawable.power_confirm_selector);
        updateImageView(cancel, R.drawable.power_cancel_selector);
        view.findViewById(R.id.cancel).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandle.removeMessages(mHandle.AUTO_DISMISS);
                dismiss();
                if (mListener != null) {
                    mListener.onCancel();
                }
                Log.d(TAG, "cancel");

            }
        });
        view.findViewById(R.id.confirm).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandle.removeMessages(mHandle.AUTO_DISMISS);
                dismiss();
                if (mListener != null) {
                    mListener.onConfirm();
                }
                Log.d(TAG, "confirmed");
            }
        });
    }

    class DialogHandler extends Handler {
        final static int AUTO_DISMISS = 1;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (AUTO_DISMISS == msg.what) {
                dismiss();
            }
        }
    }
    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }
}
