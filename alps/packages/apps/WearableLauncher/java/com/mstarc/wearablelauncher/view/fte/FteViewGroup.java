package com.mstarc.wearablelauncher.view.fte;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.commonbase.zxing.matchconnect.MatchingConnection;
import com.mstarc.fakewatch.bind.BindWizard;
import com.mstarc.wearablelauncher.CommonManager;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.common.CircleIndicator;

import java.util.ArrayList;
import java.util.Timer;
import com.mstarc.wearablelauncher.ThemeUtils;
/**
 * Created by wangxinzhi on 17-6-3.
 */

public class FteViewGroup extends FrameLayout {

    private static final String TAG = FteViewGroup.class.getSimpleName();
    int mStep = 0;
    private static final boolean DEBUG = false;
    UiHandler mHandler = new UiHandler();
    IFteListener mFteListener;
    LocusPassWordView mFtePwView, mFtePwAgainView;
    View mFteQrView, mFteQrPromotionView;
    String mPassword;
    RelativeLayout mConfirmLayout, mHasInstallLayout, mNoInstallLayout;
    TextView mHasInstall, mNoInstall, mQrBottomText;
    ImageView mIntallConfirm, mNotIntallConfirm;

    ViewPager mViewPager;
    private CircleIndicator mIndicator;
    private GuideViewPagerAdapter mAdapter;
    ArrayList<View> mItemViewList;
    private static int[] mPageImgRes = {R.drawable.icon_introduce_1, R.drawable.icon_introduce_2,
            R.drawable.icon_introduce_3, R.drawable.icon_introduce_4};
    private Timer timer = new Timer();
    private int autoCurrIndex = 0; // 当前选中的图片索引

    final static int FTE_QR_PROMOTION_INDEX = 0;
    final static int FTE_QR_INDEX = 1;
    final static int DURATION_QR_LOOP_PROMOTION = 3000;
    final static int DURATION_QR_LOOP = 10000;
    final static int DURATION_STEP_DURATION = 10000;
    BindWizard mBindWizard;
    Bitmap mQR;
    boolean isGetQRCode;
    boolean isSyncingWithPhone = false;

    public FteViewGroup(@NonNull Context context) {
        super(context);
    }

