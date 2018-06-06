package com.mstarc.wearablemms.fragment;

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablemms.activity.AddContactAcivity;
import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.common.Constants;
import com.mstarc.wearablemms.common.DecorationSettingItem;
import com.mstarc.wearablemms.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.data.Contact;
import com.mstarc.wearablemms.data.NativeImageLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangxinzhi on 17-3-10.
 */

public class ContactsFragment extends Fragment implements RecyclerViewItemTouchListener.OnItemClickEventListener {

    /**获取库Phon表字段**/
    private static final String[] PHONES_PROJECTION = new String[] {
            Phone.DISPLAY_NAME, Phone.NUMBER, Photo.PHOTO_ID,Phone.CONTACT_ID };

    /**联系人显示名称**/
    private static final int PHONES_DISPLAY_NAME_INDEX = 0;

    /**电话号码**/
    private static final int PHONES_NUMBER_INDEX = 1;

    /**头像ID**/
    private static final int PHONES_PHOTO_ID_INDEX = 2;

    /**联系人的ID**/
    private static final int PHONES_CONTACT_ID_INDEX = 3;

    ContentResolver resolver;

    // 获取手机联系人
    Cursor phoneCursor;
    private List<Contact> mData = new ArrayList<Contact>();
    private RecyclerView mListView;
    private MessageAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.message_contacts, container, false);
        mAdapter = new MessageAdapter(getActivity());
        mListView = (RecyclerView) rootView.findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        mListView.addOnItemTouchListener(new RecyclerViewItemTouchListener(getActivity(), this));
//        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//                if(isSlideToBottom(recyclerView)){
//                    new ContactTask().execute();
//                }
//                }
//        });
        mData.clear();
        resolver = getActivity().getContentResolver();
        new ContactTask().execute();
        return rootView;
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.CONTACT_NAME, mData.get(adapterPosition).mName);
        bundle.putString(Constants.PHONE_NUM, mData.get(adapterPosition).mPhoneNumber);
        bundle.putString(Constants.URL, mData.get(adapterPosition).mUri);

        Intent intent = new Intent();
        intent.setClass(getActivity(), AddContactAcivity.class);
        intent.putExtras(bundle);
        getActivity().startActivity(intent);

       // ((AddContactAcivity) getActivity()).showFragment(MainActivity.FRAGMENT_ADD_NEW_MESSAGE, bundle);
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    class MessageAdapter extends RecyclerView.Adapter<MessageListItemVH> {
        private LayoutInflater mInflater;

        public MessageAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }


        @Override
        public MessageListItemVH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MessageListItemVH(mInflater.inflate(R.layout.message_contact_item, null));
        }

        @Override
        public void onBindViewHolder(MessageListItemVH holder, int position) {
            Contact contact = mData.get(position);
            if(contact.mUri != null) {
                holder.mProfile.setTag(contact.mUri);
                Drawable bitmap = NativeImageLoader.getInstance().loadNativeImage(contact.mUri, getActivity(), new NativeImageLoader.NativeImageCallBack() {

                    @Override
                    public void onImageLoader(Drawable bitmap, String path) {
                        ImageView mImageView = (ImageView) mListView.findViewWithTag(path);
                        if(bitmap != null && mImageView != null){
                            mImageView.setImageDrawable(bitmap);
                        }
                    }
                });

                if(bitmap != null){
                    holder.mProfile.setImageDrawable(bitmap);
                }else{
                    ThemeUtils.updateImageView(holder.mProfile,R.drawable.ic_mms_profile_list);
                }
            }else{
                ThemeUtils.updateImageView(holder.mProfile,R.drawable.ic_mms_profile_list);
            }
            holder.mName.setText(contact.mName);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    class MessageListItemVH extends RecyclerView.ViewHolder {
        ImageView mProfile;
        TextView mName;

        public MessageListItemVH(View itemView) {
            super(itemView);
            mProfile = (ImageView) itemView.findViewById(R.id.msg_junk_list_profile);
            mName = (TextView) itemView.findViewById(R.id.msg_junk_contact);
        }
    }

    private class ContactTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            getPhoneContacts();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mAdapter.notifyDataSetChanged();
            super.onPostExecute(aVoid);
        }
    }

    private void getPhoneContacts() {
        ContentResolver resolver = getActivity().getContentResolver();

        // 获取手机联系人
        if(phoneCursor == null) {
            phoneCursor = resolver.query(Phone.CONTENT_URI, PHONES_PROJECTION, null, null, null);
        }

        if (phoneCursor != null) {
            while (phoneCursor.moveToNext()) {
//                if(mItem >mCurrentItem + ADDED_NUM){
//                    break;
//                }
                //得到手机号码
                String phoneNumber = phoneCursor.getString(PHONES_NUMBER_INDEX);
                Log.i("vista","phoneNumber " + phoneNumber);
                //当手机号码为空的或者为空字段 跳过当前循环
                if (TextUtils.isEmpty(phoneNumber))
                    continue;

                //得到联系人名称
                String contactName = phoneCursor.getString(PHONES_DISPLAY_NAME_INDEX);

                //得到联系人ID
                Long contactid = phoneCursor.getLong(PHONES_CONTACT_ID_INDEX);

                //得到联系人头像ID
                Long photoid = phoneCursor.getLong(PHONES_PHOTO_ID_INDEX);

                //得到联系人头像Bitamp
                Drawable drawable = null;

                //photoid 大于0 表示联系人有头像 如果没有给此人设置头像则给他一个默认的
                String photoUri = null;
                if (photoid > 0) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactid);
                    photoUri = uri.toString();

                }
                Contact contact = new Contact(photoUri,contactName,phoneNumber);
                mData.add(contact);
            }
        }
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
        if(phoneCursor !=null) {
            phoneCursor.close();
            phoneCursor = null;
        }
    }
}
