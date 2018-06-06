package com.mstarc.wearablemms.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.fragment.AddMessageFragment;
import com.mstarc.wearablemms.fragment.ChatMessageFragment;
import com.mstarc.wearablemms.fragment.ContactsFragment;
import com.mstarc.wearablemms.fragment.ConversationFragment;
import com.mstarc.wearablemms.fragment.InputNumFragment;
import com.mstarc.wearablemms.fragment.JunkMmsFragment;
import com.mstarc.wearablemms.fragment.SingleMessageFragment;
import com.mstarc.wearablemms.fragment.VoiceRecognizeFragment;

import java.util.ArrayList;

public class BaseActivity extends Activity {

    public static final int FRAGMENT_INDEX_MESSAGE_LISTS = 0;
    public static final int FRAGMENT_INDEX_SINGLE_MESSAGE = 1;
    public static final int FRAGMENT_INDEX_MESSAGE_VOICE = 2;
    public static final int FRAGMENT_INDEX_CHAT_MESSAGE = 3;
    public static final int FRAGMENT_ADD_NEW_MESSAGE = 4;
    public static final int FRAGMENT_INPUT_NUM_MESSAGE = 5;
    public static final int FRAGMENT_CONTACTS_MESSAGE = 6;
    public static final int FRAGMENT_JUNK_MESSAGE = 7;
    ArrayList<Fragment> mFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public BaseActivity() {
        mFragments = new ArrayList<Fragment>();
//        mFragments.add(new MessagesFragment());
        mFragments.add(new ConversationFragment());
        mFragments.add(new SingleMessageFragment());
        mFragments.add(new VoiceRecognizeFragment());
        mFragments.add(new ChatMessageFragment());
        mFragments.add(new AddMessageFragment());
        mFragments.add(new InputNumFragment());
        mFragments.add(new ContactsFragment());
        mFragments.add(new JunkMmsFragment());
    }

    public void showFragment(int index, Bundle bundle) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = mFragments.get(index);
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}
