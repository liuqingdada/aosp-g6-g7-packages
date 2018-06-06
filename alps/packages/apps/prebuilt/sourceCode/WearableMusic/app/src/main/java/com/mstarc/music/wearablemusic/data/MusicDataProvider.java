package com.mstarc.music.wearablemusic.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mstarc.music.wearablemusic.data.MusicDataContract.Music;

import java.io.File;

/**
 * description
 * <p/>
 * Created by andyding on 2017/6/5.
 */

public class MusicDataProvider extends ContentProvider {
    private static final String TAG = MusicDataProvider.class.getSimpleName();

    private final static String DATABASE_PATH = Environment.getExternalStorageDirectory().getPath()
            + File.separator + "music.db";

    private static int DB_VERSION = 1;
    private DbHelper helper;
    private SQLiteDatabase mDatabase;

    @Override
    public boolean onCreate() {
        Log.d("dingyichen", "Content provider DeviceData onCreate!");
        helper = new DbHelper(getContext());
        try {
            mDatabase = helper.getReadableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("dingyichen", "music_helper.getReadableDatabase() Exception");
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (mDatabase == null) {
            mDatabase = helper.getReadableDatabase();
        }
        if (!tabIsExist()) {
            return null;
        }
        return mDatabase.query(Music.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        long id = mDatabase.insert(Music.TABLE_NAME, null, values);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int ret;
        if (mDatabase == null) {
            mDatabase = helper.getReadableDatabase();
        }
        ret = mDatabase.delete(Music.TABLE_NAME, selection, selectionArgs);

        return ret;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }


    public static Uri insertData(Context context, ContentValues values) {
        ContentResolver cr = context.getContentResolver();

        Log.d("dingyichen", "insertData cv = " + values.toString());
        return cr.insert(Music.CONTENT_URI, values);

    }

    public static void deteleItem(Context context, MusicData data) {
        ContentResolver cr = context.getContentResolver();
        String where = Music.Column.MUSIC_ID + " = ? ";
        String[] selectionArgs = {data.getMusicId()};
        Log.d("dingyichen", "deteleItem cv = ");
        cr.delete(Music.CONTENT_URI, where, selectionArgs);
    }

    public static Cursor getAllMusicData(Context context) {
        ContentResolver cr = context.getContentResolver();
        return cr.query(Music.CONTENT_URI, null, null, null, null);
    }


    public static class DbHelper extends SQLiteOpenHelper {

        public DbHelper(Context context) {
            this(context, DATABASE_PATH, null, DB_VERSION);
        }

        public DbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        public DbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
            super(context, name, factory, version, errorHandler);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d("dingyichen", "DbHelper onCreate, create tables.");
            //createTables(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        private void createTables(SQLiteDatabase db) {

            db.execSQL("CREATE TABLE IF NOT EXISTS " + Music.TABLE_NAME +
                    "(" + Music.Column._ID + " integer primary key," +
                    Music.Column.MUSIC_ID + " varchar," +
                    Music.Column.MUSIC_FILE_NAME + " varchar," +
                    Music.Column.DOWNLOAD_URL + " varchar," +
                    Music.Column.SAVE_URL + " varchar," +
                    Music.Column.MUSIC_SINGER + " varchar," +
                    Music.Column.MUSIC_NAME + " varchar," +
                    Music.Column.MUSIC_ICON + " varchar," +
                    Music.Column.MUSIC_TIME + " varchar," +
                    Music.Column.MUSIC_SIZE + " varchar" +
                    ")");
        }
    }

    /**
     * 判断music是否存在
     *
     * @return
     */
    public boolean tabIsExist() {
        boolean result = false;
        String tabName = Music.TABLE_NAME;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = helper.getReadableDatabase();//此this是继承SQLiteOpenHelper类得到的
            String sql = "select count(*) as c from sqlite_master where type ='table' and name ='" + tabName.trim() + "' ";
            cursor = db.rawQuery(sql, null);
            if (cursor.moveToNext()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    result = true;
                }
            }
            cursor.close();
        } catch (Exception e) {
            // TODO: handle exception
        }
        return result;
    }

}
