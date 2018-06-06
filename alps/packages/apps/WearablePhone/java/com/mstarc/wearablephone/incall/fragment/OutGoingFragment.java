package com.mstarc.wearablephone.incall.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablephone.PhoneApplication;
import com.mstarc.wearablephone.R;
import com.mstarc.wearablephone.incall.BaseFragment;
import com.mstarc.wearablephone.incall.Call;
import com.mstarc.wearablephone.incall.CallList;
import com.mstarc.wearablephone.incall.Log;
import com.mstarc.wearablephone.incall.OutgoingPresenter;
import com.mstarc.wearablephone.incall.OutgoingPresenter.OutgoingUi;

import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_BOOLEAN;


/**
 * Created by wangxinzhi on 17-4-4.
 */


public class OutGoingFragment extends BaseFragment<OutgoingPresenter, OutgoingPresenter.OutgoingUi> implements View.OnClickListener, OutgoingPresenter.OutgoingUi {
    private static final String TAG = OutGoingFragment.class.getSimpleName();
    TextView mLable;
    ImageView mProfile;
    ImageView mTerminateButton;
    ImageView mCallWithPhoneImageView, mCallWithWatchImageView;

    @Override
    protected OutgoingPresenter createPresenter() {
        return new OutgoingPresenter();
    }

    @Override
    protected OutgoingUi getUi() {
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        int themeResID = ((PhoneApplication)getActivity().getApplicationContext()).getThemeStyle();
        Log.d(TAG, "themeResID: "+themeResID);
        if(themeResID!=0){
            getActivity().getTheme().applyStyle(themeResID, true);
        }
        return (ViewGroup) inflater.inflate(R.layout.dial_out,
                container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLable = (TextView) view.findViewById(R.id.text_dial_out_name);
        mProfile = (ImageView) view.findViewById(R.id.image_dial_out_profile);
        mTerminateButton = (ImageView) view.findViewById(R.id.image_dial_out_terminate);
        mTerminateButton.setOnClickListener(this);
        mCallWithWatchImageView = (ImageView ) view.findViewById(R.id.image_dial_out_watch);
        mCallWithPhoneImageView = (ImageView ) view.findViewById(R.id.image_dial_out_phone);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean isByPhone = getArguments()!=null && getArguments().getBoolean(INTENT_DIAL_BY_PHONE_BOOLEAN, false);
        if(isByPhone){
            mCallWithWatchImageView.setVisibility(View.INVISIBLE);
            mCallWithPhoneImageView.setVisibility(View.VISIBLE);
        }else{
            mCallWithWatchImageView.setVisibility(View.VISIBLE);
            mCallWithPhoneImageView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final CallList calls = CallList.getInstance();
        Call call = calls.getOutgoingCall();
        if(call == null){
            call = calls.getPendingOutgoingCall();
        }
        final Call outgoingCall = call;
        getPresenter().init(getActivity(), outgoingCall);
    }

    @Override
    public void onClick(View v) {
        Log.d(this, "onClick: " + v);
        if (v.getId() == R.id.image_dial_out_terminate) {
            getPresenter().endCallClicked();
        }
    }

    @Override
    public void setVisible(boolean on) {
        getView().setVisibility(on ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setPrimary(String number, String name, boolean nameIsNumber, String label, Drawable photo, boolean isSipCall) {
        mLable.setText(name);
        Log.d(this, "Hawking name:"+name+" number:"+number+"photo:"+photo);
    }

    @Override
    public void setPrimaryImage(Drawable image) {
        Log.d(this, "Hawking setPrimaryImage: image"+image);
        if (image != null) {
            mProfile.setImageDrawable(image);
        }

    }

    @Override
    public void setPrimaryPhoneNumber(String phoneNumber) {

    }

    @Override
    public void setPrimaryLabel(String label) {

    }
}
