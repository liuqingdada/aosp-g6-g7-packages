package com.mstarc.wearablelauncher.view.clock;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.WallpaperInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.watchfacemanager.WatchfaceManager;
import com.mstarc.wearablelauncher.R;
import com.mstarc.wearablelauncher.view.adpter.NolinearMapLayoutManager;
import com.mstarc.wearablelauncher.watchface.WatchfaceFirsttDialog;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by hawking on 17-4-21.
 */

public class WatchFaceSelectActivity extends Activity {
    public static final String EXTRA_PREVIEW_MODE = "android.service.wallpaper.PREVIEW_MODE";

    RecyclerView mListView;
    RecyclerView.Adapter mListAdapter;
    private static final String TAG = WatchFaceSelectActivity.class.getSimpleName();
    WatchfaceManager mWatchfaceManager;
    List<WallpaperInfoWrapper> mWallpapers = new ArrayList<>();
    static final int VIEW_FAKE = 0;
    static final int VIEW_NORMAL = 1;
    ObjectAnimator mRotateAnimator;
    View mRotateImg;
    WatchfaceFirsttDialog mWatchfaceFirsttDialog;
    WallpaperInfo mWallpaperInfo;

    class WallpaperInfoWrapper {
        WallpaperInfo mWallpaperInfo;

        public WallpaperInfoWrapper(WallpaperInfo mWallpaperInfo) {
            this.mWallpaperInfo = mWallpaperInfo;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watchfacepicker);
        mWatchfaceManager = WatchfaceManager.getInstance(this);
        List<WallpaperInfo> wallpaperInfos = mWatchfaceManager.getWallpaperList();
        mWallpapers.clear();
        for (WallpaperInfo wallpaperInfo : wallpaperInfos) {
            mWallpapers.add(new WallpaperInfoWrapper(wallpaperInfo));
        }
        if (mWallpapers.size() != 0) {
            mWallpapers.add(0, new WallpaperInfoWrapper(null));
            boolean isG7 = getResources().getBoolean(R.bool.g7_target);
            //if (!isG7) {
                mWallpapers.add(new WallpaperInfoWrapper(null));
            //}
        }
        mListAdapter = new WatchFaceListAdapter(this);
        mListView = (RecyclerView) findViewById(R.id.list);
        mListView.setLayoutManager(new NolinearMapLayoutManager(this, mListView));
        mListView.setAdapter(mListAdapter);

        WallpaperInfo currentWallpaper = mWatchfaceManager.getCurrentWallpaper();
        if (currentWallpaper != null) {
            Log.d(TAG, "currentWallpaper: " + currentWallpaper.getComponent());
            int size = mWallpapers.size();
            for (int i = 0; i < size; i++) {
                WallpaperInfo wallpaperInfo = mWallpapers.get(i).mWallpaperInfo;
                if (wallpaperInfo != null) {
                    Log.d(TAG, "" + i + " " + wallpaperInfo.getComponent());
                    if (currentWallpaper.getComponent().equals(wallpaperInfo.getComponent())) {
                        mListView.scrollToPosition(i);
                        break;
                    }
                }
            }
        }
        Intent intent = getIntent();
        boolean isFte  = false;
        if(intent!=null){
            isFte = intent.getBooleanExtra("watchface_selector_firsttime_show", false);
        }
        if(isFte) {
            findViewById(R.id.watchface_fte).setVisibility(View.VISIBLE);
            mRotateImg = findViewById(R.id.rotation);
            mRotateAnimator = ObjectAnimator.ofFloat(mRotateImg, "rotation", 0, 359);
            mRotateAnimator.setInterpolator(new LinearInterpolator());
            mRotateAnimator.setDuration(1500);
            mRotateAnimator.setRepeatMode(ValueAnimator.RESTART);
            mRotateAnimator.setRepeatCount(-1);
            mRotateAnimator.start();
            mWatchfaceFirsttDialog = new WatchfaceFirsttDialog(this);
        }else{
            findViewById(R.id.watchface_fte).setVisibility(View.GONE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    private class WatchFaceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final LayoutInflater mInflater;

        Context mContext;

        @SuppressWarnings("unchecked")
        WatchFaceListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if (viewType == VIEW_FAKE) {
                return new FakeViewHolder(mInflater.inflate(R.layout.watchface_preview_item_empty, null));

            } else {
                return new WatchFaceViewHolder(mInflater.inflate(R.layout.watchface_preview_item, null));
            }
        }

        @Override
        public int getItemViewType(int position) {
            final WallpaperInfo wallpaperInfo = mWallpapers.get(position).mWallpaperInfo;
            if (wallpaperInfo == null) {
                return VIEW_FAKE;
            } else {
                return VIEW_NORMAL;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewholder, int position) {
            {
                if (viewholder instanceof WatchFaceViewHolder) {
                    final WallpaperInfo wallpaperInfo = mWallpapers.get(position).mWallpaperInfo;
                    mWallpaperInfo = wallpaperInfo;
                    WatchFaceViewHolder holder = (WatchFaceViewHolder) viewholder;
                    holder.mThumbImageView.setImageDrawable(wallpaperInfo.loadThumbnail(getPackageManager()));
                    holder.mDescriptionTextView.setText(wallpaperInfo.loadLabel(getPackageManager()));
                    String setting = wallpaperInfo.getSettingsActivity();
                    if (setting == null) {
                        holder.mSettingImageButton.setVisibility(View.GONE);
                    }
                    holder.mThumbImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mWatchfaceFirsttDialog != null) {
                                mWatchfaceFirsttDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        mWatchfaceFirsttDialog = null;
                                        mWatchfaceManager.selectWallpaper(wallpaperInfo);
                                        WatchFaceSelectActivity.this.finish();
                                    }
                                });

                                mWatchfaceFirsttDialog.show();
                            } else {
                                mWatchfaceManager.selectWallpaper(wallpaperInfo);
                                WatchFaceSelectActivity.this.finish();
                            }
                        }
                    });
                    holder.mSettingImageButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName(wallpaperInfo.getPackageName(), wallpaperInfo.getSettingsActivity()));
                            intent.putExtra(EXTRA_PREVIEW_MODE, true);
                            startActivity(intent);

                        }
                    });
                }
            }

        }

        @Override
        public int getItemCount() {
            if (mWallpapers == null) {
                return 0;
            }
            return mWallpapers.size();
        }
    }

    class WatchFaceViewHolder extends RecyclerView.ViewHolder {
        ImageView mThumbImageView;
        TextView mDescriptionTextView;
        ImageButton mSettingImageButton;

        WatchFaceViewHolder(View itemView) {
            super(itemView);
            mThumbImageView = (ImageView) itemView.findViewById(R.id.watchface_thumb);
            mDescriptionTextView = (TextView) itemView.findViewById(R.id.watchface_description);
            mSettingImageButton = (ImageButton) itemView.findViewById(R.id.watchface_setting);
        }
    }

    class FakeViewHolder extends RecyclerView.ViewHolder {
        FakeViewHolder(View itemView) {
            super(itemView);
        }
    }
}
