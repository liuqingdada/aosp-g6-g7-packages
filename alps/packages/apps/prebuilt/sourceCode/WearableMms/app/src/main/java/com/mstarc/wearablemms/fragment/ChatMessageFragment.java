package com.mstarc.wearablemms.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;
import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.activity.VoiceActivity;
import com.mstarc.wearablemms.common.Constants;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.data.Constant;
import com.mstarc.wearablemms.data.ConversationDetailBaseAdapter;
import com.mstarc.wearablemms.data.Utils;

/**
 * Created by wangxinzhi on 17-3-8.
 */

public class ChatMessageFragment extends Fragment implements View.OnClickListener {
    private ListView lv_conversation_detail;
    private String mAddress;
    private int mThreadId;
    private ConversationDetailBaseAdapter mAdapter;
    private final static int SCAN_MESSAGE = 1;
    public static Boolean isRefresh = false;
    private static final String TAG = ChatMessageFragment.class.getSimpleName();
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.message_chat, container, false);
        lv_conversation_detail = (ListView) rootView.findViewById(R.id.lv_conversation_detail);
        //只要ListView刷新，就会滑动
        lv_conversation_detail.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        (rootView.findViewById(R.id.voice_button)).setOnClickListener(this);
        ThemeUtils.updateImageView((ImageView) rootView.findViewById(R.id.voice_button), R.drawable.ic_voice_2);
        initData();
        Log.d(TAG, "onCreateView");
        return rootView;
    }
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SCAN_MESSAGE:
                    initData();


                    break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if(isRefresh) {
            mHandler.sendEmptyMessageDelayed(SCAN_MESSAGE, 3000);
            isRefresh=false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(isRefresh)
        mHandler.removeMessages(SCAN_MESSAGE);
    }

    public void initData() {
        //拿到传递过来的数据
        Bundle bundle = getArguments();
        if (bundle != null) {

            mAddress = bundle.getString("address");
            mThreadId = bundle.getInt("thread_id");
        }
        Log.d(TAG, "mThreadId==" + mThreadId);
        Log.d(TAG, "mAddress==" + mAddress);
//        //给会话详细界面的listview设置adapter，显示会话的所有短信
//        mAdapter = new ConversationDetailAdapter(getActivity(),null,lv_conversation_detail);
//        lv_conversation_detail.setAdapter(mAdapter);
        //按照会话id查询属于该会话的所有短信
        String[] projection = {
                "_id",
                "body",
                "type",
                "date",
                "address"
        };
        String selection = "thread_id = " + mThreadId;
////        异步查询短信
//        SimpleQueryHander queryHander = new SimpleQueryHander(getActivity().getContentResolver());
//        queryHander.startQuery(0,mAdapter, Constant.URI.URI_SMS,projection,selection,null,"date asc");
        Cursor cursor = getActivity().getContentResolver().query(Constant.URI.URI_SMS, projection, selection, null, "date asc");
        mAdapter = new ConversationDetailBaseAdapter(getActivity(), cursor);
        lv_conversation_detail.setAdapter(mAdapter);
        if (cursor != null) {
            lv_conversation_detail.setSelection(cursor.getCount());
        }
    }

    @Override
    public void onClick(View v) {
        if (!Utils.isNetworkAvailable(getActivity())) {
            Toast.makeText(getActivity(), getString(R.string.open_network), Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(Constants.PHONE_NUM, mAddress);
        intent.setClass(getActivity(), VoiceActivity.class);
        startActivityForResult(intent, 1000);
    }
}
