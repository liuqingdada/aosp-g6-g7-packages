package com.mstarc.wechat.wearwechat.dao;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.mstarc.wechat.wearwechat.LaunchActivity;
import com.mstarc.wechat.wearwechat.model.Msg;

import java.util.ArrayList;
import java.util.List;

public class MessageManager {
    private static final String TAG = "MessageManager";
    private WechatDemoDbHelper mDbHelper;
    private Context mContext;
    private SharedPreferences mPrefs;

    public MessageManager(Context paramContext) {
        mContext = paramContext;
        this.mDbHelper = new WechatDemoDbHelper(paramContext);
    }

    public List<Msg> getMsg(String paramString) {
        SQLiteDatabase localSQLiteDatabase = this.mDbHelper.getReadableDatabase();
        ArrayList<Msg> localArrayList = new ArrayList<>();
        String[] arrayOfString = {paramString, paramString};
        try {
            Cursor localCursor = localSQLiteDatabase.query("message", null, "from_username = ? or to_username = ?", arrayOfString, null, null, "create_time ASC");
            while (localCursor.moveToNext()) {
                Msg localMsg = new Msg();
                localMsg.MsgId = localCursor.getString(localCursor.getColumnIndexOrThrow("msg_id"));
                localMsg.ClientMsgId = localCursor.getLong(localCursor.getColumnIndexOrThrow("client_msg_id"));
                localMsg.MsgType = localCursor.getInt(localCursor.getColumnIndexOrThrow("msg_type"));
                localMsg.Content = localCursor.getString(localCursor.getColumnIndexOrThrow("content"));
                localMsg.FromUserName = localCursor.getString(localCursor.getColumnIndexOrThrow("from_username"));
                localMsg.ToUserName = localCursor.getString(localCursor.getColumnIndexOrThrow("to_username"));
                localMsg.fromNickName = localCursor.getString(localCursor.getColumnIndexOrThrow("from_nickname"));
                localMsg.toNickName = localCursor.getString(localCursor.getColumnIndexOrThrow("to_nickname"));
                localMsg.fromMemberUserName = localCursor.getString(localCursor.getColumnIndexOrThrow("from_member_username"));
                localMsg.fromMemberNickName = localCursor.getString(localCursor.getColumnIndexOrThrow("from_member_nickname"));
                localMsg.VoiceLength = localCursor.getLong(localCursor.getColumnIndexOrThrow("voice_length"));
                localMsg.CreateTime = localCursor.getLong(localCursor.getColumnIndexOrThrow("create_time"));
                localArrayList.add(localMsg);
            }
            if (!localCursor.isClosed()) {
                localCursor.close();
            }
        } catch (Exception e){
            Log.e(TAG, "getMsg: ", e);
            mPrefs = mContext.getSharedPreferences("isLogin", Activity.MODE_PRIVATE);

            if (mPrefs.getBoolean("islogin", false)) {
                // 已登录

            } else {
                // 未登录
                mContext.startActivity(new Intent(mContext, LaunchActivity.class));
            }
        } finally {
            localSQLiteDatabase.close();
        }
        localSQLiteDatabase.close();
        return localArrayList;
    }

    public void insertMessage(Msg paramMsg) {
        SQLiteDatabase localSQLiteDatabase = this.mDbHelper.getWritableDatabase();
        try {
            ContentValues localContentValues = new ContentValues();
            if (paramMsg.CreateTime == 0L)
                paramMsg.CreateTime = System.currentTimeMillis();
            localContentValues.put("client_msg_id", Long.valueOf(paramMsg.ClientMsgId));
            localContentValues.put("msg_id", paramMsg.MsgId);
            localContentValues.put("msg_type", Integer.valueOf(paramMsg.MsgType));
            localContentValues.put("content", paramMsg.Content);
            localContentValues.put("from_username", paramMsg.FromUserName);
            localContentValues.put("to_username", paramMsg.ToUserName);
            localContentValues.put("from_nickname", paramMsg.fromNickName);
            localContentValues.put("to_nickname", paramMsg.toNickName);
            localContentValues.put("from_member_username", paramMsg.fromMemberUserName);
            localContentValues.put("from_member_nickname", paramMsg.fromMemberNickName);
            localContentValues.put("voice_length", Long.valueOf(paramMsg.VoiceLength));
            localContentValues.put("create_time", Long.valueOf(paramMsg.CreateTime));
            localSQLiteDatabase.insert("message", null, localContentValues);
            return;
        } finally {
            localSQLiteDatabase.close();
        }
    }
}

