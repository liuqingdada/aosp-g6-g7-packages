package com.mstarc.wearablemms.data;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.common.ThemeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2016/3/31.
 */
public class ConversationDetailAdapter extends CursorAdapter {

    private ListView lv;
    public ConversationDetailAdapter(Context context, Cursor c,ListView lv) {
        super(context, c);
        this.lv = lv;
    }
    /**
     * 只负责填充对象
     * @param context
     * @param cursor
     * @param parent
     * @return
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        Sms sms = Sms.createFromCursor(cursor);
        if(sms.getType() == Constant.SMS.TYPE_RECEIVE) {
            return View.inflate(context,R.layout.message_chat_item1,null);
        }else{
            return View.inflate(context,R.layout.message_chat_item2,null);
        }

    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        //组件对象全在holder里
        ViewHolder holder = getHolder(view);
        //数据全在sms对象里
        Sms sms = Sms.createFromCursor(cursor);
        showData(context,holder,sms);
        holder.tv_conversation_detail_body.setText(sms.getBody());
    }

    private ViewHolder getHolder(View view){
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder == null){
            holder = new ViewHolder(view);
            view.setTag(holder);
        }
        return holder;
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
            tv_conversation_detail_name.setTextColor(ThemeUtils.getCurrentPrimaryColor());
            ThemeUtils.updateImageView(tv_conversation_detail_photo,R.drawable.ic_mms_profile_list);
        }
    }
    private void showData(Context context,ViewHolder holder,Sms sms){
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
                ThemeUtils.updateImageView(holder.tv_conversation_detail_photo,R.drawable.ic_mms_profile_send);
            }else {
                Drawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(),avatar);
                ((RoundedBitmapDrawable)drawable).setAntiAlias(true);
                ((RoundedBitmapDrawable)drawable).setCornerRadius(drawable.getIntrinsicWidth() / 2);
                holder.tv_conversation_detail_photo.setImageDrawable(drawable);
            }
        }else{
            holder.tv_conversation_detail_name.setText(context.getString(R.string.me));
        }
    }

    /**
     * 上一条短信的时间
     * @return
     */
    private long getPreviousSmsDate(int positon){
        Cursor cursor = (Cursor)getItem(positon-1);
        Sms sms = Sms.createFromCursor(cursor);
        return sms.getDate();
    }

    /**
     * 打开短信的时候，需要滑动到最新的条目，则需要重写changCursor
     */
    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        //让listView滑动到指定的条目上
        lv.setSelection(getCount());
    }
}
