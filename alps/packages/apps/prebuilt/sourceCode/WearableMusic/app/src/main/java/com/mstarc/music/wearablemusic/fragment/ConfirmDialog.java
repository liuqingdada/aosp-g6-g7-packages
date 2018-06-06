package com.mstarc.music.wearablemusic.fragment;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mstarc.music.ThemeUtils;
import com.mstarc.music.wearablemusic.R;


public class ConfirmDialog extends Dialog {
    interface Listener {
        void onConfirm();

        void onCancel();
    }

    Listener mListener;
    int mLayoutID;
    String mInfoText;
    private static final String TAG = ConfirmDialog.class.getSimpleName();

    public ConfirmDialog(Context context, int layoutID, Listener listener, String msginfo) {
        super(context, R.style.Dialog);
        mLayoutID = layoutID;
        mListener = listener;
        mInfoText = msginfo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), mLayoutID, null);
        setContentView(view);
        ((TextView)view.findViewById(R.id.content_text)).setText(mInfoText);

        setCanceledOnTouchOutside(false);

        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.height = LinearLayout.MarginLayoutParams.MATCH_PARENT;
        lp.width = LinearLayout.MarginLayoutParams.MATCH_PARENT;
        win.setAttributes(lp);

        ImageView confirm = (ImageView) view.findViewById(R.id.confirm);
        ImageView cancel = (ImageView) view.findViewById(R.id.cancel);
        updateImageView(confirm, R.drawable.record_confirm_bg);
        updateImageView(cancel, R.drawable.record_cancel_bg);

        cancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (mListener != null) {
                    mListener.onCancel();
                }
                Log.d(TAG, "cancel");

            }
        });
        confirm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                if (mListener != null) {
                    mListener.onConfirm();
                }
                Log.d(TAG, "confirmed");
            }
        });
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
