package com.mstarc.record.wearablerecorder.fragment;


import android.app.Dialog;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kw.rxbus.RxBus;
import com.mstarc.record.wearablerecorder.R;
import com.mstarc.record.wearablerecorder.RecordManager;
import com.mstarc.record.wearablerecorder.ThemeUtils;
import com.mstarc.record.wearablerecorder.bean.RecordBus;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecordFragment extends Fragment implements ConfirmDialog.Listener, RecordManager.StopListener {
    private static final String TAG = RecordFragment.class.getSimpleName();
    private long mLastCmdTime;
    private static final int DEFAULT_COMMAND_GAP = 600;

    private String mRecordName;
    private ImageView mControl;
    private ImageView mComplete;
    private TextView mTips;
    Chronometer mChronometer;
    String mPath;
//    private boolean bRecording = false;
//    private boolean bPause = false;
//    private long mRecordTime = 0;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Anhry/" + getActivity().getPackageName() + "/Record";
        View view = inflater.inflate(R.layout.fragment_record, container, false);
        mControl = (ImageView) view.findViewById(R.id.control);
        mComplete = (ImageView) view.findViewById(R.id.complete);
        mChronometer = (Chronometer) view.findViewById(R.id.timer_text);
        mTips = (TextView) view.findViewById(R.id.control_tips);
        ImageView iconbadge = (ImageView) view.findViewById(R.id.icon_record_badge);

        RecordManager.getInstance().setStopListener(this);
        updateImageView(iconbadge, R.drawable.icon_record_badge);
        updateImageView(mControl, R.drawable.record_start_bg);
        updateImageView(mComplete, R.drawable.record_complete_bg);
        mTips.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        mControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_COMMAND_GAP) {
                    return;
                }
                toggleRecord();
                mLastCmdTime = System.currentTimeMillis();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RxBus.getInstance()
             .toObservable(RecordBus.class)
             .observeOn(AndroidSchedulers.mainThread())
             .subscribe(new Consumer<RecordBus>() {
                 @Override
                 public void accept(RecordBus recordBus) {
                     Log.i(TAG, "accept: " + recordBus);
                     try {
                         if (recordBus.isStartOrPauseRecord()) {
                             if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_COMMAND_GAP) {
                                 return;
                             }
                             toggleRecord();
                             mLastCmdTime = System.currentTimeMillis();

                         } else if (recordBus.isStartRecord()) {
                             if (!RecordManager.getInstance()
                                               .isRecording()) {
                                 File file = new File(mPath);
                                 File[] files = file.listFiles();
                                 if (files != null && files.length > 19) {
                                     showDilog();
                                     return;
                                 }

                                 if (RecordManager.getInstance()
                                                  .startRecord()) {
                                     mControl.post(new Runnable() {
                                         @Override
                                         public void run() {
                                             updateImageView(mControl, R.drawable.record_stop_bg);
                                         }
                                     });
                                     onRecordStart();
                                 } else {
                                     mControl.post(new Runnable() {
                                         @Override
                                         public void run() {
                                             updateImageView(mControl, R.drawable.record_start_bg);
                                         }
                                     });
                                     onRecordStop();
                                 }
                             }

                         } else if (recordBus.isPauseRecord()) {
                             if (RecordManager.getInstance()
                                              .isRecording()) {
                                 if (RecordManager.getInstance()
                                                  .pauseRecord()) {
                                     mControl.post(new Runnable() {
                                         @Override
                                         public void run() {
                                             updateImageView(mControl, R.drawable.record_start_bg);
                                         }
                                     });
                                 }
                                 onRecordPause();
                             }

                         } else if (recordBus.isStopRecord()) {
                             finishRecord();
                         } else {}
                     } catch (Exception e) {
                         Log.e("RecordSuhen", "RxBus: ", e);
                     }
                 }
             });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCompleteView();
        Log.d(TAG, "RecordFragment  isrecording : " + RecordManager.getInstance().isRecording()
                + ", is pause = " + RecordManager.getInstance().bPause());
        if (RecordManager.getInstance().isRecording()) {
            onRecordStart();
            updateImageView(mControl, R.drawable.record_stop_bg);
        } else if (RecordManager.getInstance().bPause()) {
            mChronometer.setBase(SystemClock.elapsedRealtime() - RecordManager.getInstance().getRecordTime());// 跳过已经记录了的时间，起到继续计时的作用
            mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                public void onChronometerTick(final Chronometer cArg) {
                    long t = SystemClock.elapsedRealtime() - cArg.getBase();
                    SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.US);
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String str = df.format(t);
                    cArg.setText(str);
                }
            });
            updateImageView(mControl, R.drawable.record_start_bg);
            onRecordPause();
            Log.d(TAG, "RecordFragment oncreate time = : " + RecordManager.getInstance().getRecordTime());
        } else {
            mTips.setText("录音");
            mChronometer.stop();
            mChronometer.setText("00:00:00");
            updateImageView(mControl, R.drawable.record_start_bg);
        }
    }

    private void refreshCompleteView() {
        mComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishRecord();
            }
        });
    }

    private void finishRecord() {
        boolean bValid;
        if (RecordManager.getInstance()
                         .isRecording()) {
            bValid = RecordManager.getInstance()
                                  .pauseRecord();
            if (bValid) {
                mControl.post(new Runnable() {
                    @Override
                    public void run() {
                        updateImageView(mControl, R.drawable.record_start_bg);
                    }
                });
                onRecordPause();
            }
        } else {
            bValid = RecordManager.getInstance()
                                  .getRecordTime() > 1000;
        }
        if (bValid) {
            mRecordName = RecordManager.getInstance()
                                       .getCompleteTimeName();
            ConfirmDialog confirmDialog = new ConfirmDialog(RecordFragment.this.getActivity(),
                                                            R.layout.record_confirm_dialog,
                                                            RecordFragment.this,
                                                            mRecordName);
            confirmDialog.show();
            onRecordStop();
        }
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    private void showDilog() {
        final Dialog dialog = new Dialog(getActivity(), R.style.tip_dialog);//指定自定義樣式
        //1. 先获取布局的view
        RelativeLayout view = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(R.layout.switch_dialog, null);
        //2. 加载 view
        dialog.setContentView(view);//指定自定義layout
        dialog.setCanceledOnTouchOutside(false);
        //3. 获取dialog view 下的控件
        ImageView ok = (ImageView) view.findViewById(R.id.btn_Y);
        updateImageView(ok, R.drawable.btn_ok);
        //4.对控件做设置或者设置listenner
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        //  5. 直接
        dialog.show();
    }

    private void toggleRecord() {
        if (!RecordManager.getInstance().isRecording()) {
            File file = new File(mPath);
            if (file != null) {
                File[] files = file.listFiles();
                if (files != null && files.length > 19) {
                    showDilog();
                    return;
                }
            }

            if (RecordManager.getInstance().startRecord()) {
                mControl.post(new Runnable() {
                    @Override
                    public void run() {
                        updateImageView(mControl, R.drawable.record_stop_bg);
                    }
                });
                onRecordStart();
            } else {
                mControl.post(new Runnable() {
                    @Override
                    public void run() {
                        updateImageView(mControl, R.drawable.record_start_bg);
                    }
                });
                onRecordStop();
            }
        } else {
            if (RecordManager.getInstance().pauseRecord()) {
                mControl.post(new Runnable() {
                    @Override
                    public void run() {
                        updateImageView(mControl, R.drawable.record_start_bg);
                    }
                });
            }
            onRecordPause();
        }
    }

    public void onRecordStart() {
        mChronometer.setBase(SystemClock.elapsedRealtime() - RecordManager.getInstance().getRecordTime());// 跳过已经记录了的时间，起到继续计时的作用
        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            public void onChronometerTick(final Chronometer cArg) {
                long t = SystemClock.elapsedRealtime() - cArg.getBase();
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.US);
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                String str = df.format(t);
                cArg.setText(str);
            }
        });
        mChronometer.start();
