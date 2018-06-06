package com.mstarc.wearablephone.view;

import android.app.Fragment;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mstarc.wearablephone.PhoneApplication;
import com.mstarc.wearablephone.R;
import com.mstarc.wearablephone.view.common.DecorationSettingItem;
import com.mstarc.wearablephone.view.common.RecyclerViewItemTouchListener;
import com.android.contacts.common.ContactPhotoManager;

import java.util.ArrayList;

/**
 * Created by wangxinzhi on 17-5-17.
 */

public class VoiceResultFragment extends ContractsFragment {
    private static final String TAG = "VoiceResultFragment";
    String mVoiceResult;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        int themeResID = ((PhoneApplication)getContext().getApplicationContext()).getThemeStyle();
        if(themeResID!=0){
            getContext().getTheme().applyStyle(themeResID, true);
        }
        View view =  LayoutInflater.from(getActivity()).inflate(R.layout.voice_result, container, false);
        mItems = new ArrayList<>();
        Bundle args = getArguments();
        mVoiceResult = args == null ? null : getArguments().getString("RESULT");
        mContactPhotoManager = ContactPhotoManager.getInstance(getActivity());
        mAdapter = new RecordAdapter(getActivity());
        mListView = (RecyclerView) view.findViewById(R.id.serach_result_list);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        mListView.addOnItemTouchListener(new RecyclerViewItemTouchListener(getActivity(), this));
        mNoMsg = view.findViewById(R.id.serach_no_msg);
        if(mNoMsg.getVisibility() == View.VISIBLE) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getActivity().finish();
                }
            },2000);
        }
        return view;
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri baseUri;
        baseUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        return new android.support.v4.content.CursorLoader(getActivity(), baseUri,
                PhoneQuery.PROJECTION, PhoneQuery.PROJECTION[PhoneQuery.DISPLAY_NAME] + " = ?", new String[]{mVoiceResult}, null);
    }

    @Override
    public void byPhone() {
        super.byPhone();
        getActivity().finish();
    }

    @Override
    public void byWatch() {
        super.byWatch();
        getActivity().finish();
    }
}
