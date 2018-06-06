package com.mstarc.wechat.wearwechat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mstarc.wechat.wearwechat.fragment.ContactsFragment;
import com.mstarc.wechat.wearwechat.fragment.HomeFragment;
import com.mstarc.wechat.wearwechat.model.Contact;
import com.mstarc.wechat.wearwechat.model.Msg;
import com.mstarc.wechat.wearwechat.model.SyncKey;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.model.User;
import com.mstarc.wechat.wearwechat.net.CookieRequest;
import com.mstarc.wechat.wearwechat.net.VolleySingleton;
import com.mstarc.wechat.wearwechat.protocol.BatchContactRequest;
import com.mstarc.wechat.wearwechat.protocol.BatchContactResponse;
import com.mstarc.wechat.wearwechat.protocol.ContactResponse;
import com.mstarc.wechat.wearwechat.protocol.InitRequest;
import com.mstarc.wechat.wearwechat.protocol.InitResponse;
import com.mstarc.wechat.wearwechat.protocol.MsgSyncRequest;
import com.mstarc.wechat.wearwechat.protocol.MsgSyncResponse;
import com.mstarc.wechat.wearwechat.protocol.NotifyRequest;
import com.mstarc.wechat.wearwechat.protocol.NotifyResponse;
import com.mstarc.wechat.wearwechat.service.HomeService;
import com.mstarc.wechat.wearwechat.utils.Constants;
import com.mstarc.wechat.wearwechat.utils.FileUtil;
import com.mstarc.wechat.wearwechat.utils.NetUtil;
import com.mstarc.wechat.wearwechat.utils.StringUtil;
import com.mstarc.wechat.wearwechat.utils.TimeUtil;
import com.mstarc.wechat.wearwechat.utils.VoiceUtil;
import com.mstarc.wechat.wearwechat.utils.WxHome;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class CommunicationActivity extends FragmentActivity {

    ViewPager mViewPager;

    // Dot image view list
    private ImageView[] mDotImageViews;
    private LinearLayout mDotViewViewgroup;

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    private Token token;
    private RequestQueue mQueue;
    private User user;

    private ArrayList<Contact> contactList = new ArrayList();
    private ArrayList<Contact> exContactList = new ArrayList();
    private ArrayList<Contact> initList;
    private static SyncKey syncKey;
    private Set<String> chatSet = new HashSet();
    private boolean contactLoaded = false;
    private boolean exContactLoaded = false;
    private boolean initLoaded = false;
    private boolean mIsLoaded = false;
    private HomeConnection conn;
    private HomeService.HomeBinder myBinder;
    private MediaPlayer player;
    private AudioManager audioManager;
    private boolean isOut;
    private ContactsFragment contactFragment;
    private HomeFragment initFragment;
    private FragmentPagerAdapter mAdapter;
    private List<Fragment> mFragments = new ArrayList();
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mDotViewViewgroup = (LinearLayout) findViewById(R.id.view_page_indicator);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        Log.d("TAG", "CommunicationActivity_onCreate");
        mPrefs = getSharedPreferences("isLogin", Activity.MODE_PRIVATE);
        mEditor = mPrefs.edit();
        mEditor.putBoolean("islogin", true).commit();

        Intent localIntent = getIntent();
        Log.d("TAG", "CommunicationActivity_onCreate:Bundle=" + localIntent.getExtras());

        if (localIntent.getExtras() == null) {
            mEditor.putBoolean("islogin", false).commit();
            Intent intent = new Intent(CommunicationActivity.this, QrcodeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        this.token = new Token();
        this.token.fromBundle(localIntent.getExtras());
        Log.d("TAG", "CommunicationActivity_onCreate:token=" + JSON.toJSONString(this.token));
        this.mQueue = VolleySingleton.getInstance().getRequestQueue();



        initWx();
        initContact(0);
        initPlayer();
        initViewPager();
        //initDotViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("TAG", "onDestroy!!");
        mEditor.putBoolean("islogin", false).commit();
        if (conn != null) {
            unbindService(conn);
        }
        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("TAG", "HomeActivity_onResume");
        if (isOut) {
            showDialog("已退出");
            isOut = false;
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    // Init view pager
    private void initViewPager() {

//        mViewPager.setAdapter(new ChatsPagerAdapter(getSupportFragmentManager()));
        mViewPager.setCurrentItem(1);

    }

    // Init bottom dot views
    private void initDotViews() {
        Log.d("TAG", "CommunicationActivity_initDotViews, mFragments.size=" + mFragments.size());
        mDotImageViews = new ImageView[mFragments.size()];
        for (int i = 0; i < mFragments.size(); i++) {
            ImageView view = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dip2px(this, 10), dip2px(this, 10));
            params.setMargins(dip2px(this, 20), 0, dip2px(this, 20), 0);
            view.setLayoutParams(params);

            mDotImageViews[i] = view;
            if (i == 0) {
                mDotImageViews[i].setBackgroundResource(R.mipmap.icon_wechat_dot_white);
            } else {
                mDotImageViews[i].setBackgroundResource(R.mipmap.icon_wechat_dot_black);
            }
            mDotViewViewgroup.addView(mDotImageViews[i]);
        }
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(final Activity activity, final float dpValue) {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final float scale = metrics.density;
        return (int) ((dpValue * scale) + 0.5f);
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dp2px(Context context, float dp) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((dp * scale) + 0.5f);
    }

    private void initPlayer() {

        if (player != null) {
            player.reset();
            player.release();
            player = null;
        }
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
        player.setLooping(false);

        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        try {
            player.setDataSource(this, alert);
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    //1 初始化微信,拿到的相当于最近联系人;
    private void initWx() {
        String str = WxHome.getInitUrl(this.token);
        InitRequest localInitRequest = WxHome.formInitRequest(this.token);
        Log.d("TAG", "initWx:" + JSON.toJSONString(localInitRequest));
        CookieRequest localCookieRequest = new CookieRequest(1, str, JSON.toJSONString(localInitRequest), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                Log.d("TAG", "onResponse_initWx:" + response.toString());
                InitResponse localInitResponse = JSON.parseObject(response.toString(), InitResponse.class);

                CommunicationActivity.this.user = localInitResponse.User;

                CommunicationActivity.this.openNotify(localInitResponse.User);

                CommunicationActivity.this.initList = filterAccounts(localInitResponse.ContactList);
                Log.d("TAG", "localInitResponse_initList:" + CommunicationActivity.this.initList.size());
                CommunicationActivity.this.syncKey = localInitResponse.SyncKey;

                for (String str : localInitResponse.ChatSet.split(",")) {
                    CommunicationActivity.this.chatSet.add(str);
                }
                CommunicationActivity.this.initBatchContact(CommunicationActivity.this.chatSet);
                CommunicationActivity.this.bindHomeService(CommunicationActivity.this.token, WxHome.randomDeviceId());
                CommunicationActivity.this.initLoaded = true;
                CommunicationActivity.this.onInitComplete();
            }

        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError paramAnonymousVolleyError) {
                Log.e("TAG", "initWx:error " + paramAnonymousVolleyError.getMessage(), paramAnonymousVolleyError);
            }
        });
        localCookieRequest.setCookie(this.token.cookie);
        localCookieRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 2.0F));
        this.mQueue.add(localCookieRequest);
    }
    private void openNotify(User user) {
        String str = WxHome.getNotifyUrl(this.token);
        NotifyRequest localNotifyRequest = WxHome.formNotifyRequest(this.token, user);
        CookieRequest localCookieRequest = new CookieRequest(1, str, JSON.toJSONString(localNotifyRequest), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                Log.d("TAG", "onResponse_openNotify:" + response.toString());

                NotifyResponse localNotifyResponse = JSON.parseObject(response.toString(), NotifyResponse.class);

                Log.d("TAG", "openNotify返回值:" + localNotifyResponse.BaseResponse.Ret);
                if (localNotifyResponse.BaseResponse.Ret == 0) {
                    Log.d("TAG", "状态通知开启成功");
                } else {
                    Log.d("TAG", "状态通知开启失败");
                }

            }

        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError paramAnonymousVolleyError) {
                Log.e("TAG", "openNotify:error " + paramAnonymousVolleyError.getMessage(), paramAnonymousVolleyError);
            }
        });
        localCookieRequest.setCookie(this.token.cookie);
        localCookieRequest.setRetryPolicy(new DefaultRetryPolicy(7000, 1, 2.0F));
        this.mQueue.add(localCookieRequest);
    }

    //过滤公众号
    public ArrayList<Contact> filterAccounts(ArrayList<Contact> list) {

        ArrayList<Contact> newList = new ArrayList<>();
        if (null != list && list.size() != 0) {
            Contact contact;
            for (int i = 0; i < list.size(); i++) {
                contact = list.get(i);
                //一般公众号/服务号：8  微信自家的服务号：24   微信官方账号微信团队：56
                if (contact.VerifyFlag == 8 || contact.VerifyFlag == 24 || contact.VerifyFlag == 56) {
                    continue;
                }
                // 特殊联系人
                if (Constants.FILTER_USERS.contains(contact.UserName)) {
                    continue;
                }
                //先不过滤自己
//                if (contact.UserName.equals(this.user.UserName)) {
//                    continue;
//                }
                newList.add(contact);
            }
        }

        return newList;
    }

    //2 批量初始化,群?
    private void initBatchContact(Set<String> paramSet) {
        String str = WxHome.getBatchContactUrl(this.token);
        BatchContactRequest localBatchContactRequest = WxHome.formBatchContactRequest(this.token, paramSet);

        Log.d("TAG", "localBatchContactRequest.List==" + localBatchContactRequest.List.isEmpty());
        if ((localBatchContactRequest == null) || (localBatchContactRequest.List.isEmpty()))
            return;
        Log.d("TAG", "initBatchContact:" + JSON.toJSONString(localBatchContactRequest));
        CookieRequest localCookieRequest = new CookieRequest(1, str, JSON.toJSONString(localBatchContactRequest), new Response.Listener() {
            @Override
            public void onResponse(Object response) {

                Log.d("TAG", "onResponse_initBatchContact:" + response.toString());
                BatchContactResponse localBatchContactResponse = JSON.parseObject(response.toString(), BatchContactResponse.class);
                Log.d("TAG", "initBatchContact_ContactList:" + localBatchContactResponse.ContactList.size());
                CommunicationActivity.this.exContactList.addAll(localBatchContactResponse.ContactList);
                CommunicationActivity.this.exContactLoaded = true;
            }

        }
                , new Response.ErrorListener() {
            public void onErrorResponse(VolleyError paramAnonymousVolleyError) {
                Log.e("TAG", "initBatchContact:error " + paramAnonymousVolleyError.getMessage(), paramAnonymousVolleyError);
            }
        });
        localCookieRequest.setCookie(this.token.cookie);
        localCookieRequest.setRetryPolicy(new DefaultRetryPolicy(8000, 1, 2.0F));
        this.mQueue.add(localCookieRequest);
    }


    //3   初始化联系人
    private void initContact(int paramInt) {
        CookieRequest localCookieRequest = new CookieRequest(1, WxHome.getContactUrl(this.token, paramInt), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                Log.d("TAG", "initContact:" + response.toString());
                ContactResponse localContactResponse = JSON.parseObject(response.toString(), ContactResponse.class);
                Log.d("TAG", "initContact_contactList:" + localContactResponse.MemberCount);
                CommunicationActivity.this.contactList.addAll(filterAccounts(localContactResponse.MemberList));
                if (localContactResponse.Seq == 0) {
                    Collections.sort(CommunicationActivity.this.contactList);
                    CommunicationActivity.this.contactLoaded = true;
                    CommunicationActivity.this.onInitComplete();
                    return;
                }
                CommunicationActivity.this.initContact(localContactResponse.Seq);

            }

        }
                , new Response.ErrorListener() {
            public void onErrorResponse(VolleyError paramAnonymousVolleyError) {
                if ((paramAnonymousVolleyError instanceof NoConnectionError)) {
                    CommunicationActivity.this.contactLoaded = true;
                    CommunicationActivity.this.onInitComplete();
                    Log.w("TAG", "initContact:error " + paramAnonymousVolleyError.getMessage());
                    return;
                }
                Log.e("TAG", "initContact:error " + paramAnonymousVolleyError.getMessage(), paramAnonymousVolleyError);
            }
        });
        localCookieRequest.setCookie(this.token.cookie);
        localCookieRequest.setRetryPolicy(new DefaultRetryPolicy(15000, 1, 2.0F));
        this.mQueue.add(localCookieRequest);
    }

    private void onInitComplete() {
        Log.d("TAG", "onInitComplete_this.initLoaded:" + initLoaded + "====this.contactLoaded:" + contactLoaded);
        if ((!initLoaded) || (!contactLoaded)) {
            return;
        }

        //请求联系人失败的话,将最近联系人赋给联系人;
        if ((contactList.isEmpty()) && (initList != null) && (!initList.isEmpty())) {
            contactList.addAll(initList);
            Collections.sort(contactList);
        }
        mIsLoaded = true;
        mProgressBar.setVisibility(View.GONE);
        initFragment = HomeFragment.newInstance(token, user, initList, contactList, exContactList);
        mFragments.add(initFragment);
        contactFragment = ContactsFragment.newInstance(token, user, contactList);
        mFragments.add(contactFragment);
        mAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            public int getCount() {
                return mFragments.size();
            }

            public Fragment getItem(int paramAnonymousInt) {
                return mFragments.get(paramAnonymousInt);
            }

        };
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < mFragments.size(); i++) {
                    if (i == position) {
                        mDotImageViews[i].setBackgroundResource(R.mipmap.icon_wechat_dot_white);
                    } else {
                        mDotImageViews[i].setBackgroundResource(R.mipmap.icon_wechat_dot_black);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        initDotViews();
    }

    private void bindHomeService(Token paramToken, String paramString) {
        Intent localIntent = new Intent(this, HomeService.class);
        localIntent.putExtra("token", paramToken.toBundle());
        localIntent.putExtra("deviceId", paramString);
        localIntent.putExtra("syncKey", this.syncKey.toString());
        this.conn = new HomeConnection();
        bindService(localIntent, this.conn, Context.BIND_AUTO_CREATE);
    }

    private class HomeConnection implements ServiceConnection {
        private HomeConnection() {
        }

        public void onServiceConnected(ComponentName paramComponentName, IBinder paramIBinder) {


            CommunicationActivity.this.myBinder = (HomeService.HomeBinder) paramIBinder;
            CommunicationActivity.this.myBinder.getService().setCallBack(new HomeService.CallBack() {
                public void handleServiceData(Properties paramAnonymousProperties) {

                    if ((paramAnonymousProperties == null) || (paramAnonymousProperties.isEmpty())) {
                        return;
                    }

                    String str1 = paramAnonymousProperties.getProperty(WxHome.SYNC_CHECK_KEY);
                    Log.d("TAG", "HomeConnection_onServiceConnected:window.synccheck=" + str1);
                    JSONObject localJSONObject = JSON.parseObject(str1);
                    String retcode = localJSONObject.getString(WxHome.RETCODE);

                    Log.d("TAG", "操作前retcode" + retcode);

                    Log.e("dingyichen", "onServiceConnected !!!");
                    if ("0".equals(retcode)) {
                        Log.d("TAG", "onServiceConnected正常retcode");

                        String selector = localJSONObject.getString(WxHome.SELECTOR);
                        Log.d("TAG", "操作前selector" + selector);

                        if (!"0".equals(selector)) {
                            Log.e("dingyichen", "onServiceConnected syncMsg!!!");
                            CommunicationActivity.this.syncMsg();
                        }

                    } else if ("1100".equals(retcode)) {
                        Log.d("TAG", "onServiceConnected从微信客户端上登出" + retcode);
                        CommunicationActivity.this.myBinder.stopTimer();
                        CommunicationActivity.this.clearFiles();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (!NetUtil.isApplicationBroughtToBackground(CommunicationActivity.this)) {
                                    showDialog("已退出");
                                } else {
                                    isOut = true;
                                }

                            }
                        });

                    } else if ("1101".equals(retcode)) {

                        Log.d("TAG", "onServiceConnected已从其他设备登录" + retcode);
                        CommunicationActivity.this.myBinder.stopTimer();
                        CommunicationActivity.this.clearFiles();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!NetUtil.isApplicationBroughtToBackground(CommunicationActivity.this)) {
                                    showDialog("已退出");
                                } else {
                                    isOut = true;
                                }

                            }
                        });
                    }


