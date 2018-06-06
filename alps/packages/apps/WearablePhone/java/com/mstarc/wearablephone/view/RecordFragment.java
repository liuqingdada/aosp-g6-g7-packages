package com.mstarc.wearablephone.view;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablephone.DialOutActivity;
import com.mstarc.wearablephone.PhoneApplication;
import com.mstarc.wearablephone.R;
import com.mstarc.wearablephone.bluetooth.BTCallManager;
import com.mstarc.wearablephone.bluetooth.DataProviderContract;
import com.mstarc.wearablephone.calllog.BTCallLogQuery;
import com.mstarc.wearablephone.calllog.BTCallLogQueryHandler;
import com.mstarc.wearablephone.calllog.CallLogQuery;
import com.mstarc.wearablephone.calllog.CallLogQueryHandler;
import com.mstarc.wearablephone.view.common.DecorationSettingItem;
import com.mstarc.wearablephone.view.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablephone.view.common.SimDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_BOOLEAN;
import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_NUMBER;
import static com.mstarc.wearablephone.view.RecordFragment.Item.TYPE_IN_WATCH;
import static com.mstarc.wearablephone.view.RecordFragment.Item.TYPE_LOST_WATCH;
import static com.mstarc.wearablephone.view.RecordFragment.Item.TYPE_OUT_PHONE;
import static com.mstarc.wearablephone.view.RecordFragment.Item.TYPE_OUT_WATCH;


/**
 * Created by wangxinzhi on 17-3-6.
 */

public class RecordFragment extends Fragment implements RecyclerViewItemTouchListener.OnItemClickEventListener, CallLogQueryHandler.Listener, DeviceSelectDialog.Listener, BTCallLogQueryHandler.Listener {
    private final static String TAG = RecordFragment.class.getSimpleName();
    RecyclerView mListView;
    ArrayList<Item> mItems;
    String[] mDemoString;
    RecordAdapter mAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;
    private BTCallLogQueryHandler mBTCallLogQueryHandler;
    private KeyguardManager mKeyguardManager;
    private final Handler mHandler = new Handler();
    private static final String KEY_FILTER_TYPE = "filter_type";
    private static final String KEY_LOG_LIMIT = "log_limit";
    private static final String KEY_DATE_LIMIT = "date_limit";
    private static final String KEY_SHOW_FOOTER = "show_footer";
    private static final String KEY_IS_REPORT_DIALOG_SHOWING = "is_report_dialog_showing";
    private static final String KEY_REPORT_DIALOG_NUMBER = "report_dialog_number";
    // See issue 6363009
    private final ContentObserver mCallLogObserver = new CustomContentObserver();
    private final ContentObserver mContactsObserver = new CustomContentObserver();
    private boolean mRefreshDataRequired = true;


    // Default to all calls.
    private int mCallTypeFilter = CallLogQueryHandler.CALL_TYPE_ALL;

    // Log limit - if no limit is specified, then the default in {@link CallLogQueryHandler}
    // will be used.
    private int mLogLimit = 100;

    // Date limit (in millis since epoch) - when non-zero, only calls which occurred on or after
    // the date filter are included.  If zero, no date-based filtering occurs.
    private long mDateLimit = 0;
    String mOutgongNumber;

    SortComparator itemComparator = new SortComparator();

    boolean isG7 = false;
    SimDialog mSimDialog;
    boolean isSimValid = false;
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
    View mNoMsg;
    @Override
    public void byPhone() {
        Intent intent = new Intent(getActivity(), DialOutActivity.class);
        intent.putExtra(INTENT_DIAL_BY_PHONE_BOOLEAN, true);
        intent.putExtra(INTENT_DIAL_BY_PHONE_NUMBER, mOutgongNumber);
        startActivity(intent);
        mOutgongNumber = null;
    }

