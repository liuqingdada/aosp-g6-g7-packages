package com.cleveroad.loopbar.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cleveroad.loopbar.model.SportInfo;
import com.mstarc.wearablesport.R;
import com.mstarc.wearablesport.ThemeUtils;

import java.util.List;


@SuppressWarnings("WeakerAccess")
public class SimpleCategoriesAdapter extends RecyclerView.Adapter<SimpleCategoriesAdapter.SimpleCategoriesHolder> {

    private List<ICategoryItem> mCategoryItems;
    Vibrator mVibrator;

    public SimpleCategoriesAdapter(List<ICategoryItem> items) {
        mCategoryItems = items;
    }

    /**
     * Create itemView
     */
    public View createView(ViewGroup parent) {
        mVibrator = (Vibrator) parent.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.sport_progress_item, parent, false);
    }

    @Override
    public SimpleCategoriesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SimpleCategoriesHolder(createView(parent), mVibrator);
    }

    @Override
    public void onBindViewHolder(SimpleCategoriesHolder holder, int position) {
        holder.bindItem(getItem(position));
    }

    public ICategoryItem getItem(int position) {
        return mCategoryItems.get(position);
    }

    @Override
    public int getItemCount() {
        return mCategoryItems.size();
    }

    public static class SimpleCategoriesHolder extends BaseRecyclerViewHolder<ICategoryItem> {
        private static final String TAG = "SimpleCategoriesHolder";
        private TextView itemName, itemValue, itemDanwei;
        private ImageView itemIconTimer;
        Vibrator mVibrator;

        public SimpleCategoriesHolder(@NonNull View itemView, Vibrator vibrator) {
            super(itemView);
            mVibrator = vibrator;
            itemName = (TextView) itemView.findViewById(R.id.sport_progress_item_name);
            itemValue = (TextView) itemView.findViewById(R.id.sport_progress_item_value);
            itemDanwei = (TextView) itemView.findViewById(R.id.sport_progress_item_danwei);
            itemIconTimer = (ImageView) itemView.findViewById(R.id.sport_progress_item_timer);
            updateImageView(itemIconTimer, R.drawable.miaobiao);
        }

        @Override
        protected void onBindItem(ICategoryItem item) {
            itemName.setText(item.getName());
            itemValue.setText(item.getValue());
            itemDanwei.setText(item.getDanwei());
            itemIconTimer.setVisibility(item.isTargetReached() ? View.VISIBLE : View.INVISIBLE);
            if (item instanceof SportInfo) {
                SportInfo sportInfo = (SportInfo) item;
                Log.d(TAG, item.getName() + " " + item.getValue() + " " + sportInfo.hasVibrated + item.isTargetReached());
                if (!(((SportInfo) item).mValue.equals("0.0") || ((SportInfo) item).mValue.equals("0")) && !sportInfo.hasVibrated && item.isTargetReached()) {
                    sportInfo.hasVibrated = true;
                    //  mVibrator.vibrate(1000);
                    //震动放在服务里,此处注掉  wyg
                }
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
                    if (view instanceof ImageView) {
                        ((ImageView) view).setImageDrawable(drawable);
                    } else {
                        view.setBackground(drawable);
                    }
                }
            });
        }
    }
}
