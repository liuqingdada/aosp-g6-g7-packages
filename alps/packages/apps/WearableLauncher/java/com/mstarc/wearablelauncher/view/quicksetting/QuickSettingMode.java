package com.mstarc.wearablelauncher.view.quicksetting;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.provider.Telephony;

import mstarc_os_api.Profiles;
import mstarc_os_api.MobileData;

import com.android.internal.telephony.TelephonyIntents;
import com.mstarc.mobiledataservice.IMobileDataAidl;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.ThemeUtils;

import java.util.List;
import com.mstarc.fakewatch.settings.Settings;
import com.mstarc.wearablelauncher.view.SharedPreferencesHelper;

import static android.content.Context.TELEPHONY_SERVICE;
import static android.telephony.TelephonyManager.SIM_STATE_ABSENT;
import static com.mstarc.wearablelauncher.view.SharedPreferencesHelper.MODEDATA;
import com.mstarc.wearablelauncher.MainActivity;
/**
 * Created by wangxinzhi on 17-3-4.
 */

public class QuickSettingMode extends RelativeLayout implements View.OnClickListener {
    private static final String TAG = QuickSettingMode.class.getSimpleName();
    private static final String SETTING_PACKAGE_NAME = "com.mstarc.wearablesettings";
    private static final String SETTING_WIFI_ACTIVITY = "com.mstarc.wearablesettings.activitys.WifiConnectActivity";
    private static final String SETTING_BT_ACTIVITY = "com.mstarc.wearablesettings.activitys.BTConnectActivity";
    private static final String SETTING_SINGAL_ACTIVITY = "com.mstarc.wearablesettings.activitys.NetWorkActivity";

    ImageView mWifiView;
    ImageView mSignalView;
    ImageView mBtView;
    ImageView mMuteView;
    ImageView mMusicView;
    ImageView mZhendongView;
    ImageButton mVolumeAddView, mVolumeDecView;
    ProgressBar mVolumeProgressBar;
    WifiBroadcastReceiver mWifiBroadcastReceiver;
    boolean isWifiReceiverRegisted = false;
    boolean isSimSignalListenerRegisted = false;
    BluetoothAdapter mBlueadapter;
    WifiManager mWifiManager;
    TelephonyManager mTelephonyManager;
    private SubscriptionInfo mSir = null;
    private PhoneStateListener mPhoneStateListener;
    public static final int WIFI_ICON_LEVELS = 5;
    public static final int SIM_SIGNAL_ICON_LEVELS = 5;
    private SubscriptionManager mSubscriptionManager;
    boolean hasSimeCard = false;
    AudioManager mAudioManager;
    static final int MAX_VOLUME = 5;
    int mVoume = 2;

    private SharedPreferencesHelper mSPH;
    private IMobileDataAidl iMobileDataAidl;

    class SimOnSubscriptionsChangedListener extends OnSubscriptionsChangedListener {
        public void onSubscriptionsChanged() {
            updateSimState();
        }
    }

    public static String simStatetoString(int state) {
        String string;
        switch (state) {
            case TelephonyManager.SIM_STATE_ABSENT:
                string = "SIM_STATE_ABSENT";
                break;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                string = "SIM_STATE_PIN_REQUIRED";
                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                string = "SIM_STATE_PUK_REQUIRED";
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                string = "SIM_STATE_NETWORK_LOCKED";
                break;
            case TelephonyManager.SIM_STATE_READY:
                string = "SIM_STATE_READY";
                break;
            case TelephonyManager.SIM_STATE_NOT_READY:
                string = "SIM_STATE_NOT_READY";
                break;
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                string = "SIM_STATE_PERM_DISABLED";
                break;
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                string = "SIM_STATE_CARD_IO_ERROR";
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
            default:
                string = "SIM_STATE_UNKNOWN";
                break;
        }
        return string;
    }

    private void updateSimState() {
        int simState = mTelephonyManager.getSimState();
        Log.d(TAG, "SimState: " + simStatetoString(simState));
        if (TelephonyManager.SIM_STATE_READY == simState) {
            hasSimeCard = true;
            updateSignalStrength(0);
        } else {
            hasSimeCard = false;
            updateSignalStrength(0);
        }
    }

