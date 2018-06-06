package com.mstarc.wearablelauncher.view.quicksetting;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.internal.telephony.TelephonyIntents;
import com.mstarc.commonbase.communication.aidl.AidlCommunicate;
import com.mstarc.commonbase.communication.listener.ConnectionStateListener;
import com.mstarc.wearablelauncher.CommonManager;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.ThemeUtils;
import com.mstarc.wearablelauncher.view.notification.ConfirmDialog;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import com.mstarc.wearablelauncher.view.notification.NotificationFragment;
import java.text.NumberFormat;
import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;

/**
 * Created by wangxinzhi on 17-3-4.
 */

public class QuickSettingPower extends RelativeLayout implements View.OnClickListener, CommonManager.OnPowerModeChangeListener, CommonManager.IBtListener, CommonManager.IPhoneBatteryListener {
    private static final String TAG = QuickSettingPower.class.getSimpleName();
    View mLightLowButton, mLightHighButton,mPhoneBatteryImg;
    ImageView mWatchMode, mNormalMode, mFlightMode;
    ImageView mSignalView;
    ProgressBar mLightValue;
    BatteryManager mBatteryManager;
    TextView mWatchBatteryText;
    TextView mNoSim;
    BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    boolean isBatteryReceiverRegisted = false;
    DialogListener mDialogListener;
    String mPhoneBatteryString;
    public static final String PHONE_BATTERY_ACTION = "com.mstarc.notificationwizard.phonepower";
    public static final String BT_STATUS_ACTION = "android.bluetooth.adapter.action.STATE_CHANGED";
    private PhoneStateListener mPhoneStateListener;
    private final int MSG_BLE_DISCONNECT = 100;
    private final int MSG_BLE_CONNECT = 200;
    boolean isSimSignalListenerRegisted = false;
    public static final int SIM_SIGNAL_ICON_LEVELS = 5;
    private SubscriptionManager mSubscriptionManager;
    TelephonyManager mTelephonyManager;
    SimOnSubscriptionsChangedListener simOnSubscriptionsChangedListener;
    SimStateReceive mSimStateReceive;
    boolean hasSimeCard = false;
    BluetoothAdapter mBlueadapter;
    private NotificationFragment mNotificationFragment = new NotificationFragment();
    Handler uiHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_BLE_DISCONNECT:
                    mPhoneBatteryImg.setBackgroundResource(R.drawable.bt_unconnect);
                    updateImageView(mPhoneBatteryImg.findViewById(R.id.qs1_phonebuttary_image), R.drawable.bt_unconnect);
                    break;
                case MSG_BLE_CONNECT:
                    mPhoneBatteryImg.setBackgroundResource(R.drawable.bt_connected);
                    updateImageView(mPhoneBatteryImg.findViewById(R.id.qs1_phonebuttary_image), R.drawable.bt_connected);
                    break;
            }

        }
    };

    public QuickSettingPower(Context context) {
        super(context);
        init();
    }

    public QuickSettingPower(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QuickSettingPower(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public void setWatchBatImg(Intent intent)
    {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

       int  mBat = level;
        Log.d(TAG,"setWatchBatImg  mBat= "+mBat+"  level= "+level+"   scale="+scale);

        if(mBat == 0)
        {
            updateImageView(findViewById(R.id.qs1_watchbuttary_image), R.drawable.icon_bat_0);
        }else if(mBat <= 20)
        {
            updateImageView(findViewById(R.id.qs1_watchbuttary_image), R.drawable.icon_bat_20);
        }else if(mBat <= 40)
        {
            updateImageView(findViewById(R.id.qs1_watchbuttary_image), R.drawable.icon_bat_40);
        }else if(mBat <= 60)
        {
            updateImageView(findViewById(R.id.qs1_watchbuttary_image), R.drawable.icon_bat_60);
        }else if(mBat <= 80)
        {
            updateImageView(findViewById(R.id.qs1_watchbuttary_image), R.drawable.icon_bat_80);
        }else
        {
            updateImageView(findViewById(R.id.qs1_watchbuttary_image), R.drawable.icon_bat_100);
        }
    }
    private void init() {
        mBatteryManager = (BatteryManager) getContext().getSystemService(BATTERY_SERVICE);
        mBatteryBroadcastReceiver = new BatteryBroadcastReceiver();
        mDialogListener = new DialogListener();
        AidlCommunicate.getInstance().setConnectionStateListener(new ConnectionStateListener() {
            @Override
            public void onConnected(String s) {
                if (s.equals(ConnectionStateListener.BLE_ADVERTISE)) {
                    // ble connected
                    Log.d(TAG,"OnBtStatusChanged= true");
                    uiHandler.sendEmptyMessage(MSG_BLE_CONNECT);
                }
            }

            @Override
            public void onDisconnected() {
                // ble discinnect
                Log.d(TAG,"OnBtStatusChanged= false");
                uiHandler.sendEmptyMessage(MSG_BLE_DISCONNECT);


            }
        });
    }

    protected boolean isCover() {
        boolean cover = false;
        Rect rect = new Rect();
        cover = getGlobalVisibleRect(rect);
        if (cover) {
            if (rect.width() >= getMeasuredWidth() && rect.height() >= getMeasuredHeight()) {
                return !cover;
            }
        }
        return true;
    }

    @Override
    public void OnPowerModeChange(int powermode) {
        selectMode(powermode, false);
    }

    @Override
    public void OnBtStatusChanged(boolean connected) {

    }

    @Override
    public void OnPhoneBatteryhanged(String batteryText) {
     //   mPhoneBatteryString = batteryText;
     //   mPhoneBattery.setText(mPhoneBatteryString);

     //       Log.d(TAG,"OnPhoneBatteryhanged");


    }

    class BatteryBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == Intent.ACTION_BATTERY_CHANGED) {
                mWatchBatteryText.setText(getBatteryStatus(intent));
                setWatchBatImg(intent);
            } else if (intent.getAction() == PHONE_BATTERY_ACTION) {
                String extra = intent.getStringExtra("phonepower");
                Log.e(TAG, "extra: "+extra);
                OnPhoneBatteryhanged(extra);
            }
        }
    }
    private void updateBtStatus()
    {
        if (mBlueadapter != null && mBlueadapter.isEnabled())
        {
            Log.d(TAG, "mBlueadapter.isEnabled() = "+mBlueadapter.isEnabled());
        }else
        {
            uiHandler.sendEmptyMessage(MSG_BLE_DISCONNECT);
            Log.d(TAG, "mBlueadapter.isEnabled() = "+mBlueadapter.isEnabled());
        }
    }
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            if (!isBatteryReceiverRegisted) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                filter.addAction(PHONE_BATTERY_ACTION);
                Intent intent = getContext().registerReceiver(mBatteryBroadcastReceiver, filter);
                mWatchBatteryText.setText(getBatteryStatus(intent));
                setWatchBatImg(intent);
                isBatteryReceiverRegisted = true;
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
            updateBtStatus();
            if (mBlueadapter != null && mBlueadapter.isEnabled())
            Log.d(TAG, "VISIBLE");

        } else if (visibility == INVISIBLE || visibility == GONE) {
            if (isBatteryReceiverRegisted) {
                getContext().unregisterReceiver(mBatteryBroadcastReceiver);
                isBatteryReceiverRegisted = false;
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

    String getBatteryStatus(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level / (float) scale;
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(2);
        return percentFormat.format(batteryPct);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPhoneBatteryImg=(ImageView) findViewById(R.id.qs1_phonebuttary_image);
        mSignalView = (ImageView) findViewById(R.id.qs2_signal);
        mWatchMode = (ImageView) findViewById(R.id.qs1_watchmode);
        mNormalMode = (ImageView) findViewById(R.id.qs1_normalmode);
        mFlightMode = (ImageView) findViewById(R.id.qs1_flightmode);
        mLightLowButton = findViewById(R.id.qs1_light_low);
        mLightHighButton = findViewById(R.id.qs1_light_high);
        mLightValue = (ProgressBar) findViewById(R.id.qs1_progressBar);
        mNoSim = (TextView) findViewById(R.id.qs1_nosim_text);
        mNoSim.setVisibility(View.INVISIBLE);
        mWatchBatteryText = (TextView) findViewById(R.id.qs1_watchbuttary_text);

        updateImageView(mLightLowButton, R.drawable.ic_qs1_light_low);
        updateImageView(mLightHighButton, R.drawable.ic_qs1_light_hight);
        updateImageView(findViewById(R.id.qs1_watchbuttary_image), R.drawable.icon_bat_40);
        updateImageView(findViewById(R.id.qs1_phonebuttary_image), R.drawable.bt_unconnect);
        mNormalMode.setActivated(false);
        mWatchMode.setActivated(true);

        switch (ThemeUtils.getCurrentProduct()) {
            case ThemeUtils.PRODUCT_COLOR_ROSE_GOLDEN:
                mWatchMode.setImageResource(R.drawable.ic_qs1_watchmode_g7_rose);
                mNormalMode.setImageResource(R.drawable.ic_qs1_normalmode_g7_rose);
                mFlightMode.setImageResource(R.drawable.ic_qs1_flight_g7_rose);
                mSignalView.setImageResource(R.drawable.ic_qs2_signal_g7_rose);
                break;
            case ThemeUtils.PRODUCT_COLOR_HIGH_BLACK:
                mWatchMode.setImageResource(R.drawable.ic_qs1_watchmode_g7_golden);
                mNormalMode.setImageResource(R.drawable.ic_qs1_normalmode_g7_golden);
                mFlightMode.setImageResource(R.drawable.ic_qs1_flight_g7_golden);
                mSignalView.setImageResource(R.drawable.ic_qs2_signal_g7_golden);
                break;
            case ThemeUtils.PRODUCT_COLOR_APPLE_GREEN:
                mWatchMode.setImageResource(R.drawable.ic_qs1_watchmode_g7_green);
                mNormalMode.setImageResource(R.drawable.ic_qs1_normalmode_g7_green);
                mFlightMode.setImageResource(R.drawable.ic_qs1_flight_g7_green);
                mSignalView.setImageResource(R.drawable.ic_qs2_signal_g7_green);
                break;
            default:
                mWatchMode.setImageResource(R.drawable.ic_qs1_watchmode_g6);
                mNormalMode.setImageResource(R.drawable.ic_qs1_normalmode_g6);
                mFlightMode.setImageResource(R.drawable.ic_qs1_flight_g6);
                mSignalView.setImageResource(R.drawable.ic_qs2_signal_g6);
                break;
        }
        mBlueadapter = BluetoothAdapter.getDefaultAdapter();
        mLightLowButton.setOnClickListener(this);
        mLightHighButton.setOnClickListener(this);
        mWatchMode.setOnClickListener(this);
        mNormalMode.setOnClickListener(this);
        mFlightMode.setOnClickListener(this);
        mLightValue.setMax(255);
        mLightValue.setProgress(CommonManager.getInstance(getContext().getApplicationContext()).getBrightness());
        selectMode(CommonManager.getInstance(getContext().getApplicationContext()).getmPowerMode(), false);
        boolean isBtConnected = AidlCommunicate.getInstance().isAdvertiserConnect();
        //boolean isBtConnected = CommonManager.getInstance(getContext().getApplicationContext()).isBtConnected();
        if (!isBtConnected) {
            Log.d(TAG,"isBtConnected= false");
            updateImageView(findViewById(R.id.qs1_phonebuttary_image), R.drawable.bt_unconnect);
        } else {
            Log.d(TAG,"isBtConnected= true");
            updateImageView(findViewById(R.id.qs1_phonebuttary_image), R.drawable.bt_connected);
        }
        mWatchBatteryText.setText(String.valueOf(mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)));
        Log.d(TAG,"mWatchBatteryText= "+mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
        initTelephonyManager();
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
        if (mLightLowButton == v) {
            ProgressBar bar = mLightValue;
            int newvalue = bar.getProgress() - 25;
            if (newvalue < 0) {
                newvalue = 0;
            }
            bar.setProgress(newvalue);
            CommonManager.getInstance(getContext().getApplicationContext()).setBrightness(newvalue);
        } else if (mLightHighButton == v) {
            ProgressBar bar = mLightValue;
            int newvalue = bar.getProgress() + 25;
            if (newvalue > 255) {
                newvalue = 255;
            }
            bar.setProgress(newvalue);
            CommonManager.getInstance(getContext().getApplicationContext()).setBrightness(newvalue);
        } else if (mWatchMode == v && !v.isActivated()) {
            mDialogListener.setmPowerModeTarget(CommonManager.POWERMODE_WATCH);
            Dialog dialog = new ConfirmDialog(getContext(), mDialogListener, getResources().getString(R.string.qs1_watchmode_swtich));
            dialog.show();
        } else if (mNormalMode == v && !v.isActivated()) {
            mDialogListener.setmPowerModeTarget(CommonManager.POWERMODE_NORMAL);
            Dialog dialog = new ConfirmDialog(getContext(), mDialogListener, getResources().getString(R.string.qs1_normalmode_swtich));
            dialog.show();
        } else if (mFlightMode == v && !v.isActivated()) {
            mDialogListener.setmPowerModeTarget(CommonManager.POWERMODE_FLIGHT);
            Dialog dialog = new ConfirmDialog(getContext(), mDialogListener, getResources().getString(R.string.qs1_flightmode_swtich));
            dialog.show();
        }
    }

    public void selectMode(int mode, boolean updatePref) {
        switch (mode) {
            case CommonManager.POWERMODE_WATCH:
                mWatchMode.setActivated(true);
                mNormalMode.setActivated(false);
                mFlightMode.setActivated(false);
                break;
            case CommonManager.POWERMODE_NORMAL:
                mWatchMode.setActivated(false);
                mNormalMode.setActivated(true);
                mFlightMode.setActivated(false);
                break;
            case CommonManager.POWERMODE_FLIGHT:
                mWatchMode.setActivated(false);
                mNormalMode.setActivated(false);
                mFlightMode.setActivated(true);
                break;
        }
//        postInvalidate();
        if (updatePref) {
            CommonManager.getInstance(getContext().getApplicationContext()).setmPowerMode(mode);
        }
    }

    class DialogListener implements ConfirmDialog.Listener {

        public void setmPowerModeTarget(int mPowerModeTarget) {
            this.mPowerModeTarget = mPowerModeTarget;
        }

        int mPowerModeTarget;

        @Override
        public void onConfirm() {
            selectMode(mPowerModeTarget, true);

        }

        @Override
        public void onCancel() {

        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG,"onAttachedToWindow");
        CommonManager.getInstance(getContext().getApplicationContext()).addPowerModeListener(this);
        CommonManager.getInstance(getContext().getApplicationContext()).addBtListener(this);
        CommonManager.getInstance(getContext().getApplicationContext()).addPhoneBatteryListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.d(TAG,"onDetachedFromWindow");
        CommonManager.getInstance(getContext().getApplicationContext()).removePowerModeListener(this);
        CommonManager.getInstance(getContext().getApplicationContext()).removeBtListener(this);
        CommonManager.getInstance(getContext().getApplicationContext()).removePhoneBatteryListener(this);
        super.onDetachedFromWindow();
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
    class SimOnSubscriptionsChangedListener extends OnSubscriptionsChangedListener {
        public void onSubscriptionsChanged() {
            updateSimState();
        }
    }

    private void updateSimState() {
        int simState = mTelephonyManager.getSimState();
        Log.d(TAG, "SimState: " + simStatetoString(simState));
        if (TelephonyManager.SIM_STATE_READY == simState) {
            mNoSim.setVisibility(View.INVISIBLE);
            hasSimeCard = true;
            updateSignalStrength(0);
        } else {
            mNoSim.setVisibility(View.VISIBLE);
            hasSimeCard = false;
            updateSignalStrength(0);
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

    public class SimStateReceive extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                Log.d(TAG, "SimStateReceive onReceive: ");
                updateSimState();
            }
        }
    }
}
