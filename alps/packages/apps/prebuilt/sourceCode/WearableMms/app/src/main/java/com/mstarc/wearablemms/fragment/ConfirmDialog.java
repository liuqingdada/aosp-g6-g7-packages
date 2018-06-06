package com.mstarc.wearablemms.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.common.ThemeUtils;

import org.w3c.dom.Text;

/**
 * Created by wangxinzhi on 17-3-10.
 */
public class ConfirmDialog extends Dialog {
    interface Listener {
        void onConfirm();

        void onCancel();
    }

    Listener mListener;
    int mLayoutID;
    String content;
    private static final String TAG = ConfirmDialog.class.getSimpleName();

    public ConfirmDialog(Context context, int layoutID, Listener listener,String content) {
        super(context, R.style.Dialog);
        mLayoutID = layoutID;
        mListener = listener;
        this.content = content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), mLayoutID, null);
        setContentView(view);
        TextView tvContent = (TextView) view.findViewById(R.id.content_text);
        tvContent.setText(content);
        setCanceledOnTouchOutside(false);

        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        lp.height = LinearLayout.MarginLayoutParams.MATCH_PARENT;
        lp.width = LinearLayout.MarginLayoutParams.MATCH_PARENT;
        win.setAttributes(lp);

        view.findViewById(R.id.cancel).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                dismiss();
                if (mListener != null) {
                    mListener.onConfirm();
                }
                Log.d(TAG, "confirmed");
            }
        });
        ThemeUtils.updateImageView((ImageView) view.findViewById(R.id.cancel),R.drawable.ic_dialog_cancel);
        ThemeUtils.updateImageView((ImageView) view.findViewById(R.id.confirm),R.drawable.ic_dialog_confirm);
    }
}
