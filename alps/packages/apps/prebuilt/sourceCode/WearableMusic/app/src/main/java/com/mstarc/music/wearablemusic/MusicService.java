package com.mstarc.music.wearablemusic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.mstarc.music.wearablemusic.data.MusicData;
import com.mstarc.music.wearablemusic.data.MusicDataContract;
import com.mstarc.music.wearablemusic.data.MusicDataProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by andyding on 2015/11/10.
 */
public class MusicService extends Service {
    // 音频文件保存的路径
    private String MUSIC_PATH = "";
    //private ArrayList<String> mMusicNameList = new ArrayList<String>();

    public static ConcurrentHashMap<String, MusicData> mMusicMap;

    private int mMaxVolume = 0;
    private int mCurrentVolume = 0;

    private int mCurrentIndex = 0;
    private AudioManager mAudioManager;
//    private int[] resid = new int[]{R.raw.eagles_hotel_california, R.raw.lizongsheng_piaoyangguohailaikanni_live,
//            R.raw.lizongsheng_shanqiu, R.raw.the_beatles_acoustic_trio_yesterday};

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                mAudioManager.abandonAudioFocus(afChangeListener);
                if (mp.isPlaying()) {
                    playOrPause();
                }
                // Pause playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                mAudioManager.abandonAudioFocus(afChangeListener);
                try {
                    if (getMusicPathList().size() > 0) {
                        if (mp.isPlaying()) {
                            mp.pause();
                        }
                    }
                } catch (IllegalStateException e) {

                }
                // Stop playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback or Raise it back to normal
                mAudioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                playOrPause();
            }
        }
    };

    public final IBinder binder = new MyBinder();

    public class MyBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    private MusicManager.PLAY_TYPE mPlaytype = MusicManager.PLAY_TYPE.PLAY_TYPE_SHUFFLE;
    public MediaPlayer mp = new MediaPlayer();

    public boolean musicFileIsExists(String strFile) {
        try {
            File f = new File(strFile);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void getMusicData() {
        MUSIC_PATH = Environment.getExternalStorageDirectory().getPath()
                + File.separator + "MstarcMusic";
        Cursor cursor = MusicDataProvider.getAllMusicData(this);
        if (cursor !=null && cursor.moveToFirst()) {
            do {
                String musicId = cursor.getString(cursor.getColumnIndex(MusicDataContract.Music.Column.MUSIC_ID));
                String music_filename = MUSIC_PATH + File.separator + cursor.getString(cursor.getColumnIndex(MusicDataContract.Music.Column.MUSIC_FILE_NAME));
                String musicName = cursor.getString(cursor.getColumnIndex(MusicDataContract.Music.Column.MUSIC_NAME));
                String musicSinger = cursor.getString(cursor.getColumnIndex(MusicDataContract.Music.Column.MUSIC_SINGER));

                MusicData data = new MusicData(musicId, music_filename, musicName, musicSinger);
                mMusicMap.put(musicId, data);
            } while (cursor.moveToNext());
        }
        if (cursor !=null) {
            cursor.close();
        }
    }

    public void deleteMusicItem(int position) {
        String musicpath = getMusicPathList().get(position);
        MusicData data = null;
        for (MusicData data1 : mMusicMap.values()) {
            if (data1.getMusicPath().equals(musicpath)) {
                data = data1;
                break;
            }
        }
        MusicDataProvider.deteleItem(this, data);
        mMusicMap.remove(data.getMusicId());
        File file = new File(musicpath);
        if (file.exists()) {
            file.delete();
        }
        if (position == mCurrentIndex) {
            if (mp != null) {
                mp.seekTo(0);
                mp.stop();
            }
            mCurrentIndex = 0;
            MusicManager.setMusicIndex(mCurrentIndex);
            if (getMusicPathList().size() > 0
                    && musicFileIsExists(getMusicPathList().get(mCurrentIndex))) {
                try {
                    mp.reset();
                    mp.setDataSource(getMusicPathList().get(mCurrentIndex));
                    mp.prepare();
                    initListener();
                } catch (Exception e) {
                }
            }
        } else if (position < mCurrentIndex) {
            mCurrentIndex--;
        }
    }

    public MusicManager.PLAY_TYPE getPlayType() {
        Log.d("dingyichen", "getPlayType = " + mPlaytype);
        return mPlaytype;
    }

    public void setPlayType(MusicManager.PLAY_TYPE type) {
        Log.d("dingyichen", "setPlayType str = " + type);
        mPlaytype = type;
    }

    public void deleteAllMusic() {
        if (mp != null) {
            mp.stop();
        }
        // 删除所有的录音文件
        for (String path : getMusicPathList()) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
        for (MusicData data : mMusicMap.values()) {
            MusicDataProvider.deteleItem(this, data);
        }
        mMusicMap.clear();
    }

    public int getVolume(Context context) {
        AudioManager am;
        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        mCurrentVolume = am.getStreamVolume(AudioManager.STREAM_SYSTEM);
        return mCurrentVolume;
    }

    public List<String> getMusicNameList() {
        if (mMusicMap == null) {
            return null;
        }
        List<String> musicList = new ArrayList<>();
        for (MusicData data : mMusicMap.values()) {
            musicList.add(data.getMusicName());
        }
        return musicList;
    }

    public List<String> getMusicPathList() {
        List<String> musicList = new ArrayList<>();
        for (MusicData data : mMusicMap.values()) {
            musicList.add(data.getMusicPath());
        }
        return musicList;
    }

    public int getMaxVolume() {
        return mMaxVolume;
    }

    public void volumeUp(Context context) {
        if (getVolume(context) < mMaxVolume / 5) {
            setVolume(mMaxVolume / 5);
        } else if (getVolume(context) < mMaxVolume * 2 / 5) {
            setVolume(mMaxVolume * 2 / 5);
        } else if (getVolume(context) < mMaxVolume * 3 / 5) {
            setVolume(mMaxVolume * 3 / 5);
        } else if (getVolume(context) < mMaxVolume * 4 / 5) {
            setVolume(mMaxVolume * 4 / 5);
        } else {
            setVolume(mMaxVolume);
        }
    }

    public void volumeDown(Context context) {
        if (getVolume(context) > mMaxVolume * 4 / 5) {
            setVolume(mMaxVolume * 4 / 5);
        } else if (getVolume(context) > mMaxVolume * 3 / 5) {
            setVolume(mMaxVolume * 3 / 5);
        } else if (getVolume(context) > mMaxVolume * 2 / 5) {
            setVolume(mMaxVolume * 2 / 5);
        }else {
            setVolume(mMaxVolume / 5);
        }
    }

    private void setVolume(int index) {
        AudioManager am;
        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        am.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, index, 0);
        am.setStreamVolume(AudioManager.STREAM_SYSTEM, index, 0);
        am.setStreamVolume(AudioManager.STREAM_RING, index, 0);
        am.setStreamVolume(AudioManager.STREAM_ALARM, index, 0);
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, index, 0);
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    public void setCurrentIndex(int index) {
        mCurrentIndex = index;
    }

    public void init() {
        Log.d("dingyichen", "Musicservice onCreate!!!");
        mMusicMap = new ConcurrentHashMap<>();
        getMusicData();
        try {
            mCurrentIndex = MusicManager.getMusicIndex();
            if (!mp.isPlaying()) {
                if (getMusicPathList().size() > 0) {
                    if (musicFileIsExists(getMusicPathList().get(mCurrentIndex))) {
                        mp.setDataSource(getMusicPathList().get(mCurrentIndex));
                        mp.prepare();
                        mp.seekTo(0);
                        initListener();
                    } else {
                        deleteMusicItem(mCurrentIndex);
                    }
                }
            }
        } catch (Exception e) {
            Log.d("dingyichen", "can't get to the song");
            e.printStackTrace();
        }
        mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(afChangeListener,AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private void initListener() {
        if (mp != null) {
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d("dingyichen", "onCompletion!!! play type : " + getPlayType());
                    if (getPlayType() == MusicManager.PLAY_TYPE.PLAY_TYPE_LOOP_ONE) {
                        playIndex(mCurrentIndex);
                    } else {
                        mCurrentIndex = getNextIndex(true);
                    }
                    MusicManager.getInstance().playIndex(mCurrentIndex);
                }
            });
        }
    }

    public void play() {
        if (getMusicPathList().size() > 0) {
            mp.start();
        }
    }

    public void pasue() {
        if (getMusicPathList().size() > 0 && mp.isPlaying()) {
            mp.pause();
        }
    }

    public void playOrPause() {
        if (getMusicPathList().size() > 0) {
            if (mp.isPlaying()) {
                mp.pause();
            } else {
                mp.start();
            }
        }
    }

    public void stop() {
        if (mp != null) {
            mp.stop();
            try {
                //mp.prepareAsync();
                //mp.seekTo(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int getNextIndex(boolean next) {
        int size = mMusicMap.size();
        switch (mPlaytype) {
            case PLAY_TYPE_SHUFFLE:
                if (size == 1) {
                    mCurrentIndex = 0;
                } else {
                    int index = -1;
                    index = new Random().nextInt(size);
                    while (index == mCurrentIndex) {
                        index = new Random().nextInt(size);
                    }
                    mCurrentIndex = index;
                }
                break;
            case PLAY_TYPE_ORDER:
            case PLAY_TYPE_LOOP_ONE:
            case PLAY_TYPE_LOOP_ALL:
                if (next) {
                    if (mCurrentIndex < size - 1) {
                        mCurrentIndex++;
                    } else {
                        mCurrentIndex = 0;
                    }
                } else {
                    if (mCurrentIndex > 0) {
                        mCurrentIndex--;
                    } else {
                        mCurrentIndex = size - 1;
                    }
                }
                break;
        }
        return mCurrentIndex;
    }

    public void playIndex(int index) {
        if (mp != null) {
            mp.stop();
            try {
                mp.reset();
                mCurrentIndex = index;
                Log.d("dingyichen", "play playIndex!! new = " + index);
                mp.setDataSource(getMusicPathList().get(mCurrentIndex));
                mp.prepare();
                MusicManager.setMusicIndex(index);
                mp.seekTo(0);
                mp.start();
                initListener();
            } catch (Exception e) {
                Log.d("hint", "can't jump next music");
                e.printStackTrace();
            }
        }
    }

    public void nextMusic() {
        if (mp != null) {
            mp.stop();
            try {
                mp.reset();
                mCurrentIndex = getNextIndex(true);
                Log.d("dingyichen", "play nextMusic!! new = " + mCurrentIndex);
                mp.setDataSource(getMusicPathList().get(mCurrentIndex));
                mp.prepare();
                /*AssetFileDescriptor afd = getResources().openRawResourceFd(resid[mCurrentIndex]);
                if (afd == null) return;
                mp.setDataSource(afd.getFileDescriptor());
                afd.close();*/
                MusicManager.setMusicIndex(mCurrentIndex);
                mp.seekTo(0);
                mp.start();
                initListener();
            } catch (Exception e) {
                Log.d("hint", "can't jump next music");
                e.printStackTrace();
            }
        }
    }

    public void preMusic() {
        if (mp != null) {
            mp.stop();
            try {
                mp.reset();
                mCurrentIndex = getNextIndex(false);
                mp.setDataSource(getMusicPathList().get(mCurrentIndex));
                mp.prepare();
                Log.d("dingyichen", "preMusic current index = " + mCurrentIndex);
                mp.seekTo(0);
                mp.start();
                initListener();
            } catch (Exception e) {
                Log.d("hint", "can't jump pre music");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mp != null) {
            try {
                mp.stop();
                mp.release();
            } catch (IllegalStateException e) {

            }
        }
        super.onDestroy();
    }

    /**
     * onBind 是 Service 的虚方法，因此我们不得不实现它。
     * 返回 null，表示客服端不能建立到此服务的连接。
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


}
