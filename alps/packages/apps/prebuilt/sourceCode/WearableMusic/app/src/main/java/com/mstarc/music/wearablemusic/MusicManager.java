package com.mstarc.music.wearablemusic;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.kw.rxbus.RxBus;
import com.mstarc.music.wearablemusic.data.MusicBus;
import com.mstarc.music.wearablemusic.data.MusicDataContract;
import com.mstarc.music.wearablemusic.data.MusicDataProvider;
import com.mstarc.music.wearablemusic.receiver.HeadsetButtonReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * description
 * <p/>
 * Created by andyding on 2017/5/27.
 */

@SuppressLint("ApplySharedPref")
public class MusicManager {
    private static final String TAG = "MusicSuhen";
    private static Context mContext = null;
    private MusicService mMusicService;
    private MediaSessionCompat mSessionCompat;
    private HeadsetPlugInReceiver mHeadsetPlugInReceiver;
    private HeadsetReceiver mHeadsetReceiver;

    public enum PLAY_TYPE {
        PLAY_TYPE_SHUFFLE,
        PLAY_TYPE_ORDER,
        PLAY_TYPE_LOOP_ALL,
        PLAY_TYPE_LOOP_ONE
    }


    private static MusicManager sInstance = null;

    public static MusicManager getInstance() {
        if (sInstance == null) {
            sInstance = new MusicManager();
        }
        return sInstance;
    }

    private MusicManager() {
        //initMusicList();
        mMusicService = new MusicService();
        //        initDemo();
    }

    public void stopPlay() {
        if (getPlayer().isPlaying()) {
            getPlayer().pause();
        }
        mMusicService.stop();
    }

    public MediaPlayer getPlayer() {
        return mMusicService.mp;
    }

    public void setApplicationContext(Context context) {
        mContext = context;
        // initData(context);

        // init media session
        initMediaSession();
    }

