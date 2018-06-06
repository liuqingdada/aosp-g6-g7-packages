package com.mstarc.record.wearablerecorder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

/**
 * description
 * <p/>
 * Created by andyding on 2017/5/27.
 */

public class RecordManager {

    public static Context mContext = null;
    private RecordService mRecoedService;

    private static RecordManager sInstance = null;

    public static RecordManager getInstance() {
        if (sInstance == null) {
            sInstance = new RecordManager();
        }
        return sInstance;
    }

    private RecordManager() {
        mRecoedService = new RecordService();
    }

    public static void setApplicationContext(Context context) {
        mContext = context;
    }

    public ArrayList<String> getRecordList() {
        return mRecoedService.getRecordList();
    }

    public ArrayList<String> getReverseList() {
        return mRecoedService.getReverseList();
    }

    public boolean bPause() {
        Log.d("dingyichen", "bPause : " + mRecoedService.isPause());
        return mRecoedService.isPause();
    }

    public boolean startRecord() {
        return mRecoedService.startRecord();
    }

    public boolean pauseRecord() {
        return mRecoedService.pauseRecord();
    }

    public String getCompleteTimeName() {
        return mRecoedService.getCurrentTime();
    }

    public boolean isRecording() {
        Log.d("dingyichen", "isRecording : " + mRecoedService.isRecording());
        return mRecoedService.isRecording();
    }

    public void stopRecord(String timeName) {
        mRecoedService.stopRecord(timeName);
    }

    public void reset() {
        mRecoedService.reset();
    }

    public boolean playRrecord(String filePath) {
        return mRecoedService.playRrecord(filePath);
    }

    public void pausePlay() {
        mRecoedService.pausePlay();
    }

    public void ContinuePlay() {
        mRecoedService.ContinuePlay();
    }

    public void stopPlay() {
        mRecoedService.stopPlay();
    }

    public boolean isPlaying() {
        return mRecoedService.isPlaying();
    }

    public void releaseRecord() {
        mRecoedService.releaseRecord();
    }

    public void deleteRecordItem(int position) {
        mRecoedService.deleteRecordItem(position);
    }

    public void deleteAllRecord() {
        mRecoedService.deleteAllRecord();
    }

    public int getVolume() {
        return mRecoedService.getVolume();
    }

    public int getMaxVolume() {
        return mRecoedService.getMaxVolume();
    }

    public void volumeUp() {
        mRecoedService.volumeUp();
    }

    public void volumeDown() {
        mRecoedService.volumeDown();
    }

    public static final String SHARED_PREFERENCE_DATE = "date";
    public static final String SHARED_PREFERENCE_TIMES = "times";

    public static String getLastRecordDate() {
        Log.d("dingyichen", "getLastRecordDate  ");
        Log.getStackTraceString(new Exception());
        SharedPreferences preferences = mContext.
                getSharedPreferences(SHARED_PREFERENCE_DATE, Context.MODE_PRIVATE);
        return preferences.getString(SHARED_PREFERENCE_DATE, "");
    }

    public static void setRecordDate(String date) {
        Log.d("dingyichen", "setRecordDate str = " + date);
        SharedPreferences preferences = mContext.
                getSharedPreferences(SHARED_PREFERENCE_DATE, Context.MODE_PRIVATE);
        preferences.edit().putString(SHARED_PREFERENCE_DATE, date).commit();
    }

    public static String getLastRecordTimes() {
        Log.d("dingyichen", "getLastRecordTimes  ");
        SharedPreferences preferences = mContext.
                getSharedPreferences(SHARED_PREFERENCE_TIMES, Context.MODE_PRIVATE);
        return preferences.getString(SHARED_PREFERENCE_TIMES, "1");
    }

    public static void setRecordTimes(String times) {
        Log.d("dingyichen", "setRecordTimes times = " + times);
        SharedPreferences preferences = mContext.
                getSharedPreferences(SHARED_PREFERENCE_TIMES, Context.MODE_PRIVATE);
        preferences.edit().putString(SHARED_PREFERENCE_TIMES, times).commit();
    }

    public long getRecordTime() {
        Log.d("dingyichen", "getRecordTime time = " + mRecoedService.getRecordTime());
        return mRecoedService.getRecordTime();
    }

//    public void setRecordTime(long time) {
//        mRecoedService.setRecordTime(time);
//    }

    private Listener mListener;

    public interface Listener {
        void onDataChanged();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void notifylistener() {
        mListener.onDataChanged();
    }

    private StopListener mStopListener;

    public interface StopListener {
        void stop();
    }

    public void setStopListener(StopListener stopListener) {
        mStopListener = stopListener;
    }

    public void notiStopListener() {
        mStopListener.stop();
    }

    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("dingyichen", "onServiceConnected connected");
            mRecoedService = ((RecordService.MyBinder) iBinder).getService();
            mRecoedService.init();
            notifylistener();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("dingyichen", "onServiceConnected disconnected");
            mRecoedService = null;
            notifylistener();
        }
    };

    public void bindServiceConnection(Context context) {
        Log.d("dingyichen", "bindServiceConnection!!!");
        Intent intent = new Intent(context.getApplicationContext(), RecordService.class);
        context.startService(intent);
        context.bindService(intent, sc, Context.BIND_AUTO_CREATE);
    }

    public void unBindService(Context context) {
        context.unbindService(sc);
    }
}
