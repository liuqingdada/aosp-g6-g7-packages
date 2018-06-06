package com.mstarc.wearablemms.fragment;

import android.app.Fragment;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.mstarc.wearablemms.activity.AddContactAcivity;
import com.mstarc.wearablemms.activity.BaseActivity;
import com.mstarc.wearablemms.activity.ChooseContactAcivity;
import com.mstarc.wearablemms.activity.VoiceActivity;
import com.mstarc.wearablemms.common.Constants;
import com.mstarc.wearablemms.common.DecorationSettingItem;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.data.Contact;
import com.mstarc.wearablemms.data.MessageData;
import com.mstarc.wearablemms.data.Utils;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by wangxinzhi on 17-3-10.
 */

public class AddMessageFragment extends Fragment {
    private ImageView mVoiceButton;
    private TextView mPhoneNumView;
    private String mContentName;
    private String mPhoneNum;
    public ImageView mPhone;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.message_new, container, false);
        mPhone = (ImageView)rootView.findViewById(R.id.msg_chat2_profile);
        ThemeUtils.updateImageView(mPhone,R.drawable.ic_mms_profile_send);
        mVoiceButton = (ImageView) rootView.findViewById(R.id.voice_button);
        ThemeUtils.updateImageView(mVoiceButton,R.drawable.ic_voice_1);
        mVoiceButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!Utils.isNetworkAvailable(getActivity())) {
                    Toast.makeText(getActivity(),getString(R.string.open_network),Toast.LENGTH_LONG).show();
                    return;
                }
                if(!TextUtils.isEmpty(mPhoneNumView.getText())) {
                    Intent intent = new Intent();
                    intent.putExtra(Constants.PHONE_NUM,mPhoneNum!=null?mPhoneNum:mPhoneNumView.getText());
                    intent.setClass(getActivity(),VoiceActivity.class);
                    startActivityForResult(intent, 2000);
                }else{
                    Toast.makeText(getActivity(),getString(R.string.input_num),Toast.LENGTH_LONG).show();
                }

            }
        });
        mPhoneNumView = (TextView)rootView.findViewById(R.id.msg_chat2_contact);
        mPhoneNumView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AddContactAcivity) getActivity()).showFragment(BaseActivity.FRAGMENT_INPUT_NUM_MESSAGE, null);
            }
        });
        rootView.findViewById(R.id.msg_chat2_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             //   Toast.makeText(getActivity(),"in contacts",Toast.LENGTH_SHORT).show();
                //((AddContactAcivity) getActivity()).showFragment(BaseActivity.FRAGMENT_CONTACTS_MESSAGE, null);
                Intent intent = new Intent();
                intent.setClass(getActivity(), ChooseContactAcivity.class);
                getActivity().startActivity(intent);
            }
        });
       // Bundle bundle = getArguments();
        Bundle bundle = getActivity().getIntent().getExtras();
        if(bundle != null) {
            mPhoneNum = bundle.getString(Constants.PHONE_NUM);
            mContentName = bundle.getString(Constants.CONTACT_NAME);
            mPhoneNumView.setText((null==mContentName)?mPhoneNum:mContentName);
            String url = bundle.getString(Constants.URL);
            if(url != null) {
                new PhotoDrawableTask().execute();
            }else{
                ThemeUtils.updateImageView(mPhone,R.drawable.ic_mms_profile_send);
            }
        }
        return rootView;
    }
    class ChatMessageData extends MessageData {
        boolean isOwn;

        public ChatMessageData(boolean isOwn, Contact mContact, String mContent, String mDate) {
            super(mContact, mContent, mDate,1);
            this.isOwn = isOwn;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 2000) {
            getActivity().finish();
        }
    }

    public Drawable getDrawable(Context context,String phoneNumber) {
        Uri uriNumber2Contacts = Uri.parse("content://com.android.contacts/"
                + "data/phones/filter/" + phoneNumber);
        Cursor cursorCantacts = context.getContentResolver().query(
                uriNumber2Contacts,
                null,
                null,
                null,
                null);
        Drawable drawable = null;
        if (cursorCantacts!=null) {
            if(cursorCantacts.moveToFirst()) {
                Long contactID = cursorCantacts.getLong(cursorCantacts.getColumnIndex("contact_id"));
                Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID);
                InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
                drawable =
                        RoundedBitmapDrawableFactory.create(getResources(), input);
                ((RoundedBitmapDrawable)drawable).setAntiAlias(true);
                ((RoundedBitmapDrawable)drawable).setCornerRadius(drawable.getIntrinsicWidth() / 2);
                cursorCantacts.close();
            }else{
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_mms_profile_list);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
            }
            cursorCantacts.close();
        }else{
            int color = ThemeUtils.getCurrentPrimaryColor();
            ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_mms_profile_list);
            drawable.clearColorFilter();
            drawable.mutate().setColorFilter(filter);
        }
        return drawable;
    }

    private class PhotoDrawableTask extends AsyncTask<Void, Void, Drawable> {

        @Override
        protected Drawable doInBackground(Void... voids) {
            return getDrawable(getActivity(),mPhoneNum);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            super.onPostExecute(drawable);
            mPhone.setImageDrawable(drawable);
        }

    }
}
