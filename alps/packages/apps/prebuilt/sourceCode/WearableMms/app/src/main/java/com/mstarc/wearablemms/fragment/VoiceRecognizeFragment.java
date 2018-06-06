package com.mstarc.wearablemms.fragment;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.common.Constants;
import com.mstarc.wearablemms.common.JsonParser;
import com.mstarc.wearablemms.common.SwipeDismissLayout;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.common.WaveformView;
import com.mstarc.wearablemms.data.SmsDao;

/**
 * Created by wangxinzhi on 17-3-8.
 */

public class VoiceRecognizeFragment extends Fragment implements ConfirmDialog.Listener {
    ImageView mVoiceButton;
    WaveformView mWaveView;
    private SpeechRecognizer mIat;
    private StringBuffer mStringBuffer;
    private String mMessageContent;
    private String mPhoneNum;
    private static final String TAG = VoiceRecognizeFragment.class.getSimpleName();
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SwipeDismissLayout rootView = (SwipeDismissLayout) inflater.inflate(R.layout.voice,container,false);
        mWaveView = (WaveformView) rootView.findViewById(R.id.voice_wave);
        mVoiceButton = (ImageView) rootView.findViewById(R.id.voice_button);
        ThemeUtils.updateImageView(mVoiceButton,R.drawable.ic_voice);
        mWaveView.setVisibility(View.VISIBLE);
     //   mWaveView.setVisibility(View.INVISIBLE);
        mVoiceButton.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Pressed
                    // record and show animation
                    //mWaveView.startAnimation();
//                    update(7000);
                    mWaveView.setVisibility(View.VISIBLE);
                    mIat.startListening(mRecoListener);
                }
//
                return true;
            }
        });
//        rootView.setOnSwipeProgressChangedListener(new OnSwipeProgressChangedListener(){
//
//            @Override
//            public void onSwipeProgressChanged(SwipeDismissLayout layout, float progress, float translate) {
//                if(progress>0.5){
//                    ((MainActivity)getActivity()).showFragment(MainActivity.FRAGMENT_INDEX_SINGLE_MESSAGE,null);
//                }
//
//            }
//
//            @Override
//            public void onSwipeCancelled(SwipeDismissLayout layout) {
//
//            }
//        });
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln Mms 语音打开常亮-----------");
        initYuYin();
        mStringBuffer = new StringBuffer();
        mPhoneNum = getArguments().getString(Constants.PHONE_NUM);
        mIat.startListening(mRecoListener);
        return rootView;
    }

    @Override
    public void onConfirm() {
        SmsDao.sendSms(getActivity(),mPhoneNum,mMessageContent);
        //设置返回的数据
//        Intent intent = new Intent();
//        intent.putExtra("isRefresh", true);
        new ChatMessageFragment().isRefresh=true;
        getActivity().finish();
    }

    @Override
    public void onCancel() {
        mWaveView.setVisibility(View.INVISIBLE);
        mIat.stopListening();
    }
    @Override
    public void onPause(){
        super.onPause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln Mms语音关闭常亮-----------");
    }
    @Override
    public void onDetach() {
        super.onDetach();
        if (mIat.isListening()) {
            mIat.cancel();
            mIat.destroy();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    private void update(final float volume) {
        mWaveView.post(new Runnable() {
            @Override
            public void run() {
                mWaveView.updateAmplitude(volume * 0.1f / 2000);
            }
        });
    }
    private void initYuYin() {
        mIat = SpeechRecognizer.createRecognizer(getActivity(), null);
        //2.设置听写参数，详见《科大讯飞MSC API手册(Android)》SpeechConstant类
        mIat.setParameter(SpeechConstant.DOMAIN, "iat");
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin ");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "3000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "1500");
    }

    //听写监听器
    private RecognizerListener mRecoListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            update(i*500);
        }

        @Override
        public void onBeginOfSpeech() {
            Log.d("TAG", "onBeginOfSpeech");
//            update(7000);
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {

            String text = JsonParser.parseIatResult(recognizerResult.getResultString());
            mStringBuffer.append(text);
            Log.d("TAG", "布尔值:" + b + "   录音结果:" + text);
            if(b) {
                mMessageContent = mStringBuffer.toString();
                (new ConfirmDialog(VoiceRecognizeFragment.this.getActivity(),R.layout.voice_confirm_dialog,VoiceRecognizeFragment.this,mMessageContent)).show();
                mStringBuffer = new StringBuffer();
                mWaveView.setVisibility(View.INVISIBLE);
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
