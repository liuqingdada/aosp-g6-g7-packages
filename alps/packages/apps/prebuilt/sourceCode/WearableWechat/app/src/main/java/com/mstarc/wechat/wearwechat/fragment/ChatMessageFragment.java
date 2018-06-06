package com.mstarc.wechat.wearwechat.fragment;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mstarc.wechat.wearwechat.LaunchActivity;
import com.mstarc.wechat.wearwechat.MessageHandleActivity;
import com.mstarc.wechat.wearwechat.R;
import com.mstarc.wechat.wearwechat.ThemeUtils;
import com.mstarc.wechat.wearwechat.adapter.ChatMsgViewAdapter;
import com.mstarc.wechat.wearwechat.common.DecorationSettingItem;
import com.mstarc.wechat.wearwechat.common.SwipeDismissLayout;
import com.mstarc.wechat.wearwechat.dao.MessageManager;
import com.mstarc.wechat.wearwechat.model.ChatMsgEntity;
import com.mstarc.wechat.wearwechat.model.Msg;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.model.User;
import com.mstarc.wechat.wearwechat.net.CookieRequest;
import com.mstarc.wechat.wearwechat.net.VolleySingleton;
import com.mstarc.wechat.wearwechat.protocol.MsgRequest;
import com.mstarc.wechat.wearwechat.protocol.MsgResponse;
import com.mstarc.wechat.wearwechat.utils.NetUtil;
import com.mstarc.wechat.wearwechat.utils.StringUtil;
import com.mstarc.wechat.wearwechat.utils.TimeUtil;
import com.mstarc.wechat.wearwechat.utils.WxHome;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by wangxinzhi on 17-3-8.
 */

public class ChatMessageFragment extends Fragment implements View.OnClickListener {
    RecyclerView mListView;
    ChatMsgViewAdapter mAdapter;
    private TextView mHeaderView;
    private LinearLayout mNoNetworkLayout;

    private RequestQueue mQueue;
    private User fromUser = new User();
    private User toUser = new User();
    private MsgReceiver msgReceiver;
    private Token token;
    private List<ChatMsgEntity> mDataArrays = new ArrayList<>();

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor m2Editor;

