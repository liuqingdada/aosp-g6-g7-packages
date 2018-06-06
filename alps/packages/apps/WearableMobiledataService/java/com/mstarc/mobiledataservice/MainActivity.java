package com.mstarc.mobiledataservice;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.mstarc.watchfacemanager.WatchfaceManager;

import java.io.File;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {
    WatchfaceManager mWatchfaceManager;
    List<WallpaperInfo> mWatchfaceList;
    TextView tv_all, tv_cur;
    WallpaperInfo curentWallpaper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_all = (TextView) findViewById(R.id.tv_all);
        tv_cur = (TextView) findViewById(R.id.tv_cur);
        mWatchfaceManager = WatchfaceManager.getInstance(this);
    }

    //install2
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.install) {
            Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory().getPath() + "/stopwatch.apk");
            Log.d("MainActivity", "try to install uri: " + uri.toString());
            mWatchfaceManager.installPackage(uri);
        } else if (view.getId() == R.id.install2) {
            Uri uri = Uri.fromFile(new File(Environment.getExternalStorageDirectory().getPath() + "/stopwatch.apk"));
            Log.d("MainActivity", "try to install 2 uri: " + uri.toString());
            mWatchfaceManager.installPackage(uri);
        } else if (view.getId() == R.id.uninstall) {
            ComponentName cn = new ComponentName("com.mstarc.app.stopwatch", "com.mstarc.app.stopwatch.MainActivity");
            mWatchfaceManager.uninstallComponnet(cn);

         /*   for (WallpaperInfo info : mWatchfaceList) {
                if (info.getComponent().getPackageName().equals("com.mstarc.g6.watchface")) {
                    mWatchfaceManager.uninstallWallpaper(info);
                    break;
                }
            }*/
        } else if (view.getId() == R.id.btn_all) {
            mWatchfaceList = mWatchfaceManager.getWallpaperList();
            for (int i = 0; i < mWatchfaceList.size(); i++) {
                WallpaperInfo info = mWatchfaceList.get(i);
                tv_all.setText("" + (i + 1) + ":Component:" + info.getComponent() + "\n"
                        + ",Component2:" + info.getComponent().getShortClassName() + "\n"
                        + ",PackageName:" + info.getPackageName() + "\n"
                        + ",ServiceName:" + info.getServiceName() + "\n"
                        + ",ServiceInfo:" + info.getServiceInfo().packageName + "\n");
                Log.d("MainActivity", "" + (i + 1) + ":Component:" + info.getComponent() + "\n"
                        + ",Component2:" + info.getComponent().getShortClassName() + "\n"
                        + ",PackageName:" + info.getPackageName() + "\n"
                        + ",ServiceName:" + info.getServiceName() + "\n"
                        + ",ServiceInfo:" + info.getServiceInfo().packageName + "\n");
            }
        } else if (view.getId() == R.id.btn_cur) {
            curentWallpaper = mWatchfaceManager.getCurrentWallpaper();
            if (curentWallpaper != null) {
                tv_cur.setText(curentWallpaper.toString());
                //curentWallpaper.getServiceName()
                Log.d("MainActivity", curentWallpaper.toString());
            }
        }
    }
}
