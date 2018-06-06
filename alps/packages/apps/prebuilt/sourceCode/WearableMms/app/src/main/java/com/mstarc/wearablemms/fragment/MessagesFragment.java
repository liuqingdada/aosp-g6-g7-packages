package com.mstarc.wearablemms.fragment;

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.activity.AddContactAcivity;
import com.mstarc.wearablemms.activity.MessageActivity;
import com.mstarc.wearablemms.common.Constants;
import com.mstarc.wearablemms.common.DecorationSettingItem;
import com.mstarc.wearablemms.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.data.Contact;
import com.mstarc.wearablemms.data.MessageData;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/**
 * Created by wangxinzhi on 17-3-8.
 */

public class MessagesFragment extends Fragment implements RecyclerViewItemTouchListener.OnItemClickEventListener, ConfirmDialog.Listener {

    private static final int ADDED_NUM = 10;
    private int mItem = 0;
    private int mCurrentItem = 0;
    private ContentResolver cr;
    private String[] projection = new String[] { "body", "address", "person", "date", "read"};
    ArrayList<MessageData> mData;
    RecyclerView mListView;
    MessageAdapter mAdapter;
    View mNoMessageText;
    private Cursor mCursor;
    private static final String[] PHONES_PROJECTION = new String[] {
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Photo.PHOTO_ID, ContactsContract.CommonDataKinds.Phone.CONTACT_ID };
    private Uri SMS_INBOX = Uri.parse("content://sms/inbox");
    private UiHandler mHandler;

