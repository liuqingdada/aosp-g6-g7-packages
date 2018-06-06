package com.mstarc.myapplication;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.mstarc.watchfacemanager.WatchfaceManager;

import java.util.List;


public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    RecyclerView mList;
    ViewAdapter mAdapter;
    WatchfaceManager mWatchfaceManager;
    List<WallpaperInfo> mWatchfaceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mList = (RecyclerView) findViewById(R.id.list);
        mList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mAdapter = new ViewAdapter(this);
        mList.setAdapter(mAdapter);
        mWatchfaceManager = WatchfaceManager.getInstance(this);
        mWatchfaceList = mWatchfaceManager.getWallpaperList();
        WallpaperInfo curentWallpaper = mWatchfaceManager.getCurrentWallpaper();
        Log.d(TAG, "currentWallpaper: " + curentWallpaper.getComponent().getPackageName() + "/" + curentWallpaper.getComponent().getShortClassName());
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.install) {
            Uri uri = Uri.parse("file://"+ Environment.getExternalStorageDirectory().getPath()+"/WatchFace_G6.apk");
            Log.d(TAG,"try to install uri: "+uri);
            mWatchfaceManager.installPackage(uri);
        } else if (view.getId() == R.id.uninstall) {
            for (WallpaperInfo info : mWatchfaceList) {
                if (info.getComponent().getPackageName().equals("com.mstarc.g6.watchface")) {
                    mWatchfaceManager.uninstallWallpaper(info);
                    break;
                }
            }
        }
    }

    class VH extends RecyclerView.ViewHolder {
        ImageView mThumbnailView;
        TextView mDescriptionView;

        public VH(View itemView) {
            super(itemView);
            mThumbnailView = (ImageView) itemView.findViewById(R.id.thumbnail);
            mDescriptionView = (TextView) itemView.findViewById(R.id.description);
        }
    }

    class ViewAdapter extends RecyclerView.Adapter<VH> {
        Context mContext;

        public ViewAdapter(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return new VH(inflater.inflate(R.layout.item, null));
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            final WallpaperInfo info = mWatchfaceList.get(position);
            holder.mThumbnailView.setImageDrawable(info.loadThumbnail(getPackageManager()));
            holder.mDescriptionView.setText(info.loadLabel(getPackageManager()));
            holder.mThumbnailView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mWatchfaceManager.selectWallpaperByComponent(info.getComponent());
                }
            });
        }

        @Override
        public int getItemCount() {
            return mWatchfaceList.size();
        }
    }
}
