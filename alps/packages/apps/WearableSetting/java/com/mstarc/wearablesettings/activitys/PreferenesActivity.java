package com.mstarc.wearablesettings.activitys;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.common.DecorationSettingItem;
import com.mstarc.wearablesettings.utils.SharedPreferencesHelper;
import com.mstarc.wearablesettings.utils.ThemeUtils;

import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.AWAYBODY;
import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.MODEDATA;
import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.POWER_SAVE;
import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.SCHEDULE;
import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.SEDENT;

public class PreferenesActivity extends BaseActivity {

    private final static String TAG = PreferenesActivity.class.getSimpleName();
    private String[] mListItems;
    private SharedPreferencesHelper mSPH;
    private WifiManager mWifiManager;
    private BluetoothAdapter mBluetoothAdapter;
    enum type {
        HAND,
        LIGHT,
        POWER
    }
    private type currentType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSPH = SharedPreferencesHelper.getInstance(this);
        initListView();
        initDataManager();
    }

    private void initListView() {
        mListItems = getResources().getStringArray(R.array.preferences);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclelist);
        PreferenesAdapter mAdapter = new PreferenesAdapter(PreferenesActivity.this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DecorationSettingItem(this, LinearLayoutManager.VERTICAL, R.drawable.list_divider));
    }

    private void initDataManager() {
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }
    private void showDilog(final ImageView button) {
        final Dialog dialog = new Dialog(this, R.style.tip_dialog);//指定自定義樣式
        //1. 先获取布局的view
        RelativeLayout view = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.switch_dialog, null);
        TextView content = (TextView)view.findViewById(R.id.tv_content);
        if (currentType == type.POWER) {
            content.setText(R.string.power_tip);
        }
        //2. 加载 view
        dialog.setContentView(view);//指定自定義layout
        dialog.setCanceledOnTouchOutside(false);
        //3. 获取dialog view 下的控件
        ImageView cancel = (ImageView) view.findViewById(R.id.btn_N);
        ImageView ok = (ImageView) view.findViewById(R.id.btn_Y);
        updateImageView(ok, R.drawable.btn_ok);
        updateImageView(cancel, R.drawable.btn_cancel);
        //4.对控件做设置或者设置listenner
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                button.setImageBitmap(null);
                updateImageView(button, R.mipmap.kai);
                switch (currentType) {
                    case HAND:
                        Settings.System.putInt(getContentResolver(), "HandUpLight", 1);
                        break;
                    case LIGHT:
                        mSettings.setConstantLight(true);
                        break;
                    case POWER:
                        if (mWifiManager.isWifiEnabled()) {
                            mWifiManager.setWifiEnabled(false);
                        }
                        if (mBluetoothAdapter.isEnabled()) {
                            mBluetoothAdapter.disable();
                        }
                        if (mSPH.getBoolean(MODEDATA,true)) {
                            try {
                                mSettings.setMobileData(false);
                                mSPH.putBoolean(MODEDATA,false);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                Log.e(TAG,"set mobile data error " + e);
                            }
                        }
                        mSPH.putBoolean(POWER_SAVE,true);
                        break;
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        //  5. 直接
        dialog.show();
    }


    public class PreferenesAdapter extends RecyclerView.Adapter<ViewHolder> {
        // Set numbers of Card in RecyclerView.
        // Only one line for current job
        private LayoutInflater mInflater;

        public PreferenesAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.setting_safe_item_layout, null));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.mTitle.setText(mListItems[position]);
            switch (position) {
                case 0:
                    holder.mImageBg.setVisibility(View.GONE);
                    updateImageView(holder.mImageNext, R.mipmap.jiantou);
                    break;
                case 1:
                    try {
                        if (mSettings.getScheduleReminder()) {
                            holder.mImageNext.setImageBitmap(null);
                            updateImageView(holder.mImageNext, R.mipmap.kai);
                            mSPH.putBoolean(SCHEDULE, true);
                        } else {
                            holder.mImageNext.setBackground(null);
                            holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            mSPH.putBoolean(SCHEDULE, false);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        if (mSPH.getBoolean(SCHEDULE, true)) {
                            holder.mImageNext.setImageBitmap(null);
                            updateImageView(holder.mImageNext, R.mipmap.kai);
                        } else {
                            holder.mImageNext.setBackground(null);
                            holder.mImageNext.setImageResource(R.mipmap.guanbi);
                        }
                    }
                    break;
                case 2:
                    try {
                        if (mSettings.getSedentarinessReminder()) {
                            holder.mImageNext.setImageBitmap(null);
                            updateImageView(holder.mImageNext, R.mipmap.kai);
                            mSPH.putBoolean(SEDENT, true);
                        } else {
                            holder.mImageNext.setBackground(null);
                            holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            mSPH.putBoolean(SEDENT, false);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        if (mSPH.getBoolean(SEDENT, true)) {
                            holder.mImageNext.setImageBitmap(null);
                            updateImageView(holder.mImageNext, R.mipmap.kai);
                        } else {
                            holder.mImageNext.setBackground(null);
                            holder.mImageNext.setImageResource(R.mipmap.guanbi);
                        }
                    }
                    break;
                case 3:
                    if (mSettings.isConstantLight()) {
                        holder.mImageNext.setImageBitmap(null);
                        updateImageView(holder.mImageNext, R.mipmap.kai);
                    } else {
                        holder.mImageNext.setBackground(null);
                        holder.mImageNext.setImageResource(R.mipmap.guanbi);
                    }
                    break;
                case 4:
                    if (mSettings.isClickScreenLight()) {
                        holder.mImageNext.setImageBitmap(null);
                        updateImageView(holder.mImageNext, R.mipmap.kai);
                    } else {
                        holder.mImageNext.setBackground(null);
                        holder.mImageNext.setImageResource(R.mipmap.guanbi);
                    }
                    break;
                case 5:
                    int light = Settings.System.getInt(getContentResolver(), "HandUpLight", 0);
                    if (light == 1) {
                        holder.mImageNext.setImageBitmap(null);
                        updateImageView(holder.mImageNext, R.mipmap.kai);
                    } else {
                        holder.mImageNext.setBackground(null);
                        holder.mImageNext.setImageResource(R.mipmap.guanbi);
                    }

                    break;
                case 6:
                    try {
                        if (mSettings.getMobileAwayBodyRemind()) {
                            Log.i(TAG, "body remind true");
                            holder.mImageNext.setImageBitmap(null);
                            updateImageView(holder.mImageNext, R.mipmap.kai);
                            mSPH.putBoolean(AWAYBODY, true);
                        } else {
                            Log.i(TAG, "body remind false");
                            holder.mImageNext.setBackground(null);
                            holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            mSPH.putBoolean(AWAYBODY, false);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "body remind exception " + e);
                        e.printStackTrace();
                        if (mSPH.getBoolean(AWAYBODY, false)) {
                            holder.mImageNext.setImageBitmap(null);
                            updateImageView(holder.mImageNext, R.mipmap.kai);
                        } else {
                            holder.mImageNext.setBackground(null);
                            holder.mImageNext.setImageResource(R.mipmap.guanbi);
                        }
                    }
                    break;
                case 7:
                    int stop = Settings.System.getInt(getContentResolver(), "stopstranger", 1);
                    if (stop == 1) {
                        holder.mImageNext.setImageBitmap(null);
                        updateImageView(holder.mImageNext, R.mipmap.kai);
                    } else {
                        holder.mImageNext.setBackground(null);
                        holder.mImageNext.setImageResource(R.mipmap.guanbi);
                    }
                    break;
                case 8:
                    if (mWifiManager.isWifiEnabled() || mSPH.getBoolean(MODEDATA,true) || mBluetoothAdapter.isEnabled()) {
                        holder.mImageNext.setBackground(null);
                        holder.mImageNext.setImageResource(R.mipmap.guanbi);
                        mSPH.putBoolean(POWER_SAVE,false);
                    } else {
                        holder.mImageNext.setImageBitmap(null);
                        updateImageView(holder.mImageNext, R.mipmap.kai);
                        mSPH.putBoolean(POWER_SAVE,true);
                    }
            /*    case 8:
                    int stopSms = Settings.System.getInt(getContentResolver(), "stopsms", 1);
                    if (stopSms == 1) {
                        holder.mImageNext.setImageBitmap(null);
                        updateImageView(holder.mImageNext, R.mipmap.kai);
                    } else {
                        holder.mImageNext.setBackground(null);
                        holder.mImageNext.setImageResource(R.mipmap.guanbi);
                    }
                    break;*/
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (position) {
                        case 0:
                            Intent i = new Intent(PreferenesActivity.this, LockTimeActivity.class);
                            startActivity(i);
                            break;
                        case 1:
                            if (mIsSettingReady) {
                                try {
                                    mSettings.setScheduleReminder(!mSPH.getBoolean(SCHEDULE, true));
                                    mSPH.putBoolean(SCHEDULE, !mSPH.getBoolean(SCHEDULE, true));
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                    Toast.makeText(PreferenesActivity.this, getString(R.string.option_error), Toast.LENGTH_LONG).show();
                                }
                                if (mSPH.getBoolean(SCHEDULE, true)) {
                                    holder.mImageNext.setImageBitmap(null);
                                    updateImageView(holder.mImageNext, R.mipmap.kai);
                                } else {
                                    holder.mImageNext.setBackground(null);
                                    holder.mImageNext.setImageResource(R.mipmap.guanbi);
                                }
                            } else {
                                Toast.makeText(PreferenesActivity.this, getString(R.string.option_error), Toast.LENGTH_LONG).show();
                            }

                            break;
                        case 2:
                            if (mIsSettingReady) {
                                try {
                                    mSettings.setSedentarinessReminder(!mSPH.getBoolean(SEDENT, true));
                                    mSPH.putBoolean(SEDENT, !mSPH.getBoolean(SEDENT, true));
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                    Toast.makeText(PreferenesActivity.this, getString(R.string.option_error), Toast.LENGTH_LONG).show();
                                }
                                if (mSPH.getBoolean(SEDENT, true)) {
                                    holder.mImageNext.setImageBitmap(null);
                                    updateImageView(holder.mImageNext, R.mipmap.kai);
                                } else {
                                    holder.mImageNext.setBackground(null);
                                    holder.mImageNext.setImageResource(R.mipmap.guanbi);
                                }
                            } else {
                                Toast.makeText(PreferenesActivity.this, getString(R.string.option_error), Toast.LENGTH_LONG).show();
                            }
                            break;
                        case 3:
                            if (mSettings.isConstantLight()) {
                                mSettings.setConstantLight(false);
                                holder.mImageNext.setBackground(null);
                                holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            } else {
                                currentType = type.LIGHT;
                                showDilog(holder.mImageNext);
                            }

                            break;
                        case 4:
                            mSettings.setClickScreenLight(!mSettings.isClickScreenLight());
                            if (mSettings.isClickScreenLight()) {
                                holder.mImageNext.setImageBitmap(null);
                                updateImageView(holder.mImageNext, R.mipmap.kai);
                            } else {
                                holder.mImageNext.setBackground(null);
                                holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            }
                            break;
                        case 5:
                            int last = Settings.System.getInt(getContentResolver(), "HandUpLight", 0);
                            if (last == 1) {
                                Settings.System.putInt(getContentResolver(), "HandUpLight", 0);
                                holder.mImageNext.setBackground(null);
                                holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            } else {
                                currentType = type.HAND;
                                showDilog(holder.mImageNext);
                            }

                            break;
                        case 6:
                            if (mIsSettingReady) {
                                try {
                                    Log.e(TAG, "setMobileAwayBodyRemind is : " + !mSPH.getBoolean(AWAYBODY, false));
                                    mSettings.setMobileAwayBodyRemind(!mSPH.getBoolean(AWAYBODY, false));
                                    mSPH.putBoolean(AWAYBODY, !mSPH.getBoolean(AWAYBODY, true));
                                    Log.e(TAG, "getMobileAwayBodyRemind is : " + mSettings.getMobileAwayBodyRemind());
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                    Toast.makeText(PreferenesActivity.this, getString(R.string.option_error), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "setMobileAwayBodyRemind exception " + e);
                                }
                                if (mSPH.getBoolean(AWAYBODY, false)) {
                                    holder.mImageNext.setImageBitmap(null);
                                    updateImageView(holder.mImageNext, R.mipmap.kai);
                                } else {
                                    holder.mImageNext.setBackground(null);
                                    holder.mImageNext.setImageResource(R.mipmap.guanbi);
                                }
                            } else {
                                Toast.makeText(PreferenesActivity.this, getString(R.string.option_error), Toast.LENGTH_LONG).show();
                            }
                            break;
                        case 7:
                            int stop = Settings.System.getInt(getContentResolver(), "stopstranger", 1);
                            Settings.System.putInt(getContentResolver(), "stopstranger", stop == 1 ? 0 : 1);
                            int now = Settings.System.getInt(getContentResolver(), "stopstranger", 1);
                            if (now == 1) {
                                holder.mImageNext.setImageBitmap(null);
                                updateImageView(holder.mImageNext, R.mipmap.kai);
                            } else {
                                holder.mImageNext.setBackground(null);
                                holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            }
                            break;
                        case 8:
                            if (mSPH.getBoolean(POWER_SAVE,false)) {
                                holder.mImageNext.setBackground(null);
                                holder.mImageNext.setImageResource(R.mipmap.guanbi);
                                mSPH.putBoolean(POWER_SAVE,false);
                            } else {
                                currentType = type.POWER;
                                showDilog(holder.mImageNext);
                            }
                            break;
                       /* case 8:
                            //stopsms
                            int stopsms = Settings.System.getInt(getContentResolver(), "stopsms", 1);
                            Settings.System.putInt(getContentResolver(), "stopsms", stopsms == 1 ? 0 : 1);
                            int nowSms = Settings.System.getInt(getContentResolver(), "stopsms", 1);
                            if (nowSms == 1) {
                                holder.mImageNext.setImageBitmap(null);
                                updateImageView(holder.mImageNext, R.mipmap.kai);
                            } else {
                                holder.mImageNext.setBackground(null);
                                holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            }
                            break;*/
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
        ImageView mImageNext;
        ImageView mImageBg;

        public ViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView) itemView.findViewById(R.id.title);
            mImageNext = (ImageView) itemView.findViewById(R.id.imagenext);
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
}
