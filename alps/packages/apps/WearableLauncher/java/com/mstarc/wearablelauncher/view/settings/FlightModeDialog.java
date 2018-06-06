package com.mstarc.wearablelauncher.view.settings;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mstarc.wearablelauncher.R;

/**
 * Created by wangxinzhi on 17-3-4.
 */

public class FlightModeDialog extends Dialog {
    DialogHandler mHandle;

    private static final String TAG = FlightModeDialog.class.getSimpleName();

    public FlightModeDialog(Context context) {
        super(context, R.style.Dialog);
        mHandle = new DialogHandler();
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
        View view = View.inflate(getContext(), R.layout.setting_flight_mode_dialog, null);
        setContentView(view);

        setCanceledOnTouchOutside(false);
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
