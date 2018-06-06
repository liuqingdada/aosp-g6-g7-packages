package com.mstarc.wearablesettings.activitys;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.fakewatch.ota.api.bean.OTAUpdate;
import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.utils.ThemeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;

import mstarc_os_api.mstarc_os_api_msg;
import mstarc_os_api.mstarc_os_retmsg;

public class DownloadActivity extends BaseActivity {

    private static final String TAG = DownloadActivity.class.getSimpleName();
    private static final int PROGRESS_UPDATE_FINISH_DOWNLOAD_MESSAGE = 1;
    private static final int MAX_VALUE = 100;
    private static final String OTA_FILE = "/data/ota/update.zip";
    private ProgressBar mProgressBar;
    private ImageView mWaiting;
    private TextView mPercent;
    private TextView mProportion;
    private TextView mStatus;
    private Animation mAnimation;
    private OTAUpdate mOTAUpdate;
    private int mCurDataIndex = 0;
    private int mFinisCopyTime = 0;
    List<OTAUpdate.DatasEntity> mDatas;
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
                    mAnimation = AnimationUtils.loadAnimation(DownloadActivity.this, R.anim.img_animation);
                    LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
                    mAnimation.setInterpolator(lin);
                    mWaiting.startAnimation(mAnimation);
                    Log.i(TAG, "start animation");
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
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        updateProgressBarColor();
        mWaiting = (ImageView) findViewById(R.id.waiting);
        mPercent = (TextView) findViewById(R.id.percent);
        mStatus = (TextView) findViewById(R.id.status);
        int theme = ThemeUtils.getCurrentPrimaryColor();
        Log.i(TAG, "gln theme" + theme);
        mStatus.setTextColor(theme);
        mProportion = (TextView) findViewById(R.id.proportion);
        mOTAUpdate = getOTAUpdate();
        mDatas = mOTAUpdate.getDatas();
        for (OTAUpdate.DatasEntity data : mDatas) {
            Log.i(TAG, "updatetype" + data.getUpdatetype());
            Log.i(TAG, data.getUrl());
            Log.i(TAG, "md5 " + data.getMd5());
        }
        if (mDatas.size() > 0) {
            new DownloadTask(this, mDatas.get(0)).execute();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeMessages(PROGRESS_UPDATE_FINISH_DOWNLOAD_MESSAGE);
    }

    private class DownloadTask extends AsyncTask<Void, Integer, String> {
        private Context context;
        private OTAUpdate.DatasEntity data;
        private PowerManager.WakeLock mWakeLock;
        private String fileName = "/sdcard/temp";
        private int fileLength;
        private File file;

