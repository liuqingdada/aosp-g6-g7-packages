package com.mstarc.wearablephone.view;

import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mstarc.wearablephone.DialOutActivity;
import com.mstarc.wearablephone.PhoneApplication;
import com.mstarc.wearablephone.R;
import com.mstarc.wearablephone.bluetooth.BTCallManager;
import com.mstarc.wearablephone.view.common.SimDialog;

import static com.android.internal.telephony.TelephonyIntents.ACTION_SIM_STATE_CHANGED;
import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_BOOLEAN;
import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_NUMBER;


/**
 * Created by wangxinzhi on 17-3-6.
 */

public class DialFragment extends Fragment implements View.OnClickListener, DeviceSelectDialog.Listener {

    private static final String TAG = DialFragment.class.getSimpleName();
    TextView mNumberTextView;
    ImageButton mDialButton;
    ImageButton mCancelButton;
    Context mContext;
    Vibrator mVibrator;
    private final long vibrateDuration = SystemProperties.getLong("persist.phone.vibrate.duration", 50);
    SimStateReceiver mSimStateReceiver = new SimStateReceiver();
    boolean isSimValid = false;
    SimDialog mSimDialog;

    public DialFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSimDialog = new SimDialog(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int themeResID = ((PhoneApplication) getActivity().getApplicationContext()).getThemeStyle();
        if (themeResID != 0) {
            getActivity().getTheme().applyStyle(themeResID, true);
        }
        mContext = container.getContext();
        ViewGroup viewgroup = (ViewGroup) inflater.inflate(R.layout.dial, container, false);
        mNumberTextView = (TextView) viewgroup.findViewById(R.id.dial_number);
        mDialButton = (ImageButton) viewgroup.findViewById(R.id.dial_do);
        mCancelButton = (ImageButton) viewgroup.findViewById(R.id.dial_cancel);
        addOnClickListenersRecursive(viewgroup);
        mNumberTextView.setText("");
        mVibrator = (Vibrator) mContext.getSystemService(Service.VIBRATOR_SERVICE);
        return viewgroup;
    }

    private void addOnClickListenersRecursive(ViewGroup vg) {
        vg.setOnClickListener(this);
        for (int i = 0; i < vg.getChildCount(); ++i) {
            View nextChild = vg.getChildAt(i);
            if (nextChild instanceof ViewGroup) addOnClickListenersRecursive((ViewGroup) nextChild);
            else nextChild.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        char pressedNumber = 0;
        String currentNumber = mNumberTextView.getText().toString();
        boolean action = false;
        boolean dial = false;
        switch (v.getId()) {
            case R.id.button0:
                pressedNumber = '0';
                break;
            case R.id.button1:
                pressedNumber = '1';
                break;
            case R.id.button2:
                pressedNumber = '2';
                break;
            case R.id.button3:
                pressedNumber = '3';
                break;
            case R.id.button4:
                pressedNumber = '4';
                break;
            case R.id.button5:
                pressedNumber = '5';
                break;
            case R.id.button6:
                pressedNumber = '6';
                break;
            case R.id.button7:
                pressedNumber = '7';
                break;
            case R.id.button8:
                pressedNumber = '8';
                break;
            case R.id.button9:
                pressedNumber = '9';
                break;
            case R.id.buttonstar:
                pressedNumber = '*';
                break;
            case R.id.buttonhex:
                pressedNumber = '#';
                break;
            case R.id.dial_do:
                action = true;
                dial = true;
                break;
            case R.id.dial_cancel:
                action = true;
                dial = false;
                break;
            default:
                return;
        }
        if (action) {
            if (dial) {
                Log.d(TAG, "dial: " + currentNumber);
                if (PhoneNumberUtils.isGlobalPhoneNumber(currentNumber)) {
                    if (BTCallManager.getInstance(getActivity().getApplicationContext()).isBTPhoneEnnable()) {
                        Dialog dialog = new DeviceSelectDialog(mContext, this);
                        dialog.show();
                    } else {
                        byWatch();
                    }
                } else {
                    Dialog dialog = new NumberErrorDialog(mContext);
                    dialog.show();
                }
            } else {
                if (currentNumber.length() != 0) {
                    mNumberTextView.setText(currentNumber.substring(0, currentNumber.length() - 1));
                }
            }
        } else {
            currentNumber += pressedNumber;
            mNumberTextView.setText(currentNumber);

        }
        mVibrator.vibrate(vibrateDuration);
    }

    @Override
    public void byPhone() {
        final String number = mNumberTextView.getText().toString();
        if (number != null) {

            Intent intent = new Intent(getActivity(), DialOutActivity.class);
            intent.putExtra(INTENT_DIAL_BY_PHONE_BOOLEAN, true);
            intent.putExtra(INTENT_DIAL_BY_PHONE_NUMBER, number);

            startActivity(intent);
        }
    }

    @Override
    public void byWatch() {


        final String number = mNumberTextView.getText().toString();
            if (!isSimValid && mSimDialog != null) {
                mSimDialog.show();
                Log.d(TAG,"show no sim dialog");
                return;
            }
        if (number != null) {
            String uri = "tel:" + number;
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(uri));
            startActivity(callIntent);
        }

    }


    @Override
    public void onStop() {
        try {
            mNumberTextView.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mSimStateReceiver, new IntentFilter(ACTION_SIM_STATE_CHANGED));
        updateSimState();
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mSimStateReceiver);
        if (mSimDialog != null && mSimDialog.isShowing()) {
            mSimDialog.dismiss();
        }
        super.onPause();
    }

    class SimStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {
                updateSimState();
            }
        }
    }

    void updateSimState() {
        // Sim state changed
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Service.TELEPHONY_SERVICE);
        int state = tm.getSimState();
        Log.d(TAG, "Sim state: " + state);
        switch (state) {
            case TelephonyManager.SIM_STATE_READY:
                Log.d(TAG, "Sim ready");
                isSimValid = true;
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
            default:
                isSimValid = false;
                Log.d(TAG, "Sim invalid");
                break;
        }
    }

}
