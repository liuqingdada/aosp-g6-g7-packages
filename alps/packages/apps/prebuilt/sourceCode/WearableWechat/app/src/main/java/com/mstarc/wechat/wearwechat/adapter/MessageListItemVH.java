package com.mstarc.wechat.wearwechat.adapter;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.mstarc.wechat.wearwechat.R;
import com.mstarc.wechat.wearwechat.ThemeUtils;

/**
 * description
 * <p>
 * Created by andyding on 2017/3/30.
 */
public class MessageListItemVH extends RecyclerView.ViewHolder{

    public boolean isComMsg = true;
    public NetworkImageView ivUserhead;
    public TextView tvContent;
    public TextView tvSendTime;
    public TextView tvTime;
    public TextView tvUserName;

    public MessageListItemVH(View itemView) {
        super(itemView);
        tvSendTime = ((TextView) itemView.findViewById(R.id.msg_chat_time));
        tvUserName = ((TextView) itemView.findViewById(R.id.msg_chat_contact));
        tvContent = ((TextView) itemView.findViewById(R.id.msg_chat_content));
        tvTime = ((TextView) itemView.findViewById(R.id.msg_chat_date));
        ivUserhead = ((NetworkImageView) itemView.findViewById(R.id.msg_chat_profile));
        updateImageView(ivUserhead, R.mipmap.icon_wechat_avata_sent);
        tvUserName.setTextColor(ThemeUtils.getCurrentPrimaryColor());
    }

    public void setComMsg(boolean flag) {
        isComMsg = flag;
    }

    public boolean getIsComMsg() {
        return isComMsg;
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
}
