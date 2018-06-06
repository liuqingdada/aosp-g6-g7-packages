package com.mstarc.wearablesettings.activitys;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
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
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.*;
import android.app.Service;
import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.common.DecorationSettingItem;
import com.mstarc.wearablesettings.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablesettings.utils.SharedPreferencesHelper;
import com.mstarc.wearablesettings.utils.ThemeUtils;
import com.mstarc.wearablesettings.common.SimDialog;

import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.MODEDATA;
//import  com.android.internal.telephony.TelephonyIntents.ACTION_SIM_STATE_CHANGED;


public class NetWorkActivity extends BaseActivity implements RecyclerViewItemTouchListener.OnItemClickEventListener {

    private static final String TAG = NetWorkActivity.class.getSimpleName();
    String[] mListItems;
    private SharedPreferencesHelper mSPH;
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    SimStateReceiver mSimStateReceiver = new SimStateReceiver();
    boolean isSimValid = false;
    SimDialog mSimDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSPH = SharedPreferencesHelper.getInstance(this);
        initListView();
        mSimDialog = new SimDialog(this);
    }

    @Override
    public void onResume() {
        this.registerReceiver(mSimStateReceiver, new IntentFilter(ACTION_SIM_STATE_CHANGED));
        updateSimState();
        super.onResume();
    }

    @Override
    public void onPause() {
        this.unregisterReceiver(mSimStateReceiver);
        if (mSimDialog != null && mSimDialog.isShowing()) {
            mSimDialog.dismiss();
        }
        super.onPause();
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);

    }
    class SimStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {
                updateSimState();
            }
        }
    }

    void updateSimState() {
        // Sim state changed
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Service.TELEPHONY_SERVICE);
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

    private void initListView() {
        mListItems = getResources().getStringArray(R.array.network);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclelist);
        NetworkAdapter mAdapter = new NetworkAdapter(NetWorkActivity.this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnItemTouchListener(new RecyclerViewItemTouchListener(this, this));
        recyclerView.addItemDecoration(new DecorationSettingItem(this, LinearLayoutManager.VERTICAL, R.drawable.list_divider));
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        Intent intent = new Intent();
        switch (adapterPosition) {
            case 0:
                intent.setClass(NetWorkActivity.this, WifiConnectActivity.class);
                startActivity(intent);
                break;
            case 1:
                intent.setClass(NetWorkActivity.this, BTConnectActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    public class NetworkAdapter extends RecyclerView.Adapter<ViewHolder> {
        // Set numbers of Card in RecyclerView.
        // Only one line for current job
        private LayoutInflater mInflater;

        public NetworkAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.setting_network_item_layout, null));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.mTitle.setText(mListItems[position]);
            if (position == 2) {
                if(!isSimValid) {
                    holder.mImage.setBackground(null);
                    holder.mImage.setImageResource(R.mipmap.guanbi);
                } else {
                    try {
                        if(mSettings.isMobileData()) {
                            holder.mImage.setImageBitmap(null);
                            updateImageView(holder.mImage,R.mipmap.kai);
                        }else{
                            holder.mImage.setBackground(null);
                            holder.mImage.setImageResource(R.mipmap.guanbi);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        Log.e(TAG,"get is mobile data error " + e);
                        if(mSPH.getBoolean(MODEDATA,true)) {
                            holder.mImage.setImageBitmap(null);
                            updateImageView(holder.mImage,R.mipmap.kai);
                        }else{
                            holder.mImage.setBackground(null);
                            holder.mImage.setImageResource(R.mipmap.guanbi);
                        }
                    }
                }
            } else {
                holder.mImageBg.setVisibility(View.GONE);
                updateImageView(holder.mImage,R.mipmap.jiantou);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (position == 2) {
//                        if(!isHasSimCard()) {
//                            Toast.makeText(NetWorkActivity.this,getString(R.string.no_sim),Toast.LENGTH_LONG).show();
//                            return;
//                        }
                        if (!isSimValid && mSimDialog != null) {
                            mSimDialog.show();
                            Log.d(TAG,"show no sim dialog");
                            return;
                        }
                        try {
                            mSettings.setMobileData(!mSPH.getBoolean(MODEDATA,true));
                            mSPH.putBoolean(MODEDATA,!mSPH.getBoolean(MODEDATA,true));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Toast.makeText(NetWorkActivity.this,getString(R.string.option_error),Toast.LENGTH_LONG).show();
                            Log.e(TAG,"set mobile data error " + e);
                        }
                        if(mSPH.getBoolean(MODEDATA,true)) {
                            holder.mImage.setImageBitmap(null);
                            updateImageView(holder.mImage,R.mipmap.kai);
                        }else{
                            holder.mImage.setBackground(null);
                            holder.mImage.setImageResource(R.mipmap.guanbi);
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mListItems.length;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;
        ImageView mImage;
        ImageView mImageBg;

        public ViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.title);
            mImage = (ImageView) itemView.findViewById(R.id.imageview);
            mImageBg = (ImageView) itemView.findViewById(R.id.imagebg);
        }
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    private boolean isHasSimCard() {
        TelephonyManager telMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                result = false; // 没有SIM卡
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false;
                break;
        }
        Log.d(TAG, result ? "有SIM卡" : "无SIM卡");
        return result;
    }
}
