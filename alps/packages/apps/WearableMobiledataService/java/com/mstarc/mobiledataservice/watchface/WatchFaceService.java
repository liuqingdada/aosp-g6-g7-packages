package com.mstarc.mobiledataservice.watchface;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.WallpaperInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.mstarc.commonbase.communication.aidl.AidlCommunicate;
import com.mstarc.commonbase.communication.listener.ICommonAidlListener;
import com.mstarc.commonbase.communication.message.transmite.WatchFace;
import com.mstarc.mobiledataservice.R;
import com.mstarc.watchfacemanager.WatchfaceManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 6.
 * com.mstarc.g6.watchface.BlackGoldWatchFace		黑金永恒
 * com.mstarc.g6.watchface.GoldWatchFace			黄金罗盘
 * com.mstarc.g6.watchface.TriangleWatchFace		康定斯基
 * com.mstarc.g6.watchface.BluerideWatchFace		蓝调爵士
 * com.mstarc.g6.watchface.LightBlueWatchFace		蓝光
 * com.mstarc.g6.watchface.DongganWatchFace		    动感地带
 * <p>
 * 7.
 * com.mstarc.g7.watchface.AegeanWatchFace			爱琴海
 * com.mstarc.g7.watchface.GoldRideWatchFace		黄金骑士
 * com.mstarc.g7.watchface.FragWatchFace			夕雾
 * com.mstarc.g7.watchface.DiamondWatchFace		    金粉世家
 * com.mstarc.g7.watchface.XuanliWatchFace			彩虹丽人
 * com.mstarc.g7.watchface.StarlightWatchFace		星光熠熠
 * com.mstarc.g7.watchface.PinkWatchFace			甜蜜时刻
 */
public class WatchFaceService extends Service {
    private Context mContext;
    private AidlCommunicate mCommunicate;
    private boolean hasConnected;
    private ICommonAidlListener<WatchFace> mWatchFaceListener;
    WatchfaceManager mWatchfaceManager;
    List<WallpaperInfo> mWatchfaceList;
    String[] G6 = {
            "com.mstarc.g6.watchface.BlackGoldWatchFace",
            "com.mstarc.g6.watchface.GoldWatchFace",
            "com.mstarc.g6.watchface.TriangleWatchFace",
            "com.mstarc.g6.watchface.BluerideWatchFace",
            "com.mstarc.g6.watchface.LightBlueWatchFace",
            "com.mstarc.g6.watchface.DongganWatchFace"};
    /**
     * ,
     * "com.mstarc.app.watchfaceg6.wallpaper.BlueNightWatchFace",
     * "com.mstarc.app.watchfaceg6.wallpaper.ModernCityWatchFace",
     * "com.mstarc.app.watchfaceg6.wallpaper.GoldKnightWatchFace"
     */
    String[] G7 = {
            "com.mstarc.g7.watchface.AegeanWatchFace",
            "com.mstarc.g7.watchface.GoldRideWatchFace",
            "com.mstarc.g7.watchface.FragWatchFace",
            "com.mstarc.g7.watchface.DiamondWatchFace",
            "com.mstarc.g7.watchface.XuanliWatchFace",
            "com.mstarc.g7.watchface.StarlightWatchFace",
            "com.mstarc.g7.watchface.PinkWatchFace"};
    /**
     * ,
     * "com.mstarc.app.watchfaceg7.wallpaper.ModernCityWatchFace"
     */
    List<String> G6_list;
    List<String> G7_list;