//                    if ("0".equals(selector)) {
//                        Log.d("TAG", "onServiceConnected正常selector");
//
//                    } else if ("2".equals(selector)) {
//                        Log.d("TAG", "onServiceConnected新的消息selector");
//                        CommunicationActivity.this.syncMsg();
//
//                    } else if ("7".equals(selector)) {
//                        Log.d("TAG", "onServiceConnected进入/离开聊天界面selector");
//                    }

                }
            });
            CommunicationActivity.this.myBinder.startTimer();
        }
        
        public void onServiceDisconnected(ComponentName paramComponentName) {
            Log.d("TAG", "HomeConnection_CallBack:onServiceDisconnected" + paramComponentName);
        }
    }

    private void syncMsg() {
        Log.d("TAG", "syncMsg_this.mIsLoaded:" + this.mIsLoaded + "====this.exContactLoaded:" + this.exContactLoaded);
//        if ((!this.mIsLoaded) || (!this.exContactLoaded)) {
//            return;
//        }
        Log.e("dingyichen", "syncMsg !!!");
        String str = WxHome.getMsgSyncUrl(this.token);
        MsgSyncRequest localMsgSyncRequest = WxHome.formMsgSyncRequest(this.token, this.syncKey);
        Log.d("TAG", "syncMsg_localMsgSyncRequest:" + JSON.toJSONString(localMsgSyncRequest));
        CookieRequest localCookieRequest = new CookieRequest(1, str, JSON.toJSONString(localMsgSyncRequest), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                Log.d("TAG", "syncMsg_onResponse:" + response.toString());
                MsgSyncResponse localMsgSyncResponse = JSON.parseObject(response.toString(), MsgSyncResponse.class);

                if (localMsgSyncResponse.BaseResponse.Ret != 0) {
                    Log.d("TAG", "syncMsg_onResponse:BaseResponse.Ret==" + localMsgSyncResponse.BaseResponse.Ret);
                    return;
                }

                if (localMsgSyncResponse.SyncKey.toString().equals(CommunicationActivity.this.syncKey.toString())) {
                    Log.d("TAG", "syncMsg_onResponse:SyncKey相同");
                    return;
                }


                Log.d("TAG", "syncMsg:receive " + localMsgSyncResponse.AddMsgList.size() + "  messages");
                Iterator localIterator = localMsgSyncResponse.AddMsgList.iterator();
                while (localIterator.hasNext()) {
                    Msg localMsg = (Msg) localIterator.next();
                    if ((localMsg.MsgType == 1) || (localMsg.MsgType == 34)) {
                        Log.d("TAG", "syncMsg:text=" + localMsg.Content + " msgId= " + localMsg.MsgId + " ToUserName:" + localMsg.ToUserName);
                        Log.e("dingyichen", "syncMsg localMsg!!!");
                        if ((WxHome.isGroupUserName(localMsg.FromUserName)) && (!CommunicationActivity.this.chatSet.contains(localMsg.FromUserName))) {
                            Log.e("dingyichen", "syncMsg localMsg111!!!");
                            CommunicationActivity.this.addNewGroupThenProcessMsg(localMsg.FromUserName, localMsg);
                        } else if ((WxHome.isGroupUserName(localMsg.ToUserName)) && (!CommunicationActivity.this.chatSet.contains(localMsg.ToUserName))) {
                            Log.e("dingyichen", "syncMsg localMsg2222!!!");
                            CommunicationActivity.this.addNewGroupThenProcessMsg(localMsg.ToUserName, localMsg);
                        } else {
                            Log.e("dingyichen", "syncMsg processMsg");
                            CommunicationActivity.this.processMsg(localMsg);
                        }
                    }
                }

                CommunicationActivity.this.syncKey = localMsgSyncResponse.SyncKey;
                CommunicationActivity.this.myBinder.updateSyncKey(CommunicationActivity.this.syncKey.toString());
            }

        }
                , new Response.ErrorListener() {
            public void onErrorResponse(VolleyError paramAnonymousVolleyError) {
                Log.e("TAG", "syncMsg:error" + paramAnonymousVolleyError.getMessage(), paramAnonymousVolleyError);
            }
        });
        localCookieRequest.setCookie(this.token.cookie);
        localCookieRequest.setRetryPolicy(new DefaultRetryPolicy(7000, 1, 2.0F));
        this.mQueue.add(localCookieRequest);
    }

    private void addNewGroupThenProcessMsg(String paramString, final Msg paramMsg) {
        Log.e("dingyichen", "addNewGroupThenProcessMsg");
        final HashSet localHashSet = new HashSet();
        localHashSet.add(paramString);
        String str = WxHome.getBatchContactUrl(this.token);
        BatchContactRequest localBatchContactRequest = WxHome.formBatchContactRequest(this.token, localHashSet);
        if ((localBatchContactRequest == null) || (localBatchContactRequest.List.isEmpty()))
            return;
        Log.d("HomeActivity", "addNewGroupThenProcessMsg:" + JSON.toJSONString(localBatchContactRequest));
        CookieRequest localCookieRequest = new CookieRequest(1, str, JSON.toJSONString(localBatchContactRequest), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                Log.d("HomeActivity", "onResponse_addNewGroupThenProcessMsg:" + response.toString());
                CommunicationActivity.this.chatSet.addAll(localHashSet);
                BatchContactResponse localBatchContactResponse = JSON.parseObject(response.toString(), BatchContactResponse.class);
                CommunicationActivity.this.exContactList.addAll(localBatchContactResponse.ContactList);
                Log.e("dingyichen", "addNewGroupThenProcessMsg: processMsg");
                CommunicationActivity.this.processMsg(paramMsg);
            }
        }
                , new Response.ErrorListener() {
            public void onErrorResponse(VolleyError paramAnonymousVolleyError) {
                Log.e("TAG", "addNewGroupThenProcessMsg:error " + paramAnonymousVolleyError.getMessage(), paramAnonymousVolleyError);
            }
        });
        localCookieRequest.setCookie(this.token.cookie);
        localCookieRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 1, 2.0F));
        this.mQueue.add(localCookieRequest);
    }


    private void processMsg(Msg paramMsg) {
        Log.d("TAG", "processMsg:MsgType=" + paramMsg.MsgType + "   FromUserName=" + paramMsg.FromUserName);
        if (Constants.FILTER_USERS.contains(paramMsg.FromUserName)) {
            Log.d("TAG", "新消息被过滤掉");
            return;
        }
        if (WxHome.isGroupUserName(paramMsg.FromUserName)) {
            String[] arrayOfString2 = paramMsg.Content.split(":<br/>");
            if ((arrayOfString2 != null) && (arrayOfString2.length == 2)) {
                paramMsg.fromMemberUserName = arrayOfString2[0].trim();
                paramMsg.fromMemberNickName = getShowName(paramMsg.FromUserName, paramMsg.fromMemberUserName);
                paramMsg.Content = arrayOfString2[1].trim();
            }
        } else {
            paramMsg.Content = paramMsg.Content.replaceAll("<br/>", "\n");
        }
        if (paramMsg.FromUserName.equals(this.user.UserName)) {
            paramMsg.fromNickName = this.user.NickName;
        } else {
            paramMsg.fromNickName = getShowName(paramMsg.FromUserName);
        }
        if (paramMsg.ToUserName.equals(this.user.UserName)) {
            paramMsg.toNickName = this.user.NickName;
        } else {
            paramMsg.toNickName = getShowName(paramMsg.ToUserName);
        }
        Log.d("TAG", "processMsg:fromNickname=" + paramMsg.fromNickName + " toNickName=" + paramMsg.toNickName);
        paramMsg.CreateTime = TimeUtil.toTimeMillis(paramMsg.CreateTime);
        paramMsg.ClientMsgId = WxHome.randomClientMsgId();

        //todo 保存语音消息
        if (paramMsg.MsgType == 34) {
            paramMsg.Content = (paramMsg.MsgId + ".mp3");
            VoiceTask localVoiceTask = new VoiceTask();
            String[] arrayOfString1 = new String[1];
            arrayOfString1[0] = paramMsg.MsgId;
            localVoiceTask.execute(arrayOfString1);
        }


        Log.d("dingyichen", "processMsg come NewMessage!!");
        if (this.initFragment != null) {
            this.initFragment.comeNewMessage(paramMsg);
        }
        start();

        Log.d("TAG", "processMsg: FromUserName=" + paramMsg.FromUserName + "   ToUserName:" + paramMsg.ToUserName);

        broadcastMsg(paramMsg);


        if ((!paramMsg.FromUserName.equals(this.user.UserName))) {

            sendNotification(paramMsg);
            //    openChatWindow(paramMsg);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("TAG", "onBackPressed!!");
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);
        overridePendingTransition(0, 0);
    }

    private void sendNotification(Msg paramMsg) {
        if (paramMsg.MsgType == 34) {
            paramMsg.Content = "[ 语音 ]";
        }

        Log.d("HomeActivity", "sendNotification:content=" + paramMsg.Content);
        // Andy TODO
        Intent localIntent = new Intent(this, MessageHandleActivity.class);
        localIntent.putExtra("token", this.token.toBundle());
        User localUser = new User();
        localUser.UserName = paramMsg.FromUserName;
        localUser.NickName = paramMsg.fromNickName;
        localUser.HeadImgUrl = "";
        localIntent.putExtra("to", localUser.toBundle());
        localIntent.putExtra("from", this.user.toBundle());
        //134217728   FLAG_UPDATE_CURRENT  FLAG_ONE_SHOT
        PendingIntent localPendingIntent = PendingIntent.getActivity(this, paramMsg.FromUserName.hashCode(), localIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification localNotification = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_wechat_gold).setContentTitle(paramMsg.fromNickName).setContentText(paramMsg.Content).setContentIntent(localPendingIntent).setAutoCancel(true).build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(paramMsg.FromUserName.hashCode(), localNotification);
    }

    public String getShowName(String paramString) {
        if (this.contactList == null) {
            Log.d("TAG", "getShowName:contact list not initial");
            return "";
        }
        Log.d("TAG", "getShowName一个参数");
        Iterator localIterator1 = this.contactList.iterator();
        while (localIterator1.hasNext()) {
            Contact localContact2 = (Contact) localIterator1.next();
            if (localContact2.UserName.equals(paramString))
                return localContact2.getShowName();
        }
        Iterator localIterator2 = this.exContactList.iterator();
        while (localIterator2.hasNext()) {
            Contact localContact1 = (Contact) localIterator2.next();
            if (localContact1.UserName.equals(paramString))
                return localContact1.getShowName();
        }
        return "";
    }

    public String getShowName(String paramString1, String paramString2) {
        if (this.exContactList == null) {
            Log.d("TAG", "getShowName:extra Contact list not initial");
            return "";
        }
        if (StringUtil.isNullOrEmpty(paramString1)) {
            Log.d("TAG", "getShowName:groupUserName can not be empty");
            return "";
        }
        Log.d("TAG", "getShowName两个参数");
        Contact localContact1;
        Contact localContact2;
        Iterator localIterator1 = this.exContactList.iterator();
        while (localIterator1.hasNext()) {
            localContact1 = (Contact) localIterator1.next();
            Iterator localIterator2 = localContact1.MemberList.iterator();
            while (localIterator2.hasNext()) {
                localContact2 = (Contact) localIterator2.next();
                if (localContact2.UserName.equals(paramString2)) {
                    return localContact2.getShowName();
                }
            }
        }

        return "";
    }

    private class VoiceTask extends AsyncTask<String, Void, String> {
        private VoiceTask() {
        }

        protected String doInBackground(String[] paramArrayOfString) {
            DefaultHttpClient localDefaultHttpClient = new DefaultHttpClient();
            HttpGet localHttpGet = new HttpGet(WxHome.getVoiceUrl(CommunicationActivity.this.token, paramArrayOfString[0]));
            String str1 = Environment.getExternalStorageDirectory() + "/weixinQingliao/";
            FileUtil.createDir(str1);
            String str2 = str1 + paramArrayOfString[0] + ".mp3";
            Log.d("TAG", "HomeActivity_VoiceTask::audio output path=" + str2);
            try {
                localHttpGet.setHeader("cookie", CommunicationActivity.this.token.cookie);
                InputStream localInputStream = localDefaultHttpClient.execute(localHttpGet).getEntity().getContent();
                FileOutputStream localFileOutputStream = new FileOutputStream(str2);
                VoiceUtil.fromInStreamToOutStream(localInputStream, localFileOutputStream);
                localFileOutputStream.close();
                return str2;
            } catch (IOException localIOException) {
                Log.w("TAG", "HomeActiHomeActivityvity_VoiceTask::exception", localIOException);
            }
            return "";
        }

        protected void onPostExecute(String paramString) {
            Log.d("TAG", "HomeActivity_VoiceTask::onPostExecute:filePath=" + paramString);
        }
    }

    private void clearFiles() {
        File localFile = new File(Environment.getExternalStorageDirectory() + "/weixinQingliao/");
        if (localFile.isDirectory()) {
            String[] arrayOfString = localFile.list();
            for (int i = 0; i < arrayOfString.length; i++)
                new File(localFile, arrayOfString[i]).delete();
        }
    }

    private void broadcastMsg(Msg paramMsg) {
        // Andy TODO
        Intent localIntent = new Intent("com.g6.action.wx.new_msg");
        localIntent.putExtras(paramMsg.toBundle());
        sendBroadcast(localIntent);
    }

    void start() {
        if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
            player.start();
        }
    }

    void stop() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    protected void showDialog(String str) {

        mEditor.putBoolean("islogin", false).commit();

        AlertDialog.Builder builder = new AlertDialog.Builder(CommunicationActivity.this);

        builder.setTitle("提示");
        builder.setMessage(str + ",是否重新登录?");

        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(CommunicationActivity.this, QrcodeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        //builder.create().show();
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
