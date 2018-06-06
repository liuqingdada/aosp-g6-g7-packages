package com.mstarc.wechat.wearwechat.fragment;


import android.app.Fragment;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.mstarc.wechat.wearwechat.MessageHandleActivity;
import com.mstarc.wechat.wearwechat.R;
import com.mstarc.wechat.wearwechat.ThemeUtils;
import com.mstarc.wechat.wearwechat.common.SwipeDismissLayout;
import com.mstarc.wechat.wearwechat.common.WaveformView;
import com.mstarc.wechat.wearwechat.utils.JsonParser;
import android.view.WindowManager;

/**
 * Created by wangxinzhi on 17-3-8.
 */

public class VoiceRecognizeFragment extends Fragment implements ConfirmDialog.Listener {
    ImageButton mVoiceButton;
    WaveformView mWaveView;
    private static final String TAG = VoiceRecognizeFragment.class.getSimpleName();
    private SpeechRecognizer mIat;
    //语音相关
    private StringBuffer mStringBuffer = new StringBuffer();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        SwipeDismissLayout rootView = (SwipeDismissLayout) inflater.inflate(R.layout.voice, container,false);
        mWaveView = (WaveformView) rootView.findViewById(R.id.voice_wave);
        mVoiceButton = (ImageButton) rootView.findViewById(R.id.voice_button);
        updateImageView(mVoiceButton, R.mipmap.icon_wechat_voice_1);
        mWaveView.setVisibility(View.INVISIBLE);
        rootView.setOnSwipeProgressChangedListener(new SwipeDismissLayout.OnSwipeProgressChangedListener() {

            @Override
            public void onSwipeProgressChanged(SwipeDismissLayout layout, float progress, float translate) {
                Log.d("dingyichen", "Voice fragment onswipeprogresschanged");
                if (progress > 0.5) {
                    ((MessageHandleActivity) getActivity()).showFragment(MessageHandleActivity.FRAGMENT_INDEX_CHAT_MESSAGE, null);
                }

            }

            @Override
            public void onSwipeCancelled(SwipeDismissLayout layout) {

            }
        });
        initYuYin();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln wechat 语音打开常亮-----------");
        return rootView;
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    @Override
    public void onConfirm() {
        Bundle bundle = new Bundle();
        bundle.putString("voice_content", mStringBuffer.toString());
        bundle.putBoolean("send_confirm", true);
        ((MessageHandleActivity) getActivity()).showFragment(MessageHandleActivity.FRAGMENT_INDEX_CHAT_MESSAGE, bundle);
        mStringBuffer.setLength(0);
    }

    @Override
    public void onCancel() {
        mStringBuffer.setLength(0);
        ((MessageHandleActivity) getActivity()).showFragment(MessageHandleActivity.FRAGMENT_INDEX_CHAT_MESSAGE, null);
    }

    private void update(final float volume) {
        mWaveView.post(new Runnable() {
            @Override
            public void run() {
                mWaveView.updateAmplitude(volume * 0.1f / 2000);
            }
        });
    }
    @Override
    public void onPause(){
        super.onPause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln wechat语音关闭常亮-----------");
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 退出时释放连接.
        if (mIat.isListening()) {
            mIat.cancel();
            mIat.destroy();
        }
    }

    private void initYuYin() {
        Log.d("dingyichen", "initYuYin");
        mIat = SpeechRecognizer.createRecognizer(getActivity(), null);
        //2.设置听写参数，详见《科大讯飞MSC API手册(Android)》SpeechConstant类
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin ");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "3000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1500");
        mIat.startListening(mRecoListener);
    }

    //听写监听器
    private RecognizerListener mRecoListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            update(i * 500);
        }

        @Override
        public void onBeginOfSpeech() {
            Log.d("dingyichen", "onBeginOfSpeech!!!");
            mWaveView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onEndOfSpeech() {
            Log.d("dingyichen", "onEndOfSpeech!!!");

            mWaveView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            Log.d("dingyichen", "onResult!!!");
            String text = JsonParser.parseIatResult(recognizerResult.getResultString());
            Log.d("TAG", "布尔值:" + b + "   录音结果:" + text);
            mStringBuffer.append(text);
            if (b) {
                Log.d("dingyichen", "onResult + test!!!");
                ConfirmDialog confirmDialog = new ConfirmDialog(VoiceRecognizeFragment.this.getActivity(),
                        R.layout.voice_confirm_dialog, VoiceRecognizeFragment.this, mStringBuffer.toString());
                confirmDialog.show();
            }

        }

        @Override
        public void onError(SpeechError speechError) {
            Log.d("TAG", "录音出错:" + speechError);
            Toast.makeText(getActivity(), speechError.toString().substring(0, speechError.toString().indexOf(".")) + "!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };
}
