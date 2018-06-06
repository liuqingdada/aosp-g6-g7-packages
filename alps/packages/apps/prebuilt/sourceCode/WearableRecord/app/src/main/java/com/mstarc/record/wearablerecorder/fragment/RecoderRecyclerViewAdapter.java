package com.mstarc.record.wearablerecorder.fragment;

import android.content.Intent;
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
import android.widget.Toast;

import com.mstarc.record.wearablerecorder.R;
import com.mstarc.record.wearablerecorder.RecordManager;
import com.mstarc.record.wearablerecorder.RecordPlayActivity;
import com.mstarc.record.wearablerecorder.ThemeUtils;

import swipereveallayout.SwipeRevealLayout;
import swipereveallayout.ViewBinderHelper;

public class RecoderRecyclerViewAdapter extends RecyclerView.Adapter<RecoderRecyclerViewAdapter.ViewHolder> implements ConfirmDialog.Listener{

    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();
    public RecoderRecyclerViewAdapter() {
        viewBinderHelper.setOpenOnlyOne(true);
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_record_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mRecordName.setText(RecordManager.getInstance().getReverseList().get(position));
        holder.mDeleteItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecordManager.getInstance().deleteRecordItem(position);
                notifyDataSetChanged();
            }
        });

        viewBinderHelper.bind(holder.mSwipeLayout, RecordManager.getInstance().getReverseList().get(position));
        holder.itemView.setTag(position);

        holder.mRecordName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RecordManager.getInstance().isRecording()) {
                    Toast.makeText(holder.mView.getContext(), "正在录音中，请完成录音后播放！",Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("position", position);
                    intent.setClass(holder.mView.getContext(), RecordPlayActivity.class);
                    holder.mView.getContext().startActivity(intent);
                }
            }
        });

        holder.mRecordName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                String clear = v.getResources().getString(R.string.clear_record);
                ConfirmDialog confirmDialog = new ConfirmDialog(v.getContext(),
                        R.layout.dialog_clear, RecoderRecyclerViewAdapter.this, clear);
                confirmDialog.show();
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return RecordManager.getInstance().getReverseList().size();
    }

    @Override
    public void onConfirm() {
        RecordManager.getInstance().deleteAllRecord();
        RecordManager.setRecordTimes("1");
    }

    @Override
    public void onCancel() {

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

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public TextView mRecordName;
        public ImageView mDeleteItem;
        SwipeRevealLayout mSwipeLayout;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mRecordName = (TextView) view.findViewById(R.id.record_name_view);
            mDeleteItem = (ImageView) view.findViewById(R.id.item_delete);
            mSwipeLayout = (SwipeRevealLayout) view.findViewById(R.id.swipe_layout);
            updateImageView(mDeleteItem, R.drawable.icon_record_delete);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mRecordName.getText() + "'";
        }
    }
}