    SimOnSubscriptionsChangedListener simOnSubscriptionsChangedListener;
    SimStateReceive mSimStateReceive;

    public QuickSettingMode(Context context) {
        super(context);
        init(context);
    }

    public QuickSettingMode(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public QuickSettingMode(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        iMobileDataAidl = MobileServiceClient.getInstance().getmIMobileDataAidl();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSPH = SharedPreferencesHelper.getInstance(getContext());
        mWifiView = (ImageView) findViewById(R.id.qs2_wifi);
        mSignalView = (ImageView) findViewById(R.id.qs2_signal);
        mBtView = (ImageView) findViewById(R.id.qs2_bt);
        mMuteView = (ImageView) findViewById(R.id.qs2_mute);
        mMusicView = (ImageView) findViewById(R.id.qs2_music);
        mZhendongView = (ImageView) findViewById(R.id.qs2_zhendong);
        mVolumeAddView = (ImageButton) findViewById(R.id.qs2_volume_add);
        mVolumeDecView = (ImageButton) findViewById(R.id.qs2_volume_dec);
        mVolumeProgressBar = (ProgressBar) findViewById(R.id.qs2_progressBar);
        mVolumeProgressBar.setMax(MAX_VOLUME);
        mMusicView.setActivated(true);
        mMuteView.setOnClickListener(this);
        mMusicView.setOnClickListener(this);
        mZhendongView.setOnClickListener(this);
        Drawable add = getResources().getDrawable(R.drawable.volume_add);
        Drawable dec = getResources().getDrawable(R.drawable.volume_dec);
        add.setTint(ThemeUtils.getCurrentPrimaryColor());
        mVolumeAddView.setBackground(add);
        dec.setTint(ThemeUtils.getCurrentPrimaryColor());
        mVolumeDecView.setBackground(dec);

        switch (ThemeUtils.getCurrentProduct()) {
            case ThemeUtils.PRODUCT_COLOR_ROSE_GOLDEN:
                mWifiView.setImageResource(R.drawable.ic_qs2_wifi_g7_rose);
                mSignalView.setImageResource(R.drawable.ic_qs2_data_g7_rose);
                mBtView.setImageResource(R.drawable.ic_qs2_bt_g7_rose);
                mMuteView.setImageResource(R.drawable.ic_qs2_mute_g7_rose);
                mMusicView.setImageResource(R.drawable.ic_qs2_music_g7_rose);
                mZhendongView.setImageResource(R.drawable.ic_qs2_zhendong_g7_rose);
                break;
            case ThemeUtils.PRODUCT_COLOR_HIGH_BLACK:
                mWifiView.setImageResource(R.drawable.ic_qs2_wifi_g7_golden);
                mSignalView.setImageResource(R.drawable.ic_qs2_data_g7_golden);
                mBtView.setImageResource(R.drawable.ic_qs2_bt_g7_golden);
                mMuteView.setImageResource(R.drawable.ic_qs2_mute_g7_golden);
                mMusicView.setImageResource(R.drawable.ic_qs2_music_g7_golden);
                mZhendongView.setImageResource(R.drawable.ic_qs2_zhendong_g7_golden);
                break;
            case ThemeUtils.PRODUCT_COLOR_APPLE_GREEN:
                mWifiView.setImageResource(R.drawable.ic_qs2_wifi_g7_green);
                mSignalView.setImageResource(R.drawable.ic_qs2_data_g7_green);
                mBtView.setImageResource(R.drawable.ic_qs2_bt_g7_green);
                mMuteView.setImageResource(R.drawable.ic_qs2_mute_g7_green);
                mMusicView.setImageResource(R.drawable.ic_qs2_music_g7_green);
                mZhendongView.setImageResource(R.drawable.ic_qs2_zhendong_g7_green);
                break;
            default:
                mWifiView.setImageResource(R.drawable.ic_qs2_wifi_g6);
                mSignalView.setImageResource(R.drawable.ic_qs2_data_g6);
                mBtView.setImageResource(R.drawable.ic_qs2_bt_g6);
                mMuteView.setImageResource(R.drawable.ic_qs2_mute_g6);
                mMusicView.setImageResource(R.drawable.ic_qs2_music_g6);
                mZhendongView.setImageResource(R.drawable.ic_qs2_zhendong_g6);
                break;
        }

        mAudioManager = (AudioManager) getContext().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mVoume = getNomorlizedVolume();
        mVolumeProgressBar.setProgress(mVoume);
        selectMode(Profiles.getProfiles(getContext()), false);
        mVolumeAddView.setOnClickListener(this);
        mVolumeDecView.setOnClickListener(this);
        mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        initTelephonyManager();
        mBlueadapter = BluetoothAdapter.getDefaultAdapter();
        updateBtView();
        updateDataView();
        addViewAction();
    }

    private void updateVolume(boolean sound) {
        int mode = Profiles.getProfiles(getContext());
        Log.d(TAG, "mode: " + mode);
        if (mode != AudioManager.RINGER_MODE_NORMAL) {
            Log.d(TAG, "skip mode: " + mode);
            return;
        }
        logVolume();
        int[] VolumesType = {
                AudioManager.STREAM_SYSTEM,
//                AudioManager.STREAM_MUSIC,
//                AudioManager.STREAM_ALARM,
//                AudioManager.STREAM_VOICE_CALL,
//                AudioManager.STREAM_RING,
        };
        for (int i = 0; i < VolumesType.length; i++) {
            int volume = mAudioManager.getStreamVolume(VolumesType[i]);
            int max = mAudioManager.getStreamMaxVolume(VolumesType[i]);
            int newvolume = mVoume * max / MAX_VOLUME;
            Log.d(TAG, String.format("Adjust Volume type %d - volume: %d --> %d [max %d]", VolumesType[i], volume, newvolume, max));
//            if (VolumesType[i] == AudioManager.STREAM_SYSTEM && sound) {
//                mAudioManager.setStreamVolume(VolumesType[i], newvolume, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_VIBRATE);
//            } else {
                mAudioManager.setStreamVolume(VolumesType[i], newvolume, 0);
//            }
        }
        logVolume();
    }

    void logVolume() {
//        int[] CheckResultVolumesType = {
//                AudioManager.STREAM_SYSTEM,
//                AudioManager.STREAM_RING, //
//                AudioManager.STREAM_MUSIC,
//                AudioManager.STREAM_ALARM,
//                AudioManager.STREAM_NOTIFICATION, //
//                AudioManager.STREAM_DTMF, //
//                AudioManager.STREAM_VOICE_CALL,
//        };
//        Log.d(TAG, "isVolumeFixed: "+mAudioManager.isVolumeFixed());
//        for (int i = 0; i < CheckResultVolumesType.length; i++) {
//            int volume = mAudioManager.getStreamVolume(CheckResultVolumesType[i]);
//            Log.d(TAG, String.format("Check Volume type %d - volume: %d", CheckResultVolumesType[i], volume));
//        }
    }


    int getNomorlizedVolume() {
        int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        volume = volume * MAX_VOLUME / max;
        if (volume < 1) volume = 1;
        else if (volume > MAX_VOLUME) volume = MAX_VOLUME;
        return volume;
    }

    private void addViewAction() {
        mWifiView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"start wifi activity");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SETTING_PACKAGE_NAME,
                        SETTING_WIFI_ACTIVITY));
                getContext().startActivity(intent);
            }
        });
        mWifiView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.i(TAG,"start wifi activity");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SETTING_PACKAGE_NAME,
                        SETTING_WIFI_ACTIVITY));
                getContext().startActivity(intent);
                return false;
            }
        });
        mBtView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"start bt activity");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SETTING_PACKAGE_NAME,
                        SETTING_BT_ACTIVITY));
                getContext().startActivity(intent);
            }
        });
        mBtView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.i(TAG,"start bt activity");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SETTING_PACKAGE_NAME,
                        SETTING_BT_ACTIVITY));
                getContext().startActivity(intent);
                return false;
            }
        });
        mSignalView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"start data activity");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SETTING_PACKAGE_NAME,
                        SETTING_SINGAL_ACTIVITY));
                getContext().startActivity(intent);
            }
        });
        mSignalView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.i(TAG,"start data activity");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SETTING_PACKAGE_NAME,
                        SETTING_SINGAL_ACTIVITY));
                getContext().startActivity(intent);
                return false;
            }
        });
    }


    private void updateBtView() {
        Log.d(TAG, "---------------updateBtView---------------");
        mBtView.post(new Runnable() {
            @Override
            public void run() {
                if (mBlueadapter != null && mBlueadapter.isEnabled()) {
                    mBtView.setActivated(true);
                } else {
                    mBtView.setActivated(false);
                }
            }
        });
    }
    private void updateDataView()
    {

        if(!hasSimeCard) {
            mSignalView.setActivated(false);
            Log.e(TAG,"hasSimeCard " + hasSimeCard);
        } else {
            try {
                boolean mobileDataState = iMobileDataAidl.getMobileDataState();
                if(mobileDataState) {
                    mSignalView.setActivated(true);
                    Log.e(TAG,"isMobileData " + mobileDataState);
                }else{
                    mSignalView.setActivated(false);
                    Log.e(TAG,"isMobileData " + mobileDataState);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,"get is mobile data error " + e);
                if(mSPH.getBoolean(MODEDATA,true)) {
                    Log.e(TAG,"MODEDATA " + mSPH.getBoolean(MODEDATA,true));
                    mSignalView.setActivated(true);
                }else{
                    Log.e(TAG,"MODEDATA " + mSPH.getBoolean(MODEDATA,true));
                    mSignalView.setActivated(false);
                }
            }
        }



        Log.d(TAG, "---------------updateDataView---------------");

    }

    private void initTelephonyManager() {
        mTelephonyManager = (TelephonyManager) getContext().getSystemService(TELEPHONY_SERVICE);
        mSubscriptionManager = SubscriptionManager.from(getContext());

        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                updateSignalStrength(signalStrength.getGsmSignalStrength());
            }

