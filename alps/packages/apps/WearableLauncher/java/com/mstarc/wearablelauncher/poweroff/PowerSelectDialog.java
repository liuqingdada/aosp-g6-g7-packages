package com.mstarc.wearablelauncher.poweroff;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.mstarc.wearablelauncher.R;

/**
 * Created by wangxinzhi on 17-3-4.
 */

public class PowerSelectDialog extends Dialog implements View.OnClickListener,
        PowerConfirmDialog.Listener {
    int mLayoutId;
    DialogHandler mHandle;
    int mSelectedIndex;
    Listener mListener;
    private static final String TAG = PowerSelectDialog.class.getSimpleName();
    PowerConfirmDialog mPowerConfirmDialog;

    public PowerSelectDialog(Context context, Listener listener, int layoutId) {
        super(context, R.style.PowerDialog);
        mListener = listener;
        mLayoutId = layoutId;
        mHandle = new DialogHandler();
        mSelectedIndex = -1;
    }

    @Override
    public void show() {
        super.show();
        mHandle.sendEmptyMessageDelayed(mHandle.AUTO_DISMISS, 8000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), mLayoutId, null);
        setContentView(view);
        setCanceledOnTouchOutside(false);
        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();

        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        lp.height = dm.heightPixels;
        lp.width = dm.widthPixels;
        win.setAttributes(lp);
        win.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
        win.setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        view.findViewById(R.id.poweroff_select1).setOnClickListener(this);
        view.findViewById(R.id.poweroff_select2).setOnClickListener(this);
        view.findViewById(R.id.poweroff_select3).setOnClickListener(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (mPowerConfirmDialog != null) {
            mPowerConfirmDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        String promotion = "";
        mHandle.removeMessages(DialogHandler.AUTO_DISMISS);
        switch (v.getId()) {
            case R.id.poweroff_select1:
                mSelectedIndex = 0;
                promotion = getContext().getString(R.string.power_reboot_promtion);
                break;
            case R.id.poweroff_select2:
                mSelectedIndex = 1;
                promotion = getContext().getString(R.string.power_off_promtion);
                break;
            case R.id.poweroff_select3:
                if (R.layout.poweroff_watch_mode == mLayoutId) {
                    promotion = getContext().getString(R.string.power_exit_watch_mode_promtion);
                } else {
                    promotion = getContext().getString(R.string.power_factoryreset_promotion);
                }
                mSelectedIndex = 2;
                break;
            default:
                mSelectedIndex = -1;
                mHandle.sendEmptyMessageDelayed(mHandle.AUTO_DISMISS, 8000);
                break;
        }
        if (mPowerConfirmDialog != null) {
            mPowerConfirmDialog.dismiss();
        }
        mPowerConfirmDialog = new PowerConfirmDialog(getContext(), this, promotion);
        mPowerConfirmDialog.show();
    }

    @Override
    public void onConfirm() {
        if (mPowerConfirmDialog != null) {
            mPowerConfirmDialog.dismiss();
        }
        dismiss();
        if (mListener != null) {
            mListener.onSelected(mSelectedIndex);
        }
    }

    @Override
    public void onCancel() {
        mHandle.sendEmptyMessageDelayed(mHandle.AUTO_DISMISS, 8000);
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

    public interface Listener {
        void onSelected(int index);
    }

}
