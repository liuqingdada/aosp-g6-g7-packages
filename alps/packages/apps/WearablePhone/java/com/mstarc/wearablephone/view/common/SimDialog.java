package com.mstarc.wearablephone.view.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.mstarc.wearablephone.R;

/**
 * Created by hawking on 2017/9/12.
 */

public class SimDialog extends Dialog {
    DialogHandler mHandle;

    public SimDialog(@NonNull Context context) {
        super(context,R.style.Dialog);
        mHandle = new DialogHandler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.simdialog, null);
        setContentView(view);

        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        lp.height = dm.heightPixels;
        lp.width = dm.widthPixels;
        win.setAttributes(lp);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    @Override
    public void show() {
        super.show();
        mHandle.sendEmptyMessageDelayed(DialogHandler.AUTO_DISMISS, 5000);
    }

    @Override
    public void dismiss() {
        mHandle.removeMessages(DialogHandler.AUTO_DISMISS);
        super.dismiss();
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
}
