package com.mstarc.record.wearablerecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by andyding on 2015/11/10.
 */
public class RecordService extends Service {
    private static final String TAG = RecordService.class.getSimpleName();

    // 音频文件保存的路径
    private String mRecordPath = "";
    // 语音文件
    private String mRecordFileName = null;
    private String mRecordFragFileName = null;

    private int mMaxVolume = 0;
    private int mCurrentVolume = 0;

    private long mLimitTime = 0;// 录音文件最短事件1秒
    private long mRecordTIme = 0; // 录音时间,毫秒
    Timer timer;

    public static MediaPlayer mPlayer = new MediaPlayer();
    private MediaRecorder mRecorder = null;// 录音器
    private boolean isPause = false;// 当前录音是否处于暂停状态
    private boolean isRecord = false;// 当前录音状态
    private ArrayList<String> mList = new ArrayList<String>();// 待合成的录音片段
    private ArrayList<String> mRecordList = new ArrayList<String>();// 已合成的录音片段

    public final IBinder binder = new MyBinder();
    private AudioManager mAudioManager;

    public class MyBinder extends Binder {
        RecordService getService() {
            return RecordService.this;
        }
    }

    public void init() {
        initRecordList();
        mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    /**
     * 这块的回调并没有处理
     */
    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                mAudioManager.abandonAudioFocus(afChangeListener);
                pausePlay();
                // Pause playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                mAudioManager.abandonAudioFocus(afChangeListener);
                stopPlay();
                // Stop playback
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback or Raise it back to normal
                mAudioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                pausePlay();
            }
        }
    };

    public int getVolume() {
        AudioManager am;
        am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        mMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        mCurrentVolume = am.getStreamVolume(AudioManager.STREAM_SYSTEM);
        return mCurrentVolume;
    }

    public int getMaxVolume() {
        return mMaxVolume;
    }

    public void volumeUp() {
        if (getVolume() < mMaxVolume / 5) {
            setVolume(mMaxVolume / 5);
        } else if (getVolume() < mMaxVolume * 2 / 5) {
            setVolume(mMaxVolume * 2 / 5);
        } else if (getVolume() < mMaxVolume * 3 / 5) {
            setVolume(mMaxVolume * 3 / 5);
        } else if (getVolume() < mMaxVolume * 4 / 5) {
            setVolume(mMaxVolume * 4 / 5);
        } else {
            setVolume(mMaxVolume);
        }
    }

    public void volumeDown() {
        if (getVolume() > mMaxVolume * 4 / 5) {
            setVolume(mMaxVolume * 4 / 5);
        } else if (getVolume() > mMaxVolume * 3 / 5) {
            setVolume(mMaxVolume * 3 / 5);
        } else if (getVolume() > mMaxVolume * 2 / 5) {
            setVolume(mMaxVolume * 2 / 5);
        } else {
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

    // 初始化录音列表
    private void initRecordList() {
        mRecordList.clear();
        mRecordPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Anhry/" + this.getPackageName() + "/Record";
        //Toast.makeText(this, mRecordPath, Toast.LENGTH_LONG).show();
        // 判断SD卡是否存在
        if (!Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "SD卡状态异常，无法获取录音列表！", Toast.LENGTH_LONG).show();
        } else {
            // 根据后缀名进行判断、获取文件夹中的音频文件
            File file = new File(mRecordPath);
            File files[] = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().indexOf(".") >= 0) {
                        // 只取.aac .mp3
                        // .mp4 文件
                        String fileStr = files[i].getName().substring(
                                files[i].getName().indexOf("."));
                        if ((fileStr.toLowerCase().equals(".mp3")
                                || fileStr.toLowerCase().equals(".aac")
                                || fileStr.toLowerCase().equals(".mp4"))) {
                            String name = files[i].getName();
                            if (name.contains("-")) {
                                if (files[i].length() == 0) {
                                    files[i].delete();
                                } else {
                                    String[] names = name.split("\\.");
                                    mRecordList.add(names[0]);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    TimerTask task;
          /*  = new TimerTask() {
        @Override
        public void run() {
            mRecordTIme = mRecordTIme + 1000;
        }
    };*/

    public ArrayList<String> getRecordList() {
        return mRecordList;
    }

    public ArrayList<String> getReverseList() {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < mRecordList.size(); i++) {
            list.add(mRecordList.get(mRecordList.size() - 1 - i));
        }
        return list;
    }

    public long getRecordTime() {
        return mRecordTIme;
    }

    public void setRecordTime(long time) {
        mRecordTIme = time;
    }

    public boolean startRecord() {
        Log.d(TAG, "startRecord!!");
        if (!isPause) {
            // 新录音清空列表
            mList.clear();
        }
        File file = new File(mRecordPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        mRecordFragFileName = mRecordPath + "/" + getFragTimeName() + ".aac";
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        //设置保存文件格式为aac
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        //设置采样频率,44100是所有安卓设备都支持的频率,频率越高，音质越好，当然文件越大
        mRecorder.setAudioSamplingRate(44100);
        //设置声音数据编码格式,音频通用格式是AAC
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //设置编码频率
        mRecorder.setAudioEncodingBitRate(96000);
        //设置录音保存的文件
        mRecorder.setOutputFile(mRecordFragFileName);


        try {
            mRecorder.prepare();
        } catch (Exception e) {
            // 若录音器启动失败就需要重启应用，屏蔽掉按钮的点击事件。 否则会出现各种异常。
            Toast.makeText(RecordManager.mContext, "录音器启动失败，请返回重试！", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            if (mRecorder != null) {
                mRecorder.release();
            }
            mRecorder = null;
        }
        if (mRecorder != null) {
            isPause = false;
            isRecord = true;
            mRecorder.start();
            mLimitTime = System.currentTimeMillis();
            Log.d(TAG, "startRecord!! true");
            timer = new Timer();
            task = new TimerTask() {
                @Override
                public void run() {
                    mRecordTIme = mRecordTIme + 1000;
                    if (mRecordTIme == 20 * 60 * 1000) {
                        Log.d(TAG, "服务中到时间自动保存");
                        mHandler.sendEmptyMessage(MSG_SAVE);
                    }
                }
            };
            timer.schedule(task, 1000, 1000);
            return true;
        } else {
            isPause = true;
            Log.d(TAG, "startRecord!! false");
            return false;
        }
    }

    final int MSG_SAVE = 1001;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SAVE:
                    RecordManager.getInstance().notiStopListener();
                    break;
                default:
                    break;
            }
        }
    };

    public boolean pauseRecord() {
        Log.d(TAG, "pauseRecord!!");
        if (System.currentTimeMillis() - mLimitTime < 1100) {
            //录音文件不得低于一秒钟
            Toast.makeText(RecordManager.mContext, "录音时间长度不得低于1秒钟！", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
        }
        isPause = true;
        isRecord = false;
        // 将录音片段加入列表
        mList.add(mRecordFragFileName);
        task.cancel();
        return true;
    }

    public String getCompleteTimeName() {
        return getCurrentTime();
    }

    public boolean isRecording() {
        return isRecord;
    }

    public boolean isPause() {
        return isPause;
    }

    public void releaseRecord() {
        Log.d(TAG, "releaseRecord !!!");
        if (mRecorder != null) {
            mRecorder.release();
        }
        mRecorder = null;
        isPause = false;
        isRecord = false;
    }

    public void stopRecord(String timeName) {
        Log.d(TAG, "stopRecord timeName " + timeName);
        if (mRecorder != null) {
            mRecorder.release();
        }
        mRecorder = null;
        isPause = false;
        isRecord = false;
        // 最后合成的音频文件
        mRecordFileName = mRecordPath + "/" + timeName + ".aac";
        String fileName1 = getCurrentTime();
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(mRecordFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        FileInputStream fileInputStream = null;
        try {
            for (int i = 0; i < mList.size(); i++) {
                Log.d(TAG, "路径" + i + ":" + mList.get(i));
                File file = new File(mList.get(i));
                // 把因为暂停所录出的多段录音进行读取
                fileInputStream = new FileInputStream(file);
                byte[] mByte = new byte[fileInputStream.available()];
                int length = mByte.length;
                Log.d(TAG, "stopRecord length =  " + length);
                // 第一个录音文件的前六位是不需要删除的
                if (length > 0) {
//                    if (i == 0) {
                    while (fileInputStream.read(mByte) != -1) {
                        fileOutputStream.write(mByte, 0, length);
                    }
//                    }
                    // 之后的文件，去掉前六位
//                    else {
//                        while (fileInputStream.read(mByte) != -1) {
//                            fileOutputStream.write(mByte, 6, length - 6);
//                        }
//                    }
                }
            }
            boolean bHasFile = false;
            for (String name : mRecordList) {
                if (name.equals(fileName1)) {
                    bHasFile = true;
                }
            }
            if (!bHasFile) {
                mRecordList.add(fileName1);
            }
            RecordManager.getInstance().notifylistener();
        } catch (Exception e) {
            // 这里捕获流的IO异常，万一系统错误需要提示用户
            e.printStackTrace();
            new File(mRecordFileName).delete();
            Toast.makeText(RecordManager.mContext, "录音合成出错，请重试！", Toast.LENGTH_LONG).show();
        } finally {
            try {
                fileOutputStream.flush();
                fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 不管合成是否成功、删除录音片段
        for (int i = 0; i < mList.size(); i++) {
            File file = new File(mList.get(i));
            if (file.exists()) {
                file.delete();
            }
        }
        task.cancel();
//        timer.cancel();
        mRecordTIme = 0;
    }

    public void reset() {
        if (mRecorder != null) {
            mRecorder.release();
        }
        mList.clear();
        mRecorder = null;
        isPause = false;
        isRecord = false;
        if (task != null) {
            task.cancel();
        }
        mRecordTIme = 0;
    }

    public boolean playRrecord(String filePath) {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mPlayer = new MediaPlayer();
        // 播放完毕的监听
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // 播放完毕改变状态，释放资源
                mPlayer.release();
                mPlayer = null;
            }
        });
        try {
            // 播放所选中的录音
            mPlayer.setDataSource(filePath);
            mPlayer.prepare();
            mPlayer.start();
            return true;
        } catch (Exception e) {
            // 若出现异常被捕获后，同样要释放掉资源
            // 否则程序会不稳定，不适合正式项目上使用
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
            return false;
        }
    }

    public void pausePlay() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void ContinuePlay() {
        if (mPlayer != null) {
            mPlayer.start();
        }
    }

    public boolean isPlaying() {
        boolean isPlaying = false;
        if (mPlayer != null) {
            isPlaying = mPlayer.isPlaying();
        }
        return isPlaying;
    }

    public void stopPlay() {
        if (mPlayer != null) {
            // 释放资源
            // 对MediaPlayer多次使用而不释放资源就会出现MediaPlayer create faild 的异常
            mPlayer.release();
            mPlayer = null;
        }
    }


    public void deleteRecordItem(int position) {
        // 删除所选中的录音文件
        ArrayList<String> list = getReverseList();
        File file = new File(mRecordPath + "/" + list.get(position) + ".aac");
        if (file.exists()) {
            file.delete();
            mRecordList.remove(list.get(position));
        } else {
            mRecordList.remove(list.get(position));
        }
        RecordManager.getInstance().notifylistener();
    }

    public void deleteAllRecord() {
        // 删除所有的录音文件
        for (int i = 0; mRecordList.size() != 0; i++) {
            deleteRecordItem(0);
        }
        // 根据后缀名进行判断、获取文件夹中的音频文件
        File file = new File(mRecordPath);
        File files[] = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().contains(".")) {
                    // 只取.aac .mp3
                    // .mp4 文件
                    String fileStr = files[i].getName().substring(
                            files[i].getName().indexOf("."));
                    if (fileStr.toLowerCase().equals(".mp3")
                            || fileStr.toLowerCase().equals(".aac")
                            || fileStr.toLowerCase().equals(".mp4")) {
                        files[i].delete();
                    }
                }
            }
        }
    }

    private String getFragTimeName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyymmddhhmmss");
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        String time = formatter.format(curDate);
        return time;
    }

    //获取当前时间，以其为名来保存录音
    public String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Date date = new Date(System.currentTimeMillis());
        String str = format.format(date);
        String times = "1";
        Log.d(TAG, "date = " + RecordManager.getLastRecordDate() + " , times = " +
                RecordManager.getLastRecordTimes());
        if ("".equals(RecordManager.getLastRecordDate())) {
            RecordManager.setRecordDate(str);
        }
        if (str.equals(RecordManager.getLastRecordDate())) {
            times = RecordManager.getLastRecordTimes();
        }
        times = String.format(Locale.US, "%03d", Integer.valueOf(times));
        return str + "-" + times;

    }

    @Override
    public void onDestroy() {
        mPlayer.stop();
        mPlayer.release();
        mRecorder.stop();
        mRecorder.release();
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
