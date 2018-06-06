package com.mstarc.wearablesport;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cleveroad.loopbar.adapter.ICategoryItem;
import com.cleveroad.loopbar.adapter.SimpleCategoriesAdapter;
import com.cleveroad.loopbar.model.SportInfo;
import com.cleveroad.loopbar.widget.LoopBarView;
import com.mstarc.watchbase.service.sportservice.IService;
import com.mstarc.watchbase.service.sportservice.bean.SportNodeInfo;
import com.mstarc.watchbase.service.sportservice.bean.SportType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.cleveroad.loopbar.widget.LoopBarView.SCROLL_MODE_INFINITE;

/**
 * Created by wangxinzhi on 17-3-11.
 */

public class ProgressFragment extends Fragment implements IService.IDataChangedListener, View.OnClickListener {
    private static final String TAG = ProgressFragment.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;
    LoopBarView mLoopBarView;
    RelativeLayout mScrollView;
    View mEmptyView, mText3, mText2, mText1, mFinalView;
    UiHandler mHandler;
    TextView mHeartBeatTextView;
    ImageButton mStartButton, mStopButton;
    private long mTime = 0;
    List<ICategoryItem> mItems = new ArrayList<>();
    boolean isListenerAttached = false;
    SimpleCategoriesAdapter mAdatper;
    SportNodeInfo mSportNodeInfo;
    boolean isSportStarted = false;
    boolean isSportPaused = false;
    SportType mSportType;
    TextView mTimeText;
    ImageView mTimerImage;
    static final long maxSportTime = 99 * 60 * 60;
    SoundPool mSoundPool;
    boolean hasTimeReachedVibrated = false;
    int soundId;
    AnimatorSet mAnimatorSet;
    NotificationManager mNotificationManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.scrollprocess, container, false);
