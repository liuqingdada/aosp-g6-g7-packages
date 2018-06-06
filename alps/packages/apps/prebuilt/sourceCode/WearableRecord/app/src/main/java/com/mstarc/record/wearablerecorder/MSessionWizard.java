package com.mstarc.record.wearablerecorder;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import com.kw.rxbus.RxBus;
import com.mstarc.record.wearablerecorder.bean.RecordBus;
import com.mstarc.record.wearablerecorder.receiver.HeadsetButtonReceiver;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by liuqing
 * 18-1-3.
 * Email: 1239604859@qq.com
 */

public class MSessionWizard {
    private static final String TAG = "RecordSuhen";
    private static AtomicBoolean flag = new AtomicBoolean(false);
    private static MSessionWizard mMSessionWizard;
    private Context mContext;
    private MediaSessionCompat mSessionCompat;
    private HeadsetPlugInReceiver mHeadsetPlugInReceiver;
    private HeadsetReceiver mHeadsetReceiver;
    private TelephonyManager mTelephonyManager;
    ///
    private boolean isRecordView;
    private boolean isPlayView;
    ///

    static MSessionWizard getInstance(Context context) {
        if (flag.compareAndSet(false, true)) {
            if (mMSessionWizard == null) {
                mMSessionWizard = new MSessionWizard(context);
            }
        }
        return mMSessionWizard;
    }

    private MSessionWizard(Context context) {
        mContext = context;
    }

    void initMediaSession() {
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
        //
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        assert mTelephonyManager != null;
        mTelephonyManager.listen(mTelephonyListener, PhoneStateListener.LISTEN_CALL_STATE);
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
                    if (isRecordView) {
                        RxBus.getInstance()
                             .send(new RecordBus().setStartOrPauseRecord(true));
                    }

                    if (isPlayView) {
                        RxBus.getInstance()
                             .send(new RecordBus().setStartOrPausePlay(true));
                    }
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    if (isRecordView) {
                        RxBus.getInstance()
                             .send(new RecordBus().setStartRecord(true));
                    }

                    if (isPlayView) {
                        RxBus.getInstance()
                             .send(new RecordBus().setStartPlay(true));
                    }
                    return true;

                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    rxbusPause();
                    return true;

                case KeyEvent.KEYCODE_MEDIA_STOP:
                    if (isRecordView) {
                        RxBus.getInstance()
                             .send(new RecordBus().setStopRecord(true));
                    }

                    if (isPlayView) {
                        RxBus.getInstance()
                             .send(new RecordBus().setPausePlay(true));
                    }
                    return true;

                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    break;

                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    break;

                default:
                    break;
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
                rxbusPause();
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
            switch (intent.getAction()) {
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                    if (bluetoothAdapter != null &&
                            BluetoothProfile.STATE_DISCONNECTED == bluetoothAdapter
                                    .getProfileConnectionState(
                                            BluetoothProfile.HEADSET)) {
                        rxbusPause();

                    } else if (bluetoothAdapter != null &&
                            BluetoothProfile.STATE_CONNECTED == bluetoothAdapter
                                    .getProfileConnectionState(
                                            BluetoothProfile.HEADSET)) {
                        rxbusPause();
                    }
                    break;

                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    break;
            }
        }
    }

    /**
     * 电话状态广播接收器
     */
    private PhoneStateListener mTelephonyListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE: // 空闲
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK: // 摘机, 正在通话中
                    rxbusPause();
                    break;
                case TelephonyManager.CALL_STATE_RINGING: // 来电
                    rxbusPause();
                    break;
                default:
                    break;
            }
        }
    };

    void finish() {
        setMediaSessionActive(false);
        mSessionCompat.release();
        mHeadsetReceiver.unregister(mContext);
        mHeadsetPlugInReceiver.unregister(mContext);
        mTelephonyManager.listen(mTelephonyListener, PhoneStateListener.LISTEN_NONE);
    }

    ///
    public void setRecordView(boolean recordView) {
        isRecordView = recordView;
    }

    public void setPlayView(boolean playView) {
        isPlayView = playView;
    }
    ///

    private void rxbusPause() {
        if (isRecordView) {
            RxBus.getInstance()
                 .send(new RecordBus().setPauseRecord(true));
        }

        if (isPlayView) {
            RxBus.getInstance()
                 .send(new RecordBus().setPausePlay(true));
        }
    }
}
