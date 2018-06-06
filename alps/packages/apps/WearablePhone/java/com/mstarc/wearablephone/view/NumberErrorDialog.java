package com.mstarc.wearablephone.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.mstarc.wearablephone.R;

/**
 * Created by wangxinzhi on 17-3-4.
 */

public class NumberErrorDialog extends Dialog {
    Context mContext;
    private static final String TAG = NumberErrorDialog.class.getSimpleName();

    public NumberErrorDialog(Context context){
        super(context, R.style.Dialog);
        mContext = context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.dial_number_error_dialog, null);
        setContentView(view);
        setCanceledOnTouchOutside(false);
        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();
        boolean g7 = mContext.getResources().getBoolean(R.bool.g7_target);
        if(g7){
            lp.height = 340;
            lp.width = 272;
        }else {
            lp.height = 320;
            lp.width = 320;
        }
        win.setAttributes(lp);

        view.findViewById(R.id.confirm).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();

            }
        });
    }
}
