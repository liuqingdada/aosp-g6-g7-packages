package com.mstarc.wechat.wearwechat.adapter;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.mstarc.wechat.wearwechat.R;
import com.mstarc.wechat.wearwechat.model.ChatMsgEntity;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.net.VolleySingleton;
import com.mstarc.wechat.wearwechat.utils.StringUtil;
import com.mstarc.wechat.wearwechat.utils.WxHome;

import java.util.List;

public class ChatMsgViewAdapter extends RecyclerView.Adapter<MessageListItemVH> {
    private static final String TAG = ChatMsgViewAdapter.class.getSimpleName();
    private List<ChatMsgEntity> coll;
    private Context ctx;
    private ImageLoader imageLoader;
    private LayoutInflater mInflater;
    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private RequestQueue mQueue;
    private Token token;

    public final static int MESSAGE_TYPE_IN = 0;
    public final static int MESSAGE_TYPE_OUT = 1;

    public ChatMsgViewAdapter(Context paramContext, List<ChatMsgEntity> paramList, Token paramToken) {
        token = paramToken;
        ctx = paramContext;
        coll = paramList;
        mQueue = VolleySingleton.getInstance().getRequestQueue();
        if (paramToken!= null) {
            imageLoader = VolleySingleton.getInstance().getImageLoader(paramToken.cookie);
        }
        mInflater = LayoutInflater.from(paramContext);
    }

    private void playMusic(String paramString) {
        try {
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(paramString);
            mMediaPlayer.prepare();
            mMediaPlayer.setVolume(1.0f, 1.0f);
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                public void onCompletion(MediaPlayer paramAnonymousMediaPlayer) {
                    Log.d("TAG", "CHAT_adapter_语音播放完成");
                }
            });
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }

    public long getItemId(int paramInt) {
        return paramInt;
    }

    public int getItemViewType(int paramInt) {
        return coll.get(paramInt).getMsgType() ? MESSAGE_TYPE_OUT : MESSAGE_TYPE_IN;
    }

    @Override
    public MessageListItemVH onCreateViewHolder(ViewGroup parent, int viewType) {
        int resLayoutID;
        if(viewType == MESSAGE_TYPE_OUT){
            resLayoutID = R.layout.message_chat_item1;
        }else{
            resLayoutID = R.layout.message_chat_item2;
        }
        return new MessageListItemVH(mInflater.inflate(resLayoutID, null));
    }

    @Override
    public void onBindViewHolder(MessageListItemVH holder, int position) {
        final ChatMsgEntity localChatMsgEntity = coll.get(position);
        boolean bool = localChatMsgEntity.getMsgType();
        holder.setComMsg(bool);
        //gettime 有,就是语音信息吧,所以显示就不一样了;

        holder.tvSendTime.setText(localChatMsgEntity.getDate());

        if ((localChatMsgEntity.getTime() == null) || (localChatMsgEntity.getTime().isEmpty())) {

            holder.tvContent.setText(localChatMsgEntity.getText());
            holder.tvContent.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            holder.tvTime.setText("");

        } else {

            holder.tvContent.setText("");
            holder.tvContent.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.chatto_voice_playing, 0);
            holder.tvTime.setText(localChatMsgEntity.getTime());

            holder.tvContent.setOnClickListener(new OnClickListener() {
                public void onClick(View paramAnonymousView) {
                    if ((localChatMsgEntity.getTime() != null) && (!localChatMsgEntity.getTime().isEmpty()))
                        ChatMsgViewAdapter.this.playMusic(Environment.getExternalStorageDirectory() + "/weixinQingliao/" + localChatMsgEntity.getText());
                }
            });
        }

        String str = "";
        if (!WxHome.isGroupUserName(localChatMsgEntity.getUserName())) {
            holder.tvUserName.setText(StringUtil.filterHtml(localChatMsgEntity.getNickName()));
            str = WxHome.getIconUrlByUsername(token, localChatMsgEntity.getUserName());
        } else {
            holder.tvUserName.setText(StringUtil.filterHtml(localChatMsgEntity.getMemberNickName()));
            str = WxHome.getIconUrlByUsername(token, localChatMsgEntity.getMemberUserName());
        }
        holder.ivUserhead.setImageUrl(str, imageLoader);
    }

    @Override
    public int getItemCount() {
        return coll.size();
    }
}

