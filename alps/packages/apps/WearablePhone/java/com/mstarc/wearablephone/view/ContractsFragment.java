package com.mstarc.wearablephone.view;

import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.common.ContactPhotoManager;
import com.mstarc.wearablephone.DialOutActivity;
import com.mstarc.wearablephone.PhoneApplication;
import com.mstarc.wearablephone.R;
import com.mstarc.wearablephone.bluetooth.BTCallManager;
import com.mstarc.wearablephone.view.common.DecorationSettingItem;
import com.mstarc.wearablephone.view.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablephone.view.common.SimDialog;

import java.io.IOException;
import java.util.ArrayList;

import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_BOOLEAN;
import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_NUMBER;


/**
 * Created by wangxinzhi on 17-3-6.
 */

public class ContractsFragment extends Fragment implements RecyclerViewItemTouchListener.OnItemClickEventListener, LoaderManager.LoaderCallbacks<Cursor>, DeviceSelectDialog.Listener {
    private static final String TAG = ContractsFragment.class.getSimpleName();
    RecyclerView mListView;
    ArrayList<Item> mItems;
    String[] mDemoString;
    RecordAdapter mAdapter;
    ContactPhotoManager mContactPhotoManager;
    String mOutgongNumber;
    View mVoiceSearch;
    View mNoMsg;
    SimDialog mSimDialog;
    boolean isSimValid = false;

    @Override
    public void byPhone() {
        Intent intent = new Intent(getActivity(), DialOutActivity.class);
        intent.putExtra(INTENT_DIAL_BY_PHONE_BOOLEAN, true);
        intent.putExtra(INTENT_DIAL_BY_PHONE_NUMBER, mOutgongNumber);
        startActivity(intent);
        mOutgongNumber = null;
    }

    void updateSimState() {
        // Sim state changed
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Service.TELEPHONY_SERVICE);
        int state = tm.getSimState();
        Log.d(TAG, "Sim state: " + state);
        switch (state) {
            case TelephonyManager.SIM_STATE_READY:
                Log.d(TAG, "Sim ready");
                isSimValid = true;
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
            default:
                isSimValid = false;
                Log.d(TAG, "Sim invalid");
                break;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSimDialog = new SimDialog(getActivity());
    }