    public MessagesFragment() {
        mData = new ArrayList<MessageData>();
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
                    mData.clear();
                    mAdapter.notifyDataSetChanged();
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
        View rootView = inflater.inflate(R.layout.message_list, container, false);
        mNoMessageText = rootView.findViewById(R.id.no_messages);
        rootView.findViewById(R.id.add_message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AddContactAcivity.class));
            }
        });
        ThemeUtils.updateImageView((ImageView) rootView.findViewById(R.id.add_new),R.mipmap.new_message);
        ((TextView)rootView.findViewById(R.id.new_message)).setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mHandler = new UiHandler();
        mData.clear();
        mAdapter = new MessageAdapter(getActivity());
        mListView = (RecyclerView) rootView.findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        mListView.addOnItemTouchListener(new RecyclerViewItemTouchListener(getActivity(), this));
        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(isSlideToBottom(recyclerView)){
                    new MessageTask().execute();
                }
            }
        });
        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new MessageItemToucCallback(getActivity(), 0, ItemTouchHelper.LEFT));
        swipeToDismissTouchHelper.attachToRecyclerView(mListView);
        cr = getActivity().getContentResolver();
        new MessageTask().execute();
        return rootView;
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {
        (new ConfirmDialog(getActivity(), R.layout.message_cleanall_dialog, this,getActivity().getResources().getString(R.string.msg_clean_all))).show();

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        MessageData data = mData.get(adapterPosition);
        Intent intent = new Intent();
        intent.putExtra(Constants.PHONE_NUM,data.mContact.mPhoneNumber);
        intent.putExtra(Constants.CONTACT_NAME,data.mContact.mName);
        intent.putExtra(Constants.CONTENT,data.mContent);
        intent.putExtra(Constants.DATE,data.mDate);
        intent.setClass(getActivity(), MessageActivity.class);
        startActivity(intent);
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    @Override
    public void onConfirm() {
        mHandler.dispatchMessage(Message.obtain(mHandler, UiHandler.MSG_REMOVE_ALL));
    }

    @Override
    public void onCancel() {

    }

    class MessageAdapter extends RecyclerView.Adapter<MessageListItemVH> {
        private LayoutInflater mInflater;

        public MessageAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }


        @Override
        public MessageListItemVH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MessageListItemVH(mInflater.inflate(R.layout.message_list_item, null));
        }

        @Override
        public void onBindViewHolder(MessageListItemVH holder, int position) {
            MessageData message = mData.get(position);
            holder.mContent.setText(message.mContent);
            holder.mDate.setText(message.mDate);
            holder.mName.setText((message.mContact.mName != null) && !message.mContact.mName.equals("0") ? message.mContact.mName : message.mContact.mPhoneNumber);
            if (message.mContact.mProfile != null) {
                holder.mProfile.setImageDrawable(message.mContact.mProfile);
            }
        }

        @Override
        public int getItemCount() {
            int size = mData.size();
            if(size == 0){
                mHandler.dispatchMessage(Message.obtain(mHandler, UiHandler.MSG_REMOVE_SHOW_NO_MESSAGE));
            }else{
                mHandler.dispatchMessage(Message.obtain(mHandler, UiHandler.MSG_REMOVE_HIDE_NO_MESSAGE));
            }
            return mData.size();        }
    }

    class MessageListItemVH extends RecyclerView.ViewHolder {
        ImageView mProfile;
        TextView mName, mDate, mContent;

        public MessageListItemVH(View itemView) {
            super(itemView);
            mProfile = (ImageView) itemView.findViewById(R.id.msg_junk_list_profile);
            mName = (TextView) itemView.findViewById(R.id.msg_junk_contact);
            mDate = (TextView) itemView.findViewById(R.id.msg_junk_time);
            mContent = (TextView) itemView.findViewById(R.id.msg_junk_content);
            ThemeUtils.updateImageView(mProfile,R.drawable.ic_mms_profile_list);
            mName.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        }
    }

    class MessageItemToucCallback extends ItemTouchHelper.SimpleCallback {
        boolean initiated = false;
        Context mContext;
        Drawable xMark;
        int xMarkMargin;

        public MessageItemToucCallback(Context context, int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
            mContext = context;
            int color = ThemeUtils.getCurrentPrimaryColor();
            ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
            xMark = ContextCompat.getDrawable(context, R.drawable.ic_delete_pressed);
            xMark.clearColorFilter();
            xMark.mutate().setColorFilter(filter);
            xMarkMargin = (int) mContext.getResources().getDimension(R.dimen.ic_clear_margin);
            initiated = true;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            mData.remove(viewHolder.getAdapterPosition());
            mAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;

            // not sure why, but this method get's called for viewholder that are already swiped away
            if (viewHolder.getAdapterPosition() == -1) {
                // not interested in those
                return;
            }

            // draw x mark
            int itemHeight = itemView.getBottom() - itemView.getTop();
            int intrinsicWidth = xMark.getIntrinsicWidth();
            int intrinsicHeight = xMark.getIntrinsicWidth();

            int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
            int xMarkRight = itemView.getRight() - xMarkMargin;
            int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int xMarkBottom = xMarkTop + intrinsicHeight;
            xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
            c.clipRect(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            xMark.draw(c);
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    public void getSmsFromPhone() {
        if(mCursor == null) {
            mCursor= cr.query(SMS_INBOX, projection, null, null, "date desc");
        }
        if (null == mCursor)
            return;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        SimpleDateFormat dateFormat;
        while (mCursor.moveToNext()) {
            if(mItem >mCurrentItem + ADDED_NUM){
                break;
            }
            String number = mCursor.getString(mCursor.getColumnIndex("address"));//手机号
            String body = mCursor.getString(mCursor.getColumnIndex("body"));
            long longDate = Long.parseLong(mCursor.getString(mCursor.getColumnIndex("date")));

            if (longDate  >= calendar.getTimeInMillis()){
                dateFormat = new SimpleDateFormat("hh:mm");
            }else{
                dateFormat = new SimpleDateFormat("MM/dd");
            }
            Date d = new Date(longDate);
            String date = dateFormat.format(d);
            int read = mCursor.getInt(mCursor.getColumnIndex("read"));
            Contact contact = getContactInfo(getActivity(),number);
            MessageData data = new MessageData(contact,body,date,read);
            mData.add(data);
            mItem ++;
        }
        mCurrentItem = mItem;
    }


    private class MessageTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            getSmsFromPhone();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter.notifyDataSetChanged();
            super.onPostExecute(aVoid);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public Contact getContactInfo(Context context,String phoneNumber) {
        Uri uriNumber2Contacts = Uri.parse("content://com.android.contacts/"
                + "data/phones/filter/" + phoneNumber);
        Cursor cursorCantacts = context.getContentResolver().query(
                uriNumber2Contacts,
                null,
                null,
                null,
                null);
        Drawable drawable = null;
        String name = phoneNumber;
        if (cursorCantacts!=null) {
            if(cursorCantacts.moveToFirst()) {
                name = cursorCantacts.getString(cursorCantacts.getColumnIndex("display_name"));
                Long contactID = cursorCantacts.getLong(cursorCantacts.getColumnIndex("contact_id"));
                Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID);
                InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
                drawable =
                        RoundedBitmapDrawableFactory.create(getResources(), input);
                ((RoundedBitmapDrawable)drawable).setAntiAlias(true);
                ((RoundedBitmapDrawable)drawable).setCornerRadius(drawable.getIntrinsicWidth() / 2);
                cursorCantacts.close();
            }else{
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_mms_profile_list);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
            }
            cursorCantacts.close();
        }else{
            int color = ThemeUtils.getCurrentPrimaryColor();
            ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
            drawable = ContextCompat.getDrawable(context, R.drawable.ic_mms_profile_list);
            drawable.clearColorFilter();
            drawable.mutate().setColorFilter(filter);
        }
        return new Contact(drawable,name,phoneNumber);
    }

    public static boolean isSlideToBottom(RecyclerView recyclerView) {
        if (recyclerView == null) return false;
        if (recyclerView.computeVerticalScrollExtent() + recyclerView.computeVerticalScrollOffset()
                >= recyclerView.computeVerticalScrollRange())
            return true;
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mCursor != null) {
           mCursor.close();
        }
    }
}
