package com.mstarc.wechat.wearwechat.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.mstarc.wechat.wearwechat.MessageHandleActivity;
import com.mstarc.wechat.wearwechat.R;
import com.mstarc.wechat.wearwechat.ThemeUtils;
import com.mstarc.wechat.wearwechat.common.DecorationSettingItem;
import com.mstarc.wechat.wearwechat.common.RecyclerViewItemTouchListener;
import com.mstarc.wechat.wearwechat.model.Contact;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.model.User;
import com.mstarc.wechat.wearwechat.net.VolleySingleton;
import com.mstarc.wechat.wearwechat.utils.WxHome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * create an instance of this fragment.
 */
public class ContactsFragment extends Fragment implements RecyclerViewItemTouchListener.OnItemClickEventListener {
    RecyclerView mListView;
    //ArrayList<Contact> mItems;
    String[] mDemoString;
    RecordAdapter mAdapter;

    private ArrayList<Contact> contactList;
    private List<HashMap<String, Object>> mData;
    private Token token;
    private User user;

    public ContactsFragment() {
        // Required empty public constructor
    }

    public static ContactsFragment newInstance(Token paramToken, User paramUser, ArrayList<Contact> paramArrayList) {
        ContactsFragment localContactFragment = new ContactsFragment();
        Bundle localBundle = new Bundle();
        localBundle.putBundle("token", paramToken.toBundle());
        localBundle.putBundle("user", paramUser.toBundle());
        localBundle.putParcelableArrayList("contact", paramArrayList);
        localContactFragment.setArguments(localBundle);
        return localContactFragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.token = new Token();
            this.token.fromBundle(getArguments().getBundle("token"));
            this.user = new User();
            this.user.fromBundle(getArguments().getBundle("user"));
            this.contactList = getArguments().getParcelableArrayList("contact");
        }
        Log.d("TAG", "ContactFragment_onCreate:token=" + JSON.toJSONString(this.token));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_contacts, container, false);
        this.mData = getData(this.contactList);
        mAdapter = new RecordAdapter(getActivity());
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        mListView = (RecyclerView) view.findViewById(R.id.contacts_list);
        mListView.setAdapter(mAdapter);

        mListView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        mListView.addOnItemTouchListener(new RecyclerViewItemTouchListener(getActivity(), this));
        return view;
    }

    class RecordItemViewHolder extends RecyclerView.ViewHolder {
        public NetworkImageView mProfileImageView;
        public TextView mTextNameOrNumber;

        public RecordItemViewHolder(View itemView) {
            super(itemView);
            mProfileImageView = (NetworkImageView) itemView.findViewById(R.id.contracts_profile_image);
            mTextNameOrNumber = (TextView) itemView.findViewById(R.id.contracts_name);
            int color = ThemeUtils.getCurrentPrimaryColor();
            if(color==0xFFAFFF00)
                updateImageView(mProfileImageView, R.mipmap.icon_wechat_avata_g);
            else
            updateImageView(mProfileImageView, R.mipmap.icon_wechat_avata);
        }
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
                view.setImageBitmap(drawableToBitmap(drawable));
            }
        });
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    class RecordAdapter extends RecyclerView.Adapter<RecordItemViewHolder> {
        private final LayoutInflater mInflater;
        private Context mContext;

        RecordAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public RecordItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecordItemViewHolder(mInflater.inflate(R.layout.phone_contacts_item, null));
        }

        @Override
        public void onBindViewHolder(RecordItemViewHolder holder, int position) {
            String str = ((HashMap) mData.get(position)).get("img").toString();
            ImageLoader imageLoader = VolleySingleton.getInstance().getImageLoader(token.cookie);
//            updateImageView(holder.mProfileImageView, R.mipmap.icon_wechat_avata);
            holder.mProfileImageView.setImageUrl(str, imageLoader);
//            holder.mProfileImageView.setDefaultImageResId(R.mipmap.icon_wechat_avata);
            holder.mTextNameOrNumber.setText(mData.get(position).get("title").toString());
        }

        @Override
        public int getItemCount() {
            return mData.size();
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
        startActivity(localIntent);
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    private List<HashMap<String, Object>> getData(List<Contact> paramList) {
        ArrayList localArrayList = new ArrayList();
        Iterator localIterator = paramList.iterator();
        while (localIterator.hasNext()) {
            Contact localContact = (Contact) localIterator.next();
            if (localContact.ContactFlag != 1) {
                HashMap localHashMap = new HashMap();
                localHashMap.put("title", localContact.getShowName());
                localHashMap.put("time", "");
                localHashMap.put("info", "");
                localHashMap.put("img", WxHome.getHeadImgUrl(localContact.HeadImgUrl));
                localHashMap.put("userName", localContact.UserName);
                localArrayList.add(localHashMap);
            }
        }
        return localArrayList;
    }
}
