package com.mstarc.wearablemms.data;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.database.DatabaseWizard;
import com.mstarc.wearablemms.database.bean.JunkMMS;
import com.mstarc.wearablemms.database.greendao.JunkMMSDao;
import com.mstarc.wearablemms.view.SwipeListView;

import net.vidageek.mirror.dsl.Mirror;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2016/3/29.
 */

public class ConversationListAdapter extends CursorAdapter {

    private static final String TAG = ConversationListAdapter.class.getSimpleName();
    private Point mPoint = new Point(0, 0);
    private SwipeListView mList;
    private Context ctx;

    public ConversationListAdapter(Context context, Cursor c, SwipeListView list) {
        super(context, c);
        mList = list;
        this.ctx = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //return super.getView(position, convertView, parent);
        try {
            Cursor mCursor = (Cursor) new Mirror().on(this)
                                                  .get()
                                                  .field("mCursor");
            mCursor.moveToPosition(position);

            ////////////////////////////////////////////////////////////
            Conversation conversation = Conversation.createFromCursor(mCursor);
            Log.d(TAG, "bindView: " + conversation);
            if (intercept(conversation)) {
                Log.d(TAG, "newView: 已拦截");
                return new View(ctx);
            }
            ////////////////////////////////////////////////////////////

            View v;
            if (convertView == null) {
                v = newView(ctx, mCursor, parent);
            } else {
                v = convertView;
                if (!(v instanceof RelativeLayout)) {
                    v = View.inflate(ctx, R.layout.message_list_item, null);
                }
            }
            bindView(v, ctx, mCursor);
            return v;
        } catch (Exception e) {
            Log.e(TAG, "getView: ", e);
            return new View(ctx);
        }
    }

