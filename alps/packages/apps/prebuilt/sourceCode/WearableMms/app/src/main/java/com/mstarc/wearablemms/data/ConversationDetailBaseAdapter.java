package com.mstarc.wearablemms.data;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.common.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by vista on 2017/8/12.
 */

public class ConversationDetailBaseAdapter extends BaseAdapter {
    private Cursor mCursor;
    private Context mContext;

    public ConversationDetailBaseAdapter(Context mContext,Cursor cursor) {
        this.mContext = mContext;
        this.mCursor = cursor;
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        if (mCursor != null) {
            mCursor.moveToPosition(position);
            return mCursor;
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        mCursor.moveToPosition(position);
        Sms sms = Sms.createFromCursor(mCursor);
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        if (sms.getType() == Constant.SMS.TYPE_RECEIVE) {
            convertView = mInflater.inflate(R.layout.message_chat_item1, null);
        } else {
            convertView = mInflater.inflate(R.layout.message_chat_item2, null);
        }
        viewHolder = new ViewHolder(convertView);
//            convertView.setTag(viewHolder);
        showData(mContext,viewHolder,sms);
        viewHolder.tv_conversation_detail_body.setText(sms.getBody());
        return convertView;
    }

    private void showData(Context context, ViewHolder holder, Sms sms){
        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("HH:mm");
        Date d = new Date(sms.getDate());
        String date = dateFormat.format(d);
        holder.tv_conversation_detail_date.setText(date);
        dateFormat = new SimpleDateFormat("MM-dd");
        d = new Date(sms.getDate());
        date = dateFormat.format(d);
        holder.tv_conversation_detail_time.setText(date);
        if(sms.getType() == Constant.SMS.TYPE_RECEIVE) {
            holder.tv_conversation_detail_name.setText(sms.getName(context.getContentResolver()).replace("+86",""));
            //获取联系人头像
            Bitmap avatar = sms.getAvatarByAddress(context.getContentResolver());
            //判断是否成功拿到头像
            if (avatar == null){
                holder.tv_conversation_detail_photo.setBackgroundResource(R.drawable.ic_mms_profile_list);

                ThemeUtils.updateImageView(holder.tv_conversation_detail_photo,R.drawable.ic_mms_profile_list);
            }else {
                Drawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(),avatar);
                ((RoundedBitmapDrawable)drawable).setAntiAlias(true);
                ((RoundedBitmapDrawable)drawable).setCornerRadius(drawable.getIntrinsicWidth() / 2);
                holder.tv_conversation_detail_photo.setImageDrawable(drawable);
            }
        }else{
            holder.tv_conversation_detail_name.setText(context.getString(R.string.me));
            // holder.tv_conversation_detail_photo.setImageResource(R.drawable.ic_mms_profile_send);
            holder.tv_conversation_detail_photo.setBackgroundResource(R.drawable.ic_mms_profile_send);
            ThemeUtils.updateImageView(holder.tv_conversation_detail_photo,R.drawable.ic_mms_profile_send);
        }
    }

    class ViewHolder{

        private final TextView tv_conversation_detail_time;
        private final TextView tv_conversation_detail_date;
        private final TextView tv_conversation_detail_body;
        private final TextView tv_conversation_detail_name;
        private final ImageView tv_conversation_detail_photo;

        public ViewHolder(View view){
            tv_conversation_detail_date =(TextView)view.findViewById(R.id.msg_chat_time);
            tv_conversation_detail_time =(TextView)view.findViewById(R.id.msg_chat_date);
            tv_conversation_detail_body =(TextView)view.findViewById(R.id.msg_chat_content);
            tv_conversation_detail_name =(TextView)view.findViewById(R.id.msg_chat_contact);
            tv_conversation_detail_photo = (ImageView)view.findViewById(R.id.msg_chat_profile);
            tv_conversation_detail_name.setSelected(true);

        }
    }
}
