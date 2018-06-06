package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.provider.Telephony;
import android.os.UserHandle;

public class SysIpMsgProvider extends ContentProvider {

    private static final String TAG = "Mms/Provider/SysIp";
    private static final String MSG_TABLE = "sys_ipmsg";
    
    private static final int URI_SYS_IPMSG          = 1;
    private static final int URI_SYS_IPMSG_ID       = 2;

    private static final String AUTHORITY = "system-ipmsg";
    private static final Uri CONTENT_URI = Uri.parse("content://system-ipmsg");
    
    private static final Uri RCS_MESSAGE_URI = Uri.parse("content://org.gsma.joyn.provider.chat/message");
    
    private static final String[] SYS_MSG_PROJECTION = {"_id", "thread_id", "ipmsg_id"};

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, null, URI_SYS_IPMSG);
        uriMatcher.addURI(AUTHORITY, "#", URI_SYS_IPMSG_ID);
    }

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        Log.d(TAG, "delete start, uri=" + uri + ", where=" + where + ", whereArgs=" + whereArgs);
        int match = uriMatcher.match(uri);
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch(match) {
            case URI_SYS_IPMSG:
                count = db.delete(MSG_TABLE, where, whereArgs);
                break;
            case URI_SYS_IPMSG_ID:
                count = db.delete(MSG_TABLE, concatSelections("_id=" + uri.getLastPathSegment(), where), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        Log.d(TAG, "delete end, count = " + count);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert start, uri=" + uri + ", values=" + values);
        int match = uriMatcher.match(uri);
        long id = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch(match) {
            case URI_SYS_IPMSG:
            case URI_SYS_IPMSG_ID:
                if (!values.containsKey("thread_id")) {
                    Log.d(TAG, "insert thread_id is null");
                    return null;
                }
                id = db.insert(MSG_TABLE, null, values);
                uri = ContentUris.withAppendedId(CONTENT_URI, id);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        Log.d(TAG, "insert end, uri=" + uri);
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(Telephony.MmsSms.CONTENT_URI, null, true,
                UserHandle.USER_ALL);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null, true,
                UserHandle.USER_ALL);
        cr.notifyChange(Uri.parse("content://mms-sms/rcs/conversations/"), null, true,
                UserHandle.USER_ALL);
        return uri;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query start, uri=" + uri);
        int match = uriMatcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        switch(match) {
            case URI_SYS_IPMSG:
                break;
            case URI_SYS_IPMSG_ID:
                String selection2 = "_id=" + Long.valueOf(uri.getLastPathSegment());
                selection = concatSelections(selection, selection2);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return db.query(MSG_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        return 0;
    }

    private String concatSelections(String selection1, String selection2) {
        if (TextUtils.isEmpty(selection1)) {
            return selection2;
        } else if (TextUtils.isEmpty(selection2)) {
            return selection1;
        } else {
            return selection1 + " AND " + selection2;
        }
    }

    public static int deleteMessages(SQLiteDatabase db,
            String selection, String[] selectionArgs) {
        return db.delete(MSG_TABLE, selection, selectionArgs);
    }
}