    @Override
    public void byWatch() {
        updateSimState();
        if (!isSimValid && mSimDialog != null) {
            mSimDialog.show();
            Log.d(TAG,"show no sim dialog");
            return;
        }
        String uri = "tel:" + mOutgongNumber;
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(uri));
        startActivity(callIntent);
    }


    private class CustomContentObserver extends ContentObserver {
        public CustomContentObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mRefreshDataRequired = true;
            refreshData();
        }
    }

    public RecordFragment() {

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int themeResID = ((PhoneApplication)getActivity().getApplicationContext()).getThemeStyle();
        if(themeResID!=0){
            getActivity().getTheme().applyStyle(themeResID, true);
        }
        mDemoString = getActivity().getResources().getStringArray(R.array.record_name_demo_list);
        mItems = new ArrayList<>();
        mAdapter = new RecordAdapter(getActivity());
        View view = inflater.inflate(R.layout.record, container, false);
        mListView = (RecyclerView) view.findViewById(R.id.record_list);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(container.getContext(), LinearLayoutManager.VERTICAL, false));
        mListView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        mListView.addOnItemTouchListener(new RecyclerViewItemTouchListener(this.getContext(), this));
        mNoMsg = view.findViewById(R.id.record_no_msg);
        return view;
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition >= mItems.size()) {
            return;
        }
        final String number = mItems.get(adapterPosition).mNameOrNumber;
        if (number != null) {
            mOutgongNumber = mItems.get(adapterPosition).mNumber;
            if (BTCallManager.getInstance(getActivity().getApplicationContext()).isBTPhoneEnnable()) {
                Dialog dialog = new DeviceSelectDialog(getActivity(), this);
                dialog.show();
            } else {
                byWatch();
                mOutgongNumber = null;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_FILTER_TYPE, mCallTypeFilter);
        outState.putInt(KEY_LOG_LIMIT, mLogLimit);
        outState.putLong(KEY_DATE_LIMIT, mDateLimit);
    }

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        mCallLogQueryHandler = new CallLogQueryHandler(getActivity().getContentResolver(),
                this, mLogLimit);
        mBTCallLogQueryHandler = new BTCallLogQueryHandler(getActivity().getContentResolver(),
                this, mLogLimit);
        mKeyguardManager =
                (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        if (state != null) {
            mCallTypeFilter = state.getInt(KEY_FILTER_TYPE, mCallTypeFilter);
            mLogLimit = state.getInt(KEY_LOG_LIMIT, mLogLimit);
            mDateLimit = state.getLong(KEY_DATE_LIMIT, mDateLimit);
        }


        mKeyguardManager =
                (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        getActivity().getContentResolver().registerContentObserver(CallLog.CONTENT_URI, true,
                mCallLogObserver);
        getActivity().getContentResolver().registerContentObserver(DataProviderContract.CONTENT_URI, true,
                mCallLogObserver);
        getActivity().getContentResolver().registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, mContactsObserver);
        setHasOptionsMenu(true);
        updateCallList(mCallTypeFilter, mDateLimit);
        isG7 = getResources().getBoolean(R.bool.g7_target);
        mSimDialog = new SimDialog(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onPause() {
        if (mSimDialog != null && mSimDialog.isShowing()) {
            mSimDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        try {
            mListView.scrollToPosition(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStop();
        updateOnExit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        mAdapter.stopRequestProcessing();
//        mAdapter.changeCursor(null);
        getActivity().getContentResolver().unregisterContentObserver(mCallLogObserver);
        getActivity().getContentResolver().unregisterContentObserver(mContactsObserver);
    }

    private void updateCallList(int filterType, long dateLimit) {
        mCallLogQueryHandler.fetchCalls(filterType, dateLimit);
        mBTCallLogQueryHandler.fetchCalls(filterType, dateLimit);
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    @Override
    public boolean onCallsFetched(Cursor cursor) {
        ArrayList<Item> watchRecord = new ArrayList<>();
        for (Item item : mItems){
            switch (item.mType){
                case Item.TYPE_IN_WATCH:
                case Item.TYPE_OUT_WATCH:
                case Item.TYPE_LOST_WATCH:
                    watchRecord.add(item);
                    break;
                default:
                    break;
            }
        }
        mItems.removeAll(watchRecord);
        if (cursor.getCount() == 0) return true;
        cursor.moveToFirst();
        do {
            final int callType = cursor.getInt(CallLogQuery.CALL_TYPE);
            final String number = cursor.getString(CallLogQuery.NUMBER);
            final String name = cursor.getString(CallLogQuery.CACHED_NAME);
            final long date = cursor.getLong(CallLogQuery.DATE);

            int type;
            switch (callType) {
                case CallLog.Calls.INCOMING_TYPE:
                    type = Item.TYPE_IN_WATCH;
                    break;
                case CallLog.Calls.OUTGOING_TYPE:
                    type = Item.TYPE_OUT_WATCH;
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    type = Item.TYPE_LOST_WATCH;
                    break;
                default:
                    type = -1;
                    break;
            }
            if (type != -1) {
                Item item = new Item(type, name == null ? number : name, date);
                item.mNumber = number;
                mItems.add(item);
            }
        } while (cursor.moveToNext());
        cursor.close();
        Collections.sort(mItems,itemComparator);
        for(Item item : mItems){
            Log.d(TAG,"onCallsFetched "+item);
        }
        mAdapter.notifyDataSetChanged();
        return true;
    }

    public boolean onBTCallsFetched(Cursor cursor) {
        ArrayList<Item> phoneRecord = new ArrayList<>();
        for (Item item : mItems){
            switch (item.mType){
                case Item.TYPE_IN_PHONE:
                case Item.TYPE_OUT_PHONE:
                case Item.TYPE_LOST_PHONE:
                    phoneRecord.add(item);
                    break;
                default:
                    break;
            }
        }
        mItems.removeAll(phoneRecord);
        if (cursor.getCount() == 0) return true;
        cursor.moveToFirst();
        do {
            final int callType = cursor.getInt(BTCallLogQuery.CALL_TYPE);
            final String number = cursor.getString(BTCallLogQuery.NUMBER);
            final String name = cursor.getString(BTCallLogQuery.NAME);
            final long date = cursor.getLong(BTCallLogQuery.DATE);

            int type;
            switch (callType) {
                case DataProviderContract.INCOMING_TYPE:
                    type = Item.TYPE_IN_PHONE;
                    break;
                case DataProviderContract.OUTGOING_TYPE:
                    type = Item.TYPE_OUT_PHONE;
                    break;
                case DataProviderContract.MISSED_TYPE:
                    type = Item.TYPE_LOST_PHONE;
                    break;
                default:
                    type = -1;
                    break;
            }
            if (type != -1) {
                Item item = new Item(type, name == null ? number : name,date);
                item.mNumber = number;
                mItems.add(item);
            }
        } while (cursor.moveToNext());
        cursor.close();
        Collections.sort(mItems,itemComparator);
        for(Item item : mItems){
            Log.d(TAG,"onBTCallsFetched "+item);
        }
        mAdapter.notifyDataSetChanged();
        return true;
    }

    public void startCallsQuery() {
        mCallLogQueryHandler.fetchCalls(mCallTypeFilter, mDateLimit);
        mBTCallLogQueryHandler.fetchCalls(mCallTypeFilter, mDateLimit);
    }

    /**
     * Requests updates to the data to be shown.
     */
    private void refreshData() {
        // Prevent unnecessary refresh.
        if (mRefreshDataRequired) {
//            mAdapter.invalidateCache();
            startCallsQuery();
            updateOnEntry();
            mRefreshDataRequired = false;
        }
    }

    /**
     * Updates call data and notification state while leaving the call log tab.
     */
    private void updateOnExit() {
        updateOnTransition(false);
    }

    /**
     * Updates call data and notification state while entering the call log tab.
     */
    private void updateOnEntry() {
        updateOnTransition(true);
    }

    // TODO: Move to CallLogActivity
    private void updateOnTransition(boolean onEntry) {
        // We don't want to update any call data when keyguard is on because the user has likely not
        // seen the new calls yet.
        // This might be called before onCreate() and thus we need to check null explicitly.
        if (mKeyguardManager != null && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
            // On either of the transitions we update the missed call and voicemail notifications.
            // While exiting we additionally consume all missed calls (by marking them as read).
            mCallLogQueryHandler.markNewCallsAsOld();
            if (!onEntry) {
                mCallLogQueryHandler.markMissedCallsAsRead();
            }
        }
    }

    class Item {
        public final static int TYPE_IN_WATCH = 0;
        public final static int TYPE_OUT_WATCH = 1;
        public final static int TYPE_LOST_WATCH = 2;
        public final static int TYPE_IN_PHONE = 3;
        public final static int TYPE_OUT_PHONE = 4;
        public final static int TYPE_LOST_PHONE = 5;
        int mType;
        String mNumber;
        String mNameOrNumber;
        Long mDate;

        public Item(int mType, String mNameOrNumber, Long date) {
            this.mType = mType;
            this.mNameOrNumber = mNameOrNumber;
            this.mDate = date;
        }

        @Override
        public String toString() {
            SimpleDateFormat dspFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            return "Item{" +
                    "mType=" + mType +
                    ", mNumber='" + mNumber + '\'' +
                    ", mNameOrNumber='" + mNameOrNumber + '\'' +
                    ", mDate=" + dspFmt.format(mDate) +
                    '}';
        }
    }

    public class SortComparator implements Comparator {
        @Override
        public int compare(Object lhs, Object rhs) {
            Item a = (Item) lhs;
            Item b = (Item) rhs;

            return (int) (b.mDate - a.mDate);
        }
    }
    class RecordItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageType;
        public TextView mTextNameOrNumber;

        public RecordItemViewHolder(View itemView) {
            super(itemView);
            mImageType = (ImageView) itemView.findViewById(R.id.record_type_image);
            mTextNameOrNumber = (TextView) itemView.findViewById(R.id.record_name);
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
            return new RecordItemViewHolder(mInflater.inflate(R.layout.phone_record_item, null));
        }

        @Override
        public void onBindViewHolder(RecordItemViewHolder holder, int position) {
            int drawableResId;
            switch (mItems.get(position).mType) {
                case Item.TYPE_IN_WATCH:
                case Item.TYPE_IN_PHONE:
                    drawableResId = R.drawable.ic_phone_record_in;
                    break;
                case Item.TYPE_OUT_PHONE:
                    drawableResId = R.drawable.ic_phone_record_out_phone;
                    break;
                case Item.TYPE_OUT_WATCH:
                    drawableResId = R.drawable.ic_phone_record_out_watch;
                    break;
                default:
                    drawableResId = R.drawable.ic_phone_record_lost;
                    break;
            }
            holder.mImageType.setImageResource(drawableResId);
            String nameOrNumber = mItems.get(position).mNameOrNumber;
            if (isG7) {
                if (PhoneNumberUtils.isGlobalPhoneNumber(nameOrNumber)) {
                    holder.mTextNameOrNumber.setTextSize(28);
                } else {
                    holder.mTextNameOrNumber.setTextSize(30);
                }
            }
            holder.mTextNameOrNumber.setText(nameOrNumber);
        }

        @Override
        public int getItemCount() {
            int size = mItems.size();
            if(size == 0){
                mNoMsg.setVisibility(View.VISIBLE);
            }else{
                mNoMsg.setVisibility(View.GONE);
            }
            return mItems.size();
        }
    }
}