    @Override
    public void onResume() {
        super.onResume();
        if (msgReceiver == null) {
            msgReceiver = new MsgReceiver();
        }
        IntentFilter localIntentFilter = new IntentFilter();
        localIntentFilter.addAction("com.g6.action.wx.new_msg");
        getActivity().registerReceiver(msgReceiver, localIntentFilter);
        mListView.scrollToPosition(mDataArrays.size() - 1);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (msgReceiver != null) {
            getActivity().unregisterReceiver(msgReceiver);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("dingyichen", "onCreate");
        if (getArguments() != null) {
            Bundle bundle = getArguments();
            Boolean sendMsgConfirm = bundle.getBoolean("send_confirm");
            String msgText = bundle.getString("voice_content");
            if(sendMsgConfirm
                    && (msgText != null && !msgText.isEmpty())) {
                send(msgText);
            }
        }
        init();
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SwipeDismissLayout rootView = (SwipeDismissLayout) inflater.inflate(R.layout.message_chat, container, false);
        mListView = (RecyclerView) rootView.findViewById(R.id.list);
        mHeaderView = (TextView) rootView.findViewById(R.id.head_name);
        mNoNetworkLayout = (LinearLayout) rootView.findViewById(R.id.no_network_layout);
        Log.d("TAG", "onCreateView");

        mAdapter = new ChatMsgViewAdapter(getActivity(), mDataArrays, token);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        ImageButton voicebtn = (ImageButton) rootView.findViewById(R.id.voice_button);
        voicebtn.setOnClickListener(this);
        updateImageView(voicebtn, R.mipmap.icon_wechat_voice_1);
        mHeaderView.setTextColor(ThemeUtils.getCurrentPrimaryColor());

        rootView.setOnSwipeProgressChangedListener(new SwipeDismissLayout.OnSwipeProgressChangedListener() {

            @Override
            public void onSwipeProgressChanged(SwipeDismissLayout layout, float progress, float translate) {
                Log.d("dingyichen" ,"chat message fragment onswipeprogresschanged");
                if (progress > 0.5) {
                    getActivity().finish();
                    //((MessageHandleActivity) getActivity()).showFragment(MessageHandleActivity.FRAGMENT_INDEX_SINGLE_MESSAGE, null);
                }

            }

            @Override
            public void onSwipeCancelled(SwipeDismissLayout layout) {

            }
        });
        showHeadName(toUser.NickName);
        mPrefs = getActivity().getSharedPreferences("isLogin", Activity.MODE_PRIVATE);
        m2Editor = mPrefs.edit();

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (!isNetworkAvailable(v.getContext())) {
            mHandler.removeMessages(MSG_NO_NETWORK_DISMISS);
            mNoNetworkLayout.setVisibility(View.VISIBLE);
            mHandler.sendEmptyMessageDelayed(MSG_NO_NETWORK_DISMISS, 2000);
        } else {
            ((MessageHandleActivity) getActivity()).showFragment(MessageHandleActivity.FRAGMENT_INDEX_MESSAGE_VOICE, null);
        }
    }

    private void showHeadName(String paramString) {
        Log.d("TAG", "paramstring = " + paramString);
        mHeaderView.setText(StringUtil.filterHtml(paramString));
    }

    /**
     * 判断网络是否可用
     */
    public static boolean isNetworkAvailable(Context context) {
        // 获取网络连接管理器
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 获取当前网络状态信息
        NetworkInfo[] info = mgr.getAllNetworkInfo();
        if (info != null) {
            for (int i = 0; i < info.length; i++) {
                if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }

        return false;
    }

    private void init() {
            mQueue = VolleySingleton.getInstance().getRequestQueue();
            token = new Token();
            token.fromBundle(getActivity().getIntent().getBundleExtra("token"));
            fromUser = new User();
            toUser = new User();
            fromUser.fromBundle(getActivity().getIntent().getBundleExtra("from"));
            toUser.fromBundle(getActivity().getIntent().getBundleExtra("to"));
            Log.d("TAG", "ChatMessageFragment_onCreate:token=" + JSON.toJSONString(token) + " from=" + JSON.toJSONString(fromUser) + " to=" + JSON.toJSONString(toUser));
            //initYuYin();
            initData();
    }


    public void initData() {
        mDataArrays.clear();
        List localList = new MessageManager(getActivity()).getMsg(toUser.UserName);
        Log.d("TAG", "ChatMessageFragment_initData:search messages from db messages = " + JSON.toJSONString(localList));
        Iterator localIterator = localList.iterator();
        while (localIterator.hasNext()) {
            Msg localMsg = (Msg) localIterator.next();
            ChatMsgEntity localChatMsgEntity = new ChatMsgEntity();

            localChatMsgEntity.setText(localMsg.Content);
            if (toUser.UserName.equals(localMsg.FromUserName)) {
                localChatMsgEntity.setMsgType(true);
                localChatMsgEntity.setUserName(toUser.UserName);
                localChatMsgEntity.setNickName(toUser.NickName);
                if (WxHome.isGroupUserName(localMsg.FromUserName)) {
                    localChatMsgEntity.setMemberUserName(localMsg.fromMemberUserName);
                    localChatMsgEntity.setMemberNickName(localMsg.fromMemberNickName);
                }
            } else {
                localChatMsgEntity.setMsgType(false);
                localChatMsgEntity.setUserName(fromUser.UserName);
                localChatMsgEntity.setNickName(fromUser.NickName);
            }

            localChatMsgEntity.setDate(TimeUtil.timeToStr(localMsg.CreateTime));
            if (localMsg.MsgType == 34) {
                long l = TimeUtil.toCeilSecondsFromMillis(localMsg.VoiceLength);
                localChatMsgEntity.setTime(l + "\"");
            }
            mDataArrays.add(localChatMsgEntity);
        }
        Log.d("TAG", "ChatMessageFragment_mDataArrays_.size===" + mDataArrays.size() + "====CHAT_mDataArrays===" + mDataArrays);
    }


    public class MsgReceiver extends BroadcastReceiver {
        public MsgReceiver() {
        }

        public void onReceive(Context paramContext, Intent paramIntent) {
            if (!"com.g6.action.wx.new_msg".equals(paramIntent.getAction())) ;
            Msg localMsg = new Msg();
            localMsg.fromBundle(paramIntent.getExtras());
            Log.d("TAG", "MsgReceiver::receive broadcast msgType=" + localMsg.MsgType);
            Log.d("TAG", "MsgReceiver::msg fromusername=" + localMsg.FromUserName + " chat tousername=" + localMsg.ToUserName);
            //单聊 正好在这个会话里面
            if ((localMsg.ToUserName.equals(fromUser.UserName)) && (localMsg.FromUserName.equals(toUser.UserName))) {
                ChatMsgEntity localChatMsgEntity2 = new ChatMsgEntity();
                if ((localMsg.MsgType == 1) || (localMsg.MsgType == 34)) {
                    localChatMsgEntity2.setUserName(localMsg.FromUserName);
                    localChatMsgEntity2.setNickName(localMsg.fromNickName);
                    localChatMsgEntity2.setMemberUserName(localMsg.fromMemberUserName);
                    localChatMsgEntity2.setMemberNickName(localMsg.fromMemberNickName);
                    localChatMsgEntity2.setDate(TimeUtil.getDate());
                    localChatMsgEntity2.setMsgType(true);
                    if (localMsg.MsgType == 1) {
                        localChatMsgEntity2.setText(localMsg.Content);
                    }
                    if (localMsg.MsgType == 34) {
                        long l2 = TimeUtil.toCeilSecondsFromMillis(localMsg.VoiceLength);
                        localChatMsgEntity2.setTime(l2 + "\"");
                        localChatMsgEntity2.setText(localMsg.MsgId + ".mp3");
                    }
                    mDataArrays.add(localChatMsgEntity2);
                    mAdapter.notifyDataSetChanged();
                    mListView.scrollToPosition(-1 + mDataArrays.size());
//                    mListView.setSelection(-1 + mListView.getCount());
                }

            }

            //群聊?
            if ((localMsg.FromUserName.equals(fromUser.UserName)) || (localMsg.ToUserName.equals(toUser.UserName))) {
                ChatMsgEntity localChatMsgEntity1 = new ChatMsgEntity();
                if ((localMsg.MsgType == 1) || (localMsg.MsgType == 34)) {
                    localChatMsgEntity1.setUserName(localMsg.ToUserName);
                    localChatMsgEntity1.setNickName(localMsg.toNickName);
                    localChatMsgEntity1.setDate(TimeUtil.getDate());
                    localChatMsgEntity1.setMsgType(false);
                    if (localMsg.MsgType == 1) {
                        localChatMsgEntity1.setText(localMsg.Content);
                    }
                    if (localMsg.MsgType == 34) {
                        long l1 = TimeUtil.toCeilSecondsFromMillis(localMsg.VoiceLength);
                        localChatMsgEntity1.setTime(l1 + "\"");
                        localChatMsgEntity1.setText(localMsg.MsgId + ".mp3");
                    }
                    mDataArrays.add(localChatMsgEntity1);
                    mAdapter.notifyDataSetChanged();
                    mListView.scrollToPosition(-1 + mDataArrays.size());
                    //mListView.setSelection(-1 + mListView.getCount());
                }

            }

        }
    }



    //1.界面显示
    private void send(String str) {
        String paramString = StringFilter(ToDBC(str));
        ChatMsgEntity localChatMsgEntity = new ChatMsgEntity();
        localChatMsgEntity.setDate(TimeUtil.getDate());
        localChatMsgEntity.setUserName(fromUser.UserName);
        localChatMsgEntity.setNickName(fromUser.NickName);
        localChatMsgEntity.setMsgType(false);
        localChatMsgEntity.setText(paramString);
        sendTextMsg(paramString);
        mDataArrays.add(localChatMsgEntity);
        mAdapter.notifyDataSetChanged();
        mListView.scrollToPosition(-1 + mDataArrays.size());
    }

    /**
     * @param str String类型
     * @return String
     * @Description 替换、过滤特殊字符
     */
    public static String StringFilter(String str) throws PatternSyntaxException {
        str = str.replaceAll(" ", "").replaceAll(" ", "").replaceAll("：", ":").replaceAll("：", "：").replaceAll("【", "[").replaceAll("】", "]").replaceAll("！", "!");//替换中文标号
        String regEx = "[『』]"; // 清除掉特殊字符
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    /**
     * 半角转换为全角
     *
     * @param input
     * @return
     */
    public static String ToDBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == 12288) {
                c[i] = (char) 32;
                continue;
            }
            if (c[i] > 65280 && c[i] < 65375)
                c[i] = (char) (c[i] - 65248);
        }
        return new String(c);
    }

