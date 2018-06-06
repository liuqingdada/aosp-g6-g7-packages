package com.mstarc.sweat.wearablesweat;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.fakewatch.sweetwords.SweetWords;
import com.mstarc.fakewatch.sweetwords.api.bean.SweetApiBean;
import com.mstarc.sweat.wearablesweat.utils.NetUtil;
import com.mstarc.sweat.wearablesweat.utils.ThemeUtils;

public class QrcodeActivity extends Activity implements SweetWords.SweetWordsListener{
    private static final String TAG = "QrcodeActivity";

    ImageView mQrView;
    TextView mTextViewTip;
    ImageView mBtnTip;
    //private ProgressBar mProgressBar;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkAvailable(context)) {
            }
        }
    };
    IntentFilter mInterFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrcode);
        //SweetWords.getInstance().init(getApplicationContext());
        //initViewPager();
        mQrView = (ImageView) findViewById(R.id.qr_code_img);
        mTextViewTip = (TextView) findViewById(R.id.qr_code_tip);
        //mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mBtnTip = (ImageView) findViewById(R.id.qr_code_tips);

        mBtnTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(v.getContext(), R.style.Dialog);
                dialog.show();
                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                View viewDialog = inflater.inflate(R.layout.qrcode_prompt, null);
                LinearLayout tipLayout = (LinearLayout) viewDialog.findViewById(R.id.layout_tip);
                TextView helpView = (TextView) viewDialog.findViewById(R.id.help_tips);
                helpView.setTextColor(ThemeUtils.getCurrentPrimaryColor());
                tipLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                Display display = getWindowManager().getDefaultDisplay();
                int width = display.getWidth();
                int height = display.getHeight();
                // 设置dialog的宽高为屏幕的宽高
                ViewGroup.LayoutParams layoutParams = new  ViewGroup.LayoutParams(width, height);
                dialog.setContentView(viewDialog, layoutParams);
            }
        });

        if (!NetUtil.hasNet(this)) {
            //没有网络的提示
            //mQrView.setImageResource(R.mipmap.bg_net);
           // mTextViewTip.setText("没有网络！");
            setContentView(R.layout.qrcode_nonet);
        }
/*
        // test
        mQrView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(QrcodeActivity.this, SweatProfileActivity.class);
                startActivity(intent);
            }
        });
*/
        mInterFilter = new IntentFilter();
        mInterFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
     // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, mInterFilter);
        SweetWords.getInstance().setSweetWordsListener(this);
        SweetWords.getInstance().getQRBitmap(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause:");
        SweetWords.getInstance().onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop:");
        unregisterReceiver(mBroadcastReceiver);
        SweetWords.getInstance().onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    @Override
    public void onBound() {
        Intent intent = new Intent(QrcodeActivity.this, SweatProfileActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBoundFailure() {
		Toast.makeText(getApplicationContext(), "网络异常", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUnbind() {

    }

    @Override
    public void onUnbindFailure() {

    }

    @Override
    public void onGetQRBitmap(final Bitmap bitmap) {
        mQrView.post(new Runnable() {
            @Override
            public void run() {
                mQrView.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    public void onGetFriendInfo(SweetApiBean sweetApiBean) {

    }

    /**
     * 判断网络是否可用
     */
    public static boolean isNetworkAvailable(Context context) {
        // 获取网络连接管理器
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 获取当前网络状态信息
        NetworkInfo[] info = mgr.getAllNetworkInfo();
        if (info != null) {
            for (int i = 0; i < info.length; i++) {
                if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);
    }
}