    public WatchFaceService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("WatchFaceService", "表盘服务创建");
        mContext = this;
        mWatchfaceManager = WatchfaceManager.getInstance(mContext);
        initRemoteService(1);
        setAidlListener();
        G6_list = Arrays.asList(G6);
        G7_list = Arrays.asList(G7);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("WatchFaceService", "表盘onStartCommand");
        startForegroound();
        //initRemoteService(2);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCommunicate != null) {
            mCommunicate.onDestroy();
            mCommunicate = null;
        }
        startService();
    }

    private String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(c, key, value));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return value;
        }
    }

    private boolean isG7() {
        return "g7".equals(getProperty("ro.product.watch", "g6+"));
    }

    /**
     * 判断服务连接状态
     *
     * @return
     */
    public boolean isConnected() {
        return hasConnected && (mCommunicate != null && mCommunicate.isAdvertiserConnect());
    }

    /**
     * 返回所有列表 0    mWatchFaceList >= 1
     * 切换        1    mWatchFaceList == 1
     * 下载        2    mWatchFaceList == 1
     * 卸载        3    mWatchFaceList == 1
     */
    private void setAidlListener() {

        mWatchFaceListener = new ICommonAidlListener<WatchFace>() {
            @Override
            public void onReceiveBleData(WatchFace watchFace) {
                if (watchFace != null) {
                    Log.d("WatchFaceService", "收到表盘请求:" + watchFace.getType());
                    mWatchfaceList = mWatchfaceManager.getWallpaperList();
                    //用于123(使用serviceName 从其中也能拿到包名 ,当然也可以动全部里面匹配 去拿那个对象)
                    List<WatchFace.Item> faceList = watchFace.getWatchFaceList();
                    WatchFace.Item item = null;
                    if (faceList != null && faceList.size() != 0) {
                        item = faceList.get(0);
                    }
                    switch (watchFace.getType()) {
                        case 0:
                            Log.d("WatchFaceService", "收到请求所有表盘信息");
                            if (isConnected()) {
                                sendWatchFaceList();
                            }
                            break;
                        case 1:
                            Log.d("WatchFaceService", "收到请求切换表盘");
                            //中间有一个转换过程
                            if (item != null) {
                                changeWatchFace(item);
                            }
                            break;
                        case 2:
                            Log.d("WatchFaceService", "收到请求下载表盘并使用");
                            if (item != null) {
                                downAndUseWatchFace(item);
                            }
                            break;
                        case 3:
                            Log.d("WatchFaceService", "收到请求卸载表盘");
                            if (item != null) {
                                unInstallWatchFace(item);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        };
        if (mCommunicate != null && mWatchFaceListener != null) {
            mCommunicate.addOnAidlCallBack(mWatchFaceListener, WatchFace.class);
        }
    }


    /**
     * 卸载表盘(卸载正在使用的会怎样)
     *
     * @param item
     */
    private void unInstallWatchFace(WatchFace.Item item) {
        String index = item.getIndex();//转换成包名之类
        if (mWatchfaceList != null) {
            for (int i = 0; i < mWatchfaceList.size(); i++) {
                if (mWatchfaceList.get(i).getServiceName().contains(index)) {
                    //mWatchfaceManager.uninstallComponnet(null);
                    Log.d("WatchFaceService", "匹配到表盘,去卸载");
                    mWatchfaceManager.uninstallWallpaper(mWatchfaceList.get(i));
                    break;
                }
            }
        }
    }

    /**
     * 缺少回调,用于安装成功 切换刚下载的表盘
     * onPackageInstalled(basePackageName, returnCode, msg, extras)
     *
     * @param item
     */
    private void downAndUseWatchFace(final WatchFace.Item item) {
        Log.d("WatchFaceService", "apk位置:" + item.getApkPath());
        // Uri uri = Uri.fromFile(new File(item.getApkPath()));
        Uri uri = Uri.parse("file://" + item.getApkPath());
        final String serName = item.getIndex();
        final String packName = serName.substring(0, serName.lastIndexOf("."));
        mWatchfaceManager.installPackage(uri, new WatchfaceManager.IWatchFaceListener() {
            @Override
            public void onPackageInstalled(String s, int i, String s1, Bundle bundle) {
                Log.d("WatchFaceService", "安装basePackageName:" + s + "\n"
                        + "code:" + i + "\n"
                        + "msg:" + s1 + "\n"
                        + "budule:" + bundle.toString());
                mWatchfaceManager.selectWallpaperByComponent(new ComponentName(packName, serName));
            }
        });
    }

    /**
     * 切换表盘
     *
     * @param item
     */
    private void changeWatchFace(WatchFace.Item item) {
        //  mWatchfaceManager.selectWallpaper(null);
        // mWatchfaceManager.selectWallpaperByComponent(null);
        String index = item.getIndex();
        if (mWatchfaceList != null) {
            for (int i = 0; i < mWatchfaceList.size(); i++) {
                if (mWatchfaceList.get(i).getServiceName().contains(index)) {
                    Log.d("WatchFaceService", "匹配到表盘,去应用");
                    mWatchfaceManager.selectWallpaper(mWatchfaceList.get(i));
                    break;
                }
            }
        }
    }

    /**
     * 发送所有表盘列表
     */
    private void sendWatchFaceList() {
        boolean g7 = isG7();
        String curName = mWatchfaceManager.getCurrentWallpaper().getServiceName();
        if (mWatchfaceList != null) {
            List<String> watchList = new ArrayList<>();
            for (int i = 0; i < mWatchfaceList.size(); i++) {
                watchList.add(mWatchfaceList.get(i).getServiceName());
            }
            watchList.removeAll(g7 ? G7_list : G6_list);

            WatchFace wf = new WatchFace();
            List<WatchFace.Item> itemList = new ArrayList<>();
            WatchFace.Item item;
            boolean needAdd = true;
            for (int i = 0; i < watchList.size(); i++) {
                item = new WatchFace.Item();
                String serName = watchList.get(i);
                if (serName.equals(curName)) {
                    item.setHasUsed(true);
                    needAdd = false;
                } else {
                    item.setHasUsed(false);
                }
                item.setIndex(serName);
                itemList.add(item);
            }
            if (needAdd) {
                item = new WatchFace.Item();
                item.setIndex(curName);
                item.setHasUsed(true);
                itemList.add(item);
            }

            Log.d("WatchFaceService", "itemList:" + itemList.size());
            wf.setType(0);
            wf.setWatchFaceList(itemList);
            try {
                mCommunicate.sendMessage(wf, "");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
  /*  private void sendWatchFaceList() {
        boolean g7 = isG7();
        String curName = mWatchfaceManager.getCurrentWallpaper().getServiceName();
        WatchFace wf = new WatchFace();
        List<WatchFace.Item> itemList = new ArrayList<>();
        WatchFace.Item item;
        String serName;
        if (mWatchfaceList != null) {
            for (int i = 0; i < mWatchfaceList.size(); i++) {
                serName = mWatchfaceList.get(i).getServiceName();
                item = new WatchFace.Item();
                if (serName.equals(curName)) {
                    item.setHasUsed(true);
                } else {
                    item.setHasUsed(false);
                }
                // item.setIndex(serName);
                item.setIndex(serName.substring(serName.lastIndexOf(".") + 1));
                itemList.add(item);
            }
        }
        Log.d("WatchFaceService", "itemList:" + itemList.size());
        wf.setType(0);
        wf.setWatchFaceList(itemList);
        try {
            mCommunicate.sendMessage(wf, "");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }*/


    private void initRemoteService(final int code) {
        if (mCommunicate != null) {
            mCommunicate.onDestroy();
            mCommunicate = null;
        }
        mCommunicate = AidlCommunicate.getInstance();
        mCommunicate.setAidlServiceConnectListener(
                new AidlCommunicate.WatchServiceConnectListener() {
                    @Override
                    public void onAidlServiceConnected() {
                        Log.d("WatchFaceService", "远程服务已连接" + code);
                        hasConnected = true;
                    }

                    @Override
                    public void onAidlServiceDisconnected() {
                        Log.d("WatchFaceService", "远程服务已断开" + code);
                        hasConnected = false;
                        stopSelf();
                    }
                });
        mCommunicate.initAIDL(mContext);
    }

    /**
     * 开启前台服务
     */
    private void startForegroound() {
        Intent notificationIntent = new Intent(getApplicationContext(), WatchFaceService.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(
                mContext)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.ntTitle))
                .setContentText("")
                .setContentIntent(pendingIntent);
        Notification notification = mNotifyBuilder.build();
        startForeground(0, notification);
    }

    /**
     * 启动服务
     */
    private void startService() {
        Intent localIntent = new Intent(this, WatchFaceService.class);
        startService(localIntent);
    }
}