    //2.设置参数,并且存储
    private void sendTextMsg(String paramString) {
        Msg localMsg = new Msg();
        localMsg.FromUserName = fromUser.UserName;
        localMsg.ToUserName = toUser.UserName;
        localMsg.Content = paramString;
        localMsg.Type = 1;
        localMsg.ClientMsgId = WxHome.randomClientMsgId();
        localMsg.LocalID = localMsg.ClientMsgId;
        sendMsg(localMsg);
        localMsg.MsgType = 1;
        localMsg.fromNickName = fromUser.NickName;
        new MessageManager(getActivity()).insertMessage(localMsg);
    }

    //3.真正发送该消息
    private void sendMsg(final Msg paramMsg) {
        MsgRequest localMsgRequest = WxHome.formMsgRequest(token, paramMsg);
        CookieRequest localCookieRequest = new CookieRequest(1, WxHome.getSendUrl(token), JSON.toJSONString(localMsgRequest), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                Log.d("TAG", "chat_sendMsg:" + response.toString());
                MsgResponse localMsgResponse = JSON.parseObject(response.toString(), MsgResponse.class);
                if (localMsgResponse.BaseResponse.Ret == 0) {
                    Toast.makeText(getActivity(), "发送成功!", Toast.LENGTH_SHORT).show();
                    broadcastMsg(paramMsg);
                }
                if (localMsgResponse.BaseResponse.Ret == 1101) {
                    // Toast.makeText(ChatActivity.this, "手机端强制下线或其他设备登录Web微信", Toast.LENGTH_SHORT).show();
                    clearFiles();
                    if (!NetUtil.isApplicationBroughtToBackground(getActivity())) {
                        showDialog("已退出");
                    }

                }
            }
        }
                , new Response.ErrorListener() {
            public void onErrorResponse(VolleyError paramAnonymousVolleyError) {
                Log.e("TAG", "chat_sendMsg:error " + paramAnonymousVolleyError.getMessage(), paramAnonymousVolleyError);
            }
        });
        localCookieRequest.setCookie(token.cookie);
        mQueue.add(localCookieRequest);
    }

    private void broadcastMsg(Msg paramMsg) {
        Intent localIntent = new Intent("com.g6.action.wx.add_item");
        localIntent.putExtras(paramMsg.toBundle());
        getActivity().sendBroadcast(localIntent);
    }

    private void clearFiles() {
        File localFile = new File(Environment.getExternalStorageDirectory() + "/weixinQingliao/");
        if (localFile.isDirectory()) {
            String[] arrayOfString = localFile.list();
            for (int i = 0; i < arrayOfString.length; i++)
                new File(localFile, arrayOfString[i]).delete();
        }
    }

    protected void showDialog(String str) {
        m2Editor.putBoolean("islogin", false).commit();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("提示");
        builder.setMessage(str + ",是否重新登录?");

        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //HomeActivity.context.finish();
                Intent intent = new Intent(getActivity(), LaunchActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //HomeActivity.context.finish();
                getActivity().finish();
            }
        });

        //  builder.create().show();
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private static final int MSG_NO_NETWORK_DISMISS = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NO_NETWORK_DISMISS:
                    mNoNetworkLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            mNoNetworkLayout.setVisibility(View.GONE);
                        }
                    });
                    break;
            }
        }
    };
}
