package com.mstarc.wearablesettings.activitys;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.common.DecorationSettingItem;
import com.mstarc.wearablesettings.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablesettings.utils.SharedPreferencesHelper;
import com.mstarc.wearablesettings.utils.ThemeUtils;
import com.mstarc.wearablesettings.utils.WifiBean;

import java.util.ArrayList;
import java.util.List;

import static com.mstarc.wearablesettings.utils.Constants.PASSWORD;
import static com.mstarc.wearablesettings.utils.Constants.WIFI_PASSWORD_REQUESTCODE;

public class WifiConnectActivity extends Activity implements RecyclerViewItemTouchListener.OnItemClickEventListener {

    private static final String WIFI_AUTH_OPEN = "";
    private static final String WIFI_AUTH_ROAM = "[ESS]";
    private static final String TAG = WifiConnectActivity.class.getSimpleName();
    private static final int SCAN_WIFI_MESSAGE = 1;
    private static final int SCAN_WIFI_TIMEOUT = 200;
    private static final int CONNECT_WIFI_MESSAGE = 2;
    private static final int CHECK_WIFI_MESSAGE = 3;
    private static final int CONNECT_WIFI_TIMEOUT = 1;
    private ImageView mLoading;
    private WifiManager mWifiManager;
    private List<android.net.wifi.ScanResult> mOriWifiList = new ArrayList<android.net.wifi.ScanResult>();
    private List<WifiBean> mWifiList = new ArrayList<WifiBean>();
    private WifiAdapter mConnectAdapter;
    private Animation mAnimation;
    private WifiInfo mConnectWifiInfo;
    private int mCurrentIndex;
    private SharedPreferencesHelper mSPH;
    private boolean b;
    private int wcgID = 2000;
    private boolean isClickable = true;
    private int mpo;
    private int count = 0;
    private boolean themeColor = true;
    private String mPassword;
    private Handler mHandler_check = new Handler();
    private Runnable runnable_check;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SCAN_WIFI_MESSAGE:
                    scanWifi();
                    break;
                case CHECK_WIFI_MESSAGE:
                    Toast.makeText(WifiConnectActivity.this, getString(R.string.pwd_error_timeout), Toast.LENGTH_SHORT).show();
                    themeColor = false;
                    if (mpo != 100 && mpo < mWifiList.size() && mConnectAdapter != null) {
                        mWifiList.get(mpo).showPro = false;
                        mConnectAdapter.notifyDataSetChanged();
                    }
                    mHandler_check.removeCallbacks(runnable_check);
                    isClickable = true;
                    break;
                case CONNECT_WIFI_MESSAGE:

