package com.mstarc.wechat.wearwechat.utils;

import android.net.Uri;
import android.util.Log;

import com.mstarc.wechat.wearwechat.model.Contact;
import com.mstarc.wechat.wearwechat.model.Msg;
import com.mstarc.wechat.wearwechat.model.SyncKey;
import com.mstarc.wechat.wearwechat.model.Token;
import com.mstarc.wechat.wearwechat.model.User;
import com.mstarc.wechat.wearwechat.protocol.BaseRequest;
import com.mstarc.wechat.wearwechat.protocol.BatchContactRequest;
import com.mstarc.wechat.wearwechat.protocol.InitRequest;
import com.mstarc.wechat.wearwechat.protocol.MsgRequest;
import com.mstarc.wechat.wearwechat.protocol.MsgSyncRequest;
import com.mstarc.wechat.wearwechat.protocol.NotifyRequest;

import org.jsoup.Jsoup;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class WxHome {

    private static final String DIGITS = "0123456789";
    private static final String INIT_URI = "/cgi-bin/mmwebwx-bin/webwxinit?r=%d&&lang=zh_CN&pass_ticket=%s";
    public static final String RETCODE = "retcode";
    public static final String SELECTOR = "selector";
    public static final String SYNC_CHECK_KEY = "window.synccheck";
    private static final String WX_CONTACT_EX = "/cgi-bin/mmwebwx-bin/webwxbatchgetcontact?type=ex&lang=zh_CN&pass_ticket=%s";
    private static final String WX_CONTACT_URI = "/cgi-bin/mmwebwx-bin/webwxgetcontact?seq=%d&pass_ticket=%s";
    private static final String WX_GET_HEAD = "/cgi-bin/mmwebwx-bin/webwxgetheadimg?username=%s&skey=%s";
    private static final String WX_GET_ICON = "/cgi-bin/mmwebwx-bin/webwxgeticon?username=%s&skey=%s";
    private static final String WX_GET_VOICE = "/cgi-bin/mmwebwx-bin/webwxgetvoice?skey=%s&msgid=%s";
    //同步 获取最新信息(向服务器端提供的一次验证)
    private static final String WX_NOTIFY_URI = "/cgi-bin/mmwebwx-bin/webwxstatusnotify?lang=zh_CN&pass_ticket=%s";
    private static final String WX_MSG_SYNC = "/cgi-bin/mmwebwx-bin/webwxsync?sid=%s&skey=%s&lang=zh_CN";
    private static final String WX_SEND_URI = "/cgi-bin/mmwebwx-bin/webwxsendmsg?pass_ticket=%s";
    //浏览器端与服务器端的定时心跳,该请求有二个作用，一个是用于保证心跳，一个是用于暗示是否有相应的微信消息
    private static final String WX_SYNC_URL_FORMAT = "%s://webpush.%s/cgi-bin/mmwebwx-bin/synccheck";
    private static final String AGENT_URL = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/51.0.2704.79 Chrome/51.0.2704.79 Safari/537.36";
    private static String deviceId;
    private static SecureRandom secureRandom = new SecureRandom();
    private static String wxBaseUri = "https://wx.qq.com";
    private static String wxHost = "wx.qq.com";
    private static String wxSchema = "https";

    private static BaseRequest formBaseRequest(Token paramToken) {
        BaseRequest localBaseRequest = new BaseRequest();
        localBaseRequest.Skey = paramToken.getSkey();
        localBaseRequest.Sid = paramToken.getWxsid();
        localBaseRequest.Uin = paramToken.getWxuin();
        localBaseRequest.DeviceID = randomDeviceId();
        return localBaseRequest;
    }

    public static BatchContactRequest formBatchContactRequest(Token paramToken, Set<String> paramSet) {
        BatchContactRequest localBatchContactRequest = new BatchContactRequest();
        localBatchContactRequest.BaseRequest = formBaseRequest(paramToken);
        ArrayList localArrayList = new ArrayList();
        localBatchContactRequest.Count = 0;
        Iterator localIterator = paramSet.iterator();
        while (localIterator.hasNext()) {
            String str = (String) localIterator.next();
            if (isGroupUserName(str)) {
                Contact localContact = new Contact();
                localContact.UserName = str;
                localArrayList.add(localContact);
                localBatchContactRequest.Count = (1 + localBatchContactRequest.Count);
            }
        }
        localBatchContactRequest.List = localArrayList;
        return localBatchContactRequest;
    }

    public static InitRequest formInitRequest(Token paramToken) {
        InitRequest localInitRequest = new InitRequest();
        localInitRequest.BaseRequest = formBaseRequest(paramToken);
        return localInitRequest;
    }

    public static MsgRequest formMsgRequest(Token paramToken, Msg paramMsg) {
        MsgRequest localMsgRequest = new MsgRequest();
        localMsgRequest.BaseRequest = formBaseRequest(paramToken);
        localMsgRequest.Msg = paramMsg;
        return localMsgRequest;
    }

    public static MsgSyncRequest formMsgSyncRequest(Token paramToken, SyncKey paramSyncKey) {
        MsgSyncRequest localMsgSyncRequest = new MsgSyncRequest();
        localMsgSyncRequest.BaseRequest = formBaseRequest(paramToken);
        localMsgSyncRequest.SyncKey = paramSyncKey;
        return localMsgSyncRequest;
    }

    public static Map<String, String> formSyncParams(Token paramToken, String paramString1, String paramString2) {
        HashMap localHashMap = new HashMap();
        localHashMap.put("_", String.valueOf(System.currentTimeMillis()));
        localHashMap.put("skey", paramToken.getSkey());
        localHashMap.put("sid", paramToken.getWxsid());
        localHashMap.put("uin", paramToken.getWxuin());
        localHashMap.put("deviceid", paramString1);
        localHashMap.put("synckey", paramString2);
        localHashMap.put("r", String.valueOf(System.currentTimeMillis()));
        return localHashMap;
    }

    public static NotifyRequest formNotifyRequest(Token paramToken, User user) {
        NotifyRequest localNotifyRequest = new NotifyRequest();
        localNotifyRequest.BaseRequest = formBaseRequest(paramToken);
        localNotifyRequest.ClientMsgId = System.currentTimeMillis();
        localNotifyRequest.Code = 3;
        localNotifyRequest.FromUserName = user.UserName;
        localNotifyRequest.ToUserName = user.UserName;
        return localNotifyRequest;
    }

    public static String getNotifyUrl(Token paramToken) {
        String str = wxBaseUri + WX_NOTIFY_URI;
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = paramToken.getPassTicket();
        return String.format(str, arrayOfObject);
    }

    //批量
    public static String getBatchContactUrl(Token paramToken) {
        String str = wxBaseUri + WX_CONTACT_EX;
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = paramToken.getPassTicket();
        return String.format(str, arrayOfObject);
    }

    //单个
    public static String getContactUrl(Token paramToken, int paramInt) {
        String str = wxBaseUri + WX_CONTACT_URI;
        Object[] arrayOfObject = new Object[2];
        arrayOfObject[0] = Integer.valueOf(paramInt);
        arrayOfObject[1] = paramToken.getPassTicket();
        return String.format(str, arrayOfObject);
    }

    public static String getHeadImgUrl(String paramString) {
        return wxBaseUri + paramString;
    }

    public static String getHeadUrlByUsername(Token paramToken, String paramString) {
        String str = wxBaseUri + WX_GET_HEAD;
        Object[] arrayOfObject = new Object[2];
        arrayOfObject[0] = paramString;
        arrayOfObject[1] = paramToken.getSkey();
        return String.format(str, arrayOfObject);
    }

    public static String getIconUrlByUsername(Token paramToken, String paramString) {
        String str = wxBaseUri + WX_GET_ICON;
        Object[] arrayOfObject = new Object[2];
        arrayOfObject[0] = paramString;
        arrayOfObject[1] = paramToken.getSkey();
        return String.format(str, arrayOfObject);
    }

    //初始化微信;
    public static String getInitUrl(Token paramToken) {
        String str = wxBaseUri + INIT_URI;
        Object[] arrayOfObject = new Object[2];
        arrayOfObject[0] = Long.valueOf(System.currentTimeMillis());
        arrayOfObject[1] = paramToken.getPassTicket();
        return String.format(str, arrayOfObject);
    }

    public static String getMsgSyncUrl(Token paramToken) {
        String str = wxBaseUri + WX_MSG_SYNC;
        Object[] arrayOfObject = new Object[2];
        arrayOfObject[0] = paramToken.getWxsid();
        arrayOfObject[1] = paramToken.getSkey();
        return String.format(str, arrayOfObject);
    }

    public static String getSendUrl(Token paramToken) {
        String str = wxBaseUri + WX_SEND_URI;
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = paramToken.getPassTicket();
        return String.format(str, arrayOfObject);
    }

    public static String getVoiceUrl(Token paramToken, String paramString) {
        String str = wxBaseUri + WX_GET_VOICE;
        Object[] arrayOfObject = new Object[2];
        arrayOfObject[0] = paramToken.getSkey();
        arrayOfObject[1] = paramString;
        return String.format(str, arrayOfObject);
    }

    public static boolean isGroupUserName(String paramString) {
        return (paramString != null) && (paramString.startsWith("@@"));
    }

    public static long randomClientMsgId() {
        return System.currentTimeMillis();
    }

    public static String randomDeviceId() {
        if (StringUtil.isNullOrEmpty(deviceId)) {
            StringBuilder localStringBuilder = new StringBuilder(16);
            localStringBuilder.append("e");
            for (int i = 0; i < 15; i++)
                localStringBuilder.append(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));
            deviceId = localStringBuilder.toString();
        }
        return deviceId;
    }

    public static void setWxBaseUri(String paramString1, String paramString2) {
        wxSchema = paramString1;
        wxHost = paramString2;
        wxBaseUri = paramString1 + "://" + paramString2;
        Log.d("TAG", "setWxBaseUri:base uri=" + wxBaseUri);
    }

    public static Properties syncCheck(Token paramToken, String paramString1, String paramString2) {
        Map localMap = formSyncParams(paramToken, paramString1, paramString2);
        Uri.Builder localBuilder;

        Object[] arrayOfObject = new Object[2];
        arrayOfObject[0] = wxSchema;
        arrayOfObject[1] = wxHost;
        localBuilder = Uri.parse(String.format(WX_SYNC_URL_FORMAT, arrayOfObject)).buildUpon();
        Iterator localIterator = localMap.keySet().iterator();
        while (localIterator.hasNext()) {
            String str3 = (String) localIterator.next();
            localBuilder.appendQueryParameter(str3, (String) localMap.get(str3));
        }


        try {
            String str1 = localBuilder.build().toString();
            Log.d("TAG", "syncCheck:url=" + str1);
            String str2 = Jsoup.connect(str1).timeout(30000).userAgent(AGENT_URL).referrer("https://wx.qq.com/").header("Cookie", paramToken.cookie).validateTLSCertificates(false).get().text();
            Log.d("TAG", "syncCheck:result=" + str2);
            Properties localProperties = WxLogin.parseText(str2);
            return localProperties;
        } catch (IOException e) {
            Log.d("TAG", "syncCheck:exception:" + e);
            e.printStackTrace();
            return null;
        }

    }
}