//        refreshCompleteView(false);
        refreshCompleteView();
        mTips.setText("录音中");
    }

    public void onRecordPause() {
        mChronometer.stop();
//        refreshCompleteView(true);
        refreshCompleteView();
        mTips.setText("暂停中");
    }

    public void onRecordStop() {
        mChronometer.setBase(SystemClock.elapsedRealtime());
//        refreshCompleteView(false);
        refreshCompleteView();
        mTips.setText("录音");
    }

    @Override
    public void onConfirm() {
        RecordManager.getInstance().stopRecord(mRecordName);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Date date = new Date(System.currentTimeMillis());
        String str = format.format(date);
        if (str.equals(RecordManager.getLastRecordDate())) {
            int times = Integer.valueOf(RecordManager.getLastRecordTimes());
            RecordManager.setRecordTimes(String.valueOf(times + 1));
        } else {
            RecordManager.setRecordDate(str);
            RecordManager.setRecordTimes("1");
        }
        onRecordStop();
    }

    @Override
    public void onCancel() {
        RecordManager.getInstance().reset();
        onRecordStop();
    }

    @Override
    public void stop() {
        Log.d(TAG, "收到回调保存1");
        if (mControl != null && mChronometer != null) {
            Log.d(TAG, "收到回调保存2");
            RecordManager.getInstance().pauseRecord();
            updateImageView(mControl, R.drawable.record_start_bg);
            RecordManager.getInstance().stopRecord(RecordManager.getInstance().getCompleteTimeName());
            mChronometer.stop();
            onRecordStop();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
            Date date = new Date(System.currentTimeMillis());
            String str = format.format(date);
            if (str.equals(RecordManager.getLastRecordDate())) {
                int times = Integer.valueOf(RecordManager.getLastRecordTimes());
                RecordManager.setRecordTimes(String.valueOf(times + 1));
            } else {
                RecordManager.setRecordDate(str);
                RecordManager.setRecordTimes("1");
            }
        }
    }
}
