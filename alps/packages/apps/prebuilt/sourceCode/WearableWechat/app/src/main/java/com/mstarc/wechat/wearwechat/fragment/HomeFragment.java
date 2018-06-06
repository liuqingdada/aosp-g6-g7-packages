package com.mstarc.wechat.wearwechat.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.mstarc.wechat.wearwechat.MessageHandleActivity;
import com.mstarc.wechat.wearwechat.R;
import com.mstarc.wechat.wearwechat.ThemeUtils;
import com.mstarc.wechat.wearwechat.common.DecorationSettingItem;
import com.mstarc.wechat.wearwechat.common.RecyclerViewItemTouchListener;
import com.mstarc.wechat.wearwechat.dao.MessageManager;
import com.mstarc.wechat.wearwechat.model.Contact;
import com.mstarc.wechat.wearwechat.model.Msg;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.model.User;
import com.mstarc.wechat.wearwechat.net.VolleySingleton;
import com.mstarc.wechat.wearwechat.utils.StringUtil;
import com.mstarc.wechat.wearwechat.utils.TimeUtil;
import com.mstarc.wechat.wearwechat.utils.WxHome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements RecyclerViewItemTouchListener.OnItemClickEventListener {

    RecyclerView mListView;
    ChatsAdapter mAdapter;
    final UiHandler mHandler = new UiHandler();

    private ArrayList<Contact> initList;
    private ListView listView;
    private List<HashMap<String, Object>> mData;
    private Token token;
    private User user;
    private AddReceiver mAddReceiver;
    private ArrayList<Contact> contactList;
    private ArrayList<Contact> exContactList;


    public HomeFragment() {
        // Required empty public constructor
    }
    public static HomeFragment newInstance(Token paramToken, User paramUser, ArrayList<Contact> paramArrayList, ArrayList<Contact> contactList, ArrayList<Contact> exContactList) {
        HomeFragment localInitFragment = new HomeFragment();
        Bundle localBundle = new Bundle();
        localBundle.putBundle("token", paramToken.toBundle());
        localBundle.putBundle("user", paramUser.toBundle());
        localBundle.putParcelableArrayList("init", paramArrayList);
        localBundle.putParcelableArrayList("contacts", contactList);
        localBundle.putParcelableArrayList("excontacts", exContactList);
        localInitFragment.setArguments(localBundle);
        return localInitFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("dingyichen", "HomeFragment on onCreate!!!");
        if (getArguments() != null) {
            this.token = new Token();
            this.token.fromBundle(getArguments().getBundle("token"));
            this.user = new User();
            this.user.fromBundle(getArguments().getBundle("user"));
            this.initList = getArguments().getParcelableArrayList("init");
            this.contactList = getArguments().getParcelableArrayList("contacts");
            this.exContactList = getArguments().getParcelableArrayList("excontacts");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SyncData();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("dingyichen", "HomeFragment on onDetach!!!");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("dingyichen", "HomeFragment on CreateView!!!");
        mData = getData(this.initList);
        View rootView = inflater.inflate(R.layout.fragment_chats, container, false);

        // Inflate the layout for this fragment
        mListView = (RecyclerView) rootView.findViewById(R.id.chats_list);
        mAdapter = new ChatsAdapter(getActivity());
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        mListView.addOnItemTouchListener(new RecyclerViewItemTouchListener(getActivity(), this));

        return rootView;

    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    class ChatsAdapter extends RecyclerView.Adapter<MessageListItemVH> {
        private LayoutInflater mInflater;

        public ChatsAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }


        @Override
        public MessageListItemVH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MessageListItemVH(mInflater.inflate(R.layout.message_list_item, null));
        }

        @Override
        public void onBindViewHolder(MessageListItemVH holder, int position) {
            Log.d("TAG", "适配器中HashMap-----" + ((HashMap) mData.get(position)));
            String title = StringUtil.filterHtml(((HashMap) mData.get(position)).get("title").toString());
            String str = ((HashMap) mData.get(position)).get("img").toString();
            ImageLoader imageLoader = VolleySingleton.getInstance().getImageLoader(token.cookie);
            holder.mName.setText(title);
            holder.mContent.setText(((HashMap) mData.get(position)).get("info").toString());
            holder.mDate.setText(((HashMap) mData.get(position)).get("time").toString());
            holder.mProfile.setImageUrl(str, imageLoader);
            updateImageView(holder.mProfile, R.mipmap.icon_wechat_avata_list);
        }

        @Override
        public int getItemCount() {
            int size = mData.size();
            if(size == 0){
                mHandler.dispatchMessage(Message.obtain(mHandler, UiHandler.MSG_REMOVE_SHOW_NO_MESSAGE));
            } else{
                mHandler.dispatchMessage(Message.obtain(mHandler, UiHandler.MSG_REMOVE_HIDE_NO_MESSAGE));
            }
            return mData.size();
        }
    }

    class MessageListItemVH extends RecyclerView.ViewHolder {
        NetworkImageView mProfile;
        TextView mName, mDate, mContent;

        public MessageListItemVH(View itemView) {
            super(itemView);
            mProfile = (NetworkImageView) itemView.findViewById(R.id.msg_list_profile);
            mName = (TextView) itemView.findViewById(R.id.msg_contact);
            mDate = (TextView) itemView.findViewById(R.id.msg_time);
            mContent = (TextView) itemView.findViewById(R.id.msg_content);
            updateImageView(mProfile, R.mipmap.icon_wechat_avata_list);
            mName.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        }
    }

    class UiHandler extends Handler {
        public static final int MSG_REMOVE_ALL = 0;
        public static final int MSG_REMOVE_SHOW_NO_MESSAGE = 1;
        public static final int MSG_REMOVE_HIDE_NO_MESSAGE = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REMOVE_ALL:
                    mData.clear();
                    mAdapter.notifyDataSetChanged();
                    break;
                case MSG_REMOVE_SHOW_NO_MESSAGE:
                    break;
                case MSG_REMOVE_HIDE_NO_MESSAGE:
            }
        }
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Intent localIntent = new Intent(getActivity(), MessageHandleActivity.class);
        localIntent.putExtra("token", token.toBundle());
        User localUser = new User();
        localUser.UserName = ((HashMap) mData.get(adapterPosition)).get("userName").toString();
        localUser.NickName = ((HashMap) mData.get(adapterPosition)).get("title").toString();
        localUser.HeadImgUrl = ((HashMap) mData.get(adapterPosition)).get("img").toString();
        localIntent.putExtra("to", localUser.toBundle());
        localIntent.putExtra("from", user.toBundle());
        //((HashMap) mData.get(adapterPosition)).put("info", mData.get(adapterPosition).get("info").toString());
        //((HashMap) mData.get(adapterPosition)).put("time", mData.get(adapterPosition).get("time").toString());
        mAdapter.notifyDataSetChanged();
        startActivity(localIntent);
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    public void comeNewMessage(Msg paramMsg) {
        if (this.token == null) {
            Log.d("TAG", "InitFragment_comeNewMessage:Initfragment not created yet");
            return;
        }
        Log.d("TAG", "InitFragment_comeNewMessage_Msg:" + paramMsg);
        String str = "";
        if (paramMsg.MsgType == 34) {
            str = "[语音]";
        } else if (paramMsg.MsgType == 1) {
            str = paramMsg.Content;
        }

        HashMap localHashMap2 = new HashMap();
        if (paramMsg.FromUserName.equals(this.user.UserName)) {
            if (paramMsg.ToUserName.equals(this.user.UserName)) {

            } else {
                HashMap localHashMap1;
                boolean a = false;
                for (int i = 0; i < this.mData.size(); i++) {
                    localHashMap1 = (HashMap) this.mData.get(i);
                    if (paramMsg.ToUserName.equals(localHashMap1.get("userName"))) {

//                        if (WxHome.isGroupUserName(paramMsg.ToUserName)) {
//                            //localHashMap1.put("img", WxHome.getIconUrlByUsername(this.token, paramMsg.ToUserName));
//                            localHashMap1.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.ToUserName));
//                        } else {
//                            localHashMap1.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.ToUserName));
//                        }
                        a = true;
                        localHashMap1.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.ToUserName));
                        localHashMap1.put("time", TimeUtil.getDate());
                        localHashMap1.put("info", str);
                        localHashMap1.put("title", paramMsg.toNickName);
                        //  localHashMap1.put("title", localHashMap1.get("title"));

                        localHashMap1.put("userName", paramMsg.ToUserName);
                        Log.d("TAG", "自己发的交换位置");
                        Collections.swap(this.mData, 0, i);
                        mAdapter.notifyDataSetChanged();
                        new MessageManager(getActivity()).insertMessage(paramMsg);
                        break;
                    }
                }
                if (!a) {
                    //  if (WxHome.isGroupUserName(paramMsg.ToUserName)) {
//                            //localHashMap2.put("img", WxHome.getIconUrlByUsername(this.token, paramMsg.ToUserName));
//                            localHashMap2.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.ToUserName));
//                        } else {
//                            localHashMap2.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.ToUserName));
//                        }
                    localHashMap2.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.ToUserName));
                    localHashMap2.put("time", TimeUtil.getDate());
                    localHashMap2.put("info", str);
                    localHashMap2.put("title", paramMsg.toNickName);
                    //  localHashMap2.put("title", paramMsg.fromNickName);
                    localHashMap2.put("userName", paramMsg.ToUserName);
                    this.mData.add(0, localHashMap2);
                    Log.d("TAG", "自己发的添加列表");
                    mAdapter.notifyDataSetChanged();
                    new MessageManager(getActivity()).insertMessage(paramMsg);
                    //     }


                }
            }
        } else {
            HashMap localHashMap1;
            boolean b = false;
            for (int i = 0; i < this.mData.size(); i++) {
                localHashMap1 = (HashMap) this.mData.get(i);
                if (paramMsg.FromUserName.equals(localHashMap1.get("userName"))) {
//                    if (WxHome.isGroupUserName(paramMsg.FromUserName)) {
//                       // localHashMap1.put("img", WxHome.getIconUrlByUsername(this.token, paramMsg.FromUserName));
//                        localHashMap1.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.FromUserName));
//                    } else {
//                        localHashMap1.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.FromUserName));
//                    }
                    b = true;
                    localHashMap1.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.FromUserName));
                    localHashMap1.put("info", str);
                    localHashMap1.put("time", TimeUtil.getDate());
                    localHashMap1.put("title", paramMsg.fromNickName);
                    //todo 这里原来有个错误,没有发现
                    localHashMap1.put("userName", paramMsg.FromUserName);

                    Collections.swap(this.mData, 0, i);
                    Log.d("TAG", "别人发的交换位置");
                    mAdapter.notifyDataSetChanged();
                    new MessageManager(getActivity()).insertMessage(paramMsg);
                    break;
                }
            }

            if (!b) {
                //                    if (WxHome.isGroupUserName(paramMsg.FromUserName)) {
//                       // localHashMap2.put("img", WxHome.getIconUrlByUsername(this.token, paramMsg.FromUserName));
//                        localHashMap2.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.FromUserName));
//                    } else {
//                        localHashMap2.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.FromUserName));
//                    }
                localHashMap2.put("img", WxHome.getHeadUrlByUsername(this.token, paramMsg.FromUserName));
                localHashMap2.put("info", str);
                localHashMap2.put("time", TimeUtil.getDate());
                localHashMap2.put("title", paramMsg.fromNickName);
                localHashMap2.put("userName", paramMsg.FromUserName);
                this.mData.add(0, localHashMap2);
                Log.d("TAG", "别人发的添加列表");
                mAdapter.notifyDataSetChanged();
                new MessageManager(getActivity()).insertMessage(paramMsg);

            }

        }

    }

    private List<HashMap<String, Object>> getData(List<Contact> paramList) {
        ArrayList localArrayList = new ArrayList();
        localArrayList.clear();
        Log.d("dingyichen", "HomeFragment getData!!!");
        Iterator localIterator = paramList.iterator();
        while (localIterator.hasNext()) {
            Contact localContact = (Contact) localIterator.next();
            HashMap localHashMap = new HashMap();
            localHashMap.put("title", localContact.getShowName());
            localHashMap.put("time", "");
            localHashMap.put("info", "");
            localHashMap.put("img", WxHome.getHeadImgUrl(localContact.HeadImgUrl));
            localHashMap.put("userName", localContact.UserName);
            Log.d("TAG", "传过来的ArrayList得userName==" + localContact.UserName);

            List localList = new MessageManager(getActivity()).getMsg(localContact.UserName);
            Log.d("dingyichen", "HomeFragment getData local list size: " + localList.size());
            if (localList.size() > 0) {
                Msg localMsg = (Msg) localList.get(localList.size() - 1);
                Log.d("dingyichen", "HomeFragment localMsg: " + localMsg.Content);
                localHashMap.put("time", TimeUtil.timeToStr(localMsg.CreateTime));
                localHashMap.put("info", localMsg.Content);
            }

            localArrayList.add(localHashMap);
        }
        return localArrayList;
    }

    private void SyncData() {
        Log.d("dingyichen", "HomeFragment SyncData ");
        if (mData.size() > 0) {
            for (int i = 0; i < mData.size(); i++) {
                HashMap localHashMap = mData.get(i);
                String userName = (String) localHashMap.get("userName");
                List localList = new MessageManager(getActivity()).getMsg(userName);
                if (localList.size() > 0) {
                    Msg localMsg = (Msg) localList.get(localList.size() - 1);
                    Log.d("dingyichen", "HomeFragment localMsg: " + localMsg.Content);
                    localHashMap.put("time", TimeUtil.timeToStr(localMsg.CreateTime));
                    localHashMap.put("info", localMsg.Content);
                }
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public String getShowName(String paramString) {
        if (this.contactList == null) {
            Log.d("TAG", "getShowName:contact list not initial");
            return "";
        }
        Iterator localIterator1 = this.contactList.iterator();
        while (localIterator1.hasNext()) {
            Contact localContact2 = (Contact) localIterator1.next();
            if (localContact2.UserName.equals(paramString))
                return localContact2.getShowName();
        }
        Iterator localIterator2 = this.exContactList.iterator();
        while (localIterator2.hasNext()) {
            Contact localContact1 = (Contact) localIterator2.next();
            if (localContact1.UserName.equals(paramString))
                return localContact1.getShowName();
        }
        return "";
    }

    public class AddReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("com.g6.action.wx.add_item")) {

                Msg paramMsg = new Msg();
                paramMsg.fromBundle(intent.getExtras());

                paramMsg.toNickName = getShowName(paramMsg.ToUserName);

                HashMap localHashMap2 = new HashMap();

                if (mData.size() == 0) {
                    //直接添加
                    localHashMap2.put("img", WxHome.getHeadUrlByUsername(token, paramMsg.ToUserName));
                    localHashMap2.put("time", TimeUtil.getDate());
                    localHashMap2.put("info", paramMsg.Content);
                    localHashMap2.put("title", paramMsg.toNickName);
                    //  localHashMap2.put("title", paramMsg.fromNickName);
                    localHashMap2.put("userName", paramMsg.ToUserName);

                    mData.add(0, localHashMap2);
                    Log.d("TAG", "ChatAC_1自己发的添加列表");
                    mAdapter.notifyDataSetChanged();
                } else {
                    //在里面,就改变顺序 ,不在里面添加到零位
                    boolean a = false;
                    HashMap localHashMap1;
                    for (int i = 0; i < mData.size(); i++) {
                        localHashMap1 = (HashMap) mData.get(i);
                        if (paramMsg.ToUserName.equals(localHashMap1.get("userName"))) {
                            a = true;
                            localHashMap1.put("img", WxHome.getHeadUrlByUsername(token, paramMsg.ToUserName));
                            localHashMap1.put("time", TimeUtil.getDate());
                            localHashMap1.put("info", paramMsg.Content);
                            localHashMap1.put("title", paramMsg.toNickName);
                            //  localHashMap1.put("title", localHashMap1.get("title"));
                            localHashMap1.put("userName", paramMsg.ToUserName);
                            Log.d("TAG", "ChatAC_自己发的交换位置");
                            Collections.swap(mData, 0, i);
                            mAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                    if (!a) {
                        localHashMap2.put("img", WxHome.getHeadUrlByUsername(token, paramMsg.ToUserName));
                        localHashMap2.put("time", TimeUtil.getDate());
                        localHashMap2.put("info", paramMsg.Content);
                        localHashMap2.put("title", paramMsg.toNickName);
                        //  localHashMap2.put("title", paramMsg.fromNickName);
                        localHashMap2.put("userName", paramMsg.ToUserName);
                        mData.add(0, localHashMap2);
                        Log.d("TAG", "ChatAC_2自己发的添加列表");
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }
}
