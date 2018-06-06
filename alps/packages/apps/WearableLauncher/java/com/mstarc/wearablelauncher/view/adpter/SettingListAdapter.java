package com.mstarc.wearablelauncher.view.adpter;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.ThemeUtils;
import com.mstarc.wearablelauncher.view.settings.SettingFragment;
import com.wanjian.cockroach.App;

import java.util.ArrayList;

/**
 * Created by wangxinzhi on 17-2-12.
 */

public class SettingListAdapter extends RecyclerView.Adapter {

    private static final String TAG = SettingFragment.class.getSimpleName();
    boolean isG7Target = false;

    private static final int[] IconArrary = {
            R.drawable.ic_rightboard_1_phone,
            R.drawable.ic_rightboard_2_sport,
            R.drawable.ic_rightboard_3_webmessage,
            R.drawable.ic_rightboard_5_alipay,
            R.drawable.ic_rightboard_15_music,
            R.drawable.ic_rightboard_6_heartbeat,
            R.drawable.ic_rightboard_8_navi,
            R.drawable.ic_rightboard_10_weather,
            R.drawable.ic_rightboard_11_message,
            R.drawable.ic_rightboard_16_record,
            R.drawable.ic_rightboard_17_radio,
            R.drawable.ic_rightboard_12_calendar,
            R.drawable.ic_rightboard_13_setting
    };

    private static final int[] IconArraryG7 = {
            R.drawable.ic_rightboard_1_phone,
            R.drawable.ic_rightboard_2_sport,
            R.drawable.ic_rightboard_3_webmessage,
            R.drawable.ic_rightboard_5_alipay,
            R.drawable.ic_rightboard_15_music,
            //R.drawable.ic_rightboard_6_heartbeat,
            R.drawable.ic_rightboard_8_navi,
            R.drawable.ic_rightboard_10_weather,
            R.drawable.ic_rightboard_11_message,
            R.drawable.ic_rightboard_14_message,
            R.drawable.ic_rightboard_16_record,
            R.drawable.ic_rightboard_17_radio,
            R.drawable.ic_rightboard_13_setting

    };

    private static final boolean[] bG7IconThemeChanged = {
            false,
            true,
            false,
            false,
            true,
            false,
            false,
            true,
            true,
            false,
            false,
            true
    };

    private final LayoutInflater mInflater;
    private Context mContext;
    private String[] mSettingItemNameArray;
    ArrayList<AppItem> mAppItems;

    public SettingListAdapter(Context context, ArrayList<AppItem> appItems) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mSettingItemNameArray = mContext.getResources().getStringArray(R.array.settinglist);
        isG7Target = mContext.getResources().getBoolean(R.bool.g7_target);
        mAppItems = appItems;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(mInflater.inflate(R.layout.settingitem, null));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        ViewHolder holder = (ViewHolder) viewHolder;
        AppItem appItem = mAppItems.get(i);
        holder.mTextView.setText(appItem.mLable);
        if (ThemeUtils.isProductG7()) {
            if (appItem.isThemechanged) {
                updateImageView(holder.mImageView, appItem.mIconResID);
            } else {
                holder.mImageView.setBackgroundResource(appItem.mIconResID);
            }
        } else {
            holder.mImageView.setBackgroundResource(appItem.mIconResID);
        }
    }

    @Override
    public int getItemCount() {
        return mAppItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;
        ImageView mImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.rightboard_text);
            mImageView = (ImageView) itemView.findViewById(R.id.rightboard_image);

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

    public static class AppItem {
        public ComponentName mComponentName;
        public int mIconResID;
        public String mLable;
        public boolean disabledInflightMode;
        public boolean disabledInIOS;
        public boolean isThemechanged;

        public AppItem(ComponentName mComponentName, int mIconResID, String mLable, boolean disabledInflightMode, boolean disabledInIOS, boolean isThemechanged) {
            this.mComponentName = mComponentName;
            this.mIconResID = mIconResID;
            this.mLable = mLable;
            this.disabledInflightMode = disabledInflightMode;
            this.disabledInIOS = disabledInIOS;
            this.isThemechanged = isThemechanged;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof AppItem) {
                return mComponentName.equals(((AppItem) obj).mComponentName);
            } else {
                return false;
            }
        }
    }

}
