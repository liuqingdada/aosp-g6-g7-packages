package com.mstarc.wearablelauncher.view.notification;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.commonbase.application.image.BitmapDrawableUtils;
import com.mstarc.commonbase.communication.listener.ICommonAidlListener;
import com.mstarc.commonbase.communication.message.WatchCode;
import com.mstarc.commonbase.communication.message.transmite.WatchFace;
import com.mstarc.commonbase.database.bean.NotificationBean;
import com.mstarc.commonbase.notification.listener.OnBatteryChangedListener;
import com.mstarc.commonbase.notification.listener.OnDeleteNotificationListener;
import com.mstarc.commonbase.notification.listener.OnReceiveNotificationListener;
import com.mstarc.commonbase.notification.listener.OnUpdataNotificationListener;
import com.mstarc.fakewatch.notification.NotificationWizard;
import com.mstarc.wearablelauncher.CommonManager;
import com.mstarc.wearablelauncher.MainActivity;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.ThemeUtils;
import com.mstarc.wearablelauncher.smartnotification.SmartNotificationDialog;
import com.mstarc.wearablelauncher.view.clock.IdleFragment;
import com.mstarc.wearablelauncher.view.common.DecorationSettingItem;
import com.mstarc.wearablelauncher.view.common.DepthPageTransformer;
import com.mstarc.wearablelauncher.view.common.GravitySnapHelper;
import com.mstarc.wearablelauncher.view.common.OnItemClickListener;
import com.mstarc.wearablelauncher.view.quicksetting.QSFragment;
import com.mstarc.wearablelauncher.view.quicksetting.QuickSettingPower;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.mstarc.commonbase.communication.message.WatchCode.LOW_C1;
import static com.mstarc.commonbase.communication.message.WatchCode.LOW_C2;

/**
 * Created by wangxinzhi on 17-2-19.
 */