//            @Override
//            public void onServiceStateChanged(ServiceState serviceState) {
//                final int state = serviceState.getState();
//                if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
//                        (ServiceState.STATE_POWER_OFF == state)) {
//                    hasSimeCard = false;
//                    Log.d(TAG,"onServiceStateChanged hasSimeCard: "+hasSimeCard);
//                    updateSignalStrength(0);
//                }
//            }
        };

        simOnSubscriptionsChangedListener = new SimOnSubscriptionsChangedListener();
        mSimStateReceive = new SimStateReceive();
    }

    void updateSignalStrength(int signal) {
        Log.d(TAG, "hasSimeCard: " + hasSimeCard + " updateSignalStrength: " + signal);
        if (!hasSimeCard) {
            mSignalView.setImageLevel(0);
        } else {
            if (-1 == signal || signal == 99) {
                signal = 0;
            }
            int level = signal * SIM_SIGNAL_ICON_LEVELS / 32;
            Log.d(TAG, "Sim signalDbm:" + signal + " level:" + level);
            mSignalView.setImageLevel(level + 1);
        }
    }


    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick: " + v);
        if (mMuteView == v && !v.isActivated()) {
            selectMode(AudioManager.RINGER_MODE_SILENT, true);
        } else if (mMusicView == v && !v.isActivated()) {
            selectMode(AudioManager.RINGER_MODE_NORMAL, true);
        } else if (mZhendongView == v && !v.isActivated()) {
            selectMode(AudioManager.RINGER_MODE_VIBRATE, true);
        } else if (mVolumeAddView == v) {
            mVoume++;
            if (mVoume > MAX_VOLUME) mVoume = MAX_VOLUME;
            mVolumeProgressBar.setProgress(mVoume);
            updateVolume(true);
        } else if (mVolumeDecView == v) {
            mVoume--;
            if (mVoume < 1) mVoume = 1;
            mVolumeProgressBar.setProgress(mVoume);
            updateVolume(true);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            updateDataView();
            if (!isWifiReceiverRegisted) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                filter.addAction(WifiManager.RSSI_CHANGED_ACTION);

                Intent intent = getContext().registerReceiver(mWifiBroadcastReceiver, filter);
                int level = getWifiLevel(intent);
                mWifiView.setImageLevel(level);
                Log.d(TAG, "Wifi init level: " + level);
                isWifiReceiverRegisted = true;
            }
            if (!isSimSignalListenerRegisted) {
                mTelephonyManager.listen(mPhoneStateListener,
                        PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                                | PhoneStateListener.LISTEN_SERVICE_STATE);
                mSubscriptionManager.addOnSubscriptionsChangedListener(simOnSubscriptionsChangedListener);
            }
            IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
            getContext().registerReceiver(mSimStateReceive, intentFilter);
            updateSimState();
