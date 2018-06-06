package com.mstarc.wearablephone.view;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.wearablephone.PhoneApplication;
import com.mstarc.wearablephone.R;


import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.cloud.util.ContactManager.ContactListener;
import com.mstarc.wearablephone.view.common.FucUtil;
import com.mstarc.wearablephone.view.common.JsonParser;
import android.view.WindowManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by wangxinzhi on 17-5-17.
 */

public class VoiceFragment extends Fragment {
    private static final String TAG = VoiceFragment.class.getSimpleName();
    private WaveformView mWaveView;
    private SpeechRecognizer mIat;
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    MsgHandler mHandler;
    TextView mTextView;
    TextView mNoVoice;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    class MsgHandler extends Handler {
        public final static int MSG_START_LISTEN = 1;
        public final static int MSG_STOP_LISTEN = 2;
        public final static int MSG_AUDIO_TEST = 3;
        public final static int MSG_UPLOAD_USERWORDS = 4;
        public final static int MSG_INIT_RECOGNIZER = 5;
        public final static int MSG_NO_VOICE = 6;

        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "handleMessage:" + msg.what);
            switch (msg.what) {
                case MSG_START_LISTEN:
                    mIatResults.clear();
                    if (mIat == null) {
                        sendEmptyMessageDelayed(MSG_START_LISTEN, 500);
                        return;
                    }
                    int ret = 0; // 函数调用返回值
                    ret = mIat.startListening(mRecognizerListener);
                    if (ret != ErrorCode.SUCCESS) {
                        Log.d(TAG, "听写失败,错误码：" + ret);
                    } else {
                        Log.d(TAG, "听写开始");
                    }
                    break;
                case MSG_STOP_LISTEN:
                    if (mIat != null) {
                        mIat.cancel();
                        mIat.stopListening();
                    }
                    break;
                case MSG_AUDIO_TEST:
                    if (mIat != null) {
                        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
                        // 也可以像以下这样直接设置音频文件路径识别（要求设置文件在sdcard上的全路径）：
                        // mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-2");
                        // mIat.setParameter(SpeechConstant.ASR_SOURCE_PATH, "sdcard/XXX/XXX.pcm");
                        ret = mIat.startListening(mRecognizerListener);
                        byte[] audioData = FucUtil.readAudioFile(getContext(), "iattest.wav");
                        if (null != audioData) {
                            Log.d(TAG, "开始");
                            // 一次（也可以分多次）写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），位长16bit，单声道的wav或者pcm
                            // 写入8KHz采样的音频时，必须先调用setParameter(SpeechConstant.SAMPLE_RATE, "8000")设置正确的采样率
                            // 注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别。
                            // 音频切分方法：FucUtil.splitBuffer(byte[] buffer,int length,int spsize);
                            mIat.writeAudio(audioData, 0, audioData.length);
                            mIat.stopListening();
                        } else {
                            mIat.cancel();
                            Log.d(TAG, "读取音频流失败");
                        }
                    }
                    break;
                case MSG_UPLOAD_USERWORDS:
                    if (mIat != null) {
                        ContactManager mgr = ContactManager.createManager(getContext(),
                                mContactListener);
                        mgr.asyncQueryAllContactsName();
                    }
                    break;
                case MSG_INIT_RECOGNIZER:
                    Log.d(TAG, "开始初始化SpeechRecognizer");
                    mIat = SpeechRecognizer.createRecognizer(getContext(), mInitListener);
                    Log.d(TAG, "speechRecognizer初始化完成 " + mIat);
                    sendEmptyMessageDelayed(MSG_START_LISTEN, 500);
                    break;
                case MSG_NO_VOICE:
                    mNoVoice.setVisibility(View.VISIBLE);
                    getActivity().finish();
            }
        }
    }

    /**
     * 上传联系人/词表监听器。
     */
    private LexiconListener mLexiconListener = new LexiconListener() {

        @Override
        public void onLexiconUpdated(String lexiconId, SpeechError error) {
            if (error != null) {
                Log.d(TAG, "联系人上传失败");
            } else {
                Log.d(TAG, "联系人上传成功");
            }
        }
    };

    /**
     * 获取联系人监听器。
     */
    private ContactListener mContactListener = new ContactListener() {

        @Override
        public void onContactQueryFinish(final String contactInfos, boolean changeFlag) {
            // 注：实际应用中除第一次上传之外，之后应该通过changeFlag判断是否需要上传，否则会造成不必要的流量.
            // 每当联系人发生变化，该接口都将会被回调，可通过ContactManager.destroy()销毁对象，解除回调。
            int ret;
            if (changeFlag) {
                // 指定引擎类型
                mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
                mIat.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
                ret = mIat.updateLexicon("contact", contactInfos, mLexiconListener);
                if (ret != ErrorCode.SUCCESS) {
                    Log.d(TAG, "上传联系人失败：" + ret);
                }
            }
        }
    };

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Log.d(TAG, "开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
            Log.d(TAG, error.getPlainDescription(true));
            mIat.cancel();
//            if (error == 10118) {
//                Log.e(TAG, "Error:No input data, please try again");
//            } else {
//                Log.e(TAG, "Error: - " + error);
//            }
            mHandler.sendEmptyMessageDelayed(MsgHandler.MSG_NO_VOICE,200);
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Log.d(TAG, "结束说话");

        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            String result = getResult(results);

            if (isLast) {
                if (result == null || result == "") {
                    Log.e(TAG, "onResults with empty result");
                    mHandler.sendEmptyMessageDelayed(MsgHandler.MSG_NO_VOICE,200);
                } else {
                    Log.e(TAG, "Results = " + result);
                    sendResult(result);
                }
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            Log.d(TAG, "当前正在说话，音量大小：" + volume + " 返回音频数据：" + data.length);
            update(volume);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private String getResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        Log.d(TAG, "printResult: " + resultBuffer.toString());
        return resultBuffer.toString();
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "初始化失败，错误码：" + code);
            } else {
                setParam();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new MsgHandler();
        mHandler.sendEmptyMessage(mHandler.MSG_INIT_RECOGNIZER);
        mHandler.sendEmptyMessage(mHandler.MSG_UPLOAD_USERWORDS);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }

    private void sendResult(String result) {
        if (result == null || result.isEmpty()) {
            result = "dme";
        }
        mTextView.setText(result);
        ((VoiceActivity) getActivity()).showVoiceResult(result);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        int themeResID = ((PhoneApplication) getContext().getApplicationContext()).getThemeStyle();
        if (themeResID != 0) {
            getContext().getTheme().applyStyle(themeResID, true);
        }
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.voice, container, false);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln 电话 语音打开常亮-----------");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mWaveView = (WaveformView) view.findViewById(R.id.voice_wave);
        mTextView = (TextView) view.findViewById(R.id.textView);
        mNoVoice = (TextView) view.findViewById(R.id.noVoice);
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.removeMessages(MsgHandler.MSG_START_LISTEN);
        mHandler.removeMessages(MsgHandler.MSG_STOP_LISTEN);
        mHandler.sendEmptyMessage(MsgHandler.MSG_START_LISTEN);

//        mHandler.sendEmptyMessageDelayed(MsgHandler.MSG_START_LISTEN, 500);
    }


    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeMessages(MsgHandler.MSG_START_LISTEN);
        mHandler.removeMessages(MsgHandler.MSG_STOP_LISTEN);
        mHandler.sendEmptyMessage(MsgHandler.MSG_STOP_LISTEN);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln 电话语音关闭常亮-----------");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void update(final float volume) {
        mWaveView.post(new Runnable() {
            @Override
            public void run() {
                mWaveView.updateAmplitude(volume / 20);
            }
        });
    }


    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "1500");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "0");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }
}
