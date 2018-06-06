package com.mstarc.wearablesettings.activitys;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;
import com.mstarc.fakewatch.settings.Settings;
import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.common.DecorationSettingItem;
import com.mstarc.wearablesettings.common.RecyclerViewItemTouchListener;

public class SettingsActivity extends BaseActivity implements RecyclerViewItemTouchListener.OnItemClickEventListener{

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private String[] mListItems;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initListView();

//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED)
//        {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
//        }
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED)
//        {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
//        }
        mIsSettingReady = false;
        mSettings.setStWatchServiceConnectListener(new Settings.WatchServiceConnectListener() {

            @Override
            public void onWatchServiceConnected() {
                mIsSettingReady = true;
                m_api_msg = mSettings.getM_api_msg();
            }

            @Override
            public void onWatchServiceDisconnected() {
                mIsSettingReady = false;
		m_api_msg = null;
                mSettings.init(SettingsActivity.this);
            }
        });
        mSettings.init(this);
//        startService(new Intent(this,MyService.class));
    }

    private void initListView()   {
        mListItems = getResources().getStringArray(R.array.settings);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclelist);
        SettingAdapter mAdapter = new SettingAdapter(SettingsActivity.this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnItemTouchListener(new RecyclerViewItemTouchListener(this,this));
        recyclerView.addItemDecoration(new DecorationSettingItem(this, LinearLayoutManager.VERTICAL, R.drawable.list_divider));
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Intent intent = new Intent();
        switch (adapterPosition){
            case 0:
                intent.setClass(SettingsActivity.this,NetWorkActivity.class);
                break;
            case 1:
                intent.setClass(SettingsActivity.this,SafeActivity.class);
                break;
            case 2:
                intent.setClass(SettingsActivity.this,PreferenesActivity.class);
                break;
            case 3:
                intent.setClass(SettingsActivity.this,SystemUpdateActivity.class);
//                intent.setClass(SettingsActivity.this,DownloadActivityTest.class);
                break;
            case 4:
                intent.setClass(SettingsActivity.this,ResetWatchActivity.class);
                break;
            case 5:
                intent.setClass(SettingsActivity.this,DeviceInfoActivity.class);
                break;
        }
        startActivity(intent);
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    public class SettingAdapter extends RecyclerView.Adapter<ViewHolder> {
        // Set numbers of Card in RecyclerView.
        // Only one line for current job
        private LayoutInflater mInflater;

        public SettingAdapter (Context context){
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.setting_item_layout,null));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {

            holder.mTitle.setText(mListItems[position]);
        }

        @Override
        public int getItemCount() {
            return mListItems.length;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView)itemView.findViewById(R.id.title);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSettings.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in,R.anim.slide_right);
    }

    //    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
//    {
//
//        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
//        {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
//            {
//                Toast.makeText(SettingsActivity.this, "Permission OK", Toast.LENGTH_SHORT).show();
//            } else
//            {
//                // Permission Denied
//                Toast.makeText(SettingsActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
//            }
//            return;
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }
}
