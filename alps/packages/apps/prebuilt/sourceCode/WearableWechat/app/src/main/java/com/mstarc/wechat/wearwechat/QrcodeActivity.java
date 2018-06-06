package com.mstarc.wechat.wearwechat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mstarc.wechat.wearwechat.common.VerticalViewPager;
import com.mstarc.wechat.wearwechat.fragment.VoiceRecognizeFragment;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.utils.NetUtil;
import com.mstarc.wechat.wearwechat.utils.WxLogin;

import java.util.ArrayList;
import java.util.Properties;

public class QrcodeActivity extends Activity {
    
    // View list in viewPager
    ArrayList<View> mItemViewList;
    private static final int[] mVirticalPageLayouts = {R.layout.qrcode, R.layout.qrcode_prompt};
    //private VerticalViewPager mVerticalViewPager;
    private static final String TAG = QrcodeActivity.class.getSimpleName();
    public static final String AVATAR_KEY = "window.userAvatar";
    public static final String REDIRECT_KEY = "window.redirect_uri";
    public static final String LOGIN_CODE_KEY = "window.code";
    private String mUuid;
    ImageView mQrView;
    TextView mTextViewTip;
    ImageView mBtnTip;
    LinearLayout mNoNetWorkLayout;
    private ProgressBar mProgressBar;
    private Check_Login_Task mCheck_Login_Task;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isNetworkAvailable(context)) {
                mNoNetWorkLayout.setVisibility(View.INVISIBLE);
                if (mTask.getStatus() == AsyncTask.Status.PENDING) {
                    mTask.execute();
                }
            } else {
                mNoNetWorkLayout.setVisibility(View.VISIBLE);
            }
        }
    };
    IntentFilter mInterFilter;
    QR_Task mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrcode);
        //initViewPager();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln wechat 绑定打开常亮-----------");
        mQrView = (ImageView) findViewById(R.id.qr_code_img);
        mTextViewTip = (TextView) findViewById(R.id.qr_code_tip);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mBtnTip = (ImageView) findViewById(R.id.qr_code_tips);
        mNoNetWorkLayout = (LinearLayout) findViewById(R.id.no_network_layout);

        mBtnTip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(v.getContext(), R.style.Dialog);
                dialog.show();
                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                View viewDialog = inflater.inflate(R.layout.qrcode_prompt, null);
                LinearLayout tipLayout = (LinearLayout) viewDialog.findViewById(R.id.layout_tip);
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
            mNoNetWorkLayout.setVisibility(View.VISIBLE);
        }
        mInterFilter = new IntentFilter();
        mInterFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        //使用轮询或者什么其他的 等待扫描结果;
        mTask = new QR_Task();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, mInterFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
    }
    @Override
    public void onPause(){
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln wechat绑定关闭常亮-----------");
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    // Init view pager
/*    private void initViewPager() {
        mItemViewList = new ArrayList<>();
        for (int i = 0; i < mVirticalPageLayouts.length; i++) {
            View view = LayoutInflater.from(this).inflate(mVirticalPageLayouts[i], null);

            mItemViewList.add(view);
        }

        mVerticalViewPager.setAdapter(new VirticalViewPagerAdapter());
        mVerticalViewPager.setCurrentItem(0);
    }*/

    class VirticalViewPagerAdapter extends PagerAdapter {
        @Override
        public void destroyItem(View v, int position, Object arg2) {
            ((VerticalViewPager) v).removeView(mItemViewList.get(position));
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
            ((VerticalViewPager) v).addView(mItemViewList.get(position));
            if (position == 0) {
                mQrView = (ImageView) v.findViewById(R.id.qr_code_img);
                mTextViewTip = (TextView) v.findViewById(R.id.qr_code_tip);
                mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar);
                if (!NetUtil.hasNet(v.getContext())) {
                    //没有网络的提示
                    mQrView.setImageResource(R.mipmap.bg_net);
                    mTextViewTip.setText("没有网络！");
                }
            }
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

    @Override
    public void onBackPressed() {
        if (mTask != null) {
            mTask.cancel(true);
        }
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);
    }



    private class QR_Task extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... params) {


            mUuid = WxLogin.getUUid().replaceAll("\"", "");
            //.replaceAll("\"", "")

            Log.d("TAG", "UUID_KEY======" + mUuid);
            Bitmap urlImage = WxLogin.getURLImage(WxLogin.formatQRUrl(mUuid));
            Log.d("TAG", "urlImage======" + urlImage);

            return urlImage;
        }


        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            // Drawable drawable = new BitmapDrawable(urlImage);
            Log.d("TAG", "drawable======" + bitmap);
            if (bitmap != null) {
                mQrView.setImageBitmap(bitmap);
                //轮询检查状态
                mProgressBar.setVisibility(View.GONE);
                mCheck_Login_Task = new Check_Login_Task();
                mCheck_Login_Task.execute(mUuid);
                mTextViewTip.setText("请用微信扫描登陆");
            }
        }
    }

    protected void showDialog(String str) {
        AlertDialog.Builder builder = new AlertDialog.Builder(QrcodeActivity.this);

        builder.setTitle("提示");
        builder.setMessage(str + ",重新生成二维码?");

        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                new QR_Task().execute();
            }
        });

        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        // builder.create().show();
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private class Check_Login_Task extends AsyncTask<String, Void, Properties> {

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d("TAG", "Check_Login_Task已关闭");

        }

        @Override
        protected Properties doInBackground(String... params) {

            Properties properties = WxLogin.checkLoginStatus(params[0]);
            return properties;
        }

        @Override
        protected void onPostExecute(Properties properties) {
            super.onPostExecute(properties);

            if (properties == null) {
                return;
            }
            //轮询;
            String rescode = properties.getProperty(LOGIN_CODE_KEY);
            Log.d("TAG", "原始登录结果" + rescode);
            if ("408".equals(rescode)) {

                Log.d("TAG", "登录超时" + rescode);
                if (!NetUtil.isApplicationBroughtToBackground(QrcodeActivity.this)) {
                    if (!QrcodeActivity.this.isFinishing()) {
                        showDialog("登录超时");
                    } else {
                        Log.d("TAG", "准备弹出时 点了关闭");
                    }

                } else {
                    finish();
                }

            } else if ("201".equals(rescode)) {
                Log.d("TAG", "扫描成功还未登陆" + rescode);
                //展示头像
                //继续请求状态;

                Bitmap localBitmap = WxLogin.getBase64Image(properties.getProperty(AVATAR_KEY));
                Log.d("TAG", "扫描成功还未登陆头像" + localBitmap);
                if (localBitmap != null) {
                    mQrView.setImageBitmap(localBitmap);
                    mTextViewTip.setText("请确认登录!");
                    SystemClock.sleep(2000);
                    new Check_Login_Task().execute(mUuid);
                }
            } else if ("200".equals(rescode)) {
                Log.d("TAG", "确认登录" + rescode);
                // mTextViewTip.setText("确认登录" + rescode);
                String re_url = properties.getProperty(REDIRECT_KEY);
                Log.d("TAG", "re_url====" + re_url);
                if (re_url != null && re_url != "") {
                    new Token_Task().execute(re_url);
                }
            }
        }
    }


    private class Token_Task extends AsyncTask<String, Void, Token> {

        @Override
        protected Token doInBackground(String... params) {
            Token token = WxLogin.getToken(params[0]);
            return token;
        }

        @Override
        protected void onPostExecute(Token token) {
            super.onPostExecute(token);
            if (token != null) {
                Intent intent = new Intent(QrcodeActivity.this, CommunicationActivity.class);
                intent.putExtras(token.toBundle());
                startActivity(intent);
                finish();
            }
        }
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
}
