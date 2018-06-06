package com.mstarc.wearablemms.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.activity.AddContactAcivity;
import com.mstarc.wearablemms.activity.JunkMmsActivity;
import com.mstarc.wearablemms.activity.MessageActivity;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.data.Constant;
import com.mstarc.wearablemms.data.Conversation;
import com.mstarc.wearablemms.data.ConversationListAdapter;
import com.mstarc.wearablemms.data.SimpleQueryHander;
import com.mstarc.wearablemms.view.BaseSwipeListViewListener;
import com.mstarc.wearablemms.view.SwipeListView;

/**
 * Created by Administrator on 2016/3/28.
 */
public class ConversationFragment extends Fragment implements ConfirmDialog.Listener  {
    private SwipeListView mConversationList;
    private ConversationListAdapter mAdapter;
    View mNoMessageText;
    private UiHandler mHandler;

    @Override
    public void onConfirm() {
        getActivity().getContentResolver().delete(Constant.URI.URI_SMS, null, null);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCancel() {
        //TODO delete all message
    }

    class UiHandler extends Handler {
        public static final int MSG_REMOVE_ALL = 0;
        public static final int MSG_REMOVE_SHOW_NO_MESSAGE = 1;
        public static final int MSG_REMOVE_HIDE_NO_MESSAGE = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REMOVE_ALL:
                    mNoMessageText.setVisibility(View.VISIBLE);
                    break;
                case MSG_REMOVE_SHOW_NO_MESSAGE:
                    mNoMessageText.setVisibility(View.VISIBLE);
                    break;
                case MSG_REMOVE_HIDE_NO_MESSAGE:
                    mNoMessageText.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversation,container,false);
        mNoMessageText = view.findViewById(R.id.no_messages);
        // add a new mms
        view.findViewById(R.id.add_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AddContactAcivity.class));
            }
        });
        // review junk mms
        view.findViewById(R.id.enter_junk_mms).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), JunkMmsActivity.class));
            }
        });
        //
        ThemeUtils.updateImageView((ImageView) view.findViewById(R.id.add_new),R.mipmap.new_message);
        ThemeUtils.updateImageView((ImageView) view.findViewById(R.id.iv_junk_mms),R.mipmap.intercepted_mms);

        ((TextView) view.findViewById(R.id.new_message)).setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mConversationList = (SwipeListView)view.findViewById(R.id.lv_conversation_list);//为viewList每个条目设置监听
        mConversationList.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onClickFrontView(int position) {
                Intent intent = new Intent(getActivity(), MessageActivity.class);
                //携带数据：address和会话thread_id
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                Conversation conversation = Conversation.createFromCursor(cursor);
                intent.putExtra("address",conversation.getAddress());
                intent.putExtra("thread_id",conversation.getThread_id());
                startActivity(intent);
            }

            @Override
            public void onClickBackView(int position) {
                    mConversationList.closeOpenedItems();
            }

            @Override
            public void onDismiss(int[] reverseSortedPositions) {

            }

            @Override
            public void frontLongPress() {
                (new ConfirmDialog(ConversationFragment.this.getActivity(),R.layout.delete_confirm_dialog,ConversationFragment.this,getResources().getString(R.string.clear_message))).show();
            }
        });

        initData();
        return view;
    }

    public void initData() {
        mAdapter = new ConversationListAdapter(getActivity(),null,mConversationList);
        mConversationList.setAdapter(mAdapter);
        SimpleQueryHander queryHander = new SimpleQueryHander(getActivity().getContentResolver());

        String[] projection = {
                "sms.body AS snippet",
                "sms.thread_id AS _id",
                "groups.msg_count AS msg_count",
                "address AS address",
                "date AS date"
        };
        queryHander.startQuery(0, mAdapter, Constant.URI.URI_SMS_CONVERSATION, projection, null, null, "date desc");
    }

}