    private static void initData(Context context) {
        ContentValues cv = new ContentValues();
        cv.put(MusicDataContract.Music.Column.MUSIC_ID, "1");
        cv.put(MusicDataContract.Music.Column.DOWNLOAD_URL, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.SAVE_URL, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.MUSIC_FILE_NAME, "eagles_hotel_california.mp3");
        cv.put(MusicDataContract.Music.Column.MUSIC_ICON, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.MUSIC_NAME, "Hotel California");
        cv.put(MusicDataContract.Music.Column.MUSIC_SINGER, "Eagles");
        cv.put(MusicDataContract.Music.Column.MUSIC_SIZE, "123213");
        cv.put(MusicDataContract.Music.Column.MUSIC_TIME, "2,34");
        MusicDataProvider.insertData(context, cv);

        cv.put(MusicDataContract.Music.Column.MUSIC_ID, "2");
        cv.put(MusicDataContract.Music.Column.DOWNLOAD_URL, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.SAVE_URL, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.MUSIC_FILE_NAME, "lizongsheng_shanqiu.mp3");
        cv.put(MusicDataContract.Music.Column.MUSIC_ICON, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.MUSIC_NAME, "山丘");
        cv.put(MusicDataContract.Music.Column.MUSIC_SINGER, "李宗盛");
        cv.put(MusicDataContract.Music.Column.MUSIC_SIZE, "123213");
        cv.put(MusicDataContract.Music.Column.MUSIC_TIME, "2,34");
        MusicDataProvider.insertData(context, cv);

        cv.put(MusicDataContract.Music.Column.MUSIC_ID, "3");
        cv.put(MusicDataContract.Music.Column.DOWNLOAD_URL, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.SAVE_URL, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.MUSIC_FILE_NAME,
               "lizongsheng_piaoyangguohailaikanni_live.mp3");
        cv.put(MusicDataContract.Music.Column.MUSIC_ICON, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.MUSIC_NAME, "漂洋过海来看你 (Live)");
        cv.put(MusicDataContract.Music.Column.MUSIC_SINGER, "李宗盛");
        cv.put(MusicDataContract.Music.Column.MUSIC_SIZE, "123213");
        cv.put(MusicDataContract.Music.Column.MUSIC_TIME, "2,34");
        MusicDataProvider.insertData(context, cv);

        cv.put(MusicDataContract.Music.Column.MUSIC_ID, "4");
        cv.put(MusicDataContract.Music.Column.DOWNLOAD_URL, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.SAVE_URL, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.MUSIC_FILE_NAME,
               "the_beatles_acoustic_trio_yesterday.mp3");
        cv.put(MusicDataContract.Music.Column.MUSIC_ICON, "https://www.baidu.com");
        cv.put(MusicDataContract.Music.Column.MUSIC_NAME, "Yesterday");
        cv.put(MusicDataContract.Music.Column.MUSIC_SINGER, "The Beatles Acoustic Trio");
        cv.put(MusicDataContract.Music.Column.MUSIC_SIZE, "123213");
        cv.put(MusicDataContract.Music.Column.MUSIC_TIME, "2,34");
        MusicDataProvider.insertData(context, cv);
    }

    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("dingyichen", "onServiceConnected connected");
            mMusicService = ((MusicService.MyBinder) iBinder).getService();
            mMusicService.init();
            setPlayType(getMusicPlayMode());
            notifylistener();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("dingyichen", "onServiceConnected disconnected");
            mMusicService = null;
            notifylistener();
        }
    };

    public void bindServiceConnection(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        context.startService(intent);
        context.bindService(intent, sc, Context.BIND_AUTO_CREATE);
    }

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////

    private void initMediaSession() {
        ComponentName mbrComponent = new ComponentName(mContext.getPackageName(),
                                                       HeadsetButtonReceiver.class.getName());
        mSessionCompat = new MediaSessionCompat(mContext, mContext.getPackageName(), mbrComponent,
                                                null);
        //设置MediaSession回调监听,主要用于设置API21+的耳机按钮监听
        mSessionCompat.setCallback(new MediaSessionCallback());

        //设置FLAG,FLAG的用途一看名字就知道了
        mSessionCompat.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat
                        .FLAG_HANDLES_TRANSPORT_CONTROLS);

        /////////////API 21- 的方式///////////
        ComponentName mComponentName = new ComponentName(mContext.getPackageName(),
                                                         MediaButtonReceiver.class.getName());
        //        mContext.getPackageManager()
        //                .setComponentEnabledSetting(mComponentName,
        //                                            PackageManager
        //                                                    .COMPONENT_ENABLED_STATE_ENABLED,
        //                                            PackageManager.DONT_KILL_APP);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mComponentName);
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(mContext,
                                                                  0,
                                                                  mediaButtonIntent,
                                                                  PendingIntent
                                                                          .FLAG_CANCEL_CURRENT);
        mSessionCompat.setMediaButtonReceiver(mPendingIntent);
        ////////////
        mHeadsetPlugInReceiver = new HeadsetPlugInReceiver();
        mHeadsetPlugInReceiver.register(mContext);
        mHeadsetReceiver = new HeadsetReceiver();
        mHeadsetReceiver.register(mContext);
    }

    void setMediaSessionActive(boolean active) {
        //设置MediaSession启动 (很重要,不启动则无法接受到数据)
        mSessionCompat.setActive(active);
    }

    // API 21+ 的方式 MediaSessionCompat.Callback
    // 创建完成后用MediaSessionCompat.setCallback设置上即可使用
    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.i(TAG, "MusicManager onMediaButtonEvent: " + mediaButtonEvent.getAction());
            assert mediaButtonEvent.getExtras() != null;
            KeyEvent keyEvent = (KeyEvent) mediaButtonEvent.getExtras()
                                                           .get(Intent.EXTRA_KEY_EVENT);
            Log.i(TAG, "MusicManager key event:\n" + keyEvent);
            //接收到监听事件

            assert keyEvent != null;
            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_CALL:
                    break;

                case KeyEvent.KEYCODE_ENDCALL:
                    break;

                case KeyEvent.KEYCODE_HEADSETHOOK:
                    // Used to hang up calls
                    break;

                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    RxBus.getInstance()
                         .send(new MusicBus().setStartOrPause(true));
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    RxBus.getInstance()
                         .send(new MusicBus().setStart(true));
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    RxBus.getInstance()
                         .send(new MusicBus().setPause(true));
                    return true;

                case KeyEvent.KEYCODE_MEDIA_STOP:
                    RxBus.getInstance()
                         .send(new MusicBus().setPause(true));
                    return true;

                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    RxBus.getInstance()
                         .send(new MusicBus().setNext(true));
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    RxBus.getInstance()
                         .send(new MusicBus().setPre(true));
                    return true;

                default: break;
            }

            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    }

    /**
     * 耳机插入广播接收器
     */
    public class HeadsetPlugInReceiver extends BroadcastReceiver {
        final IntentFilter filter;

        HeadsetPlugInReceiver() {
            filter = new IntentFilter();

            if (Build.VERSION.SDK_INT >= 21) {
                filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
            } else {
                filter.addAction(Intent.ACTION_HEADSET_PLUG);
            }
        }

        void register(Context context) {
            context.registerReceiver(this, filter);
        }

        void unregister(Context context) {
            context.unregisterReceiver(this);
        }

        /**
         * The intent will have the following extra values:
         * state - 0 for unplugged(未插入), 1 for plugged(插入).
         * name - Headset type, human readable string
         * microphone - 1 if headset has a microphone, 0 otherwise
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            assert intent.getExtras() != null;
            int state = intent.getExtras()
                              .getInt("state");
            String name = intent.getExtras()
                                .getString("name");
            int microphone = intent.getExtras()
                                   .getInt("microphone");
            Log.i(TAG, "HeadsetPlugInReceiver:\n" +
                    "state = " + state +
                    "name = " + name +
                    "microphone = " + microphone);

            if (state == 1) {
                mMusicService.pasue();
            }
        }
    }

    /**
     * 耳机拔出/断开连接 广播接收器
     */
    private class HeadsetReceiver extends BroadcastReceiver {
        final IntentFilter filter;
        final BluetoothAdapter bluetoothAdapter;

        HeadsetReceiver() {
            filter = new IntentFilter();
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY); //有线耳机拔出变化
            filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED); //蓝牙耳机连接变化

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        void register(Context context) {
            context.registerReceiver(this, filter);
        }

        void unregister(Context context) {
            context.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            assert intent.getAction() != null;
            //当前是正在运行的时候才能通过媒体按键来操作音频
            switch (intent.getAction()) {
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                    if (bluetoothAdapter != null &&
                            BluetoothProfile.STATE_DISCONNECTED == bluetoothAdapter
                                    .getProfileConnectionState(
                                            BluetoothProfile.HEADSET) &&
                            getPlayer().isPlaying()) {
                        // 蓝牙耳机断开连接 同时当前音乐正在播放 则将其暂停
                        mMusicService.pasue();

                    } else if (bluetoothAdapter != null &&
                            BluetoothProfile.STATE_CONNECTED == bluetoothAdapter
                                    .getProfileConnectionState(
                                            BluetoothProfile.HEADSET)) {
                        mMusicService.pasue();
                    }
                    break;

                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    if (getPlayer().isPlaying()) {
                        // 有线耳机断开连接 同时当前音乐正在播放 则将其暂停
                        mMusicService.pasue();
                    }
                    break;
            }
        }
    }

    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////

    public void unBindService(Context context) {
        context.unbindService(sc);
    }

    public List<String> getMusicList() {
        return mMusicService.getMusicNameList();
        //        return mMusicNameList;
    }

    public void reset() {
        getPlayer().release();
        mSessionCompat.release();
        mHeadsetReceiver.unregister(mContext);
        mHeadsetPlugInReceiver.unregister(mContext);
    }

    private void notifylistener() {
        for (Listener listener : mListenerList) {
            listener.onDataChanged();
        }
    }

    public void play() {
        mMusicService.play();
        notifylistener();
    }

    public void pasue() {
        mMusicService.pasue();
        notifylistener();
    }

    public void playOrPause() {
        mMusicService.playOrPause();
        notifylistener();
    }

    public void playNext() {
        mMusicService.nextMusic();
        notifylistener();
    }

    public void playPrevious() {
        Log.d("dingyichen", "play playPrevious!!");
        mMusicService.preMusic();
        notifylistener();
    }

    public int getCurrentPlayingIndex() {
        return mMusicService.getCurrentIndex();
    }

    public void playIndex(int index) {
        mMusicService.playIndex(index);
        notifylistener();
    }

    public void stop() {
        mMusicService.stop();
    }

    public void setPlayType(PLAY_TYPE type) {
        Log.d("dingyichen", "setMusicPlayMode str = " + type);
        mMusicService.setPlayType(type);
        setMusicPlayMode(type);
    }

    public PLAY_TYPE getPlayType() {
        return mMusicService.getPlayType();
    }

    public void deleteMusicItem(int position) {
        mMusicService.deleteMusicItem(position);
        notifylistener();
    }

    public void delteMusicAll() {
        mMusicService.deleteAllMusic();
        notifylistener();

    }

    public int getCurrentPosition() {
        try {
            return mMusicService.mp.getCurrentPosition();
        } catch (IllegalStateException e) {

        }
        return 0;
    }

    public int getDuration() {
        try {
            return mMusicService.mp.getDuration();
        } catch (IllegalStateException e) {
        }
        return 0;
    }

    public String getCurrentName() {
        if (getMusicList().size() > 0) {
            if (getCurrentPlayingIndex() >= getMusicList().size()) {
                setMusicIndex(0);
                mMusicService.setCurrentIndex(0);
            }
            return getMusicList().get(getCurrentPlayingIndex());
        } else {
            return mContext.getString(R.string.demo_name);
        }
    }

    public void seekTo(int progress) {
        mMusicService.mp.seekTo(progress);
    }

    public int getVolume(Context context) {
        return mMusicService.getVolume(context);
    }

    public int getMaxVolume() {
        return mMusicService.getMaxVolume();
    }

    public void volumeUp(Context context) {
        mMusicService.volumeUp(context);
    }

    public void volumeDown(Context context) {
        mMusicService.volumeDown(context);
    }


    public static final String SHARED_PREFERENCE_MUSIC_INDEX = "music_index";
    public static final String SHARED_PREFERENCE_MUSIC_MODE = "music_play_mode";

    public static void setMusicIndex(int index) {
        Log.d("dingyichen", "setMusicIndex str = " + index);
        SharedPreferences preferences = mContext.getSharedPreferences(
                SHARED_PREFERENCE_MUSIC_INDEX,
                Context.MODE_PRIVATE);
        preferences.edit()
                   .putInt(SHARED_PREFERENCE_MUSIC_INDEX, index)
                   .commit();
    }

    public static int getMusicIndex() {
        Log.d("dingyichen", "getMusicIndex  ");
        SharedPreferences preferences = mContext.getSharedPreferences(
                SHARED_PREFERENCE_MUSIC_INDEX,
                Context.MODE_PRIVATE);
        return preferences.getInt(SHARED_PREFERENCE_MUSIC_INDEX, 0);
    }

    public static void setMusicPlayMode(PLAY_TYPE mode) {
        SharedPreferences preferences = mContext.getSharedPreferences(
                SHARED_PREFERENCE_MUSIC_MODE,
                Context.MODE_PRIVATE);
        int index = mode.ordinal();
        preferences.edit()
                   .putInt(SHARED_PREFERENCE_MUSIC_MODE, index)
                   .commit();
    }

    public static PLAY_TYPE getMusicPlayMode() {
        SharedPreferences preferences = mContext.getSharedPreferences(
                SHARED_PREFERENCE_MUSIC_MODE,
                Context.MODE_PRIVATE);
        int index = preferences.getInt(SHARED_PREFERENCE_MUSIC_MODE, 0);
        Log.d("dingyichen", "getMusicPlayMode  : " + index);
        return PLAY_TYPE.values()[index];
    }

    private ArrayList<Listener> mListenerList = new ArrayList<>();

    public interface Listener {
        void onDataChanged();

    }

    public void setListener(Listener listener) {
        mListenerList.add(listener);
    }
}
