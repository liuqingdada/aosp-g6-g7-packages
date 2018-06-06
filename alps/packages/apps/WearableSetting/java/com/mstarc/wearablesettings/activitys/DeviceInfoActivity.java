package com.mstarc.wearablesettings.activitys;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.common.DecorationSettingItem;

import java.util.ArrayList;
import java.util.List;

public class DeviceInfoActivity extends BaseActivity {

    private List<DeviceInfo> mDeviceInfos = new ArrayList<DeviceInfo>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        initData();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclelist);
        DeviceInfoAdapter mAdapter = new DeviceInfoAdapter(DeviceInfoActivity.this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DecorationSettingItem(this, LinearLayoutManager.VERTICAL, R.drawable.list_divider));
    }

    private void initData(){

        DeviceInfo product = new DeviceInfo();
        product.title = getResources().getString(R.string.product_name);
        product.subTitle = mSettings.getProductName();
        mDeviceInfos.add(product);

        DeviceInfo infoVersion = new DeviceInfo();
        infoVersion.title = getResources().getString(R.string.product_version);
        infoVersion.subTitle = mSettings.getProductVersion();
        mDeviceInfos.add(infoVersion);

        DeviceInfo infoSN = new DeviceInfo();
        infoSN.title = getResources().getString(R.string.sn_num);
        infoSN.subTitle = mSettings.getSERIAL_NUMBER();
        mDeviceInfos.add(infoSN);

        DeviceInfo infoImei = new DeviceInfo();
        infoImei.title = getResources().getString(R.string.imei);
        infoImei.subTitle = mSettings.getIMEI();
        mDeviceInfos.add(infoImei);

        DeviceInfo infoIccId = new DeviceInfo();
        infoIccId.title = getResources().getString(R.string.iccid);
        infoIccId.subTitle = mSettings.getICCID();
        mDeviceInfos.add(infoIccId);

        DeviceInfo infoCmiitId = new DeviceInfo();
        infoCmiitId.title = getResources().getString(R.string.cmiit_id);
        infoCmiitId.subTitle = mSettings.getCMIT_ID();
        mDeviceInfos.add(infoCmiitId);

        DeviceInfo infoStroage = new DeviceInfo();
        infoStroage.title = getResources().getString(R.string.internal_storage);
        infoStroage.subTitle = mSettings.getSD_CARD();
        mDeviceInfos.add(infoStroage);

        DeviceInfo support = new DeviceInfo();
        support.title = getResources().getString(R.string.voice_t);
        support.subTitle = getResources().getString(R.string.voice_support);
        mDeviceInfos.add(support);
    }

    public class DeviceInfo {
        public String title;
        public String subTitle;
    }

    public class DeviceInfoAdapter extends RecyclerView.Adapter<ViewHolder> {
        // Set numbers of Card in RecyclerView.
        // Only one line for current job
        private LayoutInflater mInflater;

        public DeviceInfoAdapter (Context context){
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.setting_device_info_item_layout,null));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            holder.mTitle.setText(mDeviceInfos.get(position).title);
            holder.mSubtitle.setText(mDeviceInfos.get(position).subTitle);
        }

        @Override
        public int getItemCount() {
            return mDeviceInfos.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle,mSubtitle;

        public ViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView)itemView.findViewById(R.id.title);
            mSubtitle =  (TextView)itemView.findViewById(R.id.subtitle);

        }
    }
}
