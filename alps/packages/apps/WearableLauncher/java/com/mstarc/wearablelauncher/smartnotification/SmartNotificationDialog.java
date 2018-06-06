package com.mstarc.wearablelauncher.smartnotification;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.ThemeUtils;


/**
 * Created by wangxinzhi on 17-3-4.
 */

public class SmartNotificationDialog extends Dialog implements View.OnClickListener, View.OnTouchListener {
    public static final int BEHAVIAR_TYPE_SMART_NOTIFICATION = 1;
    public static final int BEHAVIAR_TYPE_PAY = 2;
    public static final int BEHAVIAR_TYPE_FORGET = 3;
    public static final int BEHAVIAR_TYPE_JIURZUO = 4;
    public static final int BEHAVIAR_TYPE_MIYU = 5;
    public static final int BEHAVIAR_TYPE_LPOWER = 6;
    int mLayoutId;
    DialogHandler mHandle;
    String mContent1, mContent2, mContent3;
    TextView mContentView1, mContentView2, mContentView3;
    RelativeLayout mSmartViewLayout;
    ImageButton mSettingButton;
    ImageView mMiYuIcon;
    View mRootView;
    int mType;
    int mIconResId;
    PowerManager.WakeLock mWakeLock;

    @Override
    public void onClick(View v) {
        if (v == mSettingButton && mListener != null) {
            Log.d(TAG, "onClick");
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.mstarc.wearablesettings", "com.mstarc.wearablesettings.activitys.PreferenesActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            mListener.onConfirm();
            dismiss();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v != mSettingButton
                && mListener != null
                && event.getAction() == MotionEvent.ACTION_DOWN) {
            Log.d(TAG, "send confirm");
            mListener.onConfirm();
            dismiss();
            return false;
        }
        return false;
    }


    public interface Listener {
        void onSetting();

        void onConfirm();
    }

    Listener mListener;
    private static final String TAG = SmartNotificationDialog.class.getSimpleName();

    public SmartNotificationDialog(Context context, int type, Listener listener, int layoutId, String content, String content2, String content3) {
        super(context, R.style.Dialog);
        mListener = listener;
        mLayoutId = layoutId;
        mContent1 = content;
        mContent2 = content2;
        mContent3 = content3;
        mType = type;
        mHandle = new DialogHandler();
    }
    public SmartNotificationDialog(Context context, int type, Listener listener, int layoutId, String content, String content2, String content3, int iconResId) {
        super(context, R.style.Dialog);
        mListener = listener;
        mLayoutId = layoutId;
        mContent1 = content;
        mContent2 = content2;
        mContent3 = content3;
        mType = type;
        mHandle = new DialogHandler();
        mIconResId = iconResId;
    }

    private void performBehavior() {
        //TODO VIBRATE OPEN panel

        switch (mType) {
            case BEHAVIAR_TYPE_SMART_NOTIFICATION: {
                ObjectAnimator fadein = ObjectAnimator.ofFloat(mRootView, "alpha", 0f, 1f);
                fadein.start();
                mHandle.sendEmptyMessageDelayed(mHandle.FADE_OUT, 5000);
            }
            break;
            case BEHAVIAR_TYPE_LPOWER:
            case BEHAVIAR_TYPE_PAY: {
                mHandle.sendEmptyMessageDelayed(mHandle.AUTO_DISMISS, 5000);
            }
            break;
            case BEHAVIAR_TYPE_FORGET:
            case BEHAVIAR_TYPE_JIURZUO:
            case BEHAVIAR_TYPE_MIYU: {
                mHandle.sendEmptyMessageDelayed(mHandle.AUTO_DISMISS, 3000);
            }
            break;
        }
    }

    @Override
    public void show() {
        super.show();
        if(mWakeLock ==  null) {
            PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "SmartDialog");
        }
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
        }
        getContext().getMainLooper();
        performBehavior();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = View.inflate(getContext(), mLayoutId, null);
        mRootView = view;
        ViewGroup viewGroup = (ViewGroup) view;
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            viewGroup.getChildAt(i).setOnTouchListener(this);
        }
        try {
            mContentView1 = (TextView) view.findViewById(R.id.content);
            mContentView2 = (TextView) view.findViewById(R.id.content2);
            mContentView3 = (TextView) view.findViewById(R.id.content3);
            mSettingButton = (ImageButton) view.findViewById(R.id.setting);
            mSmartViewLayout = (RelativeLayout) view.findViewById(R.id.smart_view_bg);
            mMiYuIcon = (ImageView) view.findViewById(R.id.icon);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mContent1 != null && mContentView1 != null) {
            mContentView1.setText(mContent1);
            mContentView1.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        }
        if (mContent2 != null && mContentView2 != null) {
            mContentView2.setText(mContent2);
            mContentView2.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        }
        if (mContent3 != null && mContentView3 != null) {
            mContentView3.setText(mContent3);
        }
        if (mSettingButton != null) {
            mSettingButton.setOnClickListener(this);
        }
        if (ThemeUtils.isProductG7()
             &&mSmartViewLayout != null) {
            switch (ThemeUtils.getCurrentProduct()) {
                case ThemeUtils.PRODUCT_COLOR_HIGH_BLACK:
                case ThemeUtils.PRODUCT_COLOR_APPLE_GREEN:
                    if (mType == BEHAVIAR_TYPE_MIYU) {
                        mSmartViewLayout.setBackgroundResource(R.drawable.smart_bg_circle_grey);
                    } else {
                        mSmartViewLayout.setBackgroundResource(R.drawable.smart_bg_rect_grey);
                    }
                    break;
                default: //ThemeUtils.PRODUCT_COLOR_ROSE_GOLDEN:
                    if (mType == BEHAVIAR_TYPE_MIYU) {
                        mSmartViewLayout.setBackgroundResource(R.drawable.smart_bg_circle_white);
                    } else {
                        mSmartViewLayout.setBackgroundResource(R.drawable.smart_bg_rect_white);
                    }
                    break;
            }
        }
        if(mType == BEHAVIAR_TYPE_MIYU
                || mType == BEHAVIAR_TYPE_JIURZUO){
            if(mIconResId!=0) {
                ImageView icon = (ImageView) view.findViewById(R.id.icon);
                icon.setImageResource(mIconResId);
                updateImageView(mMiYuIcon, mIconResId);
            }
        }

        setContentView(view);

        setCanceledOnTouchOutside(false);
        Window win = getWindow();
        WindowManager.LayoutParams lp = win.getAttributes();

        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        lp.height = dm.heightPixels;
        lp.width = dm.widthPixels;
        win.setAttributes(lp);
        win.setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
        win.setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    class DialogHandler extends Handler {
        final static int AUTO_DISMISS = 1;
        final static int FADE_OUT = 2;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (AUTO_DISMISS == msg.what) {
                dismiss();
            } else if (FADE_OUT == msg.what) {
                //TODO
                ObjectAnimator fadein = ObjectAnimator.ofFloat(mRootView, "alpha", 1f, 0f);
                fadein.addListener(new Animator.AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dismiss();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }

                });
                fadein.setDuration(500);
                fadein.start();
            }
        }
    }

    @Override
    protected void onStop() {
        mHandle.removeMessages(DialogHandler.AUTO_DISMISS);
        mHandle.removeMessages(DialogHandler.FADE_OUT);
        super.onStop();
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
