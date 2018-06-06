package com.mstarc.wearablemms.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.mstarc.wearablemms.activity.AddContactAcivity;
import com.mstarc.wearablemms.MainActivity;
import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.activity.ChooseContactAcivity;
import com.mstarc.wearablemms.common.Constants;
import com.mstarc.wearablemms.common.KeyboardUtil;

/**
 * Created by wangxinzhi on 17-3-10.
 */

public class InputNumFragment extends Fragment implements KeyboardUtil.InputFinishedListener{
    private EditText mPhoneNum;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.message_input_num, container, false);
        mPhoneNum = (EditText)rootView.findViewById(R.id.msg_chat2_contact);
        KeyboardUtil keyboard = new KeyboardUtil(rootView, getActivity(), mPhoneNum);
        keyboard.showKeyboard();
        keyboard.registerListener(this);
        return rootView;
    }

    @Override
    public void getPhoneNum(String phoneNum) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.PHONE_NUM, phoneNum);
        //((AddContactAcivity) getActivity()).showFragment(MainActivity.FRAGMENT_ADD_NEW_MESSAGE, bundle);
        Intent intent = new Intent();
        intent.setClass(getActivity(), AddContactAcivity.class);
        intent.putExtras(bundle);
        getActivity().startActivity(intent);
    }
}
