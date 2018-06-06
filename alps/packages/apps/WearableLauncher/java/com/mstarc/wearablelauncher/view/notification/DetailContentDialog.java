package com.mstarc.wearablelauncher.view.notification;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.commonbase.database.bean.NotificationBean;
import com.mstarc.wearablelauncher.R;

/**
 * Created by wangxinzhi on 17-3-4.
 */

public class DetailContentDialog extends Dialog {
    DialogHandler mHandle;
    NotificationBean mNotificationBean;
    private static final String TAG = DetailContentDialog.class.getSimpleName();

    public DetailContentDialog(Context context, NotificationBean notificationBean) {
        super(context, R.style.NotificationDialog);
        mHandle = new DialogHandler();
        mNotificationBean = notificationBean;
    }

    @Override
    public void show() {
        super.show();
        getContext().getMainLooper();
        mHandle.removeMessages(DialogHandler.AUTO_DISMISS);
       // mHandle.sendEmptyMessageDelayed(DialogHandler.AUTO_DISMISS, 3000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), R.layout.notification_detail_content_dialog, null);
        setContentView(view);
        TextView textView = (TextView) findViewById(R.id.notification_detail_text);
        ImageView iconView = (ImageView) findViewById(R.id.notification_detail_icon);
        textView.setText(mNotificationBean.getContent());
        iconView.setImageBitmap(mNotificationBean.getIconBitmap());
        setCanceledOnTouchOutside(false);
        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();

        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        lp.height = dm.heightPixels;
        lp.width = dm.widthPixels;
        win.setAttributes(lp);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