    public FteViewGroup(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FteViewGroup(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @ViewDebug.ExportedProperty(category = "Step")
    public int getStep() {
        return mStep;
    }

    @ViewDebug.ExportedProperty(category = "Step")
    public void setStep(int step) {
        this.mStep = step;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (i == step) {
                getChildAt(i).setVisibility(View.VISIBLE);
            } else {
                getChildAt(i).setVisibility(View.GONE);
            }
        }
        if (mFteListener != null) {
            mFteListener.onFteStepSelected(mStep);
        }
        if (DEBUG) {
            if (mStep == count) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mFteListener != null) {
                            mFteListener.onFteFinished();
                        }
                    }
                }, 5000);
            }
        }
        if (mStep == 5) {
            mHandler.sendEmptyMessageDelayed(UiHandler.MSG_NEXT_STEP, 1000);
        }
        if (mStep == 7) { //sync data
            Message message = new Message();
            message.what = UiHandler.UPTATE_VIEWPAGER;
            message.arg1 = autoCurrIndex;
            autoCurrIndex++;
            mHandler.sendMessage(message);
        }

        if (mStep == 8) {
            mHandler.removeMessages(UiHandler.UPTATE_VIEWPAGER);
        }
        Log.d(TAG, "switch to " + mStep);
    }

    private void playSyncImg() {
        Message message = new Message();
        message.what = UiHandler.UPTATE_VIEWPAGER;
        if (autoCurrIndex == mPageImgRes.length - 1) {
            autoCurrIndex = -1;
        }
        message.arg1 = autoCurrIndex + 1;
        autoCurrIndex++;
        mHandler.sendMessageDelayed(message, 3000);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFtePwView = (LocusPassWordView) findViewById(R.id.fte_pw);
        mFtePwView.setOnCompleteListener(new LocusPassWordView.OnCompleteListener() {
            @Override
            public void onComplete(String password) {
                Log.d(TAG, "1 Got pw: " + password);
                mPassword = password;
                setStep(mStep + 1);
            }

            @Override
            public void shortPassword() {
                Toast.makeText(getContext(), R.string.fte_pw_short_promotion, Toast.LENGTH_SHORT).show();
            }
        });
        mFtePwAgainView = (LocusPassWordView) findViewById(R.id.fte_pw_again);
        mFtePwAgainView.setOnCompleteListener(new LocusPassWordView.OnCompleteListener() {
            @Override
            public void onComplete(String password) {
                Log.d(TAG, "2 Got pw: " + password);
                if (!mPassword.equals(password)) {
                    Toast.makeText(getContext(), R.string.fte_pw_error_promotion, Toast.LENGTH_SHORT).show();
                    mPassword = null;
                    mFtePwView.clearPassword(200);
                    mFtePwAgainView.clearPassword(200);
                    setStep(mStep - 1);
                } else {
                    CommonManager.getInstance(getContext().getApplicationContext()).setServicePassword(mPassword);
                    setStep(mStep + 1);
                }
            }

            @Override
            public void shortPassword() {
                Toast.makeText(getContext(), R.string.fte_pw_short_promotion, Toast.LENGTH_SHORT).show();
            }
        });
        mFteQrPromotionView = findViewById(R.id.fte_qr_promotion);
//        mFteQrPromotionView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                setStep(FTE_QR_INDEX);
//                mHandler.removeMessages(UiHandler.MSG_LOOP_QR);
//            }
//        });

        mConfirmLayout = (RelativeLayout) findViewById(R.id.confirm_layout);
        mHasInstallLayout = (RelativeLayout) findViewById(R.id.app_has_install_layout);
        mNoInstallLayout= (RelativeLayout) findViewById(R.id.app_not_install_layout);
        mHasInstall = (TextView) findViewById(R.id.text_install);
        mNoInstall = (TextView) findViewById(R.id.text_not_install);
        mIntallConfirm = (ImageView) findViewById(R.id.install_confirm);
        mNotIntallConfirm = (ImageView) findViewById(R.id.no_install_confirm);
        mQrBottomText = (TextView) findViewById(R.id.qr_bottom_text);
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mIndicator = (CircleIndicator) findViewById(R.id.view_page_indicator);
        int color = ThemeUtils.getCurrentPrimaryColor();//gln
        Log.d(TAG, "gln color= "+color);
        Log.d(TAG, "gln add if ");
        if(color==-5243136&&ThemeUtils.isProductG7())
        {
            mHasInstall.setTextColor(0xffafff00);
            mNoInstall.setTextColor(0xffafff00);
            mIntallConfirm.setImageResource(R.drawable.power_confirm_selector_g);
            mNotIntallConfirm.setImageResource(R.drawable.power_confirm_selector_g);
        }
        initViewPager();

        mHasInstall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfirmLayout.setVisibility(GONE);
                mHasInstallLayout.setVisibility(VISIBLE);
                mNoInstallLayout.setVisibility(GONE);
            }
        });
        mNoInstall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mConfirmLayout.setVisibility(GONE);
                mHasInstallLayout.setVisibility(GONE);
                mNoInstallLayout.setVisibility(VISIBLE);
            }
        });
        mIntallConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setStep(FTE_QR_INDEX);
                mQrBottomText.setText(R.string.fte_qr_text);
                mHandler.removeMessages(UiHandler.MSG_LOOP_QR);
            }
        });
        mNotIntallConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mQrBottomText.setText(R.string.fte_qr_install_text);
                setStep(FTE_QR_INDEX);
                mHandler.removeMessages(UiHandler.MSG_LOOP_QR);
            }
        });


        mFteQrView = findViewById(R.id.fte_qr);
        mFteQrView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeMessages(UiHandler.MSG_LOOP_QR);
                if (DEBUG) {
                    mHandler.sendEmptyMessageDelayed(UiHandler.MSG_NEXT_STEP, DURATION_STEP_DURATION);
                }
            }
        });

        setStep(0);
        mBindWizard = new BindWizard(getContext().getApplicationContext());
        try {
            mBindWizard.setOnGetQRCodeBitmapListener(new MatchingConnection.OnGetQRCodeBitmapListener() {
                @Override
                public void onOnGetQRCodeBitmap() {
                    isGetQRCode = true;

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap qrCodeBitmap = mBindWizard.getQRCodeBitmap(null);
                            setQR(qrCodeBitmap);
                        }
                    });
                }

            });

            if (isGetQRCode) {
                Bitmap qrCodeBitmap = mBindWizard.getQRCodeBitmap(null);
                setQR(qrCodeBitmap);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
//        BitmapDrawable bitmapDrawable = (BitmapDrawable) getContext().getResources().getDrawable(R.drawable.alipan_qrcode_demo);
//        setQR(bitmapDrawable.getBitmap());
//        mHandler.sendEmptyMessageDelayed(UiHandler.MSG_LOOP_QR, DURATION_QR_LOOP_PROMOTION);
    }

    class UiHandler extends Handler {
        public final static int MSG_SWITCH_STEP = 1;
        public final static int MSG_NEXT_STEP = 2;
        public final static int MSG_LOOP_QR = 3;
        public final static int MSG_UPDATE_QR_BITMAP = 4;

        private final static int UPTATE_VIEWPAGER = 5;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "handleMessage " + msg.what);
            switch (msg.what) {
                case MSG_SWITCH_STEP:
                    setStep(msg.arg1);
                    break;
                case MSG_NEXT_STEP:
                    mStep++;
                    if (mStep < getChildCount()) {
                        setStep(mStep);
                        if (DEBUG) {
                            sendEmptyMessageDelayed(MSG_NEXT_STEP, DURATION_STEP_DURATION);
                        }
                    } else {
                        mStep = getChildCount();
                        if (mFteListener != null) {
                            mFteListener.onFteFinished();
                        }
                    }
                    break;
                case MSG_LOOP_QR:
                    if (!isSyncingWithPhone) {
                        if (mStep == FTE_QR_PROMOTION_INDEX) {
                            setStep(FTE_QR_INDEX);
                            sendEmptyMessageDelayed(MSG_LOOP_QR, DURATION_QR_LOOP);
                        } else {
                            setStep(FTE_QR_PROMOTION_INDEX);
                            sendEmptyMessageDelayed(MSG_LOOP_QR, DURATION_QR_LOOP_PROMOTION);
                        }
                    }
                    break;
                case MSG_UPDATE_QR_BITMAP:
                    ((ImageView) (((ViewGroup) mFteQrView).findViewById(R.id.fte_qr_image))).setImageBitmap(mQR);
                    break;
                case UPTATE_VIEWPAGER:
                    if(DEBUG) {
                        Log.d(TAG, "UPTATE_VIEWPAGER index =  " + msg.arg1);
                    }
                    if (msg.arg1 != 0) {
                        mViewPager.setCurrentItem(msg.arg1);
                    } else {
                        //false 当从末页调到首页是，不显示翻页动画效果，
                        mViewPager.setCurrentItem(msg.arg1, false);
                    }
                    playSyncImg();
                    break;
            }
        }
    }

    public void setListener(IFteListener listener) {
        mFteListener = listener;
    }

    public interface IFteListener {
        public void onFteFinished();

        public void onFteStepSelected(int step);
    }

    public void setQR(Bitmap bitmap) {
        mQR = bitmap;
        mHandler.sendEmptyMessage(UiHandler.MSG_UPDATE_QR_BITMAP);
    }

    public void biginSyncPhone() {
        if (!isSyncingWithPhone) {
            isSyncingWithPhone = true;
            mHandler.removeMessages(UiHandler.MSG_LOOP_QR);
        }
    }

    class GuideViewPagerAdapter extends PagerAdapter {
        @Override
        public void destroyItem(View v, int position, Object arg2) {
            ((ViewPager) v).removeView(mItemViewList.get(position));
        }

        @Override
        public void finishUpdate(View arg0) {
        }

        @Override
        public int getCount() {
            return mItemViewList.size();
        }

        @Override
        public Object instantiateItem(View v, int position) {
            ((ViewPager) v).addView(mItemViewList.get(position));
            return mItemViewList.get(position);
        }

        @Override
        public boolean isViewFromObject(View v, Object arg1) {
            return v == arg1;
        }


        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }

        @Override
        public void restoreState(Parcelable arg0, ClassLoader arg1) {
        }

        @Override
        public Parcelable saveState() {
            return null;
        }
    }


    // Init view pager
    private void initViewPager() {
        mItemViewList = new ArrayList<>();
        for (int i = 0; i < mPageImgRes.length; i++) {
            View view = LayoutInflater.from(this.getContext()).inflate(R.layout.data_sync_image_item, null);
            ImageView img = (ImageView) view.findViewById(R.id.page_img);
            img.setImageResource(mPageImgRes[i]);
            mItemViewList.add(view);
        }

        mAdapter = new GuideViewPagerAdapter();
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mIndicator.setViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < mPageImgRes.length; i++) {

                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