    @Override
    public void byWatch() {
        updateSimState();
        if (!isSimValid && mSimDialog != null) {
            mSimDialog.show();
            Log.d(TAG, "show no sim dialog");
            return;
        }
        String uri = "tel:" + mOutgongNumber;
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(uri));
        startActivity(callIntent);
        mOutgongNumber = null;
    }

    public ContractsFragment() {

    }


    @Override
    public void onPause() {
        if (mSimDialog != null && mSimDialog.isShowing()) {
            mSimDialog.dismiss();
        }
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int themeResID = ((PhoneApplication) getActivity().getApplicationContext()).getThemeStyle();
        if (themeResID != 0) {
            getActivity().getTheme().applyStyle(themeResID, true);
        }
        mDemoString = getActivity().getResources().getStringArray(R.array.record_name_demo_list);
        mItems = new ArrayList<>();
        Drawable defaultProfile = getResources().getDrawable(R.drawable.ic_phone_default_profile_tinted, getActivity().getTheme());
        //mItems.add(new Item(defaultProfile, mDemoString[0]));
        //mItems.add(new Item(defaultProfile, mDemoString[1]));
        //mItems.add(new Item(defaultProfile, mDemoString[2]));
        //mItems.add(new Item(defaultProfile, mDemoString[3]));
        //mItems.add(new Item(defaultProfile, mDemoString[4]));
        //mItems.add(new Item(defaultProfile, mDemoString[5]));
        mAdapter = new RecordAdapter(getActivity());
        View view = inflater.inflate(R.layout.contracts, container, false);
        mListView = (RecyclerView) view.findViewById(R.id.contracts_list);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(container.getContext(), LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        mListView.addOnItemTouchListener(new RecyclerViewItemTouchListener(getActivity(), this));
        mContactPhotoManager = ContactPhotoManager.getInstance(getActivity());
        mVoiceSearch = view.findViewById(R.id.voice_search);
        mVoiceSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ping()) {
                    Intent intent = new Intent(getActivity(), VoiceActivity.class);
                    getActivity().startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.open_network), Toast.LENGTH_LONG).show();
                }
            }
        });

        mNoMsg = view.findViewById(R.id.contract_no_msg);
        return view;
    }

    public static boolean ping() {
        try {
            String ip = "www.baidu.com";
            Process p = Runtime.getRuntime().exec("ping -c 1 -w 100 " + ip);
            int status = p.waitFor();
            if (status == 0) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Log.d(TAG, "call: " + mItems.get(adapterPosition));

        String number = mItems.get(adapterPosition).mPhoneNumber;
        if (number != null) {
            mOutgongNumber = number;
            if (BTCallManager.getInstance(getActivity().getApplicationContext()).isBTPhoneEnnable()) {
                Dialog dialog = new DeviceSelectDialog(getActivity(), this);
                dialog.show();
            } else {
                byWatch();
            }
        }
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri baseUri;
        baseUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        return new android.support.v4.content.CursorLoader(getActivity(), baseUri,
                PhoneQuery.PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor cur) {
        mItems.clear();
        Drawable defaultProfile = getResources().getDrawable(R.drawable.ic_phone_default_profile_tinted, getActivity().getTheme());
        while (cur.moveToNext()) {
            String name = cur.getString(PhoneQuery.DISPLAY_NAME);
            String number = cur.getString(PhoneQuery.PHONE_NUMBER);
            long photoid = cur.getLong(PhoneQuery.PHOTO_ID);
            String photoUri = cur.getString(PhoneQuery.PHOTO_URI);

            mItems.add(new Item(defaultProfile, name, number, photoid, photoUri));
        }
        cur.close();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        mItems.clear();
        mAdapter.notifyDataSetChanged();
    }


    class Item {
        Drawable mProfile;

        public Item(Drawable mProfile, String mName, String mPhoneNumber, long mPhotoId, String mPhotoUri) {
            this.mProfile = mProfile;
            this.mName = mName;
            this.mPhoneNumber = mPhoneNumber;
            this.mPhotoUri = mPhotoUri;
            this.mPhotoId = mPhotoId;
        }

        String mName;
        String mPhoneNumber;
        String mPhotoUri;
        long mPhotoId;


        public Item(Drawable profile, String name) {
            mProfile = profile;
            this.mName = name;
        }

        public Item(Drawable profile, String name, String number) {
            mProfile = profile;
            this.mName = name;
            mPhoneNumber = number;
        }

        @Override
        public String toString() {
            return " Name: " + mName + " number: " + mPhoneNumber;
        }
    }

    class RecordItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView mProfileImageView;
        public TextView mTextNameOrNumber;

        public RecordItemViewHolder(View itemView) {
            super(itemView);
            mProfileImageView = (ImageView) itemView.findViewById(R.id.contracts_profile_image);
            mTextNameOrNumber = (TextView) itemView.findViewById(R.id.contracts_name);
        }
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
            Item item = mItems.get(position);
            if (item.mPhotoId == 0 && item.mPhotoUri != null && !item.mPhotoUri.isEmpty()) {
                // use photo uri
                mContactPhotoManager.loadPhoto(holder.mProfileImageView, Uri.parse(item.mPhotoUri),
                        holder.mProfileImageView.getWidth(),
                        false /* darkTheme */, true /* isCircular */, null);
                Log.d(TAG, String.format("%s : %s", item.mName, Uri.parse(item.mPhotoUri)));
            } else if (item.mPhotoId != 0) {
                mContactPhotoManager.loadThumbnail(holder.mProfileImageView, item.mPhotoId, false,
                        true, null);
                Log.d(TAG, String.format("%s : %d", item.mName, item.mPhotoId));

            } else {
                holder.mProfileImageView.setImageDrawable(mItems.get(position).mProfile);
                Log.d(TAG, String.format("%s : default photo", item.mName));
            }
            holder.mTextNameOrNumber.setText(mItems.get(position).mName);
        }

        @Override
        public int getItemCount() {
            int size = mItems.size();
            if (size == 0) {
                mNoMsg.setVisibility(View.VISIBLE);
                if (mVoiceSearch != null) {
                    mVoiceSearch.setVisibility(View.GONE);
                }
            } else {
                mNoMsg.setVisibility(View.GONE);
                if (mVoiceSearch != null) {
                    mVoiceSearch.setVisibility(View.VISIBLE);
                }
            }
            return mItems.size();
        }
    }

    public static class PhoneQuery {
        public static final String[] PROJECTION = new String[]{
                Phone.DISPLAY_NAME,                 //0
                Phone.NUMBER,                       //1
                Phone.PHOTO_ID,                     //2
                Phone.PHOTO_URI                     //3
        };

        public static final int DISPLAY_NAME = 0;
        public static final int PHONE_NUMBER = 1;
        public static final int PHOTO_ID = 2;
        public static final int PHOTO_URI = 3;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void onStop() {
        try {
            int size = mItems.size();
            if (size != 0) {
                mListView.scrollToPosition(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
    }

}

