package com.mstarc.app.radio;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mstarc.app.ThemeUtils;
import com.mstarc.app.Tools.SharedTool;
import com.mstarc.app.Tools.Tools;
import com.mstarc.app.db.DistrictRadioDao;
import com.mstarc.app.service.PlayService;
import com.situ.android.util.NetUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vov.vitamio.Vitamio;

public class RadioActivity extends Activity implements PlayService.PlayStateListener {
    public static Context mContext;
    String act_token;
    private final String SHARED_BELONG_KEY = "shared_belong_key";
    private final String RADIO_PLAY_URL = "RADIO_URL";
    private final String URL_HEAD = "http://mstarc.anquanzhuo.com:8066/iw/";
    private final String belong_url = URL_HEAD + "getcity";
    private final String Client_ID = "YmY3MDkwY2UtNzk1MC0xMWU2LTkyM2YtMDAxNjNlMDAyMGFk";
    private final String Client_Secret = "NjM5ZWNkOGItMWRmMi0zN2YyLWFiODgtYmM5ZWNiZTdiN2E1";
    public static List<Radio> mRadioList = new ArrayList<Radio>();
    List<Program> mProgramList = new ArrayList<Program>();
    ImageView lastBtn, nextBtn, startBtn;
    public static TextView radioNameTv;
    public static int currentRadioIndex = 0;
    int day_of_week;
    static String currProvice, currDistrict, imei;
    int currProviceId;
    DistrictRadioDao districtRadioDao;
    public static boolean isRight = true;
    private static final int notification_id = 10000;

    private ProgressBar mProgressBar;
    private long mLastCmdTime;
    private static final int DEFAULT_ANIMATION_GAP = 300;
    private boolean bMenuShowed = false;

    ImageView mArrowUp;
    LinearLayout mArrowUpLayout;
    LinearLayout mVolumeControl;
    LinearLayout mLayoutAnimation;
    ImageView mVolumeDown;
    ImageView mVolumeUp;

    private int mMaxVolume = 0;
    private int mCurrentVolume = 0;
    private long mLastCmdTime2;
    private static final int DEFAULT_COMMAND_GAP = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
        Vitamio.initialize(getApplicationContext());
        mContext = RadioActivity.this;
        PlayService.setPlayStateListener(this);
        imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        districtRadioDao = new DistrictRadioDao(mContext);
        if (getIntent().getIntExtra("temp", 0) == -1)
            currentRadioIndex = SharedTool.getInstance().getIntSharedInfoByKey(mContext);// 防止静态丢失
        mVolumeDown = (ImageView) findViewById(R.id.volume_down);
        mVolumeUp = (ImageView) findViewById(R.id.volume_up);
        mArrowUp = (ImageView) findViewById(R.id.arrow_up);
        mArrowUpLayout = (LinearLayout) findViewById(R.id.arrow_up_layout);
        mVolumeControl = (LinearLayout) findViewById(R.id.volume_control);
        mLayoutAnimation = (LinearLayout) findViewById(R.id.layout_animation);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        int volume = getVolume();
        mMaxVolume = getMaxVolume();
        initProgressBar(volume);

        mVolumeUp.post(new Runnable() {
            @Override
            public void run() {
                mLayoutAnimation.setTranslationY(mVolumeControl.getHeight());
                updateImageView(mVolumeDown, R.drawable.icon_record_volume_decrease);
                updateImageView(mVolumeUp, R.drawable.icon_record_volume_increase);
            }
        });

        mArrowUpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_ANIMATION_GAP) {
                    return;
                }
                mLastCmdTime = System.currentTimeMillis();
                showMenu(bMenuShowed);
            }
        });
        mArrowUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (System.currentTimeMillis() - mLastCmdTime < DEFAULT_ANIMATION_GAP) {
                    return;
                }
                mLastCmdTime = System.currentTimeMillis();
                showMenu(bMenuShowed);
            }
        });

        mVolumeUp.setClickable(true);
        mVolumeDown.setClickable(true);
        mVolumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                volumeDown();
                Log.d("dingyichen", "volume = " + getVolume());
                initProgressBar(getVolume());
            }
        });

        mVolumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                volumeUp();
                Log.d("dingyichen", "volume = " + getVolume());
                initProgressBar(getVolume());
            }
        });
        initVidget();
        new HttpAsync(5).execute();
        // 测试
        if (SharedTool.getInstance().getAllProvinceTypeInfo(mContext).size() > 0) {
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showNotification();

        if (PlayService.mVideoView != null) {
            if (PlayService.mVideoView.isPlaying()) {
                Log.d("RadioActivity", "恢复播放");
                updateImageView(startBtn, R.drawable.btn_stop);
            } else {
                Log.d("RadioActivity", "恢复暂停");
                updateImageView(startBtn, R.drawable.btn_start);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
        Log.d("dingyichen", "onFinished!!!");
        rmNotification();
        if (PlayService.mVideoView != null) {
            PlayService.mVideoView.stopPlayback();
        }
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    private void showNotification() {
        // Andy TODO
        rmNotification();
        PendingIntent localPendingIntent = PendingIntent.getActivity(this,
                0, new Intent(this, RadioActivity.class), 0);
        Notification localNotification = new NotificationCompat.Builder(this).
                setSmallIcon(R.mipmap.ic_launcher).
                setContentTitle("收音机").
                setContentText("收音机").
                setContentIntent(localPendingIntent).
                setAutoCancel(true).build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).
                notify(notification_id, localNotification);
    }

    private void rmNotification() {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notification_id);
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(RadioActivity.this, resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    private void getAllRadio(List<Radio> districtRadioList) {
        // mRadioList = districtRadioDao.getDIstrictRadioList();
        mRadioList.removeAll(mRadioList);
        if (districtRadioList != null)
            mRadioList.addAll(districtRadioList);
        mRadioList.addAll(getProvinceRadio());
        mRadioList.addAll(getCNRadio());
        playNewRadio(mRadioList.get(currentRadioIndex));
    }

    private List<Radio> getCNRadio() {
        return getRadioListFromArray(R.array.cnr_radio_id, R.array.cnr_radio_name);
    }

    private List<Radio> getProvinceRadio() {
        return getRadioListFromArray(R.array.provice_radio_id, R.array.provice_radio_name);
    }

    private List<Radio> getRadioListFromArray(int idArray, int nameArray) {
        List<Radio> radioList = new ArrayList<Radio>();
        int[] id_arr = getResources().getIntArray(idArray);
        String[] name_arr = getResources().getStringArray(nameArray);
        for (int i = 0; i < id_arr.length; i++) {
            radioList.add(new Radio(id_arr[i], name_arr[i]));
        }
        return radioList;
    }

    private void initVidget() {
        startBtn = (ImageView) findViewById(R.id.btn_radio_start);
        lastBtn = (ImageView) findViewById(R.id.btn_radio_before);
        nextBtn = (ImageView) findViewById(R.id.btn_radio_next);
        radioNameTv = (TextView) findViewById(R.id.txt_radio_fm);
        ImageView badgeView = (ImageView) findViewById(R.id.radio_badge_view);

        updateImageView(startBtn, R.drawable.btn_start);
        updateImageView(lastBtn, R.drawable.fm_left_bg);
        updateImageView(nextBtn, R.drawable.fm_right_bg);
        updateImageView(badgeView, R.drawable.icon_fm_badge);
    }

    public void onClick(View v) {
        if (mRadioList.size() <= 0) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_radio_start:
                if (System.currentTimeMillis() - mLastCmdTime2 < DEFAULT_COMMAND_GAP) {
                    return;
                }
                mLastCmdTime2 = System.currentTimeMillis();
                if (PlayService.mVideoView != null) {
                    if (PlayService.mVideoView.isPlaying()) {
                        Log.d("RadioActivity", "正在播放,应该暂停");
                        PlayService.mVideoView.pause();
                        updateImageView(startBtn, R.drawable.btn_start);
                        // 修改下播放状态
                        SharedTool.getInstance().savePlayState(mContext, false);
                    } else {
                        Log.d("RadioActivity", "没有播放,应该开始");
                        PlayService.mVideoView.resume();
                        if (!PlayService.mVideoView.isPlaying()) {
                            PlayService.mVideoView.start();
                        }
                        updateImageView(startBtn, R.drawable.btn_stop);
                        // 修改下播放状态
                        SharedTool.getInstance().savePlayState(mContext, true);
                    }
                }

                break;
            case R.id.btn_radio_before:
                if (updateNoNetworkView()) {
                    return;
                }
                if (--currentRadioIndex == -1)
                    currentRadioIndex = mRadioList.size() - 1;
                isRight = false;
                playNewRadio(mRadioList.get(currentRadioIndex));
                break;
            case R.id.btn_radio_next:
                if (updateNoNetworkView()) {
                    return;
                }
                if (++currentRadioIndex == mRadioList.size())
                    currentRadioIndex = 0;
                isRight = true;
                playNewRadio(mRadioList.get(currentRadioIndex));
                break;
        }
        Log.d("dingyichen", "mCurrentRadioIndex = " + currentRadioIndex);
        //
    }

    private boolean updateNoNetworkView() {
        if (!Tools.isNetworkAvailable()) {
            mRadioList.removeAll(mRadioList);
            radioNameTv.setText(R.string.network_anomaly);
            updateImageView(startBtn, R.drawable.btn_start);
            return true;
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (startBtn != null) {
            updateImageView(startBtn, R.drawable.btn_start);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (startBtn != null) {
            updateImageView(startBtn, R.drawable.btn_stop);
        }
    }

    public class HttpAsync extends AsyncTask<Integer, Integer, String> {
        int index, channel_id;
        String token;
        int runIndex;

        public HttpAsync(int index) {
            this.index = index;
        }

        public HttpAsync(int index, int runIndex) {
            this.index = index;
            this.runIndex = runIndex;
        }

        public HttpAsync(int index, String token) {
            this.index = index;
            this.token = token;
        }

        public HttpAsync(int index, String token, int channel_id) {
            this.index = index;
            this.token = token;
            this.channel_id = channel_id;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Integer... params) {
            String url;
            Map<String, Object> paramsMap = new HashMap<String, Object>();
            String result = null;
            switch (index) {
                case 1:
                    url = "http://api.open.qingting.fm/access";
                    paramsMap.put("grant_type", "client_credentials");
                    paramsMap.put("client_id", Client_ID);
                    paramsMap.put("client_secret", Client_Secret);
                    result = NetUtils.post(url, paramsMap, "--------------------------获取token--------------------------------------");
                    break;
                case 2:
                    Log.e("resulttt", currProviceId + "--------------------" + currPage + "-------------------------");
                    // 1209:北京的目录id
                    // url = "http://api.open.qingting.fm/v6/media/mediacenterlist";
                    url = "http://api.open.qingting.fm/v6/media/categories/5/channels/order/0/attr/" + currProviceId + "/curpage/" + currPage + "/pagesize/60";
                    paramsMap.put("access_token", token.replace("\"", ""));
                    result = NetUtils.post(url, paramsMap, "--------------------------查询某个分类(省)下电台----------------------------");
                    break;
                case 3:
                    // 获取所有分类
                    url = "http://api.open.qingting.fm/v6/media/categories/" + 5;
                    paramsMap.put("access_token", token.replace("\"", ""));
                    result = NetUtils.post(url, paramsMap, "--------------------------获取所有分类----------------------");
                    break;
                case 4:
                    // 获取某个电台下的节目==显示当前具体节目时可以用于查询
                    Calendar calendar = Calendar.getInstance();
                    day_of_week = calendar.get(Calendar.DAY_OF_WEEK);
                    Log.e("resulttt", "星期几-----" + day_of_week);
                    url = "http://api.open.qingting.fm/v6/media/channellives/" + channel_id + "/programs/day/" + day_of_week;
                    paramsMap.put("access_token", token.replace("\"", ""));
                    result = NetUtils.post(url, paramsMap, "---------------------------获取某个电台下的节目--------------");
                    break;
                case 5:
                    // 获取用户所在省市
                    // paramsMap.put("imei", imei);
                    paramsMap.put("imei", "860023810000118");
                    result = NetUtils.post(belong_url, paramsMap, "------------------获取用户所在省市-------------------");
                    break;
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                if (index == 4) {
                    new HttpAsync(1).execute();
                    return;
                }
                if (index == 2 && Tools.isNetworkAvailable()) {
                    getBelongRadioList(result);
                    return;
                }
                updateImageView(startBtn, R.drawable.btn_start);
                radioNameTv.setText(R.string.network_anomaly);
                return;
            }

            switch (index) {
                case 5:
                    // {"result":"str","data":"没有该会员"}_
                    JSONObject resultObj = JSON.parseObject(result);
                    if (resultObj == null) {
                        return;
                    }
                    if ("str".equals(resultObj.getString("result"))) {
                        radioNameTv.setText(resultObj.getString("data"));
                        return;
                    }
                    if (resultObj.getBoolean("isOk") == false) {
                        radioNameTv.setText(resultObj.getString("info"));
                        return;
                    }
                    JSONObject belong_info = JSON.parseObject(resultObj.getString("data"));
                    currProvice = belong_info.getString("sheng");
                    currDistrict = belong_info.getString("shi");
                    // currDistrict = "济南";// 测试
                    // currProvice = "阿拉伯";// 测试
                    String belong_shared = SharedTool.getInstance().getSharedDateOrBelongInfo(mContext, SHARED_BELONG_KEY);
                    SharedTool.getInstance().editSharedDateOrBelongInfo(mContext, SHARED_BELONG_KEY, currProvice + "," + currDistrict);

                    Log.e("resulttt", "所属地=" + belong_shared);
                    if (belong_shared.equals(""))// 未缓存过地区，相当于第一次登录
                    {
                        new HttpAsync(1, 3).execute();
                        return;
                    }
                    String[] belong_shareds = belong_shared.split(",");
                    currProviceId = SharedTool.getInstance().getProvinceIdFromShared(mContext, currProvice);
                    switch (currProviceId) {
                        case -1:// 地区分类未缓存成功
                            new HttpAsync(1, 3).execute();
                            return;
                        case -2:
                            districtRadioDao.delete();// 没省区台，删除之前的区台
                            getAllRadio(null);// 只列出省代表台和中央台
                            return;
                    }
                    // 地区分类缓存成功，currProviceId>0
                    // 1.省不同
                    // 2.省相同
                    if (belong_shareds[1].equals(currDistrict))// 2-1地区同
                    {
                        List<Radio> districtRadioList = districtRadioDao.getDIstrictRadioList();
                        // 2-1-1数据库里有所属地区的节目
                        if (districtRadioList.size() > 0 && (districtRadioList.get(0).getTitle().contains(currDistrict) || districtRadioList.get(0).getTitle().contains(currProvice))) {
                            getAllRadio(districtRadioList);// 用缓存的
                            return;
                        }
                    }
                    // 1.省不同&&2-1-2省区同，数据里没有地区节目&&2-2区不同
                    districtRadioDao.delete();// 删除之前的地区的电台
                    new HttpAsync(1, 2).execute();// 获取电台
                    break;
                case 1:
                    String access_token = null, token_type = null;
                    int expires_in;
                    try {
                        JsonParser jsonParser = new JsonParser();
                        JsonElement element = jsonParser.parse(result);
                        JsonObject jsonObj = element.getAsJsonObject();

                        if (jsonObj.get("error") != null) {
                            Toast.makeText(RadioActivity.this, jsonObj.get("error").toString(), Toast.LENGTH_SHORT).show();
                            Log.e("resulttt", "错误" + jsonObj.get("error").toString());
                        } else {
                            access_token = jsonObj.get("access_token").toString();
                            token_type = jsonObj.get("token_type").toString();
                            expires_in = Integer.parseInt(jsonObj.get("expires_in").toString());
                            new HttpAsync(runIndex, access_token).execute();
                            act_token = access_token;
                            // new HttpAsync(3, access_token).execute();//查询分类
                        }

                    } catch (Exception e) {

                    }
                    break;
                case 2:
                    Log.e("resulttt", "查询电台结果·······················" + result);
                    getBelongRadioList(result);
                    break;
                case 3:
                    Log.e("resulttt", "电台分类结果·······················" + result);
                    // 存入sharedpreference
                    try {
                        JsonParser jsonParser = new JsonParser();
                        JsonObject jsonObj = jsonParser.parse(result).getAsJsonObject();
                        if (jsonObj.get("errorno").getAsInt() != 0) {
                            Toast.makeText(RadioActivity.this, jsonObj.get("errormsg").toString(), Toast.LENGTH_SHORT).show();
                            // Log.e("resulttt", "错误" + jsonObj.get("errormsg").toString());
                        } else {
                            JsonArray jsonArray = jsonParser.parse(jsonObj.get("data").toString()).getAsJsonArray();
                            List<Radio> mmRadioList = new ArrayList<Radio>();
                            JsonArray provinceTypeJsonArray = jsonArray.get(0).getAsJsonObject().get("values").getAsJsonArray();
                            for (int i = 0; i < provinceTypeJsonArray.size(); i++) {
                                JsonObject jsonObject = provinceTypeJsonArray.get(i).getAsJsonObject();
                                int provinceId = jsonObject.get("id").getAsInt();
                                String provinceName = jsonObject.get("name").getAsString();
                                Log.e("resulttt", provinceId + "-------" + provinceName);
                                SharedTool.getInstance().editIntSharedInfo(mContext, provinceName, provinceId);
                                if (provinceName.equals(currProvice)) {
                                    currProviceId = provinceId;
                                    new HttpAsync(1, 2).execute();
                                }
                            }
                        }

                    } catch (Exception e) {

                    }
                    break;
                case 4:
                    Log.e("resulttt", "节目结果·······················" + result);
                    try {
                        JsonParser jsonParser = new JsonParser();
                        JsonObject jsonObj = jsonParser.parse(result).getAsJsonObject();
                        if (!jsonObj.get("errormsg").getAsString().equals("")) {
                            Toast.makeText(RadioActivity.this, jsonObj.get("errormsg").toString(), Toast.LENGTH_SHORT).show();
                            // Log.e("resulttt", "错误" + jsonObj.get("errormsg").toString());
                        } else if (jsonObj.get("error") != null && "invalid_client".equals(jsonObj.get("error").getAsString())) {
                            new HttpAsync(1).execute();
                            return;
                        } else {
                            JsonObject data_jsonObj = jsonObj.get("data").getAsJsonObject();
                            JsonArray jsonArray = data_jsonObj.get("" + day_of_week).getAsJsonArray();
                            Log.e("resulttt", "节目总数=" + jsonArray.size());
                            mProgramList.removeAll(mProgramList);
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                                Program program = new Program();
                                int media_id = jsonObject.get("mediainfo").getAsJsonObject().get("id").getAsInt();
                                program.setMediainfo_id(media_id);
                                String start_time = jsonObject.get("start_time").getAsString();
                                String end_time = jsonObject.get("end_time").getAsString();
                                program.setStart_time(start_time);
                                program.setEnd_time(end_time);
                                mProgramList.add(program);
                                if (isCurrentProgram(start_time, end_time)) {
                                    // 找到当前节目
                                    mRadioList.get(currentRadioIndex);
                                }
                            }
                            //  Log.e("resulttt", "节目总数" + mProgramList.size());
                        }

                    } catch (Exception e) {

                    }
                    break;
            }
        }

    }

    private int districtRadioCount = 0;
    private int currPage = 1;

    private void getBelongRadioList(String result) {
        if (result == null) {
            Log.e("resulttt", "------------------------返回为空-----------------------------");
            currPage = 1;
            if (districtRadioCount == 0) {// 无地方台（查询到最后一页，仍没有相应的地方台，则再从第一页查相应的省台）
                currDistrict = currProvice;
                Log.e("resulttt", "*****************返回为空*****不明白*********************" + currDistrict);
                new HttpAsync(1, 2).execute();
                return;
            }
            // 最后一页
            districtRadioCount = 0;
            Log.e("resulttt", currPage + "频道总数" + districtRadioDao.getDIstrictRadioList().size());
            getAllRadio(districtRadioDao.getDIstrictRadioList());
            return;
        }
        try {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObj = jsonParser.parse(result).getAsJsonObject();
            if (jsonObj.get("errorno").getAsInt() != 0) {
                // Log.e("resulttt", "错误" + jsonObj.get("errormsg").toString());
                Toast.makeText(RadioActivity.this, jsonObj.get("errormsg").toString(), Toast.LENGTH_SHORT).show();
            } else {
                JsonArray jsonArray = jsonParser.parse(jsonObj.get("data").toString()).getAsJsonArray();
                if (jsonArray.size() == 0) {
                    // 最后一页
                    districtRadioCount = 0;
                    Log.e("resulttt", currPage + "频道总数" + districtRadioDao.getDIstrictRadioList().size());
                    getAllRadio(districtRadioDao.getDIstrictRadioList());
                    return;
                }
                List<Radio> mmRadioList = new ArrayList<Radio>();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                    int radioId = jsonObject.get("id").getAsInt();
                    String radioName = jsonObject.get("title").getAsString();
                    if (!radioName.contains(currDistrict))
                        continue;
                    Radio radio = new Radio(radioId, radioName);
                    // radio.setUpdate_time(jsonObject.get("update_time").getAsString());
                    mmRadioList.add(radio);
                    districtRadioDao.insert(radioName, radioId);
                    // Log.e("resulttt", "频道-----" + radioId + "==" + radioName);// 存数据库
                }
                districtRadioCount += mmRadioList.size();
                if (districtRadioCount < 10) {
                    // 查询下一页
                    currPage++;
                    new HttpAsync(1, 2).execute();
                    return;
                }
                currPage = 1;
                districtRadioCount = 0;
                Log.e("resulttt", "频道总数" + mmRadioList.size());
                getAllRadio(mmRadioList);
                // playNewRadio(mRadioList.get(currentRadioIndex));
                // new HttpAsync(4, token,
                // mRadioList.get(currentRadioIndex).getId()).execute();
            }

        } catch (Exception e) {

        }
    }

    /**
     * 播放radio
     *
     * @param radio
     */
    private void playNewRadio(Radio radio) {
        Log.d("dingyichen", "id = " + radio.getId() + " , title = " + radio.getTitle());
        lastBtn.setClickable(true);
        nextBtn.setClickable(true);
        radioNameTv.setText(radio.getTitle());
        String mediaUrl = "https://ls.qingting.fm/live/" + radio.getId() + ".m3u8?bitrate=0&deviceid=" + imei;
        Intent service = new Intent(RadioActivity.this, PlayService.class);
        service.putExtra(RADIO_PLAY_URL, mediaUrl);
        startService(service);

        SharedTool.getInstance().savePlayState(mContext, true);
        // 记录下播放状态
        PlayInfo playInfo = new PlayInfo(currentRadioIndex, 1, mediaUrl);
        SharedTool.getInstance().editSharedPlayInfo(mContext, playInfo.toString());// 播放时记录一下播放状态
    }

    /***
     * 根据时间过滤当前节目（主要用于取出当前节目名称，单纯播放不过滤也可）
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public boolean isCurrentProgram(String startTime, String endTime) {
        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd ");
        String nowDate = simpleDateFormat1.format(new java.util.Date());
        long now = System.currentTimeMillis();
        if (getMills(nowDate + startTime) <= now && now <= getMills(nowDate + endTime)) {
            return true;
        }
        return false;
    }

    public static long getMills(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return sdf.parse(dateStr).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.e("resulttt", "_________________返回______________________");
            SharedTool.getInstance().editIntSharedInfo(mContext, currentRadioIndex);
            String radioName = radioNameTv.getText().toString();
            if (getResources().getString(R.string.loading).equals(radioName) || getResources().getString(R.string.network_anomaly).equals(radioName)) {
                finish();
                return true;
            }
            // moveTaskToBack(true);
            Intent intent = new Intent();
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.HOME");
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void showMenu(boolean menuShowed) {
        if (menuShowed) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(mLayoutAnimation, "translationY", 0, mVolumeControl.getHeight()).setDuration(300);
            animator.setInterpolator(new AccelerateInterpolator());
            animator.start();
            ObjectAnimator.ofFloat(mArrowUp, "rotation", 180, 0).setDuration(200).start();
            ObjectAnimator.ofObject(mVolumeControl, "backgroundColor", new ArgbEvaluator(),
                    0xff000000, 0xff171717).setDuration(300).start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
//                    mLayoutShadows.setVisibility(View.GONE);
                    mVolumeControl.setBackgroundColor(0xff171717);
                    bMenuShowed = !bMenuShowed;
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        } else {
//            mLayoutShadows.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(mLayoutAnimation, "translationY", mVolumeControl.getHeight(), 0).setDuration(300);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
            ObjectAnimator.ofFloat(mArrowUp, "rotation", 0, 180).setDuration(200).start();
//            ObjectAnimator.ofFloat(mLayoutShadows, "alpha", 0, 1).setDuration(300).start();
            ObjectAnimator.ofObject(mVolumeControl, "backgroundColor", new ArgbEvaluator(),
                    0xff171717, 0xff000000).setDuration(300).start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
//                    mLayoutShadows.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    bMenuShowed = !bMenuShowed;
                    mVolumeControl.setBackgroundColor(0xff000000);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
    }

    private void initProgressBar(int volume) {
        if (volume <= mMaxVolume / 5) {
            mProgressBar.setProgress(20);
        } else if (volume <= mMaxVolume * 2 / 5) {
            mProgressBar.setProgress(40);
        } else if (volume <= mMaxVolume * 3 / 5) {
            mProgressBar.setProgress(60);
        } else if (volume <= mMaxVolume * 4 / 5) {
            mProgressBar.setProgress(80);
        } else {
            mProgressBar.setProgress(100);
        }
    }

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
}
