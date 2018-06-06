package com.mstarc.watchfacemanager;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.wallpaper.WallpaperService;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import android.app.PackageInstallObserver;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionInfo;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.UserInfo;
import android.content.pm.VerificationParams;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IUserManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by wangxinzhi on 17-7-20.
 */

public class WatchfaceManager extends android.app.PackageInstallObserver {
    private static final String TAG = WatchfaceManager.class.getSimpleName();
    static WatchfaceManager sInstance;
    Context mContext;
    PackageManager mPackageManager;
    WallpaperManager mWallpaperManager;
    IPackageInstaller mInstaller;
    IPackageManager mPm;
	IWatchFaceListener mWatchFaceListener;


    public WatchfaceManager(Context context) {
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        mWallpaperManager = WallpaperManager.getInstance(context);
        mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        try {
            mInstaller = mPm.getPackageInstaller();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public static WatchfaceManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WatchfaceManager(context);
        }
        return sInstance;
    }

    public List<WallpaperInfo> getWallpaperList() {
        Intent intent = new Intent(WallpaperService.SERVICE_INTERFACE);
        intent.addCategory("com.mstarc.wearable.category.WATCH_FACE");
        List<ResolveInfo> list = mPackageManager.queryIntentServices(
                intent,
                PackageManager.GET_META_DATA);
        if (list == null || list.isEmpty()) {
            return null;
        }
        ArrayList<WallpaperInfo> wallpaperlist = new ArrayList<>();
        for (ResolveInfo info : list) {
            try {
                wallpaperlist.add(new WallpaperInfo(mContext, info));
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return wallpaperlist;
    }

    public WallpaperInfo getCurrentWallpaper() {
        return mWallpaperManager.getWallpaperInfo();
    }

    public void selectWallpaper(WallpaperInfo wallpaperInfo) {
        selectWallpaperByComponent(wallpaperInfo.getComponent());

    }

    public void selectWallpaperByComponent(ComponentName componentName) {
        try {
            mWallpaperManager.getIWallpaperManager().setWallpaperComponent(componentName);
        } catch (RemoteException e) {
            // do nothing
        } catch (RuntimeException e) {
            Log.w(TAG, "Failure setting wallpaper", e);
        }
    }

    public void uninstallWallpaper(WallpaperInfo wallpaperInfo) {
        uninstallComponnet(wallpaperInfo.getComponent());
    }

    public int uninstallComponnet(ComponentName componentName) {
        int flags = 0;
        int userId ;
        userId = UserHandle.USER_OWNER;
        flags |= PackageManager.DELETE_ALL_USERS;
        final LocalIntentReceiver receiver = new LocalIntentReceiver();
        try {
            mInstaller.uninstall(componentName.getPackageName(), flags, receiver.getIntentSender(), userId);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        final Intent result = receiver.getResult();
        final int status = result.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_SUCCESS) {
            Log.i(TAG,"Success");
            return 0;
        } else {
            Log.e(TAG, "Failure details: " + result.getExtras());
            Log.e(TAG,"Failure ["
                    + result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) + "]");
            return 1;
        }
//
//        Uri packageURI = Uri.parse("package:" + componentName.getPackageName());
//        Log.d(TAG,"try to uninstall: "+packageURI);
//        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageURI);
//        uninstallIntent.putExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, true);
//        uninstallIntent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
//        mContext.startActivity(uninstallIntent);
//        return 0;

    }

    private static class LocalIntentReceiver {
        private final SynchronousQueue<Intent> mResult = new SynchronousQueue<>();

        private IIntentSender.Stub mLocalSender = new IIntentSender.Stub() {
            @Override
            public int send(int code, Intent intent, String resolvedType,
                            IIntentReceiver finishedReceiver, String requiredPermission) {
                try {
                    mResult.offer(intent, 5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return 0;
            }
        };

        public IntentSender getIntentSender() {
            return new IntentSender((IIntentSender) mLocalSender);
        }

        public Intent getResult() {
            try {
                return mResult.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void installPackage(Uri uri) {
        Log.d(TAG,"try to install: "+uri);
        mPackageManager.installPackage(uri, this, 0, mContext.getPackageName());
        //TODO
    }
	  public void installPackage(Uri uri, IWatchFaceListener watchFaceListener) {
        Log.d(TAG, "try to install: " + uri);
        mWatchFaceListener = watchFaceListener;
        mPackageManager.installPackage(uri, this, 0, mContext.getPackageName());
        //TODO
    }
    public void installPackage(Uri uri, android.app.PackageInstallObserver observer) {
        Log.d(TAG,"try to install: "+uri);
        mPackageManager.installPackage(uri, observer == null ? this : observer, 0, mContext.getPackageName());
    }

    @Override
    public void onPackageInstalled(String basePackageName, int returnCode, String msg,
                                   Bundle extras) {
        Log.d(TAG, "onPackageInstalled: " + basePackageName);
		 if (mWatchFaceListener != null) {
            mWatchFaceListener.onPackageInstalled(basePackageName, returnCode, msg, extras);
        }
    }
	public interface IWatchFaceListener {
        void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras);
    }
}
