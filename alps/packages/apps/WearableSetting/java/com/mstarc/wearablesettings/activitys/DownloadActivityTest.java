package com.mstarc.wearablesettings.activitys;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mstarc.fakewatch.ota.api.bean.OTAUpdate;
import com.mstarc.wearablesettings.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.List;
import java.util.Random;

import mstarc_os_api.mstarc_os_api_msg;
import mstarc_os_api.mstarc_os_retmsg;

public class DownloadActivityTest extends BaseActivity {
    
    private static final String TAG = DownloadActivityTest.class.getSimpleName();
    private static final int PROGRESS_UPDATE_FINISH_DOWNLOAD_MESSAGE = 1;
    private static final int MAX_VALUE = 100;
    private ProgressBar mProgressBar;
    private ImageView mWaiting;
    private TextView mPercent;
    private TextView mProportion;
    private TextView mStatus;
    private float mIndex = 0;
    private Random random=new Random();
    private Animation mAnimation;
    private OTAUpdate mOTAUpdate;
    private int mCurDataIndex = 0;
    private int mFinisCopyTime = 0;
    List<OTAUpdate.DatasEntity> mDatas;
    private mstarc_os_api_msg m_api_msg;
    final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PROGRESS_UPDATE_FINISH_DOWNLOAD_MESSAGE:
                    mPercent.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.GONE);
                    mProportion.setVisibility(View.GONE);
                    mWaiting.setVisibility(View.VISIBLE);
                    mStatus.setVisibility(View.VISIBLE);
                    mAnimation = AnimationUtils.loadAnimation(DownloadActivityTest.this, R.anim.img_animation);
                    LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
                    mAnimation.setInterpolator(lin);
                    mWaiting.startAnimation(mAnimation);
                    break;
                default:
                    break;
            }

            super.handleMessage(msg);
        }

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_updating);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mWaiting = (ImageView)findViewById(R.id.waiting);
        mPercent = (TextView)findViewById(R.id.percent);
        mStatus = (TextView)findViewById(R.id.status);
        mProportion = (TextView)findViewById(R.id.proportion);
        m_api_msg=new mstarc_os_api_msg(this)
        {
            @Override
            public void onServiceConnected() {
                super.onServiceConnected();
                Log.i(TAG,"onServiceConnected");
                ota_fun();
            }
        };
    }

    public void ota_fun()
    {
        mstarc_os_retmsg t_ret = m_api_msg.mstarc_api_ota_initpage();

        if (t_ret.ret_type != 0) {
            Log.i(TAG, "error:" + t_ret.ret_error);
        } else {
            Log.i(TAG, "ok:" + t_ret.ret_success);
        }
    }
}
