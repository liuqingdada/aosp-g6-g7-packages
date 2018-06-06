package com.mstarc.wearablephone.incall.fragment;

import android.app.Service;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mstarc.wearablephone.PhoneApplication;
import com.mstarc.wearablephone.R;
import com.mstarc.wearablephone.incall.BaseFragment;
import com.mstarc.wearablephone.incall.Call;
import com.mstarc.wearablephone.incall.CallList;
import com.mstarc.wearablephone.incall.Log;
import com.mstarc.wearablephone.incall.Presenter;
import com.mstarc.wearablephone.incall.TalkingPresenter;
import com.mstarc.wearablephone.incall.Ui;

import java.util.HashMap;

/**
 * Created by wangxinzhi on 17-4-4.
 */
public class TalkingFragment extends BaseFragment<TalkingPresenter, TalkingPresenter.TalkingUi>
        implements View.OnClickListener, TalkingPresenter.TalkingUi, View.OnTouchListener {
    TextView mTalkingTimeText;
    ImageButton mKeyPadButton, mVoumeButton, mTerminateButton;
    TextView mNumberTextView, mTalkingName;
    View mKeyPadView, mVoumeView;
    ProgressBar mVoumeBar;
    ImageView mProfile;
    ImageView mCallWithPhoneImageView, mCallWithWatchImageView;
    AudioManager mAudioManager;
    int mAudioMaxVolume;

    public final static String INTENT_IN_CALL_BY_PHONE_BOOLEAN = "INTENT_IN_CALL_BY_PHONE_BOOLEAN";
    private static final HashMap<Integer, Character> mDisplayMap =
            new HashMap<Integer, Character>();
    private final int[] mButtonIds = new int[]{
            R.id.button0,
            R.id.button1,
            R.id.button2,
            R.id.button3,
            R.id.button4,
            R.id.button5,
            R.id.button6,
            R.id.button7,
            R.id.button8,
            R.id.button9,
            R.id.button0,
            R.id.buttonstar,
            R.id.buttonhex};

    static {
        // Map the buttons to the display characters
        mDisplayMap.put(R.id.button1, '1');
        mDisplayMap.put(R.id.button2, '2');
        mDisplayMap.put(R.id.button3, '3');
        mDisplayMap.put(R.id.button4, '4');
        mDisplayMap.put(R.id.button5, '5');
        mDisplayMap.put(R.id.button6, '6');
        mDisplayMap.put(R.id.button7, '7');
        mDisplayMap.put(R.id.button8, '8');
        mDisplayMap.put(R.id.button9, '9');
        mDisplayMap.put(R.id.button0, '0');
        mDisplayMap.put(R.id.buttonhex, '#');
        mDisplayMap.put(R.id.buttonstar, '*');
    }

    @Override
    protected TalkingPresenter createPresenter() {
        return new TalkingPresenter();
    }

    @Override
    protected TalkingPresenter.TalkingUi getUi() {
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        int themeResID = ((PhoneApplication)getActivity().getApplicationContext()).getThemeStyle();
        if(themeResID!=0){
            getActivity().getTheme().applyStyle(themeResID, true);
        }
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dial_talking,
                container, false);
        addOnClickListenersRecursive(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTalkingTimeText = (TextView) view.findViewById(R.id.talking_time);
        mKeyPadButton = (ImageButton) view.findViewById(R.id.image_talking_keypad_button);
        mVoumeButton = (ImageButton) view.findViewById(R.id.image_talking_volume_button);
        mKeyPadView = view.findViewById(R.id.keypad);
        mVoumeView = view.findViewById(R.id.talking_volume);
        mNumberTextView = (TextView) view.findViewById(R.id.dial_number);
        mNumberTextView.setText("");
        mVoumeBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mTerminateButton = (ImageButton) view.findViewById(R.id.image_talking_teminate);
        mTalkingName = (TextView) view.findViewById(R.id.text_talking_name);
        mProfile = (ImageView) view.findViewById(R.id.image_talking_profile);
        mCallWithWatchImageView = (ImageView) view.findViewById(R.id.image_talking_watch);
        mCallWithPhoneImageView = (ImageView) view.findViewById(R.id.image_talking_phone);
        mAudioManager = (AudioManager) getActivity().getSystemService(Service.AUDIO_SERVICE);
        mAudioMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        mVoumeBar.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) * 100 / mAudioMaxVolume);
        View dialpadKey;
        for (int i = 0; i < mButtonIds.length; i++) {
            dialpadKey = view.findViewById(mButtonIds[i]);
            dialpadKey.setOnTouchListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean isByPhone = getArguments() != null && getArguments() != null && getArguments().getBoolean(INTENT_IN_CALL_BY_PHONE_BOOLEAN, false);
        if (isByPhone) {
            mCallWithWatchImageView.setVisibility(View.INVISIBLE);
            mCallWithPhoneImageView.setVisibility(View.VISIBLE);
        } else {
            mCallWithWatchImageView.setVisibility(View.VISIBLE);
            mCallWithPhoneImageView.setVisibility(View.INVISIBLE);
        }
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Call activeCall = CallList.getInstance().getActiveOrBackgroundCall();
        getPresenter().init(getActivity(), activeCall);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        char pressedNumber = 0;
        String currentNumber = mNumberTextView.getText().toString();
        boolean actionShowVoumeBar = false;
        boolean actionShowKeypad = false;
        boolean actionTerminate = false;
        boolean actionVoumeDec = false;
        boolean actionVoumeAdd = false;
        boolean actionNumber = true;

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
            case R.id.image_talking_keypad_button:
                actionShowKeypad = true;
                actionNumber = false;
                break;
            case R.id.image_talking_volume_button:
                actionShowVoumeBar = true;
                actionNumber = false;
                break;
            case R.id.talking_volume_dec:
                actionVoumeDec = true;
                actionNumber = false;
                break;
            case R.id.talking_volume_add:
                actionVoumeAdd = true;
                actionNumber = false;
                break;
            case R.id.image_talking_teminate:
                getPresenter().endCallClicked();
                return;
            default:
                return;
        }
        if (actionNumber) {
//            currentNumber += pressedNumber;
//            mNumberTextView.setText(currentNumber);
        } else if (actionShowKeypad) {
            mVoumeView.setVisibility(View.GONE);
            if (mKeyPadView.getVisibility() == View.VISIBLE) {
                mKeyPadView.setVisibility(View.GONE);
            } else {
                mKeyPadView.setVisibility(View.VISIBLE);
            }
        } else if (actionShowVoumeBar) {
            mKeyPadView.setVisibility(View.GONE);
            if (mVoumeView.getVisibility() == View.VISIBLE) {
                mVoumeView.setVisibility(View.GONE);
            } else {
                mVoumeView.setVisibility(View.VISIBLE);
            }
        } else if (actionTerminate) {
            mKeyPadView.setVisibility(View.GONE);
            mVoumeView.setVisibility(View.GONE);
        } else if (actionVoumeDec) {
            mVoumeBar.setProgress(mVoumeBar.getProgress() - 10);
            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mVoumeBar.getProgress() * mAudioMaxVolume / 100, 0);
        } else if (actionVoumeAdd) {
            mVoumeBar.setProgress(mVoumeBar.getProgress() + 10);
            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mVoumeBar.getProgress() * mAudioMaxVolume / 100, 0);
        }
    }

    @Override
    public void setVisible(boolean on) {
        getView().setVisibility(on ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setPrimary(String number, String name, boolean nameIsNumber, String label, Drawable photo, boolean isSipCall) {
        Log.d(this, "setPrimary: name: " + name + " number: " + number);
        mTalkingName.setText(name);
        mProfile.setImageDrawable(photo);
    }

    @Override
    public void setPrimaryImage(Drawable image) {
        mProfile.setImageDrawable(image);
    }

    @Override
    public void setPrimaryCallElapsedTime(boolean show, long duration) {
        mTalkingTimeText.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        if (show) {
            String callTimeElapsed = DateUtils.formatElapsedTime(duration / 1000);
            mTalkingTimeText.setText(callTimeElapsed);
        }
    }

    @Override
    public void setPrimaryPhoneNumber(String phoneNumber) {

    }

    @Override
    public void setPrimaryLabel(String label) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(this, "onTouch");
        int viewId = v.getId();

        // if the button is recognized
        if (mDisplayMap.containsKey(viewId)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Append the character mapped to this button, to the display.
                    // start the tone
                    getPresenter().processDtmf(mDisplayMap.get(viewId));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // stop the tone on ANY other event, except for MOVE.
                    getPresenter().stopDtmf();
                    break;
            }
            // do not return true [handled] here, since we want the
            // press / click animation to be handled by the framework.
        }
        return false;
    }

    @Override
    public void appendDigitsToField(char digit) {
        if (mNumberTextView != null) {
            String oldText = mNumberTextView.getText().toString();
            oldText += digit;
            mNumberTextView.setText(oldText);
        }
    }
}