        public DownloadTask(Context context, OTAUpdate.DatasEntity data) {
            this.context = context;
            this.data = data;
            if (data.getUpdatetype() == 0) {
                fileName = OTA_FILE;
            } else {
                fileName = fileName + mCurDataIndex + ".apk";
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(data.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                fileLength = connection.getContentLength();
                // download the file
                input = connection.getInputStream();

                output = new FileOutputStream(fileName);
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressBar.setMax(MAX_VALUE);
            mProgressBar.setProgress(progress[0]);
            mPercent.setText(getResources().getString(R.string.current_percent, progress[0]) + "%");
            mProportion.setText(getResources().getString(R.string.current_proportion, (float) (fileLength / (1024 * 1024) * progress[0] / MAX_VALUE), (float) (fileLength / (1024 * 1024))));
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            if (result != null) {
                Log.e(TAG, "Download error: " + result);
                showDownloadFail(fileName);
            } else {
                Log.i(TAG, "File downloaded " + fileName);
                if (data.getUpdatetype() == 0) {
                    mHandler.sendEmptyMessageDelayed(PROGRESS_UPDATE_FINISH_DOWNLOAD_MESSAGE, 100);
                }
                new CheckMd5Task(fileName, data).execute();
            }
        }
    }

    private String getFileMD5(File file) {
        if (!file.isFile()) {
            Log.e(TAG, "get md5 error file null");
            return null;
        }
        Log.e(TAG, "get md5 start");
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "get md5 error " + e);
            return null;
        }
        Log.e(TAG, "get md5 end");
        return bytesToHexString(digest.digest());
    }

    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            Log.e(TAG, "get bytesToHexString error ");
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public class CopyFileTask extends AsyncTask<String, Void, Boolean> {

        private int type;
        private String temFile;

        public CopyFileTask(int type) {
            this.type = type;
        }

        @Override

        protected Boolean doInBackground(String... fileNames) {
            int result = -1;
            if (mIsSettingReady) {
                Log.i(TAG, "start copy doInBackground");
                if (type == 0) {
                    Log.i(TAG, "start update");
                    return true;
                } else {
                    temFile = fileNames[0];
                    mstarc_os_retmsg t_ret = m_api_msg.mstarc_api_ota_copy_file(DownloadActivity.this, temFile, fileNames[1]);
                    result = t_ret.ret_type;
                }
                if (result == 0) {
                    Log.e(TAG, "copy file right");
                    return true;
                } else {
                    Log.e(TAG, "copy file error");
                }
            } else {
                Log.e(TAG, "init api error");
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                if (type == 0) {
                    if (mIsSettingReady) {
                        ota_fun();
                        Log.i(TAG, "ota update");
                    } else {
                        Log.e(TAG, "init api error");
                        showDownloadFail(OTA_FILE);
                    }
                } else {
                    File file = new File(temFile);
                    if (file.exists()) {
                        file.delete();
                    }
                    if (mDatas.size() == 1) {
                        //reboot system
                        if (mIsSettingReady) {
                            m_api_msg.mstarc_api_adb(DownloadActivity.this, "reboot");
                            Log.i(TAG, "reboot");
                            finish();
                        } else {
                            Log.e(TAG, "init api error");
                            showDownloadFail(null);
                        }
                    } else {
                        mFinisCopyTime++;
                        if (mFinisCopyTime == mDatas.size()) {
                            //reboot system
                            if (mIsSettingReady) {
                                m_api_msg.mstarc_api_adb(DownloadActivity.this, "reboot");
                                Log.i(TAG, "reboot");
                                finish();
                            } else {
                                Log.e(TAG, "init api error");
                                showDownloadFail(null);
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "copy file fail");
                showDownloadFail(temFile);
            }
            super.onPostExecute(aBoolean);
        }
    }

    public void ota_fun() {
        mstarc_os_retmsg t_ret = m_api_msg.mstarc_api_ota_initpage();

        if (t_ret.ret_type != 0) {
            Log.e(TAG, "error:" + t_ret.ret_error);
            showDownloadFail(OTA_FILE);
        } else {
            Log.i(TAG, "ok:" + t_ret.ret_success);
        }
    }

    private void showDownloadFail(String fileName) {
        if (fileName != null) {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        }
        Toast.makeText(DownloadActivity.this, getResources().getString(R.string.download_fail), Toast.LENGTH_LONG).show();
        finish();
    }

    public class CheckMd5Task extends AsyncTask<Void, Void, String> {

        private String fileName;
        private OTAUpdate.DatasEntity data;

        public CheckMd5Task(String fileName, OTAUpdate.DatasEntity data) {
            this.fileName = fileName;
            this.data = data;
        }

        @Override
        protected String doInBackground(Void... voids) {
            return getFileMD5(new File(fileName));
        }

        @Override
        protected void onPostExecute(String md5) {
            if (md5 == null) {
                showDownloadFail(fileName);
                return;
            }
            Log.i(TAG, fileName + " md5 : " + md5 + " url md5 :" + data.getMd5());
            if (md5.equals(data.getMd5())) {
                Log.i(TAG, "download right " + data.getUpdatetype());
                if (data.getUpdatetype() == 0) {
                    Log.i(TAG, "finish download and start copy");
                    new CopyFileTask(0).execute(fileName, "");
                } else {
                    new CopyFileTask(1).execute(fileName, String.format("/%s/%s.apk", data.getSofttype(), data.getSoftname()));
                    mCurDataIndex++;
                    if (mCurDataIndex < mDatas.size()) {
                        new DownloadTask(DownloadActivity.this, mDatas.get(mCurDataIndex)).execute();
                    } else {
                        mHandler.sendEmptyMessageDelayed(PROGRESS_UPDATE_FINISH_DOWNLOAD_MESSAGE, 100);
                    }
                }
            } else {
                Log.e(TAG, "Download fail");
                showDownloadFail(fileName);
            }
            super.onPostExecute(md5);
        }
    }

    private void updateProgressBarColor() {
        LayerDrawable layerDrawable = (LayerDrawable) mProgressBar.getProgressDrawable();
        Drawable drawable = layerDrawable.findDrawableByLayerId(android.R.id.progress);
        int color = ThemeUtils.getCurrentPrimaryColor();
        ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
        drawable.clearColorFilter();
        drawable.mutate().setColorFilter(filter);
    }

}