    //返回的View对象就是listView的条目
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //只是用来填充布局对象
        return View.inflate(context, R.layout.message_list_item, null);
    }

    //设置listView每个条目显示的内容
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        ViewHolder holder = getHolder(view);
        //根据cursor内容创建会话对象，此时cursor的指针已经移动至正确的位置
        final Conversation conversation = Conversation.createFromCursor(cursor);
        //设置号码
        //按号码查询是否存有联系人
        String name = ContactDao.getNameAddress(context.getContentResolver(),
                                                conversation.getAddress());
        if (TextUtils.isEmpty(name)) {
            //holder.tv_conversation_address.setText(conversation.getAddress().replace("+86","")
            // + "("+conversation.getMsg_count()+")");
            holder.tv_conversation_address.setText(conversation.getAddress()
                                                               .replace("+86", ""));
        } else {
            //holder.tv_conversation_address.setText(name + "("+conversation.getMsg_count()+")");
            holder.tv_conversation_address.setText(name);
        }

        if (Integer.parseInt(conversation.getMsg_count()) > 1) {
            holder.rlPopCount.setVisibility(View.VISIBLE);
            holder.tvPopCount.setText(conversation.getMsg_count());
        }

        holder.tv_conversation_address.setSelected(true);
        //设置信息内容
        holder.tv_conversation_body.setText(conversation.getSnippet());
        //设置时间
        //判断是否是今天
        SimpleDateFormat dateFormat;
        if (DateUtils.isToday(conversation.getDate())) {
            dateFormat = new SimpleDateFormat("HH:mm");
            Date d = new Date(conversation.getDate());
            String date = dateFormat.format(d);
            //如果是,显示时分
            holder.tv_conversation_time.setText(date);
        } else {
            dateFormat = new SimpleDateFormat("MM-dd");
            Date d = new Date(conversation.getDate());
            String date = dateFormat.format(d);
            //如果不是,显示年月日
            holder.tv_conversation_time.setText(date);
        }
        String path = ContactDao.getUriByAddress(context.getContentResolver(),
                                                 conversation.getAddress());
        //获取联系人头像
                Bitmap avatar = ContactDao.getAvatarByAddress(context.getContentResolver(),
         conversation.getAddress());
                //判断是否成功拿到头像
                if (avatar == null){
                    holder.iv_conversation_avatar.setBackgroundResource(R.drawable
         .ic_mms_profile_list);
                    ThemeUtils.updateImageView(holder.iv_conversation_avatar,
                            R.drawable.ic_mms_profile_list);
                }else {
                    Drawable drawable = RoundedBitmapDrawableFactory.create(context
         .getResources(),avatar);
                    ((RoundedBitmapDrawable)drawable).setAntiAlias(true);
                    ((RoundedBitmapDrawable)drawable).setCornerRadius(drawable
         .getIntrinsicWidth() / 2);
                    holder.iv_conversation_avatar.setImageDrawable(drawable);
                }
        if (path != null) {
            holder.iv_conversation_avatar.setTag(path);

            NativeImageLoader.NativeImageCallBack imageCallBack = new NativeImageLoader
                    .NativeImageCallBack() {
                @Override
                public void onImageLoader(Drawable bitmap, String path) {
                    ImageView mImageView = (ImageView) mList.findViewWithTag(path);
                    if (bitmap != null && mImageView != null) {
                        mImageView.setImageDrawable(bitmap);
                    }
                }
            };

            Drawable bitmap = NativeImageLoader.getInstance()
                                               .loadNativeImage(path, context, imageCallBack);

            if (bitmap != null) {
                holder.iv_conversation_avatar.setImageDrawable(bitmap);
            } else {
                ThemeUtils.updateImageView(holder.iv_conversation_avatar,
                                           R.drawable.ic_mms_profile_list);
            }
        } else {
            ThemeUtils.updateImageView(holder.iv_conversation_avatar,
                                       R.drawable.ic_mms_profile_list);
        }

        holder.tv_conversation_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, Telephony.Sms.getDefaultSmsPackage(context));
                new DeleteSmsTask(context, conversation.getThread_id()).execute();
            }
        });
    }

    //参数就是条目的view对象
    private ViewHolder getHolder(View view) {
        ViewHolder holder = null;

        //先判断条目view对象中是否有holder
        if (view.getTag() instanceof ViewHolder) {
            holder = (ViewHolder) view.getTag();
        }

        if (holder == null) {
            //如果没有，就创建一个，并存入View对象
            holder = new ViewHolder(view);
            view.setTag(holder);
        }
        return holder;
    }

    //用于封装条目出现的所有布局（代码的可阅读量更强）
    class ViewHolder {
        private ImageView iv_conversation_avatar;
        private TextView tv_conversation_address;
        private TextView tv_conversation_body;
        private TextView tv_conversation_time;
        private ImageView tv_conversation_delete;
        private ImageView iv_check;

        // pop count
        private View rlPopCount;
        private TextView tvPopCount;
        //

        //参数就是条目的view对象
        public ViewHolder(View view) {
            //在构造方法中完成封装组件条目的所有创建
            iv_conversation_avatar = (ImageView) view.findViewById(R.id.msg_list_profile);
            tv_conversation_address = (TextView) view.findViewById(R.id.msg_contact);
            tv_conversation_body = (TextView) view.findViewById(R.id.msg_content);
            tv_conversation_time = (TextView) view.findViewById(R.id.msg_time);
            tv_conversation_delete = (ImageView) view.findViewById(R.id.delete);
            tv_conversation_address.setTextColor(ThemeUtils.getCurrentPrimaryColor());

            rlPopCount = view.findViewById(R.id.item_rl_pop_count);
            tvPopCount = view.findViewById(R.id.item_tv_pop_count);

            ThemeUtils.updateImageView(tv_conversation_delete, R.drawable.ic_delete);
            //            iv_check = (ImageView)view.findViewById(R.id.iv_check);
        }
    }

    class DeleteSmsTask extends AsyncTask<Void, Void, Void> {
        Context mContext;
        int mThreadId;

        public DeleteSmsTask(Context mContext, int mThreadId) {
            this.mContext = mContext;
            this.mThreadId = mThreadId;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.i(TAG, "onPost");
            notifyDataSetChanged();
            try {
                mList.closeOpenedItems();
            } catch (Exception e) {
                Log.e(TAG, "onPostExecute: ", e);
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.i(TAG, "doInBackground");
            deleteSms(mContext, mThreadId);
            return null;
        }
    }

    /**
     * 短信的删除
     */
    private void deleteSms(Context context, int threadId) {
        String where = "thread_id = " + threadId;
        int result = context.getContentResolver()
                            .delete(Uri.parse("content://sms/conversations/" + threadId),
                                    null, null);
        int row = context.getContentResolver()
                         .delete(Constant.URI.URI_SMS, where, null);
        Log.i(TAG, "row g7= " + row);
        Log.i(TAG, "result = " + result);
        deleteJunkSmsInGreenDao(threadId);
    }

    private void deleteJunkSmsInGreenDao(int threadId) {
        JunkMMSDao junkMMSDao = DatabaseWizard.getInstance()
                                              .getDaoSession()
                                              .getJunkMMSDao();
        List<JunkMMS> list = junkMMSDao.queryBuilder()
                                       .where(JunkMMSDao.Properties.Thread_id.eq(threadId))
                                       .list();
        junkMMSDao.deleteInTx(list);
    }

    private Conversation lastConversation;

    private synchronized boolean intercept(Conversation conversation) {
        Sms currentSms = getCurrentSms(conversation.getThread_id());
        if (currentSms != null) {
            /*
             * 1：inbox  2：sent 3：draft56  4：outbox  5：failed  6：queued
             */
            int type = currentSms.getType();
            switch (type) {
                case 1:
                    break;
                case 2:
                    return false; // if send mms, don't intercept
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    return false; // if send mms failed, don't intercept
                case 6:
                    break;
                default:
                    break;
            }
        }
        boolean flag = false;
        if (lastConversation != null
                && lastConversation.getThread_id() == conversation.getThread_id()) {
            flag = true;
        }

        String name = Util.getContactName(ctx, conversation.getAddress());
        // 拦截 0 为关  1 为开
        int stop = Settings.System.getInt(ctx.getContentResolver(),
                                          "stopstranger",
                                          1);
        boolean isIntercept = stop == 1;
        JunkMMSDao junkMMSDao = DatabaseWizard.getInstance()
                                              .getDaoSession()
                                              .getJunkMMSDao();
        if (isIntercept && TextUtils.isEmpty(name.trim())) { // 开关 - open && 通讯录没有
            if (!flag) {
                JunkMMS uniqueJM = junkMMSDao.queryBuilder()
                                             .where(JunkMMSDao.Properties.Thread_id.eq(
                                                     conversation.getThread_id()))
                                             .unique();
                if (uniqueJM == null) {
                    junkMMSDao.insertInTx(new JunkMMS(null,
                                                      conversation.getSnippet(),
                                                      conversation.getThread_id(),
                                                      conversation.getMsg_count(),
                                                      conversation.getAddress(),
                                                      conversation.getDate()));
                    Log.d(TAG, "intercept: insert to JunkMMSDao");
                } else {
                    uniqueJM.setAddress(conversation.getAddress());
                    uniqueJM.setDate(conversation.getDate());
                    uniqueJM.setMsg_count(conversation.getMsg_count());
                    uniqueJM.setSnippet(conversation.getSnippet());
                    uniqueJM.setThread_id(conversation.getThread_id());
                    junkMMSDao.updateInTx(uniqueJM);
                    Log.d(TAG, "intercept: update JunkMMSDao");
                }
            }
            lastConversation = conversation;
            return true;
        } else if (!isIntercept) { // 已经拦截的短信就不要放出去了
            long count = junkMMSDao.queryBuilder()
                                   .where(JunkMMSDao.Properties.Date.eq(
                                           conversation.getDate()))
                                   .count();
            Log.d(TAG, "intercept: count = " + count);
            if (count > 0) {
                return true;
            }
        }

        lastConversation = conversation;
        return false;
    }

    private Sms getCurrentSms(int threadId) {
        String[] projection = {
                "_id",
                "body",
                "type",
                "date",
                "address"
        };
        String selection = "thread_id = " + threadId;
        Cursor cursor = ctx.getContentResolver()
                           .query(Constant.URI.URI_SMS,
                                  projection,
                                  selection,
                                  null,
                                  "date desc");
        Sms sms = null;
        if (cursor != null) {
            cursor.moveToNext();
            sms = Sms.createFromCursor(cursor);
            Log.i(TAG, "getAllJunkSms: \n" + sms);
            cursor.close();
        }
        return sms;
    }
}
