package com.mstarc.music.wearablemusic.fragment;

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

import com.mstarc.music.ThemeUtils;
import com.mstarc.music.wearablemusic.R;
import com.mstarc.music.wearablemusic.MusicManager;

import swipereveallayout.SwipeRevealLayout;
import swipereveallayout.ViewBinderHelper;

public class MusicRecyclerViewAdapter extends RecyclerView.Adapter<MusicRecyclerViewAdapter.ViewHolder> implements ConfirmDialog.Listener{

    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();
    private int mCurrentIndex;
    private int mColor;
    public MusicRecyclerViewAdapter() {
        viewBinderHelper.setOpenOnlyOne(true);
    }

    public void setIndex(int index) {
        mCurrentIndex = index;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_music_item, parent, false);

        mColor = ThemeUtils.getCurrentPrimaryColor();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mMusicName.setText(MusicManager.getInstance().getMusicList().get(position));
        if (position == mCurrentIndex) {
//            holder.mbadgeView.setImageResource(R.drawable.icon_music_badge_playing);
            ColorFilter filter = new LightingColorFilter(Color.BLACK, mColor);
            Drawable drawable = ContextCompat.getDrawable(holder.mView.getContext(), R.drawable.icon_music_badge_playing);
            drawable.clearColorFilter();
            drawable.mutate().setColorFilter(filter);
            holder.mbadgeView.setBackground(drawable);
            holder.mMusicName.setTextColor(mColor);
        } else {
            holder.mbadgeView.setBackground(ContextCompat.getDrawable(holder.mView.getContext(), R.drawable.icon_music_badge));
            holder.mMusicName.setTextColor(Color.WHITE);
        }
        holder.mDeleteItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicManager.getInstance().deleteMusicItem(position);
                notifyDataSetChanged();
            }
        });

        viewBinderHelper.bind(holder.mSwipeLayout, holder.mMusicName.getText().toString());

        holder.mMusicName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicManager.getInstance().playIndex(position);
                mCurrentIndex = position;
                notifyDataSetChanged();
            }
        });

        holder.mMusicName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String clear = v.getResources().getString(R.string.clear_record);
                ConfirmDialog confirmDialog = new ConfirmDialog(v.getContext(),
                        R.layout.dialog_clear, MusicRecyclerViewAdapter.this, clear);
                confirmDialog.show();
                return false;
            }
        });
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

    @Override
    public int getItemCount() {
        if (MusicManager.getInstance().getMusicList() == null) {
            return 0;
        }
        return MusicManager.getInstance().getMusicList().size();
    }

    @Override
    public void onConfirm() {
        MusicManager.getInstance().delteMusicAll();
    }

    @Override
    public void onCancel() {

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public TextView mMusicName;
        public ImageView mbadgeView;
        public ImageView mDeleteItem;
        SwipeRevealLayout mSwipeLayout;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mMusicName = (TextView) view.findViewById(R.id.record_name_view);
            mbadgeView = (ImageView) view.findViewById(R.id.music_badge_view);
            mDeleteItem = (ImageView) view.findViewById(R.id.item_delete);
            mSwipeLayout = (SwipeRevealLayout) view.findViewById(R.id.swipe_layout);
            updateImageView(mDeleteItem, R.drawable.icon_music_item_delete);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mMusicName.getText() + "'";
        }
    }
}
