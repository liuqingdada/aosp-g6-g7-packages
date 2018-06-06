package com.mstarc.wearablephone.view;

import android.app.Dialog;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablephone.DialOutActivity;
import com.mstarc.wearablephone.R;
import com.mstarc.wearablephone.bluetooth.BTCallManager;
import com.mstarc.wearablephone.database.DatabaseWizard;
import com.mstarc.wearablephone.database.bean.InterceptCall;
import com.mstarc.wearablephone.view.common.DecorationSettingItem;
import com.mstarc.wearablephone.view.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablephone.view.common.SimDialog;
import com.mstarc.wearablephone.view.common.StatedFragment;

import java.util.ArrayList;
import java.util.List;

import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_BOOLEAN;
import static com.mstarc.wearablephone.DialOutActivity.INTENT_DIAL_BY_PHONE_NUMBER;

/**
 * Created by liuqing
 * 17-11-23.
 * Email: 1239604859@qq.com
 */

public class InterceptPhoneFragment extends StatedFragment
        implements RecyclerViewItemTouchListener.OnItemClickEventListener {
    private static final String TAG = InterceptPhoneFragment.class.getSimpleName();
    private List<InterceptCall> mList = new ArrayList<>();

    private RecyclerView mRecyclerView;
    private IntercepteAdapter mAdapter;
    private TextView mNoDataView;

    private String mOutgongNumber;
    private SimDialog mSimDialog;
    boolean isSimValid = false;

    ////////////////////////////////懒加载实现//////////////////////////////////
    private boolean isViewInitFinished;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        isViewInitFinished = true;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        requestData(isVisibleToUser);
        Log.d(TAG, "setUserVisibleHint: ");
    }

    private void requestData(boolean isVisibleToUser) {
        if (isViewInitFinished && isVisibleToUser) {
            // TODO request data from server
        }
    }
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected void onFirstTimeLaunched() {
        super.onFirstTimeLaunched();
        Log.d(TAG, "onFirstTimeLaunched: ");
    }

    @Override
    protected void onSaveState(Bundle outState) {
        super.onSaveState(outState);
        Log.i(TAG, "onSaveState: ");
        if (mNoDataView != null && mList.size() == 0) {
            mNoDataView.setVisibility(View.VISIBLE);
            mNoDataView.setText(R.string.intercept_no_phone);
            Log.d(TAG, "onRestoreState: " + mNoDataView.getText());
        }
    }

    @Override
    protected void onRestoreState(Bundle savedInstanceState) {
        super.onRestoreState(savedInstanceState);
        Log.i(TAG, "onRestoreState: ");
        if (mNoDataView != null && mList.size() == 0) {
            mNoDataView.setVisibility(View.VISIBLE);
            mNoDataView.setText(R.string.intercept_no_phone);
            Log.d(TAG, "onRestoreState: " + mNoDataView.getText());
        }
    }

    ////////////////////////////////////////////

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: ");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: ");
    }

    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSimDialog = new SimDialog(getActivity());
    }

    private void initData() {
        mList = DatabaseWizard.getInstance()
                              .getDaoSession()
                              .getInterceptCallDao()
                              .loadAll();
        if (mList == null) {
            mList = new ArrayList<>();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        return inflater.inflate(R.layout.intercept_phone_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.intercepte_phone_list);
        mNoDataView = (TextView) view.findViewById(R.id.intercepte_no_msg);
        mNoDataView.setText(R.string.intercept_no_phone);

        mAdapter = new IntercepteAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(),
                                                               LinearLayoutManager.VERTICAL,
                                                               false));
        mRecyclerView.addItemDecoration(new DecorationSettingItem(getActivity(),
                                                                  LinearLayoutManager.VERTICAL,
                                                                  R.drawable.list_divider));
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemTouchListener(getActivity(),
                                                                               this));
        Log.d(TAG, "onViewCreated: ");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        initData();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        if (mSimDialog != null && mSimDialog.isShowing()) {
            mSimDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {}

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Log.d(TAG, "onItemClick: " + mList.get(adapterPosition));

        String number = mList.get(adapterPosition)
                             .getNumber();
        if (number != null) {
            mOutgongNumber = number;
            if (BTCallManager.getInstance(getActivity().getApplicationContext())
                             .isBTPhoneEnnable()) {
                Dialog dialog = new DeviceSelectDialog(getActivity(), dsdListener);
                dialog.show();
            } else {
                dsdListener.byWatch();
            }
        }
    }

    private DeviceSelectDialog.Listener dsdListener = new DeviceSelectDialog.Listener() {
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
                Log.d(TAG, "show no sim dialog");
                return;
            }
            String uri = "tel:" + mOutgongNumber;
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(uri));
            startActivity(callIntent);
            mOutgongNumber = null;
        }
    };

    private void updateSimState() {
        // Sim state changed
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(
                Service.TELEPHONY_SERVICE);
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
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {
        //        DatabaseWizard.getInstance()
        //                      .getDaoSession()
        //                      .getInterceptCallDao()
        //                      .deleteInTx(mList.get(adapterPosition));
        //
        //        mList.remove(adapterPosition);
        //        mAdapter.notifyDataSetChanged();
    }

    //
    private class IntercepteAdapter
            extends RecyclerView.Adapter<IntercepteAdapter.InterceptItemViewHolder> {

        @Override
        public InterceptItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new InterceptItemViewHolder(
                    View.inflate(getActivity(), R.layout.intercepte_phone_item, null));
        }

        @Override
        public void onBindViewHolder(InterceptItemViewHolder holder, int position) {
            holder.headImg.setImageResource(R.drawable.ic_intercepte_phone_common);
            holder.name.setText(mList.get(position)
                                     .getName());
        }

        @Override
        public int getItemCount() {
            if (mList == null) {
                mList = new ArrayList<>();
            }

            if (mList.size() == 0) {
                mNoDataView.setVisibility(View.VISIBLE);
            } else {
                mNoDataView.setVisibility(View.GONE);
            }
            Log.d(TAG, "getItemCount: " + mList.size());
            return mList.size();
        }

        class InterceptItemViewHolder extends RecyclerView.ViewHolder {
            private ImageView headImg;
            private TextView name;

            InterceptItemViewHolder(View itemView) {
                super(itemView);
                headImg = (ImageView) itemView.findViewById(R.id.intercepte_image);
                name = (TextView) itemView.findViewById(R.id.intercepte_name);
            }
        }
    }
}