//        mSportType = SportType.values()[getArguments().getInt("TARGET", SportType.RIDE.ordinal())];
        mSportType = ((MainActivity) getActivity()).getSportType();
        if (mSportType == SportType.RIDE) {
            ViewStub process_sub = (ViewStub) rootView.findViewById(R.id.prosess_ride_stub);
            process_sub.inflate();
            ImageView typeIcon = (ImageView) rootView.findViewById(R.id.progress_type_icon);
            updateImageView(typeIcon, R.drawable.qixing);

        } else {
            ViewStub process_sub = (ViewStub) rootView.findViewById(R.id.prosess_stub);
            process_sub.inflate();
            ImageView typeIcon = (ImageView) rootView.findViewById(R.id.progress_type_icon);
            if (mSportType == SportType.RUN_INDOOR) {
                updateImageView(typeIcon, R.drawable.indor_paobu_small);
            } else if (mSportType == SportType.RUN_OUTDOOR) {
                updateImageView(typeIcon, R.drawable.outdor_paobu_small);
            }
        }
        mLoopBarView = (LoopBarView) rootView.findViewById(R.id.endlessView);
        mLoopBarView.setScrollMode(SCROLL_MODE_INFINITE);
        mText3 = rootView.findViewById(R.id.text3);
        mText2 = rootView.findViewById(R.id.text2);
        mText1 = rootView.findViewById(R.id.text1);
        mFinalView = rootView.findViewById(R.id.process);
        mText3.setVisibility(View.INVISIBLE);
        mText2.setVisibility(View.INVISIBLE);
        mText1.setVisibility(View.INVISIBLE);
        mFinalView.setVisibility(View.INVISIBLE);
        mScrollView = (RelativeLayout) rootView.findViewById(R.id.scrollView);
        updateImageView(rootView.findViewById(R.id.sport_progress_divider1), R.drawable.diver);
        updateImageView(rootView.findViewById(R.id.sport_progress_divider2), R.drawable.diver);
        updateImageView(rootView.findViewById(R.id.sport_progress_heart_icon), R.drawable.xin);
        updateImageView(rootView.findViewById(R.id.sport_progress_header_timer), R.drawable.miaobiao);
        updateImageView(rootView.findViewById(R.id.sport_pause), R.drawable.selector_pause);
        updateImageView(rootView.findViewById(R.id.sport_stop), R.drawable.selector_stop);
        mHandler = new UiHandler();
        mItems.clear();
        if (mSportType != SportType.RIDE) {
            mItems.add(new SportInfo(getActivity().getString(R.string.sport_progress_item_bushu), "0", null, false, SportInfo.ID_STEP));
        }
        mItems.add(new SportInfo(getActivity().getString(R.string.sport_progress_item_licheng), "0", getString(R.string.sport_progress_item_qianmi), false, SportInfo.ID_DISTANCE));
        mItems.add(new SportInfo(getActivity().getString(R.string.sport_progress_item_reliang), "0", getString(R.string.sport_progress_item_qianka), false, SportInfo.ID_CAL));
        mItems.add(new SportInfo(getActivity().getString(R.string.sport_progress_item_sudu), "0", null, false, SportInfo.ID_SPEED));
        mAdatper = new SimpleCategoriesAdapter(mItems);
        mLoopBarView.setCategoriesAdapter(mAdatper);
        mTimeText = (TextView) rootView.findViewById(R.id.text_time);
        mTimerImage = (ImageView) rootView.findViewById(R.id.sport_progress_header_timer);

        {
            SoundPool.Builder builder = new SoundPool.Builder();
            builder.setMaxStreams(1);
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            builder.setAudioAttributes(attrBuilder.build());
            mSoundPool = builder.build();
            try {
                soundId = mSoundPool.load(getContext().getAssets().openFd("timeout.ogg"), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ;
        }
        mNotificationManager = ((NotificationManager) getContext().getSystemService(NOTIFICATION_SERVICE));
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHeartBeatTextView = (TextView) view.findViewById(R.id.text_heartbit);
        mStartButton = (ImageButton) view.findViewById(R.id.sport_pause);
        mStopButton = (ImageButton) view.findViewById(R.id.sport_stop);
        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
    }


    class UiHandler extends Handler {
        public static final int MSG_ANMIATION_START = 0;
        public static final int MSG_START_SPORT = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ANMIATION_START: {
                    startAnimator();
                    Log.e(TAG, "MSG_ANMIATION_START");
                    break;
                }
                case MSG_START_SPORT:
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null && activity.mSportService != null) {
                        activity.mSportService.startSport();
                        isSportStarted = true;
                        mStartButton.setSelected(true);
                        countTimer();
                    } else {
                        Log.e(TAG, "ProgressFragment::UiHandler::handleMessage activity is null!");
                    }
                    break;
            }
        }
    }

    void startAnimator() {
        AnimatorSet set, set1, set2, set3, set4;
        if (mAnimatorSet != null && mAnimatorSet.isRunning()) {
            mAnimatorSet.cancel();
        }
        set = new AnimatorSet();
        set1 = generageAnimatorSet(mText3);
        set2 = generageAnimatorSet(mText2);
        set3 = generageAnimatorSet(mText1);
        set4 = generageFinalViewAnimator(mFinalView);
        set.addListener(new Animator.AnimatorListener() {
            boolean isCanceled = false;

            @Override
            public void onAnimationStart(Animator animation) {
                mSoundPool.play(soundId, 1, 1, 0, 0, 1);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mText1.setVisibility(View.GONE);
                mText2.setVisibility(View.GONE);
                mText3.setVisibility(View.GONE);
                mFinalView.setVisibility(View.VISIBLE);
                if (!isCanceled) {
                    mHandler.sendEmptyMessage(UiHandler.MSG_START_SPORT);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mSoundPool.stop(soundId);
                mHandler.removeMessages(UiHandler.MSG_START_SPORT);
                isCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.playSequentially(set1, set2, set3, set4);
        mAnimatorSet = set;
        set.start();
    }

    AnimatorSet generageFinalViewAnimator(final View target) {
        AnimatorSet animatorSet = new AnimatorSet();
        int parentHeight = ((ViewGroup) target.getParent()).getHeight();
        ObjectAnimator translateY = ObjectAnimator.ofFloat(target, "translationY", parentHeight, 0);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(target, "alpha", 0f, 1f);
        animatorSet.playTogether(translateY, alpha);
        animatorSet.setDuration(1000);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                target.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                target.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.setInterpolator(new DecelerateInterpolator());
        return animatorSet;
    }

    AnimatorSet generageAnimatorSet(final View target) {
        AnimatorSet animatorSet = new AnimatorSet();
        int parentHeight = ((ViewGroup) target.getParent()).getHeight();
        ObjectAnimator translateY = ObjectAnimator.ofFloat(target, "translationY", parentHeight - target.getTop(), -(target.getBottom()));
        ObjectAnimator alpha = ObjectAnimator.ofFloat(target, "alpha", 0f, 1f, 0f);
        animatorSet.playTogether(translateY, alpha);
        animatorSet.playTogether(translateY);
        animatorSet.setDuration(1000);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                target.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                target.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                target.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animatorSet.setInterpolator(new DecelerateAccelerateInterpolator());
        return animatorSet;
    }


    class DecelerateAccelerateInterpolator implements TimeInterpolator {

        @Override
        public float getInterpolation(float input) {
            float result;
            if (input <= 0.5) {
                result = (float) (Math.sin(Math.PI * input)) / 2;
            } else {
                result = (float) (2 - Math.sin(Math.PI * input)) / 2;
            }
            return result;
        }

    }

    private void rmNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    void sendNotification() {
        rmNotification();
        PendingIntent localPendingIntent = PendingIntent.getActivity(getActivity(),
                0, new Intent(getActivity(), MainActivity.class), 0);

        Notification.Builder builder = new Notification.Builder(getContext());
        builder.setSmallIcon(R.drawable.ic_notification_sport)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getContext().getText(R.string.sport_notification_content))
                .setContentText(getContext().getText(R.string.sport_notification_content))
                .setContentIntent(localPendingIntent)
                .setAutoCancel(false);
        mNotificationManager.notify(NOTIFICATION_ID, builder.build());

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mHandler.removeMessages(UiHandler.MSG_ANMIATION_START);
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            Log.e(TAG, "cancel animation");
        }
        /**
         * 返回运动状态,即上次操作
         * -1.无操作 1.开始 2.暂停 3.继续 4.停止
         */
        int sportCondition = ((MainActivity) getActivity()).mSportService.getSportCondition();
        switch (sportCondition) {
            case -1:
            case 4:
                rmNotification();
                break;
            case 1:
            case 2:
            case 3:
                sendNotification();
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!isListenerAttached) {
            ((MainActivity) getActivity()).mSportService.setDataChangedListener(this);
        }
        /**
         * 返回运动状态,即上次操作
         * -1.无操作 1.开始 2.暂停 3.继续 4.停止
         */
        int sportCondition = ((MainActivity) getActivity()).mSportService.getSportCondition();
        switch (sportCondition) {
            case -1:
            case 4:
                mHandler.sendEmptyMessageDelayed(UiHandler.MSG_ANMIATION_START, 1000);
                break;
            case 1:
                isSportStarted = true;
                isSportPaused = false;
                mStartButton.setSelected(true);
                mFinalView.setVisibility(View.VISIBLE);
                countTimer();
                break;
            case 2:
                ((MainActivity) getActivity()).mSportService.resumeSport();
                isSportStarted = true;
                isSportPaused = false;
                mStartButton.setSelected(true);
                mFinalView.setVisibility(View.VISIBLE);
                countTimer();
                break;
            case 3:
                isSportStarted = true;
                isSportPaused = false;
                mStartButton.setSelected(true);
                mFinalView.setVisibility(View.VISIBLE);
                countTimer();
                break;
        }
    }

    @Override
    public void onResult(SportNodeInfo sportNodeInfo, boolean[] booleen) {
        Log.d(TAG, "sportNodeInfo: " + sportNodeInfo + " boolean: " + booleen);
        mHeartBeatTextView.setText("" + sportNodeInfo.getHeartRate());
        mSportNodeInfo = sportNodeInfo;
        boolean isDistanceReached = booleen[0];
        boolean isTimeReached = booleen[1];
        boolean isCalReached = booleen[2];
        ArrayList<ICategoryItem> tempItems = new ArrayList<>();
        tempItems.addAll(mItems);
        mItems.clear();
        mTimerImage.setVisibility(isTimeReached ? View.VISIBLE : View.INVISIBLE);
        if (isTimeReached && !hasTimeReachedVibrated) {
            hasTimeReachedVibrated = true;
        /*    Context context = getContext();
            if(context!=null) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if(vibrator!=null) {
                    vibrator.vibrate(1000);
                }
            }*/
            //震动放在服务里,此处注掉  wyg
        }
        for (ICategoryItem item : tempItems) {
            SportInfo sportInfo = (SportInfo) item;
            mItems.add((item));
            switch (sportInfo.ID) {
                case SportInfo.ID_SPEED:
                    sportInfo.mValue = mSportNodeInfo.getPace();
                    break;
                case SportInfo.ID_STEP:
                    sportInfo.mValue = (int) mSportNodeInfo.getStep() + "";
                    break;
                case SportInfo.ID_DISTANCE:
                    if (isDistanceReached) {
                        sportInfo.setTargetReached();
                        mItems.remove(item);
                        mItems.add(0, item);
                    }
                    sportInfo.mValue = Double.toString(mSportNodeInfo.getDistance());
                    break;
                case SportInfo.ID_CAL:
                    if (isCalReached) {
                        sportInfo.setTargetReached();
                        mItems.remove(item);
                        mItems.add(0, item);
                    }
                    sportInfo.mValue = "" + mSportNodeInfo.getCal();
                    break;
            }
        }
        mSportNodeInfo = sportNodeInfo;
        mLoopBarView.getWrappedRecyclerView().getAdapter().notifyDataSetChanged();
    }


    @Override
    public void onClick(View v) {
        if (v == mStartButton) {
            if (isSportStarted) {
                if (isSportPaused) {
                    ((MainActivity) getActivity()).mSportService.resumeSport();
                    isSportPaused = false;
                    v.setSelected(true);
                    countTimer();
                } else {
                    ((MainActivity) getActivity()).mSportService.pauseSport();
                    isSportPaused = true;
                    v.setSelected(false);
                }
            } else {
                ((MainActivity) getActivity()).mSportService.startSport();
                isSportStarted = true;
                v.setSelected(true);
                countTimer();
            }
        } else if (v == mStopButton) {
            if (isSportStarted) {
                ((MainActivity) getActivity()).mSportService.stopSport();
                ((MainActivity) getActivity()).mSportService.saveData();
                isSportStarted = false;
            }
            Bundle bundle = new Bundle();
            bundle.putSerializable("RESULT", mSportNodeInfo);
            bundle.putString("TIME", String.valueOf(mTimeText.getText()));
            ((MainActivity) getActivity()).showFragment(MainActivity.FRAGMENT_INDEX_RESULT, bundle);
        }
    }

    private Runnable TimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSportStarted && !isSportPaused && isResumed()) {
                String time = ((MainActivity) getActivity()).mSportService.getSportTime();
                Log.d(TAG, "sport time: " + time);
                mTimeText.setText(time);
                if (mTime < maxSportTime) {
                    mTime++;
                    countTimer();
                }
            }
        }
    };

    private void countTimer() {
        mHandler.postDelayed(TimerRunnable, 1000);
    }

    String getFormatTime(long time) {
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", time / (60 * 60), (time / (60)) % 60, time % 60);
    }

    private void updateImageView(final View view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageDrawable(drawable);
                } else {
                    view.setBackground(drawable);
                }
            }
        });
    }
}