                    count = 0;
                    break;

            }
            super.handleMessage(msg);
        }
    };
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        mSPH = SharedPreferencesHelper.getInstance(this);
        mLoading = (ImageView) findViewById(R.id.loading);
        //动画
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.img_animation);
        LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
        mAnimation.setInterpolator(lin);
        mLoading.setVisibility(View.GONE);

        final ImageView status = (ImageView) findViewById(R.id.iv_switch);
        (findViewById(R.id.titlelayout)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                operatDeivce(status);
            }
        });

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager.isWifiEnabled()) {
            status.setImageBitmap(null);
            updateImageView(status, R.mipmap.kai);
            mLoading.setVisibility(View.VISIBLE);
            mLoading.clearAnimation();
            mLoading.startAnimation(mAnimation);
            mHandler.sendEmptyMessageDelayed(SCAN_WIFI_MESSAGE, SCAN_WIFI_TIMEOUT);
        } else {
            status.setBackground(null);
            status.setImageResource(R.mipmap.guanbi);
        }
        mConnectWifiInfo = mWifiManager.getConnectionInfo();
        initListView(); //wyg
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(scanReceiver, intentFilter);



    }


    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "gln wifi打开常亮-----------");
    }

    private void initListView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.scanresult);
        mConnectAdapter = new WifiAdapter(WifiConnectActivity.this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DecorationSettingItem(this, LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemTouchListener(this, this));
        mRecyclerView.setAdapter(mConnectAdapter);
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


    private void scanWifi() {
        mWifiManager.startScan();
    }

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            // Log.i(TAG, "intent:  " + intent);
            if (intent.getAction().equals(
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mOriWifiList = mWifiManager.getScanResults();
                mConnectWifiInfo = mWifiManager.getConnectionInfo();
                if (mOriWifiList != null) {
                    mWifiList.clear();
                    for (int i = 0; i < mOriWifiList.size(); i++) {
                        ScanResult result = mOriWifiList.get(i);
                        WifiBean bean = new WifiBean();
                        bean.capabilities = result.capabilities;
                        bean.level = result.level;
                        bean.SSID = result.SSID;
                        if (mConnectWifiInfo != null && mConnectWifiInfo.getSSID().equals("\"" + result.SSID + "\"")) {
                            bean.showPro = true;
                        } else {
                            bean.showPro = false;
                        }
                        mWifiList.add(bean);
                    }
                }
                mLoading.clearAnimation();
                mLoading.setVisibility(View.GONE);
                mConnectAdapter.notifyDataSetChanged();
            } else if (intent.getAction().equals(
                    WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    if (mCurrentIndex != 100 && mCurrentIndex < mWifiList.size()) {
                        mWifiList.get(mCurrentIndex).showPro = false;
                    }
                    mConnectWifiInfo = mWifiManager.getConnectionInfo();
                    mConnectAdapter.notifyDataSetChanged();
                }
            } else if (intent.getAction().equals(
                    WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {

                if (intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR)) {

                    int authState = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    Log.i(TAG, "authState:  " + authState);
                    if (authState == WifiManager.ERROR_AUTHENTICATING) {
                        if (count == 0) {
                            Toast.makeText(WifiConnectActivity.this, getString(R.string.pwd_error), Toast.LENGTH_SHORT).show();//
                            themeColor = false;
                            if (mpo != 100 && mpo < mWifiList.size() && mConnectAdapter != null) {
                                mWifiList.get(mpo).showPro = false;
                                mConnectAdapter.notifyDataSetChanged();
                            }
                            mHandler_check.removeCallbacks(runnable_check);
                            isClickable = true;
                            count++;
                        }
                    } else {
                        Log.i(TAG, " fail to connect ");
                    }
                    Log.d(TAG, "getAction themeColor=" + themeColor);
                } else {
                    Log.d(TAG, "supplicant state: " + getSupplicantStateText((SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
                }
            }
        }
    };

    private String getSupplicantStateText(SupplicantState supplicantState) {
        if (SupplicantState.FOUR_WAY_HANDSHAKE.equals(supplicantState)) {
            return "四路握手中...";
        } else if (SupplicantState.ASSOCIATED.equals(supplicantState)) {
            return "关联AP完成";
        } else if (SupplicantState.ASSOCIATING.equals(supplicantState)) {
            return "正在关联AP...";
        } else if (SupplicantState.COMPLETED.equals(supplicantState)) {
           /* if (mCurrentIndex != 100 && mCurrentIndex < mWifiList.size() && mConnectAdapter != null) {
                mWifiList.get(mCurrentIndex).showPro = false;
                mConnectAdapter.notifyDataSetChanged();
            }*/
            for (int i = 0; i < mWifiList.size(); i++) {
                WifiBean result= mWifiList.get(i);
                result.showPro=false;
            }
            mConnectAdapter.notifyDataSetChanged();
            themeColor = true;
            mHandler_check.removeCallbacks(runnable_check);
            isClickable = true;
            return "已连接";
        } else if (SupplicantState.DISCONNECTED.equals(supplicantState)) {
            return "已断开";
        } else if (SupplicantState.DORMANT.equals(supplicantState)) {
            return "暂停活动";
        } else if (SupplicantState.GROUP_HANDSHAKE.equals(supplicantState)) {
            return "GROUP HANDSHAKE";
        } else if (SupplicantState.INACTIVE.equals(supplicantState)) {
            return "休眠中...";
        } else if (SupplicantState.INVALID.equals(supplicantState)) {
            return "无效";
        } else if (SupplicantState.SCANNING.equals(supplicantState)) {
            return "扫描中...";
        } else if (SupplicantState.UNINITIALIZED.equals(supplicantState)) {
            return "未初始化";
        } else if (SupplicantState.AUTHENTICATING.equals(supplicantState)) {
            return "正在验证";
        } else if (SupplicantState.INTERFACE_DISABLED.equals(supplicantState)) {
            return "接口禁用";
        } else {
            return "supplicant state is bad";
        }
    }


    private void operatDeivce(ImageView status) {
        if (mWifiManager.isWifiEnabled()) {
            mLoading.setVisibility(View.GONE);
            mLoading.clearAnimation();
            status.setBackground(null);
            status.setImageResource(R.mipmap.guanbi);
            mWifiManager.setWifiEnabled(false);
            mOriWifiList.clear();
            mWifiList.clear();
            mConnectAdapter.notifyDataSetChanged();
        } else {
            mWifiManager.setWifiEnabled(true);
            mLoading.setVisibility(View.VISIBLE);
            mLoading.clearAnimation();
            mLoading.startAnimation(mAnimation);
            status.setImageBitmap(null);
            updateImageView(status, R.mipmap.kai);
            mHandler.removeMessages(SCAN_WIFI_MESSAGE);
            mHandler.sendEmptyMessageDelayed(SCAN_WIFI_MESSAGE, SCAN_WIFI_TIMEOUT);
        }
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    public static int oldPosition = 100;

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Log.i(TAG, "oldPosition:  " + oldPosition);
        Log.i(TAG, "mCurrentIndex:  " + adapterPosition);
        if (adapterPosition < 0) {
            return;
        }
        if (isClickable) {
            for (int i = 0; i < mWifiList.size(); i++) {
                if (i == adapterPosition) {
                    //mWifiList.get(i).showPro = true;
                } else {
                    mWifiList.get(i).showPro = false;
                }
            }
            mConnectAdapter.notifyDataSetChanged();

            mCurrentIndex = adapterPosition;
            if (mCurrentIndex < mWifiList.size()) {
                WifiBean result = mWifiList.get(mCurrentIndex);

                if (result.capabilities.equals(WIFI_AUTH_OPEN) || result.capabilities.equals(WIFI_AUTH_ROAM)) {
                    addNetworkWPA(result, "", true, adapterPosition);
                } else {
                    String password = mSPH.getString(result.SSID, "");
                    if (!TextUtils.isEmpty(password)) {
                        if (oldPosition != mCurrentIndex)
                            addNetworkWPA(result, password, true, adapterPosition);
                        else
                            addNetworkWPA(result, password, false, adapterPosition);

                    } else {
                        Intent intent = new Intent(WifiConnectActivity.this, KeyBoardActivity.class);
                        mSPH.putInt("myposition", adapterPosition);
                        Bundle bundle = new Bundle();
                        Log.i(TAG, "adapterPosition:  " + adapterPosition);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, WIFI_PASSWORD_REQUESTCODE);
                    }
                }
            }
            oldPosition = mCurrentIndex;
        }
    }


    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    public class WifiAdapter extends RecyclerView.Adapter<ViewHolder> {
        // Set numbers of Card in RecyclerView.
        // Only one line for current job
        private LayoutInflater mInflater;

        public WifiAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.setting_connect_item_layout, null));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            WifiBean result = mWifiList.get(position);
            if (result.showPro) {
                holder.mProgressBar.setVisibility(View.VISIBLE);
                holder.mStatus.setVisibility(View.GONE);
            } else {
                holder.mProgressBar.setVisibility(View.GONE);
                holder.mStatus.setVisibility(View.VISIBLE);
            }
            mConnectWifiInfo = mWifiManager.getConnectionInfo();
            Log.i(TAG, "onBindViewHolder themeColor:  " + themeColor);
            if (mConnectWifiInfo != null && mConnectWifiInfo.getSSID().equals("\"" + result.SSID + "\"") && themeColor) {
                Log.i(TAG, "onBindViewHolder themeColor comein should be true :  " + themeColor);
                holder.mProgressBar.setVisibility(View.GONE);
                holder.mStatus.setVisibility(View.VISIBLE);
                updateImageView(holder.mStatus, R.mipmap.wifi4);
                int level = WifiManager.calculateSignalLevel(mConnectWifiInfo.getRssi(), 5);
                if (level >= 4) {
                    updateImageView(holder.mStatus, R.mipmap.wifi4);
                } else if (level >= 3) {
                    updateImageView(holder.mStatus, R.mipmap.wifi3);
                } else if (level >= 2) {
                    updateImageView(holder.mStatus, R.mipmap.wifi2);
                } else {
                    updateImageView(holder.mStatus, R.mipmap.wifi1);
                }
                holder.mTitle.setTextColor(ThemeUtils.getCurrentPrimaryColor());

            } else {
                int level = WifiManager.calculateSignalLevel(
                        result.level, 5);
                if (result.capabilities != null && (result.capabilities.equals(WIFI_AUTH_OPEN) || result.capabilities.equals(WIFI_AUTH_ROAM))) {
                    if (level >= 4) {
                        holder.mStatus.setBackgroundResource(R.mipmap.wifinopa4);
                    } else if (level >= 3) {
                        holder.mStatus.setBackgroundResource(R.mipmap.wifinopa3);
                    } else if (level >= 2) {
                        holder.mStatus.setBackgroundResource(R.mipmap.wifinopa2);
                    } else {
                        holder.mStatus.setBackgroundResource(R.mipmap.wifinopa1);
                    }
                } else {
                    if (level >= 4) {
                        holder.mStatus.setBackgroundResource(R.mipmap.wifinocon4);
                    } else if (level >= 3) {
                        holder.mStatus.setBackgroundResource(R.mipmap.wifinocon3);
                    } else if (level >= 2) {
                        holder.mStatus.setBackgroundResource(R.mipmap.wifinocon2);
                    } else {
                        holder.mStatus.setBackgroundResource(R.mipmap.wifinocon1);
                    }
                }

                holder.mTitle.setTextColor(getResources().getColor(R.color.color_little_white));
                holder.mTitle.setSelected(true);
            }
            holder.mTitle.setText(!TextUtils.isEmpty(result.SSID) ? result.SSID : "UNKOWN");
        }

        @Override
        public int getItemCount() {
            return mWifiList.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;
        ImageView mStatus;
        ProgressBar mProgressBar;

        public ViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.title);
            mStatus = (ImageView) itemView.findViewById(R.id.status);
            mProgressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            String password = data.getStringExtra(PASSWORD);
            Log.i(TAG, password);
            if (requestCode == WIFI_PASSWORD_REQUESTCODE) {
                mConnectWifiInfo = mWifiManager.getConnectionInfo();
                if (mConnectWifiInfo != null) {
                    disconnectWifi(mConnectWifiInfo.getNetworkId());
                }
                if (mCurrentIndex < mWifiList.size()) {
                    int mypos = mSPH.getInt("myposition", 100);
                    Log.i(TAG, "mypos1:  " + mypos);
                    addNetworkWPA(mWifiList.get(mCurrentIndex), password, true, mypos);
                    mSPH.putInt("myposition", 100);

                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 断开指定ID的网络
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }

    public void addNetworkWPA(WifiBean result, String wifiPassword, boolean isshow, int po) {
        isClickable = false;
        WifiConfiguration wifiConfiguration;
        mWifiManager.disconnect();
        if (isshow) {
            if (po != 100 && po < mWifiList.size() && mConnectAdapter != null) {
                mWifiList.get(po).showPro = true;
                mConnectAdapter.notifyDataSetChanged();
            }
        }
        if (result.capabilities.equals(WIFI_AUTH_OPEN) || result.capabilities.equals(WIFI_AUTH_ROAM)) {
            wifiConfiguration = CreateWifiInfo(result.SSID, wifiPassword, 1);
        } else {
            wifiConfiguration = CreateWifiInfo(result.SSID, wifiPassword, 3);
        }
        wcgID = mWifiManager.addNetwork(wifiConfiguration);
        b = mWifiManager.enableNetwork(wcgID, true);
        Log.d(TAG, "wcgID=" + wcgID);
        Log.d(TAG, "b=" + b);
        mpo = po;
        themeColor = false;
        //count = 0;
        mPassword = wifiPassword;
        mHandler.sendEmptyMessageDelayed(CONNECT_WIFI_MESSAGE, 2000);
        if (result.capabilities.equals(WIFI_AUTH_OPEN) || result.capabilities.equals(WIFI_AUTH_ROAM)) {
            themeColor = true;
            mSPH.putString(result.SSID, mPassword);
            Log.d(TAG, "addNetworkWPA themeColor=" + themeColor);
            isClickable = true;
            return;
        }
        if (mPassword.length() < 8) {
            Toast.makeText(WifiConnectActivity.this, getString(R.string.pwd_error), Toast.LENGTH_SHORT).show();
            themeColor = false;
            if (mpo != 100 && mpo < mWifiList.size() && mConnectAdapter != null) {
                mWifiList.get(mpo).showPro = false;
                mConnectAdapter.notifyDataSetChanged();
            }

            isClickable = true;
        } else {
            runnable_check = new Runnable() {
                @Override
                public void run() {
                    mHandler.sendEmptyMessageDelayed(CHECK_WIFI_MESSAGE, 1);
                }
            };
            mHandler_check.postDelayed(runnable_check, 15000);

        }
    }

    /**
     * 创建一个wifi配置信息
     */
    private WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }
        /**连接不需要密码的wifi*/
        if (Type == 1) //WIFICIPHER_NOPASS
        {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        /**连接wep格式加密wifi*/
        if (Type == 2) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        /**连接WPA格式加密wifi（就是我们平时使用的加密方法）*/
        if (Type == 3) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private WifiConfiguration IsExsits(String str) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals(str.trim())) {
                return existingConfig;
            }
        }
        return null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "gln wifi关闭常亮-----------");
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(scanReceiver);
        super.onDestroy();
    }
}
