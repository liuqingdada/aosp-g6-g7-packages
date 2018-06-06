package com.mstarc.wearablemms.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.activity.VoiceActivity;
import com.mstarc.wearablemms.common.Constants;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.data.Utils;

/**
 * Created by wangxinzhi on 17-3-10.
 */

public class SingleMessageFragment extends Fragment {
    ImageView mVoiceButton;
    private String mPhoneNum;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        mPhoneNum = bundle.getString(Constants.PHONE_NUM);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.single_message, container, false);
        TextView name = (TextView)rootView.findViewById(R.id.msg_chat2_contact);
        name.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        name.setText((TextUtils.isEmpty(bundle.getString(Constants.CONTACT_NAME)) || bundle.getString(Constants.CONTACT_NAME).equals("0")) ? mPhoneNum : bundle.getString(Constants.CONTACT_NAME));
        TextView date = (TextView)rootView.findViewById(R.id.msg_chat1_date);
        date.setText(bundle.getString(Constants.DATE));
        ThemeUtils.updateImageView((ImageView) rootView.findViewById(R.id.msg_chat2_profile),R.drawable.ic_mms_profile_send);
        mVoiceButton = (ImageView) rootView.findViewById(R.id.voice_button);
        ThemeUtils.updateImageView(mVoiceButton,R.drawable.ic_voice_1);
        mVoiceButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!Utils.isNetworkAvailable(getActivity())) {
                    Toast.makeText(getActivity(),getString(R.string.open_network),Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra(Constants.PHONE_NUM,mPhoneNum);
                intent.setClass(getActivity(),VoiceActivity.class);
                startActivity(intent);
            }
        });
        return rootView;
    }
}
