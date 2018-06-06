package com.mstarc.wearablelauncher.watchface;

import android.app.Service;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mstarc.commonbase.communication.listener.ICommonAidlListener;
import com.mstarc.commonbase.communication.message.transmite.WatchFace;
import com.mstarc.fakewatch.notification.NotificationWizard;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by wangxinzhi on 17-7-9.
 */

public class WatchFaceService extends Service implements ICommonAidlListener<WatchFace> {
    private static final String TAG = WatchFaceService.class.getSimpleName();
    UiHandler mHandler;
    WorkHandler mWorkHandler;
    HandlerThread mWorkThread;

    NotificationWizard mNotificationWizard;
    List<WatchFace.Item> mWatchFaces = new ArrayList<>();
    PackageManager mPackageManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (flags) {
            case START_FLAG_REDELIVERY:
                Log.d(TAG, "redelivered intent to start service [" + startId + "]");
                break;
            case START_FLAG_RETRY:
                Log.d(TAG, "retry start service [" + startId + "]");
                break;
            default:
                Log.d(TAG, "delivered intent to start service [" + startId + "]");
                break;
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new UiHandler();
        mNotificationWizard = NotificationWizard.getInstance();
        mNotificationWizard.addOnAidlCallBack(this, WatchFace.class);
        mNotificationWizard.initNotificationWizard(getApplicationContext());
        mPackageManager = getApplicationContext().getPackageManager();
        mWorkThread = new HandlerThread("WatchFaceManagerWorker");
        mWorkThread.start();
        mWorkHandler = new WorkHandler(mWorkThread.getLooper());
        mHandler = new UiHandler();
        mWorkHandler.sendEmptyMessage(mWorkHandler.MSG_GET_ALL_WATCHFACE_ON_WATCH);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotificationWizard.onDestroy();
    }

    @Override
    public void onReceiveBleData(WatchFace watchFace) {
        switch (watchFace.getType()) {
            // return current all watch faces on watch
            case 0:
                watchFace.setWatchFaceList(mWatchFaces);
                break;
            // change watchface
            case 1: {
                List<WatchFace.Item> list = watchFace.getWatchFaceList();
                for (WatchFace.Item item : list) {
                    if (item.isHasUsed()) {
                        ComponentName component = ComponentName.unflattenFromString(item.getIndex());
                        mHandler.sendMessage(mHandler.obtainMessage(mHandler.MSG_SET_WALLPAPER, component));
                        break;
                    }
                }
            }
            break;
            // download watchface
            case 2: {
                List<WatchFace.Item> list = watchFace.getWatchFaceList();
                for (WatchFace.Item item : list) {
                    if (item.getDownloadUrl() != null) {
                        mWorkHandler.sendMessage(mWorkHandler.obtainMessage(mWorkHandler.MSG_DOWNLOAD_AND_INSTALL, item.getDownloadUrl()));
                        break;
                    }
                }
            }
            break;

        }

    }


    void fetchWatchFaceOnWatch() {
        mWatchFaces.clear();
        Intent intent = new Intent(WallpaperService.SERVICE_INTERFACE);
        intent.addCategory("com.mstarc.wearable.category.WATCH_FACE");
        List<ResolveInfo> list = mPackageManager.queryIntentServices(
                intent,
                PackageManager.GET_META_DATA);

        Collections.sort(list, new Comparator<ResolveInfo>() {
            final Collator mCollator;

            {
                mCollator = Collator.getInstance();
            }

            public int compare(ResolveInfo info1, ResolveInfo info2) {
                return mCollator.compare(info1.loadLabel(mPackageManager),
                        info2.loadLabel(mPackageManager));
            }
        });
        for (ResolveInfo resolveInfo : list) {
            WallpaperInfo info = null;
            try {
                info = new WallpaperInfo(getApplicationContext(), resolveInfo);
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                continue;
            } catch (IOException e) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                continue;
            }

            ComponentName component = new ComponentName(info.getPackageName(), info.getServiceName());
            WatchFace.Item item = new WatchFace.Item(component.flattenToString(), false, null);
            mWatchFaces.add(item);

//            Drawable thumb = info.loadThumbnail(mPackageManager);
//            Intent launchIntent = new Intent(WallpaperService.SERVICE_INTERFACE);
//            launchIntent.setClassName(info.getPackageName(), info.getServiceName());
//            LiveWallpaperTile wallpaper = new LiveWallpaperTile(thumb, info, launchIntent);
        }
    }

    public static class LiveWallpaperTile {
        public Drawable mThumbnail;
        public WallpaperInfo mInfo;
        public Intent mIntent;

        public LiveWallpaperTile(Drawable thumbnail, WallpaperInfo info, Intent intent) {
            mThumbnail = thumbnail;
            mInfo = info;
            mIntent = intent;
        }

        public void onClick(Context context) {
            Intent preview = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            preview.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    mInfo.getComponent());

            context.startActivity(preview);
        }
    }

    class UiHandler extends Handler {
        public static final int MSG_SET_WALLPAPER = 1;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SET_WALLPAPER:
                    ComponentName componentName = (ComponentName) msg.obj;
                    Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                    intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            componentName);
                    getApplicationContext().startActivity(intent);
                    break;
            }
        }
    }

    class WorkHandler extends Handler {
        public static final int MSG_GET_ALL_WATCHFACE_ON_WATCH = 1;
        public static final int MSG_DOWNLOAD_AND_INSTALL = 2;

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_GET_ALL_WATCHFACE_ON_WATCH:
                    fetchWatchFaceOnWatch();
                    break;
                case MSG_DOWNLOAD_AND_INSTALL: {
                    final String url = (String) msg.obj;
                    (new AsyncTask<Void,Void,Void>(){
                        private static final int TIMEOUT_CONNECT = 10000;
                        private static final int TIMEOUT_READ = 10000;

                        @Override
                        protected Void doInBackground(Void... params) {
                            HttpURLConnection httpConn = null;
                            InputStream in = null;
                            try {
                                URLConnection conn = new URL(url).openConnection();
                                if (conn instanceof HttpURLConnection) {
                                    httpConn = (HttpURLConnection) conn;
                                }
                                conn.setConnectTimeout(TIMEOUT_CONNECT);
                                conn.setReadTimeout(TIMEOUT_READ);
                                in = conn.getInputStream();
//                                mPackageManager.getPackageInstaller().createSession()

                            } catch (Exception e) {
                                Log.e(TAG, "Unable to fetch picture " + url + " : " + e.getMessage(), e);
                                return null;
                            } finally {
                                if (httpConn != null) {
                                    httpConn.disconnect();
                                    httpConn = null;
                                }
                                if (in != null) {
                                    try {
                                        in.close();
                                    } catch (Exception e) {
                                        Log.e(TAG, "Exception found when close in", e);
                                    }
                                    in = null;
                                }
                            }

                            return null;
                        }
                    }).execute();
                    //TODO download

                }
                break;
            }
        }
    }
}