public class NotificationFragment extends Fragment implements
        GravitySnapHelper.SnapListener,
        OnItemClickListener, ConfirmDialog.Listener,
        OnDeleteNotificationListener,
        OnUpdataNotificationListener,
        OnReceiveNotificationListener {
    private static final String TAG = NotificationFragment.class.getSimpleName();
    private Adapter mAdapter;
    GravitySnapHelper mGravitySnapHelper;
    Context mContext;
    View mNoMessageText;
    final UiHandler mHandler = new UiHandler();
    NotificationWizard mNotificationWizard;
    List<NotificationBean> mList;
    RecyclerView mRecyclerView;
    private Handler mhandler= new Handler();
    private  Runnable runnable;

    private Handler mhandler_20= new Handler();
    private  Runnable runnable_20;
    private AsyncTask<Void, Void, Void> mFetchTask;
    NotificationComparator mNotificationComparator;
    private static final HashSet<String> sTopAndLockPackage = new HashSet<String>() {{
        add("com.mstarc.music.wearablemusic");
        add("com.mstarc.heartrate.heartrate");
        add("com.mstarc.app.radio");
        add("com.mstarc.record.wearablerecorder");
        add("com.mstarc.plus.route_navigation");
        add("com.mstarc.wearablesport");
    }};
    boolean bRevertSort = true;
public int percent_watch;

    class FetchTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.e(TAG, "begin addAll");
                List<NotificationBean> allNotification = mNotificationWizard.getAll();
                synchronized (mLock) {
                    mList.clear();
                    if (allNotification != null) {
                        mList.addAll(allNotification);
                        Log.e(TAG, "got notification size: " + allNotification.size());
                        updateData();
                    } else {
                        Log.e(TAG, "got null notification list");
                    }
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public NotificationFragment() {
        mGravitySnapHelper = new GravitySnapHelper(Gravity.TOP);
        mNotificationComparator = new NotificationComparator();
    }

    @Override
    public void OnDeleteOneNotification(long l) {
        Log.d(TAG, "onDeOnDeleteNotification: " + l);
        for (NotificationBean notification : mList) {
            if (notification.getId() == l) {
                synchronized (mLock) {
                    Log.d(TAG, "onDeOnDeleteNotification: remove " + l);
                    mList.remove(notification);
                    updateData();
                }
                break;
            }
        }
    }

    @Override
    public void OnDeleteOneByNotificationKey(String s) {
        Log.d(TAG, "OnDeleteOneByNotificationKey: " + s);
        if (s == null) return;
        String notificationkey;
        for (NotificationBean notification : mList) {
            notificationkey = notification.getNotificationKey();
            if (notificationkey != null && notificationkey.equals(s)) {
                synchronized (mLock) {
                    Log.d(TAG, "OnDeleteOneByNotificationKey: remove " + s);
                    mList.remove(notification);
                    updateData();
                }
                break;
            }
        }
    }

    @Override
    public void OnDeleteAllNotification() {
        synchronized (mLock) {
            mList.clear();
        }
        updateData();
        Log.d(TAG, "onDeOnDeleteAllNotification");
    }

    @Override
    public void onUpdataNotification(NotificationBean notificationBean) {
        synchronized (mLock) {
            for (NotificationBean notification : mList) {
                if (notification.getId() == notificationBean.getId()) {
                    mList.remove(notification);
                    mList.add(notification);
                    if (isNeedSmartDialog(notificationBean)) {
                        mHandler.sendMessage(mHandler.obtainMessage(mHandler.MSG_SHOW_SMART_DIALOG, notificationBean));
                    }
                    break;
                }
            }
        }
        updateData();
        Log.d(TAG, "onUpdataNotification" + notificationBean.getId());
    }

    @Override
    public void onReceiveNotification(NotificationBean notificationBean) {
        if (notificationBean.getType() == WatchCode.PHONE_POWER) {
            CommonManager.getInstance(getContext().getApplicationContext()).setPhoneBatteryString(notificationBean.getContent());
        }
        if (isNeedSmartDialog(notificationBean)) {
            mHandler.sendMessage(mHandler.obtainMessage(mHandler.MSG_SHOW_SMART_DIALOG, notificationBean));
        }
        synchronized (mLock) {
            if (isOneInstance(notificationBean)) {
                for (NotificationBean tempbean : mList) {
                    if (tempbean.getType() == notificationBean.getType()) {
                        mList.remove(tempbean);
                    }
                }
            }
            mList.add(notificationBean);
        }
        updateData();
        Log.d(TAG, "onReceiveNotification" + notificationBean.getId());

    }

    class UiHandler extends Handler {
        public static final int MSG_REMOVE_ALL = 0;
        public static final int MSG_REMOVE_SHOW_NO_MESSAGE = 1;
        public static final int MSG_REMOVE_HIDE_NO_MESSAGE = 2;
        public static final int MSG_UPDATE_LIST = 3;
        public static final int MSG_SHOW_SMART_DIALOG = 4;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REMOVE_ALL:
                    mNoMessageText.setVisibility(View.VISIBLE);
                    try {
                        mNotificationWizard.removeAll();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case MSG_REMOVE_SHOW_NO_MESSAGE:
                    Log.d(TAG, "MSG_REMOVE_SHOW_NO_MESSAGE");
                    mNoMessageText.setVisibility(View.VISIBLE);
                    break;
                case MSG_REMOVE_HIDE_NO_MESSAGE:
                    Log.d(TAG, "MSG_REMOVE_HIDE_NO_MESSAGE");
                    mNoMessageText.setVisibility(View.INVISIBLE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mRecyclerView.invalidate();
                    break;
                case MSG_UPDATE_LIST:
                    processNotifications();
                    mAdapter.notifyDataSetChanged();
                    break;
                case MSG_SHOW_SMART_DIALOG:
                    NotificationBean notificationBean = (NotificationBean) msg.obj;
                    try {
                        showSmartDialog(notificationBean);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mNotificationWizard.onDestroy();
    }

    Object mLock = new Object();
    ArrayList<NotificationBean> mNotifiacitonDisplayList = new ArrayList<>();

    class NotificationComparator implements Comparator<NotificationBean> {
        @Override
        public int compare(NotificationBean o1, NotificationBean o2) {
            int result;
            if (sTopAndLockPackage.contains(o1.getPkgName()) && !sTopAndLockPackage.contains(o2.getPkgName())) {
                result = 1;
            } else if (!sTopAndLockPackage.contains(o1.getPkgName()) && sTopAndLockPackage.contains(o2.getPkgName())) {
                result = -1;
            } else {
                if (o1.getTime() < o2.getTime()) {
                    result = -1;
                } else if (o1.getTime() > o2.getTime()) {
                    result = 1;
                } else {
                    result = 0;
                }
            }
            if (bRevertSort) result *= -1;
            return result;
        }
    }

    void processNotifications() {
        mNotifiacitonDisplayList.clear();
        ArrayList<NotificationBean> tempList = new ArrayList<>();

//        int i = 0;
//        long time = System.currentTimeMillis();
//        for(NotificationBean bean : mList){
//            bean.setTime(time);
//            time = time - (++i)*60*1000;
//        }
        synchronized (mLock) {
            tempList.clear();
            tempList.addAll(mList);
            Collections.sort(tempList, mNotificationComparator);
        }
        mNotifiacitonDisplayList.clear();
        for (NotificationBean notificationBean : tempList) {
            if (isNeedDisplayinNotificaitonSheet(notificationBean)) {
                mNotifiacitonDisplayList.add(notificationBean);
            }
        }
    }

    boolean isNeedDisplayinNotificaitonSheet(NotificationBean notificationBean) {
        boolean need = false;
        switch (notificationBean.getType()) {
            case WatchCode.SCHEDULE:
            case WatchCode.SWEET_WORDS:
            case WatchCode.MOBILE_AWAY_BODY_REMIND://receive one , remove same type of previous
            case WatchCode.SEDENTARINESS://receive one , remove same type of previous
            case WatchCode.PHONE_NOTIFICATION:
            case WatchCode.PHONE_MISSED_CALL:
            case WatchCode.PHONE_UNREAD_SMS:
            case WatchCode.WATCH_NOTIFICATION:
            case WatchCode.OTA:
            case WatchCode.WATCH_LOW_POWER:
                need = true;
                break;
        }
        return need;
    }

    boolean isNeedDisplayDetailContent(NotificationBean notificationBean) {
        boolean needDetailDisplay = false;
        switch (notificationBean.getType()) {
            case WatchCode.PHONE_NOTIFICATION:
            case WatchCode.PHONE_MISSED_CALL:
            case WatchCode.PHONE_UNREAD_SMS:
            case WatchCode.WATCH_NOTIFICATION:
           // case WatchCode.WATCH_LOW_POWER:
                needDetailDisplay = true;
                break;
        }
        return needDetailDisplay;
    }

    boolean isNeedAction(NotificationBean notificationBean) {
        boolean needAction = false;
        switch (notificationBean.getType()) {
            case WatchCode.WATCH_NOTIFICATION:
            case WatchCode.OTA:
            case WatchCode.WEATHER_FORECASTING: //pending
           // case WatchCode.WATCH_LOW_POWER:
                needAction = true;
                break;
        }
        return needAction;
    }

    boolean isNeedSmartDialog(NotificationBean notificationBean) {
        boolean need = false;
        switch (notificationBean.getType()) {
            case WatchCode.SCHEDULE:
            case WatchCode.SWEET_WORDS:
            case WatchCode.MOBILE_AWAY_BODY_REMIND://receive one , remove same type of previous
            case WatchCode.SEDENTARINESS://receive one , remove same type of previous
            case WatchCode.WATCH_LOW_POWER:
                need = true;
                break;
        }
        return need;
    }

    boolean isOneInstance(NotificationBean notificationBean) {
        boolean need = false;
        switch (notificationBean.getType()) {
            case WatchCode.MOBILE_AWAY_BODY_REMIND://receive one , remove same type of previous
            case WatchCode.SEDENTARINESS://receive one , remove same type of previous
           // case WatchCode.WATCH_LOW_POWER:
                need = true;
                break;
        }
        return need;
    }
    public void showWatchMode(){

        try {
            MainActivity main = (MainActivity) getActivity();
            IdleFragment idleFragment = main.getIdleFragment();
            QSFragment.QSPower qsPower = idleFragment.getQSFragment().getQSPower();
            QuickSettingPower quickSettingPower = (QuickSettingPower) qsPower.getView();
            quickSettingPower.selectMode(1, true);
        } catch (Exception e) {
            Log.e(TAG, "onClick: ", e);
        }

    }


    public void  showLowPower(String con){

        if(con.equals(LOW_C2)) {
            final Dialog dialog1 = new Dialog(getContext());

            View contentView1 = LayoutInflater.from(getContext()).inflate(
                    R.layout.smart_lpower, null);
            dialog1.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog1.setContentView(contentView1);
            dialog1.setCanceledOnTouchOutside(true);

            TextView num_tv = (TextView) contentView1.findViewById(R.id.content);

            ImageView lpower_cancle = (ImageView) contentView1.findViewById(R.id.cancel);

            ImageView lpower_true = (ImageView) contentView1.findViewById(R.id.confirm);
            updateImageView(lpower_cancle, R.drawable.ic_notification_clean_cancel);
            updateImageView(lpower_true, R.drawable.ic_notification_clean_confirm);

            runnable = new Runnable() {
                @Override
                public void run() {
                    showWatchMode();
                }
            };
            mhandler.postDelayed(runnable,9*1000);
            dialog1.show();



            lpower_true.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mhandler.removeCallbacks(runnable);
                    showWatchMode();
                    dialog1.dismiss();
                }
            });
            lpower_cancle.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    mhandler.removeCallbacks(runnable);
                    dialog1.dismiss();
                }
            });


        }else if(con.equals(LOW_C1))
        {
            final Dialog dialog1 = new Dialog(getContext());

            View contentView1 = LayoutInflater.from(getContext()).inflate(
                    R.layout.smart_lpower_20, null);
            dialog1.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog1.setContentView(contentView1);
            dialog1.setCanceledOnTouchOutside(true);

            TextView num_tv = (TextView) contentView1.findViewById(R.id.content);

            runnable_20 = new Runnable() {
                @Override
                public void run() {
                    dialog1.dismiss();
                }
            };
            mhandler_20.postDelayed(runnable_20,5*1000);

            contentView1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dialog1.dismiss();
                }
            });
            dialog1.show();



        }

    }
    SmartNotificationDialog mDialog;

    void showSmartDialog(final NotificationBean notificationBean) {
        //TODO
        Log.d(TAG, "showSmartDialog");
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        int color = ThemeUtils.getCurrentPrimaryColor();
        if (color == 2 && ThemeUtils.isProductG7())//HIGH_BLACK
        {
            switch (notificationBean.getType()) {
                case WatchCode.SCHEDULE:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_SMART_NOTIFICATION,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_notify, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.SWEET_WORDS:

                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_MIYU,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_miyu, notificationBean.getContent(), null, null,
                            getSmartMiyuIcon(notificationBean.getIconAdress()));
                    break;
                case WatchCode.MOBILE_AWAY_BODY_REMIND:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_FORGET,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_forget_b, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.SEDENTARINESS:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_JIURZUO,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_jiuzuo_b, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.WATCH_LOW_POWER:
                    showLowPower(notificationBean.getContent());
                    break;


            }
        } else if (color == -5243136 && ThemeUtils.isProductG7())//APPLE_GREEN
        {
            switch (notificationBean.getType()) {
                case WatchCode.SCHEDULE:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_SMART_NOTIFICATION,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_notify, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.SWEET_WORDS:

                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_MIYU,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_miyu, notificationBean.getContent(), null, null,
                            getSmartMiyuIcon(notificationBean.getIconAdress()));
                    break;
                case WatchCode.MOBILE_AWAY_BODY_REMIND:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_FORGET,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_forget_g, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.SEDENTARINESS:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_JIURZUO,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_jiuzuo_g, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.WATCH_LOW_POWER:

                    showLowPower(notificationBean.getContent());

                    break;
            }
        } else {//ROSE_GOLDEN
            switch (notificationBean.getType()) {
                case WatchCode.SCHEDULE:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_SMART_NOTIFICATION,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_notify, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.SWEET_WORDS:

                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_MIYU,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_miyu, notificationBean.getContent(), null, null,
                            getSmartMiyuIcon(notificationBean.getIconAdress()));
                    break;
                case WatchCode.MOBILE_AWAY_BODY_REMIND:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_FORGET,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_forget, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.SEDENTARINESS:
                    mDialog = new SmartNotificationDialog(getContext(),
                            SmartNotificationDialog.BEHAVIAR_TYPE_JIURZUO,
                            new SmartNotificationDialog.Listener() {
                                @Override
                                public void onSetting() {
                                    Log.d(TAG, "onSetting " + notificationBean);
                                }

                                @Override
                                public void onConfirm() {
                                    Log.d(TAG, "onConfirm " + notificationBean);
                                }
                            },
                            R.layout.smart_jiuzuo, notificationBean.getContent(), null, null);
                    break;
                case WatchCode.WATCH_LOW_POWER:
                    showLowPower(notificationBean.getContent());
                    break;
            }
        }
        if(null!=mDialog)
        mDialog.show();

    }
    private void updateImageView(final View view, final int resId) {
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
    public static int getSmartMiyuIcon(String iconAdress) {
        if (iconAdress == null) {
            return 0;
        }
        int resId = 0;

        int color = ThemeUtils.getCurrentPrimaryColor();
        if (color == 2 && ThemeUtils.isProductG7())//HIGH_BLACK
        {
            if (iconAdress.equals("anxin")) {
                resId = R.drawable.smart_icon_anxin_b;
            } else if (iconAdress.equals("biehejiu")) {
                resId = R.drawable.smart_icon_biehejiu_b;
            } else if (iconAdress.equals("chiyao")) {
                resId = R.drawable.smart_icon_chiyao_b;
            } else if (iconAdress.equals("feiji")) {
                resId = R.drawable.smart_icon_feiji_b;
            } else if (iconAdress.equals("heshui")) {
                resId = R.drawable.smart_icon_heshui_b;
            } else if (iconAdress.equals("jianshen")) {
                resId = R.drawable.smart_icon_jianshen_b;
            } else if (iconAdress.equals("ningmeng")) {
                resId = R.drawable.smart_icon_ningmeng_b;
            } else if (iconAdress.equals("pijiu")) {
                resId = R.drawable.smart_icon_pijiu_b;
            } else if (iconAdress.equals("qiche")) {
                resId = R.drawable.smart_icon_qiche_b;
            } else if (iconAdress.equals("shengri")) {
                resId = R.drawable.smart_icon_shengri_b;
            } else if (iconAdress.equals("zaocan")) {
                resId = R.drawable.smart_icon_zaocan_b;
            } else if (iconAdress.equals("xiuxi")) {
                resId = R.drawable.smart_icon_xiuxi_b;
            } else if (iconAdress.equals("yanjing")) {
                resId = R.drawable.smart_icon_yanjing_b;
            } else if (iconAdress.equals("yinyue")) {
                resId = R.drawable.smart_icon_yinyue_b;
            } else if (iconAdress.equals("zhuyishenti")) {
                resId = R.drawable.smart_icon_zhuyishenti_b;
            }
        } else if (color == -5243136 && ThemeUtils.isProductG7())//APPLE_GREEN
        {
            if (iconAdress.equals("anxin")) {
                resId = R.drawable.smart_icon_anxin_g;
            } else if (iconAdress.equals("biehejiu")) {
                resId = R.drawable.smart_icon_biehejiu_g;
            } else if (iconAdress.equals("chiyao")) {
                resId = R.drawable.smart_icon_chiyao_g;
            } else if (iconAdress.equals("feiji")) {
                resId = R.drawable.smart_icon_feiji_g;
            } else if (iconAdress.equals("heshui")) {
                resId = R.drawable.smart_icon_heshui_g;
            } else if (iconAdress.equals("jianshen")) {
                resId = R.drawable.smart_icon_jianshen_g;
            } else if (iconAdress.equals("ningmeng")) {
                resId = R.drawable.smart_icon_ningmeng_g;
            } else if (iconAdress.equals("pijiu")) {
                resId = R.drawable.smart_icon_pijiu_g;
            } else if (iconAdress.equals("qiche")) {
                resId = R.drawable.smart_icon_qiche_g;
            } else if (iconAdress.equals("shengri")) {
                resId = R.drawable.smart_icon_shengri_g;
            } else if (iconAdress.equals("zaocan")) {
                resId = R.drawable.smart_icon_zaocan_g;
            } else if (iconAdress.equals("xiuxi")) {
                resId = R.drawable.smart_icon_xiuxi_g;
            } else if (iconAdress.equals("yanjing")) {
                resId = R.drawable.smart_icon_yanjing_g;
            } else if (iconAdress.equals("yinyue")) {
                resId = R.drawable.smart_icon_yinyue_g;
            } else if (iconAdress.equals("zhuyishenti")) {
                resId = R.drawable.smart_icon_zhuyishenti_g;
            }
        } else//ROSE_GOLDEN
        {
            if (iconAdress.equals("anxin")) {
                resId = R.drawable.smart_icon_anxin;
            } else if (iconAdress.equals("biehejiu")) {
                resId = R.drawable.smart_icon_biehejiu;
            } else if (iconAdress.equals("chiyao")) {
                resId = R.drawable.smart_icon_chiyao;
            } else if (iconAdress.equals("feiji")) {
                resId = R.drawable.smart_icon_feiji;
            } else if (iconAdress.equals("heshui")) {
                resId = R.drawable.smart_icon_heshui;
            } else if (iconAdress.equals("jianshen")) {
                resId = R.drawable.smart_icon_jianshen;
            } else if (iconAdress.equals("ningmeng")) {
                resId = R.drawable.smart_icon_ningmeng;
            } else if (iconAdress.equals("pijiu")) {
                resId = R.drawable.smart_icon_pijiu;
            } else if (iconAdress.equals("qiche")) {
                resId = R.drawable.smart_icon_qiche;
            } else if (iconAdress.equals("shengri")) {
                resId = R.drawable.smart_icon_shengri;
            } else if (iconAdress.equals("zaocan")) {
                resId = R.drawable.smart_icon_zaocan;
            } else if (iconAdress.equals("xiuxi")) {
                resId = R.drawable.smart_icon_xiuxi;
            } else if (iconAdress.equals("yanjing")) {
                resId = R.drawable.smart_icon_yanjing;
            } else if (iconAdress.equals("yinyue")) {
                resId = R.drawable.smart_icon_yinyue;
            } else if (iconAdress.equals("zhuyishenti")) {
                resId = R.drawable.smart_icon_zhuyishenti;
            }
        }
        Log.d(TAG, "return " + resId + " for " + iconAdress);
        return resId;
    }

    DetailContentDialog mDetailContentDialog;

    void showDetailContent(NotificationBean notificationBean) {
        //TODO
        Log.d(TAG, "showDetailContent");
        if (mDetailContentDialog != null && mDetailContentDialog.isShowing()) {
            mDetailContentDialog.dismiss();
        }
        mDetailContentDialog = new DetailContentDialog(getContext(), notificationBean);
        mDetailContentDialog.show();
    }

    boolean doAction(NotificationBean notificationBean) {
        //TODO
        Log.d(TAG, "doAction");
        String uri = notificationBean.getIntentUri();
        Log.d(TAG, "intent uri " + uri);
        if (uri != null) {
            try {
                Intent intent = Intent.parseUri(uri, 0);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                Bundle translateBundle =
                        ActivityOptions.makeCustomAnimation(getActivity(),
                                R.anim.slide_in_left, R.anim.slide_out_left).toBundle();
                getActivity().startActivity(intent, translateBundle);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            String pkgName = notificationBean.getPkgName();
            String className = notificationBean.getComponentClassName();
            if (TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(className)) {
                Log.e(TAG, " null uri, null pkg name, null class name during doAction");
                return false;
            }

            Log.d(TAG, "pkgName: " + pkgName);
            Log.d(TAG, "className: " + className);
            if (pkgName.equals("com.mstarc.wearablephone")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, null);
                intent.setType(CallLog.Calls.CONTENT_TYPE);
                getActivity().startActivity(intent);
            } else if (pkgName.equals("com.mstarc.wearablemms")) {
                Intent intent = new Intent();
                className = "MainActivity";
                ComponentName cn = new ComponentName(pkgName, pkgName+"."+className);
                intent.setComponent(cn);
                getActivity().startActivity(intent);
            } else {
                Intent intent = new Intent();
                ComponentName cn = new ComponentName(pkgName, className);
                intent.setComponent(cn);
                try {
                    getActivity().startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }

            return true;
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = container.getContext();

        mList = new CopyOnWriteArrayList<>();
        mNotificationWizard = NotificationWizard.getInstance();
        mNotificationWizard.setNwAidlConnectListener(
                new NotificationWizard.AidlConnectListener() {
                    @Override
                    public void onAidlServiceConnected() {
                        // 服务绑定成功即可调用其他方法
                        // 不再提醒
                        // mNotificationWizard.add2BlackList(notificationbean);
                        // UI组件生命周期结束后
                        // mNotificationWizard.onDestroy();
                        if (mFetchTask != null) {
                            mFetchTask.cancel(true);
                        }
                        mFetchTask = new FetchTask();
                        try {
                            mFetchTask.execute();
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onAidlServiceDisconnected() {

                    }
                });
        mNotificationWizard.addOnAidlCallBack(new ICommonAidlListener<WatchFace>() {
            @Override
            public void onReceiveBleData(WatchFace watchFace) {

            }
        }, WatchFace.class);
        mNotificationWizard.setOnDeleteNotificationListener(this);
        mNotificationWizard.setOnUpdataNotificationListener(this);
        mNotificationWizard.setOnReceiveNotification(this);
        mNotificationWizard.setOnBatteryChangedListener(new OnBatteryChangedListener() {
            @Override
            public void onBatteryChanged(int percent) {
                System.out.println("remain battery: " + percent + "%");
                percent_watch=percent;
            }
        });
        mNotificationWizard.initNotificationWizard(getContext());


        View rootView = inflater.inflate(R.layout.notification_subfragment_layout, container,
                false);
        mNoMessageText = rootView.findViewById(R.id.notification_no_messages);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.notification_container);
        rootView.setTag(DepthPageTransformer.ITEM_RIGHT_OR_BOTTOM);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mAdapter = new Adapter(getContext(), mNotifiacitonDisplayList);
        mAdapter.setOnItemClickLitener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DecorationSettingItem(getContext(), LinearLayoutManager.VERTICAL));
        mRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mGravitySnapHelper.attachToRecyclerView(mRecyclerView);
/***
        try{
            final OnBatteryChangedListener onBatteryChangedListener = (OnBatteryChangedListener) new Mirror().on(mNotificationWizard).get().field("onBatteryChangedListener");
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    onBatteryChangedListener.onBatteryChanged(power);
                    power--;
                    Log.d(TAG, "test lowpower=  " +power);
                    if(power == 10){
                        power = 25;
                    }
                }
            }, 0, 10, TimeUnit.SECONDS);

        }catch (Exception e){
            e.printStackTrace();
        }
**************/
        return rootView;
    }
    private int power = 25;

    @Override
    public void onSnap(int position) {

    }

    @Override
    public void onStop() {
        super.onStop();
        if (mFetchTask != null) {
            mFetchTask.cancel(false);
        }
    }

    private void updateData() {
        mHandler.sendEmptyMessage(UiHandler.MSG_UPDATE_LIST);
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.d(TAG, "onItemClick " + position);
        final NotificationBean notificationBean;
        synchronized (mLock) {
            notificationBean = mNotifiacitonDisplayList.get(position);
        }
        if (isNeedAction(notificationBean)) {
            Log.d(TAG, "send Action");
            if (doAction(notificationBean)) {
                removeItem(notificationBean);
                return;
            }
        }
        if (isNeedSmartDialog(notificationBean)) {
            Log.d(TAG, "show smart dialog");
            showDetailContent(notificationBean);
        } else { //if (isNeedDisplayDetailContent(notificationBean)) {
            Log.d(TAG, "show details");
            mHandler.sendMessage(mHandler.obtainMessage(mHandler.MSG_SHOW_SMART_DIALOG, notificationBean));
        }
        removeItem(notificationBean);
    }

    private void removeItem(final NotificationBean notificationBean) {
        if (!sTopAndLockPackage.contains(notificationBean.getPkgName())) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (notificationBean.getCanRemove()) {
                            mNotificationWizard.remove(notificationBean.getId());
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mList.remove(notificationBean);
                    updateData();
                }
            }, 1000);
        }
    }

    @Override
    public void onItemLongClick(View view, int position) {
        Log.d(TAG, "onItemLongClick " + position);
        Dialog dialog = new ConfirmDialog(mContext, this, R.layout.notification_cleanup);
        dialog.show();
    }

    @Override
    public void onConfirm() {
        mHandler.dispatchMessage(Message.obtain(mHandler, UiHandler.MSG_REMOVE_ALL));
    }

    @Override
    public void onCancel() {

    }

    class Adapter extends RecyclerView.Adapter<Adapter.VH> {

        private com.mstarc.wearablelauncher.view.common.OnItemClickListener mOnItemClickLitener;
        List<NotificationBean> mData;
        private final ViewBinderHelper binderHelper = new ViewBinderHelper();

        public Adapter(Context mContext, List<NotificationBean> data) {
            this.mData = data;
            binderHelper.setOpenOnlyOne(true);
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder viewType " + viewType);
            int color = ThemeUtils.getCurrentPrimaryColor();
            if (color == 0xFFAFFF00) {
                View view = View.inflate(parent.getContext(), R.layout.notification_item1_g, null);
                return new VH(view);
            } else {
                View view = View.inflate(parent.getContext(), R.layout.notification_item1, null);
                return new VH(view);
            }

        }

        @Override
        public int getItemCount() {
            int size;
            synchronized (mLock) {
                size = mData.size();
            }
            if (size == 0) {
                NotificationFragment.this.mHandler.dispatchMessage(Message.obtain(NotificationFragment.this.mHandler, UiHandler.MSG_REMOVE_SHOW_NO_MESSAGE));
            } else {
                NotificationFragment.this.mHandler.dispatchMessage(Message.obtain(NotificationFragment.this.mHandler, UiHandler.MSG_REMOVE_HIDE_NO_MESSAGE));
            }
            Log.d(TAG, "getItemCount: " + size);
            return size;
        }

        @Override
        public void onBindViewHolder(final VH holder, final int position) {
            NotificationBean data;
            synchronized (mLock) {
                data = mData.get(position);
            }
            Log.d(TAG, "" + position + "" + data.getContent());
            if (sTopAndLockPackage.contains(data.getPkgName())) {
                binderHelper.lockSwipe(data.getId().toString());
            }
            holder.swipeLayout.close(false);
            holder.mContent.setText(data.getContent());
            holder.mTime.setText(DateFormat.format("HH:mm", data.getTime()));
            holder.mData.setText(DateFormat.format("MM/dd", data.getTime()));
            if (mOnItemClickLitener != null) {
                holder.mNormalView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = holder.getLayoutPosition();
                        mOnItemClickLitener.onItemClick(holder.swipeLayout, pos);
                    }
                });

                holder.mNormalView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        int pos = holder.getLayoutPosition();
                        mOnItemClickLitener.onItemLongClick(holder.swipeLayout, pos);
                        return true;
                    }
                });
            }
            int iconResId = 0;
            switch (data.getType()) {
                case WatchCode.PHONE_UNREAD_SMS:
                    iconResId = R.drawable.notification_duanxin;
                    break;
                case WatchCode.SCHEDULE:
                    iconResId = R.drawable.notification_richeng;
                    break;
                case WatchCode.SEDENTARINESS:
                    iconResId = R.drawable.notification_jiuzuo;
                    break;
                case WatchCode.MOBILE_AWAY_BODY_REMIND:
                    iconResId = R.drawable.notification_wangdai;
                    break;
                case WatchCode.WATCH_LOW_POWER:
                    iconResId = R.drawable.notification_lpower;
                    break;
            }
            if (iconResId != 0) {
               // holder.mIcon.setImageResource(iconResId);

                int currentPrimaryColor = ThemeUtils.getCurrentPrimaryColor();
                Drawable tintDrawable = BitmapDrawableUtils.getTintDrawable(
                        ContextCompat.getDrawable(getActivity(), iconResId).mutate(), BitmapDrawableUtils.getStateList(currentPrimaryColor));
                holder.mIcon.setImageDrawable(tintDrawable);

            } else {
                Bitmap bitmap = data.getIconBitmap();
                holder.mIcon.setImageBitmap(bitmap);
            }

            binderHelper.bind(holder.swipeLayout, mData.get(position).getId().toString());
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    Log.d(TAG, "delete " + pos);
                    holder.swipeLayout.close(false);
                    synchronized (mLock) {
                        try {
                            mNotificationWizard.remove(mData.get(position).getId());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            holder.denyButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    final int pos = holder.getLayoutPosition();
                    Log.d(TAG, "deny " + pos);
                    ConfirmDialog confirmDialog = new ConfirmDialog(getContext(), new ConfirmDialog.Listener() {
                        @Override
                        public void onConfirm() {
                            Log.d(TAG, "deny " + pos);
                            holder.swipeLayout.close(false);
                            try {
                                synchronized (mLock) {
                                    mNotificationWizard.remove(mData.get(position).getId());
                                    mNotificationWizard.add2BlackList(mData.get(position));
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancel() {

                        }
                    }, R.layout.notification_deny_confirm);
                    confirmDialog.show();
                }
            });


        }

        @Override
        public int getItemViewType(int position) {
            Log.d(TAG, "getItemViewType " + position);
//            return mData.get(position).getType();
            return 0;
        }

        public class VH extends RecyclerView.ViewHolder {
            ImageView mIcon;
            TextView mTime;
            TextView mContent;
            TextView mData;
            View mNormalView;
            private SwipeRevealLayout swipeLayout;
            private ImageButton deleteButton, denyButton;


            public VH(View itemView) {
                super(itemView);
                mIcon = (ImageView) itemView.findViewById(R.id.notification_item_icon);
                mTime = (TextView) itemView.findViewById(R.id.notification_item_time);
                mData = (TextView) itemView.findViewById(R.id.notification_item_date);
                mContent = (TextView) itemView.findViewById(R.id.notification_item_content);
                swipeLayout = (SwipeRevealLayout) itemView.findViewById(R.id.swipe_layout);

                deleteButton = (ImageButton) itemView.findViewById(R.id.delete_button);
                denyButton = (ImageButton) itemView.findViewById(R.id.deny_button);

                mNormalView = itemView.findViewById(R.id.notification_normal_view);


            }

        }


        public void setOnItemClickLitener(com.mstarc.wearablelauncher.view.common.OnItemClickListener listener) {
            this.mOnItemClickLitener = listener;
        }

        /**
         * Only if you need to restore open/close state when the orientation is changed.
         * Call this method in {@link android.app.Activity#onSaveInstanceState(Bundle)}
         */
        public void saveStates(Bundle outState) {
            binderHelper.saveStates(outState);
        }

        /**
         * Only if you need to restore open/close state when the orientation is changed.
         * Call this method in {@link android.app.Activity#onRestoreInstanceState(Bundle)}
         */
        public void restoreStates(Bundle inState) {
            binderHelper.restoreStates(inState);
        }

    }

    class Data {

        public final static int TYPE_SMS = 0;
        public final static int TYPE_WX = 1;
        public final static int TYPE_SETTING = 2;
        public final static int TYPE_SPORT = 3;
        public int mType;
        public String mContent;
        public String mTime;

        public Data(int mType, String mTime, String mContent) {
            this.mType = mType;
            this.mContent = mContent;
            this.mTime = mTime;
        }

    }

}
