package com.mstarc.wechat.wearwechat.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.mstarc.wechat.wearwechat.model.Token;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class WxLogin {

    private static final String CODE_KEY = "window.QRLogin.code";
    private static final String JS_LOGIN_WX_URL = "https://login.wx.qq.com/jslogin?appid=wx782c26e4c19acffb&redirect_uri=https%3A%2F%2Fwx.qq.com%2Fcgi-bin%2Fmmwebwx-bin%2Fwebwxnewloginpage&fun=new&lang=zh_CN&_=";
    private static final String LOGIN_CHECK_URL = "https://login.wx.qq.com/cgi-bin/mmwebwx-bin/login?loginicon=true&uuid=%s&tip=1&_=";
    private static final String LOGIN_URL = "https://login.weixin.qq.com/l/";
    private static final String QR_URL = "https://login.weixin.qq.com/qrcode/";
    private static final String AGENT_URL = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/51.0.2704.79 Chrome/51.0.2704.79 Safari/537.36";
    private static final String REDIRECT_SUFFIX = "&fun=new&version=v2";
    public static final String UNKNOWN_HOST = "UNKNOWN_HOST";
    private static final String UUID_KEY = "window.QRLogin.uuid";
    public static Properties prop = new Properties();

    //使用uuid;
    public static Properties checkLoginStatus(String paramString) {
        long l = System.currentTimeMillis();
        try {
            String str1 = String.format(LOGIN_CHECK_URL, new Object[]{paramString}) + l;
            Log.d("TAG", "checkLoginStatus:url=" + str1);
            String str2 = Jsoup.connect(str1).timeout(30000).userAgent(AGENT_URL).referrer("https://wx.qq.com/").validateTLSCertificates(false).get().text();
            Log.d("TAG", "checkLoginStatus:result=" + str2);
            Properties localProperties = parseText(str2);
            return localProperties;

        } catch (IOException localIOException) {
            Log.w("TAG", localIOException);
        }
        return null;
    }

    private static String cookiesToStr(Map<String, String> paramMap) {
        StringBuilder localStringBuilder = new StringBuilder();
        Iterator localIterator = paramMap.keySet().iterator();
        while (localIterator.hasNext()) {
            String str = (String) localIterator.next();
            localStringBuilder.append(str + "=" + (String) paramMap.get(str) + ";");
        }
        return localStringBuilder.toString();
    }

    public static String formatQRUrl(String paramString) {
        return QR_URL + paramString;
    }

    public static Bitmap getBase64Image(String paramString) {
        byte[] arrayOfByte = Base64.decode(paramString.replace("data:img/jpg;base64,", ""), 0);
        return BitmapFactory.decodeByteArray(arrayOfByte, 0, arrayOfByte.length);
    }

    //参数为重定向地址;
    public static Token getToken(String s) {

        String re_url = s.substring(1, s.length() - 1);
        Uri localUri = Uri.parse(re_url);
        Log.d("TAG", "localUri" + localUri);
        WxHome.setWxBaseUri(localUri.getScheme(), localUri.getHost());

        Token localToken = new Token();
        Element localElement;
        String str;
        try {
            Connection.Response localResponse = Jsoup.connect(re_url + REDIRECT_SUFFIX).timeout(30000).userAgent(AGENT_URL)
                    .referrer(localUri.getHost()).validateTLSCertificates(false).execute();


            Elements localElements = localResponse.parse().getElementsByTag("error");

            if ((localElements == null) || (localElements.isEmpty())) {
                return null;
            }
            localToken.cookie = cookiesToStr(localResponse.cookies());
            Log.d("TAG", "localToken.cookie====" + localToken.cookie);

            Iterator localIterator = localElements.get(0).children().iterator();

            while (localIterator.hasNext()) {
                localElement = (Element) localIterator.next();
                str = localElement.tagName();

                if ("ret".equals(str)) {
                    String code = localElement.text().trim();
                    Log.d("TAG", "ret=====" + code);
                    if ("0".equals(code)) {
                        continue;
                    } else {
                        break;
                    }
                } else if ("message".equals(str)) {
                    localToken.message = localElement.text().trim();
                    continue;
                } else if ("skey".equals(str)) {
                    localToken.setSkey(localElement.text().trim());
                    continue;
                } else if ("wxsid".equals(str)) {
                    localToken.setWxsid(localElement.text().trim());
                    continue;
                } else if ("wxuin".equals(str)) {
                    localToken.setWxuin(localElement.text().trim());
                    continue;
                } else if ("pass_ticket".equals(str)) {
                    localToken.setPassTicket(localElement.text().trim());
                    continue;
                } else if ("isgrayscale".equals(str)) {
                    localToken.setIsGrayScale(localElement.text().trim());
                    continue;
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("TAG", "localToken=====" + localToken);
        Log.d("TAG", "localToken.getWxsid=====" + localToken.getWxsid());

        return localToken;

    }


    public static Bitmap getURLImage(String paramString) {
        Bitmap localBitmap = null;
        try {
            HttpURLConnection localHttpURLConnection = (HttpURLConnection) new URL(paramString).openConnection();
            localHttpURLConnection.setConnectTimeout(6000);
            localHttpURLConnection.setDoInput(true);
            localHttpURLConnection.setUseCaches(false);
            localHttpURLConnection.connect();
            InputStream localInputStream = localHttpURLConnection.getInputStream();
            localBitmap = BitmapFactory.decodeStream(localInputStream);
            localInputStream.close();
            Log.d("TAG", "localBitmap结果1" + localBitmap);
            return localBitmap;
        } catch (Exception localException) {
            Log.w("TAG", "getURLImage:exception", localException);
        }
        return localBitmap;
    }

    public static String getUUid() {
        long l = System.currentTimeMillis();
        try {
            Properties localProperties = parseText(Jsoup.connect(JS_LOGIN_WX_URL + l).userAgent(AGENT_URL).referrer("https://wx.qq.com/").validateTLSCertificates(false).get().text());
            String str1 = localProperties.getProperty(CODE_KEY);

            if ("200".equals(str1)) {
                String str2 = localProperties.getProperty(UUID_KEY).replaceAll("\"", "");

                return str2;
            }

        } catch (UnknownHostException localUnknownHostException) {
            Log.w("TAG", "getUUid:unknownHostException");
            return UNKNOWN_HOST;
        } catch (IOException localIOException) {
            Log.w("TAG", "getUUid:exception", localIOException);
        }
        return "";
    }

    public static Properties parseText(String s) {

        if ((s == null) || (s.isEmpty())) {
            return null;
        }

        String s2 = s.replaceAll(" ", "");

        if (s2.contains("window.code=201")) {
            String t1 = s2.substring(0, s2.indexOf(";"));
            String t2 = s2.substring(s2.indexOf(";") + 1, s2.lastIndexOf(";"));
            prop.setProperty("window.code", "201");
            prop.setProperty("window.userAvatar", t2.substring(t2.indexOf("'") + 1, t2.length() - 1));
        } else {
            String[] split = s2.split(";");
            for (int i = 0; i < split.length; i++) {

                String temp = split[i];

                prop.setProperty(temp.substring(0, temp.indexOf("=")), temp.substring(temp.indexOf("=") + 1));
            }
        }


        return prop;
    }
}
