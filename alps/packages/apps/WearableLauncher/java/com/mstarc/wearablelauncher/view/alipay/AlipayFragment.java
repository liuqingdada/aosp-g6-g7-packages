package com.mstarc.wearablelauncher.view.alipay;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.commonbase.communication.utils.CommonServiceConnection;
import com.mstarc.fakewatch.alipay.Alipay;
import com.mstarc.wearablelauncher.MainActivity;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.common.DepthPageTransformer;
import com.mstarc.wearablelauncher.view.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablelauncher.view.common.VerticalViewPager;

import org.w3c.dom.Text;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by wangxinzhi on 17-2-19.
 */

public class AlipayFragment extends Fragment implements RecyclerViewItemTouchListener.OnItemClickEventListener,
        CommonServiceConnection.OnServiceConnectedListener {
    private static final String TAG = AlipayFragment.class.getSimpleName();

    boolean mAliPayQR = true;
    PayAdapter mAdapter;
    private RecyclerView mRecyclerView;
    PagerSnapHelper mPageSnapHelper;
    RecyclerViewItemTouchListener mRecyclerViewItemTouchListener;
    Alipay mAlipay;
    boolean isAliPayConnected;
    Timer mTimer;
    TimerTask mTimerTask;
    Handler mHandler;
    Point mAliPayBarDimens, mAliPayQRDimens;
    AliPayBitmap mAliPayBitmap;
    boolean isG7 = false;
    int mG7color = 0xff1294c1;
    Bitmap mAlipayLogo;

    @Override
    public void onServiceConnected() {
        Log.d(TAG,"onServiceConnected 1");
        synchronized (this) {
            Log.d(TAG,"onServiceConnected 2");
            isAliPayConnected = true;
        }
    }

    @Override
    public void onServiceDisconnected() {
        synchronized (this) {
            isAliPayConnected = false;
        }
    }


    public enum PAGES {
        ALIPAY,
        NFC_PAY1,
        NFC_PAY2
    }

    class Handler extends android.os.Handler {
        public final static int MSG_UPDATE_ALIPAY_CODES = 1;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_ALIPAY_CODES:
                    mAdapter.notifyItemChanged(PAGES.ALIPAY.ordinal());
                    break;
                default:
                    break;
            }
        }
    }

    class AliPayBitmap {
        final static int VALIDATE_TIME = 30 * 1000;  // 30 Seconds;
        private Bitmap mBar = null;
        private Bitmap mQR = null;
        private long mLastUpdateDate = 0;

        synchronized public boolean update(Bitmap bar, Bitmap qr) {
            if (bar == null || qr == null) {
                return false;
            } else {
                mBar = bar;
                mQR = qr;
                Log.d(TAG, String.format("Qr: %d x %d", mQR.getWidth(),mQR.getHeight()));
                Log.d(TAG, String.format("Bar: %d x %d", mBar.getWidth(),mBar.getHeight()));
                mLastUpdateDate = SystemClock.uptimeMillis();
                return true;
            }
        }

        synchronized boolean isValidate() {
            if (mBar == null || mQR == null) {
                return false;
            } else {
                if ((SystemClock.uptimeMillis() - mLastUpdateDate) > VALIDATE_TIME) {
                    return false;
                }
            }
            return true;
        }

        synchronized Bitmap getBar() {
            return mBar;
        }

        synchronized Bitmap getQR() {
            return mQR;
        }
    }

    public AlipayFragment() {
        mPageSnapHelper = new PagerSnapHelper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pay, container, false);
        view.setTag(DepthPageTransformer.ITEM_LEFT_OR_TOP);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.vpager);
        mAdapter = new PayAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setHasFixedSize(true);
        mPageSnapHelper.attachToRecyclerView(mRecyclerView);
        mRecyclerViewItemTouchListener = new RecyclerViewItemTouchListener(getContext(), this);
        mRecyclerView.addOnItemTouchListener(mRecyclerViewItemTouchListener);
        isG7 = getResources().getBoolean(R.bool.g7_target);
        mAlipayLogo = ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_rightboard_5_alipay)).getBitmap();
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume update data");
        mAdapter.notifyDataSetChanged();
        startTimer();
    }

    private void stopTimer() {
        mTimer.purge();
        mTimer.cancel();
        mTimer = null;
        mTimerTask = null;
    }

    private void startTimer() {
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                Bitmap bar = null;
                Bitmap qr = null;
                try {
                    if (isAliPayConnected && mAlipay.isBinded()) {
                        Log.d(TAG,String.format("request bar size: %d x %d", mAliPayBarDimens.x, mAliPayBarDimens.y));
                        Log.d(TAG,String.format("request qr size: %d x %d", mAliPayQRDimens.x, mAliPayQRDimens.y));
                        bar = mAlipay.getBarImage(mAliPayBarDimens.x, mAliPayBarDimens.y);
                        qr = mAlipay.getQRImage(mAlipayLogo, mAliPayQRDimens.x, mAliPayQRDimens.y);
                        if (mAliPayBitmap.update(bar, qr)) {
                            mHandler.sendEmptyMessage(Handler.MSG_UPDATE_ALIPAY_CODES);
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        };
        mTimer.schedule(mTimerTask, 0, 20000); // update every 20 seconds;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAliPayBitmap = new AliPayBitmap();
        mAliPayBarDimens = new Point();
        mAliPayQRDimens = new Point();
        mAliPayBarDimens.x = (int) getResources().getDimension(R.dimen.alipay_bar_width);
        mAliPayBarDimens.y = (int) getResources().getDimension(R.dimen.alipay_bar_height);
        mAliPayQRDimens.x = (int) getResources().getDimension(R.dimen.alipay_qr_width);
        mAliPayQRDimens.y = (int) getResources().getDimension(R.dimen.alipay_qr_height);
        mAlipay = Alipay.getInstance();
        mAlipay.initAlipay(getActivity());
        mAlipay.setOnOnAlipayConnectionListener(this);
        mHandler = new Handler();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlipay.onDestroy(getActivity());
    }

    class PayAdapter extends RecyclerView.Adapter<PayViewHolder> {

        @Override
        public PayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = View.inflate(parent.getContext(), R.layout.alipay_layout, null);
            return new PayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PayViewHolder holder, int position) {
            PAGES page = PAGES.values()[position];
            switch (page) {
                case ALIPAY: {
                    try {
                        Log.d(TAG, "isAliPayConnected "+isAliPayConnected+" mAlipay.isBinded:"+mAlipay.isBinded());
//                        boolean isTest = SystemProperties.getBoolean("persist.launcher.alipay.test", false);
//                        if (isAliPayConnected && (mAlipay.isBinded() || isTest)) {
                        if (isAliPayConnected && (mAlipay.isBinded())) {
                            holder.mNormalView.setVisibility(View.VISIBLE);
                            holder.mHintView.setVisibility(View.GONE);
                            if (mAliPayQR) {
                                if(isG7){
                                    holder.mVendorNameText.setTextColor(mG7color);
                                    holder.mTitleTimeText.setTextColor(mG7color);
                                    holder.itemView.setBackgroundColor(Color.WHITE);
                                }
                                holder.mCardImage.setImageBitmap(mAliPayBitmap.getQR());
                                holder.mVendorNameText.setText(R.string.leftboard_alipay_title_demo0);
                            } else {
                                if (isG7) {
                                    holder.mVendorNameText.setTextColor(Color.WHITE);
                                    holder.mTitleTimeText.setTextColor(Color.WHITE);
                                    holder.itemView.setBackgroundColor(mG7color);
                                }
                                holder.mCardImage.setImageBitmap(mAliPayBitmap.getBar());
                                holder.mVendorNameText.setText(R.string.leftboard_alipay_title_demo0);
                            }
                        } else {
                            if (!holder.hintViewShown) {
                                ((ViewStub) holder.mHintView).inflate();
                                holder.hintViewShown = true;
                            }
                            holder.mNormalView.setVisibility(View.GONE);
                            holder.mHintView.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    SimpleDateFormat sdf=new SimpleDateFormat("HH:mm");
                    holder.mTitleTimeText.setText(sdf.format(new Date()));
                    break;
                }
                case NFC_PAY1:
                    holder.mCardImage.setImageResource(R.drawable.bank_card_demo);
                    holder.mVendorNameText.setText(R.string.leftboard_alipay_title_demo1);
                    break;
                case NFC_PAY2:
                    holder.mCardImage.setImageResource(R.drawable.bank_card_demo2);
                    holder.mVendorNameText.setText(R.string.leftboard_alipay_title_demo2);
                    break;
                default:
                    break;

            }
        }

        @Override
        public int getItemCount() {
            return 1;
//            return PAGES.values().length;
        }
    }

    class PayViewHolder extends RecyclerView.ViewHolder {
        View mNormalView;
        View mHintView;
        ImageView mCardImage;
        TextView mTitleTimeText;
        TextView mVendorNameText;
        boolean hintViewShown;

        public PayViewHolder(View itemView) {
            super(itemView);
            mTitleTimeText = (TextView) itemView.findViewById(R.id.alipay_time);
            mCardImage = (ImageView) itemView.findViewById(R.id.card);
            mVendorNameText = (TextView) itemView.findViewById(R.id.alipay_title);
            mNormalView = itemView.findViewById(R.id.normal_view);
            mHintView = itemView.findViewById(R.id.bound);
        }
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {
        Log.d(TAG, "onItemLongClick" + adapterPosition);

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Log.d(TAG, "onItemClick" + adapterPosition);
        if (adapterPosition == PAGES.ALIPAY.ordinal()) {
            mAliPayQR = !mAliPayQR;
            mAdapter.notifyItemChanged(adapterPosition);
        }
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {
        Log.d(TAG, "onItemDoubleClick" + adapterPosition);

    }
}

