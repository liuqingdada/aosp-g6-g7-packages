package com.mstarc.wechat.wearwechat.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.mstarc.wechat.wearwechat.MessageHandleActivity;
import com.mstarc.wechat.wearwechat.R;
import com.mstarc.wechat.wearwechat.model.ChatMsgEntity;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.net.VolleySingleton;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by wangxinzhi on 17-3-10.
 */

public class SingleMessageFragment extends Fragment {
    ImageButton mVoiceButton;
    NetworkImageView mProfileView;
    TextView mToUserName;
    TextView mMsgTime;
    TextView mMsgDate;
    TextView mLatestMsgText;

    private ImageLoader imageLoader;

    private List<ChatMsgEntity> mDataArrays = new ArrayList();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.single_message, container, false);
        mVoiceButton = (ImageButton) rootView.findViewById(R.id.voice_button);
        mProfileView = (NetworkImageView) rootView.findViewById(R.id.msg_chat2_profile);
        mToUserName = (TextView) rootView.findViewById(R.id.msg_chat2_contact);
        mMsgTime = (TextView) rootView.findViewById(R.id.msg_chat2_time);
        mMsgDate = (TextView) rootView.findViewById(R.id.msg_chat1_date);
        mLatestMsgText = (TextView) rootView.findViewById(R.id.msg_chat2_content);
        mVoiceButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ((MessageHandleActivity) getActivity()).showFragment(MessageHandleActivity.FRAGMENT_INDEX_MESSAGE_VOICE, null);
            }
        });
        return rootView;
    }

    public void updateView(final ChatMsgEntity entity, final Token token) {
        mVoiceButton.post(new Runnable() {
            @Override
            public void run() {
                imageLoader = VolleySingleton.getInstance().getImageLoader(token.cookie);
            }
        });
    }
}