//            mBtView.setActivated(mBlueadapter != null && mBlueadapter.isEnabled());
            updateBtView();
            updateDataView();
            Log.d(TAG, "VISIBLE");
        } else if (visibility == INVISIBLE || visibility == GONE) {
            if (isWifiReceiverRegisted) {
                try {
                    getContext().unregisterReceiver(mWifiBroadcastReceiver);
                } catch (Exception e) {
                    Log.e(TAG, "fail to unregisterReceiver " + mWifiBroadcastReceiver);
                }
                isWifiReceiverRegisted = false;
            }
            if (isSimSignalListenerRegisted) {
                mTelephonyManager.listen(mPhoneStateListener,
                        PhoneStateListener.LISTEN_NONE);
            }
            mSubscriptionManager.removeOnSubscriptionsChangedListener(simOnSubscriptionsChangedListener);
            try {
                getContext().unregisterReceiver(mSimStateReceive);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "INVISIBLE");

        }
    }

    int getWifiLevel(Intent intent) {
        if (intent == null) {
            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                int rssi = mWifiManager.getConnectionInfo().getRssi();
                return WifiManager.calculateSignalLevel(
                        rssi, WIFI_ICON_LEVELS);
            } else {
                return 0;
            }
        } else {

            String action = intent.getAction();
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                boolean enabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;
                if (!enabled) {
                    return 99;
                } else {
                    int rssi = mWifiManager.getConnectionInfo().getRssi();
                    return WifiManager.calculateSignalLevel(
                            rssi, WIFI_ICON_LEVELS);
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                String ssid;
                final NetworkInfo networkInfo = (NetworkInfo)
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                boolean connected = networkInfo != null && networkInfo.isConnected();
                // If Connected grab the signal strength and ssid.
                if (!connected) {
                    return 0;
                }
            } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                // Default to -200 as its below WifiManager.MIN_RSSI.
                int rssi, level;
                rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
                level = WifiManager.calculateSignalLevel(
                        rssi, WIFI_ICON_LEVELS);
                return level;
            }
            return 0;
        }
    }

    private String getSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        // OK, it's not in the connectionInfo; we have to go hunting for it
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        int length = networks.size();
        for (int i = 0; i < length; i++) {
            if (networks.get(i).networkId == info.getNetworkId()) {
                return networks.get(i).SSID;
            }
        }
        return null;
    }

    void selectMode(int mode, boolean fromUser) {
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                mMuteView.setActivated(true);
                mMusicView.setActivated(false);
                mZhendongView.setActivated(false);
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                mMuteView.setActivated(false);
                mMusicView.setActivated(true);
                mZhendongView.setActivated(false);
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                mMuteView.setActivated(false);
                mMusicView.setActivated(false);
                mZhendongView.setActivated(true);
                Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(50);
                break;
        }
        Profiles.setProfiles(mode, getContext());
        updateVolume(fromUser);
    }

    class WifiBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = getWifiLevel(intent);
            mWifiView.setImageLevel(getWifiLevel(intent));
            Log.d(TAG, "Wifi onReceive: " + level);
        }
    }

    public class SimStateReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                Log.d(TAG, "SimStateReceive onReceive: ");
                updateSimState();
            }
        }
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
}
