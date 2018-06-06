package com.mstarc.wearablelauncher.watchface;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.mstarc.wearablelauncher.R;

/**
 * Created by wangxinzhi on 17-3-4.
 */

public class WatchfaceFirsttDialog extends Dialog {
    DialogHandler mHandle;
    private static final String TAG = WatchfaceFirsttDialog.class.getSimpleName();
    Listener mlisterner;

    public WatchfaceFirsttDialog(Context context, Listener listener) {
        super(context, R.style.Dialog);
        mHandle = new DialogHandler();
        mlisterner = listener;
    }
    public WatchfaceFirsttDialog(Context context) {
        super(context, R.style.Dialog);
        mHandle = new DialogHandler();

    }

    public interface Listener {
        void onDissmiss();
    }

    @Override
    public void show() {
        super.show();
        getContext().getMainLooper();
        mHandle.removeMessages(DialogHandler.AUTO_DISMISS);
        mHandle.sendEmptyMessageDelayed(DialogHandler.AUTO_DISMISS, 3000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.dialog_watchface_first, null);
        setContentView(view);
        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();

        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        lp.height = dm.heightPixels;
        lp.width = dm.widthPixels;
        win.setAttributes(lp);

        view.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    @Override
    public void dismiss() {
        mHandle.removeMessages(DialogHandler.AUTO_DISMISS);
        super.dismiss();
        if(mlisterner!=null){
            mlisterner.onDissmiss();
        }
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
