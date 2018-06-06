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
import com.mstarc.wearablephone.incall.InCommingPresenter;
import com.mstarc.wearablephone.view.common.ClickHelper;
import com.mstarc.wearablephone.view.common.RippleLayout;

/**
 * Created by wangxinzhi on 17-4-4.
 */
public class InCommingFragment extends BaseFragment<InCommingPresenter, InCommingPresenter.AnswerUi>
        implements View.OnClickListener, InCommingPresenter.AnswerUi {
    RippleLayout mRipple;
    ImageView mProfile;
    TextView mLabel;
    ImageView mCallWithPhoneImageView, mCallWithWatchImageView;
    public final static String INTENT_INCOMMING_CALL_BY_PHONE_BOOLEAN = "INTENT_INCOMMING_CALL_BY_PHONE_BOOLEAN";

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRipple = (RippleLayout) view.findViewById(R.id.ripple_layout);
        mProfile = (ImageView) view.findViewById(R.id.image_dial_in_profile);
        mLabel = (TextView) view.findViewById(R.id.text_dial_in_name);
        mCallWithWatchImageView = (ImageView ) view.findViewById(R.id.image_dial_in_watch);
        mCallWithPhoneImageView = (ImageView ) view.findViewById(R.id.image_dial_in_phone);
    }

    @Override
    protected InCommingPresenter createPresenter() {
        return new InCommingPresenter(getActivity());
    }

    @Override
    protected InCommingPresenter.AnswerUi getUi() {
        return this;
    }

    public InCommingFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        int themeResID = ((PhoneApplication)getActivity().getApplicationContext()).getThemeStyle();
        if(themeResID!=0){
            getActivity().getTheme().applyStyle(themeResID, true);
        }
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dial_in,
                container, false);
        ClickHelper.addOnClickListenersRecursive(rootView, this);
        mRipple = (RippleLayout) rootView.findViewById(R.id.ripple_layout);
        return rootView;

    }

    @Override
    public void onPause() {
        mRipple.stopRippleAnimation();
        super.onPause();
    }

    @Override
    public void onResume() {
        mRipple.startRippleAnimation();
        super.onResume();
        boolean isByPhone = getArguments()!=null && getArguments().getBoolean(INTENT_INCOMMING_CALL_BY_PHONE_BOOLEAN, false);
        if(isByPhone){
            mCallWithWatchImageView.setVisibility(View.INVISIBLE);
            mCallWithPhoneImageView.setVisibility(View.VISIBLE);
        }else{
            mCallWithWatchImageView.setVisibility(View.VISIBLE);
            mCallWithPhoneImageView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_dial_in_accept) {
            getPresenter().onAnswer(getActivity());
        } else if (v.getId() == R.id.button_dial_in_deny) {
            getPresenter().onDecline();
        }
    }

    @Override
    public void showAnswerUi(boolean show) {
        if (show) {
            mRipple.startRippleAnimation();
            getView().setVisibility(View.VISIBLE);
        } else {
            mRipple.stopRippleAnimation();
            getView().setVisibility(View.GONE);
        }
    }

    @Override
    public void setName(String name) {
        mLabel.setText(name);
    }

    @Override
    public void setPhoto(Drawable photo) {
        mProfile.setImageDrawable(photo);
    }
}
