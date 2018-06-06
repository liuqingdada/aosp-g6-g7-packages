package com.mstarc.wechat.wearwechat;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.android.volley.RequestQueue;
import com.mstarc.wechat.wearwechat.common.SwipeDismissLayout;
import com.mstarc.wechat.wearwechat.fragment.ChatMessageFragment;
import com.mstarc.wechat.wearwechat.fragment.VoiceRecognizeFragment;
import com.mstarc.wechat.wearwechat.model.ChatMsgEntity;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.model.User;

import java.util.ArrayList;
import java.util.List;

public class MessageHandleActivity extends Activity {

    //public static final int FRAGMENT_INDEX_SINGLE_MESSAGE = 0;
    public static final int FRAGMENT_INDEX_MESSAGE_VOICE = 1;
    public static final int FRAGMENT_INDEX_CHAT_MESSAGE = 0;
    ArrayList<Fragment> mFragments;


    private SharedPreferences prefs;
    private SharedPreferences.Editor mEditor;
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor m2Editor;
    private RequestQueue mQueue;
    private User fromUser = new User();
    private User toUser = new User();
    private Token token;
    private List<ChatMsgEntity> mDataArrays = new ArrayList();

    //private SingleMessageFragment mSingleMsgFragment;
    private VoiceRecognizeFragment mVoiceFragment;
    private ChatMessageFragment mChatMsgFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = getSharedPreferences("isLogin", Activity.MODE_PRIVATE);

        if (mPrefs.getBoolean("islogin", false)) {
            // 已登录

        } else {
            // 未登录
            startActivity(new Intent(this, LaunchActivity.class));
        }

        setContentView(R.layout.activity_message_handle);

        Intent intent = getIntent();
        String contactname = intent.getStringExtra("CHATS_CONTACT");
        // TODO

        init();
        mFragments = new ArrayList<Fragment>();
        //mSingleMsgFragment = new SingleMessageFragment();
        mVoiceFragment = new VoiceRecognizeFragment();
        mChatMsgFragment = new ChatMessageFragment();
        //mFragments.add(mSingleMsgFragment);

//        Bundle localBundle = new Bundle();
//        localBundle.putBundle("token", token.toBundle());
//        localBundle.putBundle("to", toUser.toBundle());
//        localBundle.putBundle("from", fromUser.toBundle());
//        mChatMsgFragment.setArguments(localBundle);

        mFragments.add(mChatMsgFragment);
        mFragments.add(mVoiceFragment);
        showFragment(FRAGMENT_INDEX_CHAT_MESSAGE, null);

        SwipeDismissLayout rootView = (SwipeDismissLayout) findViewById(R.id.fragment_container);
        rootView.setOnSwipeProgressChangedListener(new SwipeDismissLayout.OnSwipeProgressChangedListener() {

            @Override
            public void onSwipeProgressChanged(SwipeDismissLayout layout, float progress, float translate) {
                if (progress > 0.5) {
                    finish();
                }
            }

            @Override
            public void onSwipeCancelled(SwipeDismissLayout layout) {

            }
        });
    }

    private void init() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        prefs = getSharedPreferences("name", Activity.MODE_PRIVATE);
        mEditor = prefs.edit();

        //mPrefs = getSharedPreferences("isLogin", Activity.MODE_PRIVATE);
        m2Editor = mPrefs.edit();
//        initView();
//        Intent localIntent = getIntent();
//        token = new Token();
//        token.fromBundle(localIntent.getBundleExtra("token"));
//        fromUser.fromBundle(localIntent.getBundleExtra("from"));
//        toUser.fromBundle(localIntent.getBundleExtra("to"));
//        Log.d("TAG", "MessageHandleActivity_onCreate:token=" + JSON.toJSONString(token) + " from=" + JSON.toJSONString(fromUser) + " to=" + JSON.toJSONString(toUser));
        //showHeadName(toUser.NickName);
        mEditor.putString("name", toUser.NickName).commit();
        //initYuYin();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        super.onDestroy();
        mEditor.putString("name", "").commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showFragment(FRAGMENT_INDEX_CHAT_MESSAGE, null);
    }

    public void showFragment(int index, Bundle bundle) {
        Log.d("dingyichen", "show fragment : " + index);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = mFragments.get(index);
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}
