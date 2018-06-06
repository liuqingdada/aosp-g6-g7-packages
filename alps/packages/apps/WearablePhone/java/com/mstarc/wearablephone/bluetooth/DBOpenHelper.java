package com.mstarc.wearablephone.bluetooth;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by wangxinzhi on 17-4-30.
 */

public class DBOpenHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "BtPhoneCallLogDatabase.db";
    private static final String DATABASE_TABLE = "CallLog";
    private static final int DATABASE_VERSION = 1;
    private static final String TEXT_TYPE = "TEXT";
    private static final String PRIMARY_KEY_TYPE = "INTEGER PRIMARY KEY";
    private static final String INTEGER_TYPE = "INTEGER";
    // SQL Statement to create a new database.
    // Defines an SQLite statement that builds the Picasa picture URL table
    private static final String CREATE_CALLLOG_TABLE_SQL = "CREATE TABLE" + " " +
            DataProviderContract.CALLLOG_TABLE_NAME + " " +
            "(" + " " +
            DataProviderContract.ROW_ID + " " + PRIMARY_KEY_TYPE + " ," +
            DataProviderContract.TYPE + " " + TEXT_TYPE + " ," +
            DataProviderContract.NUMBER + " " + INTEGER_TYPE + " ," +
            DataProviderContract.NAME + " " + TEXT_TYPE + " ," +
            DataProviderContract.DATE + " " + TEXT_TYPE +
            ")";
    private static final String TAG = DBOpenHelper.class.getSimpleName();

    public DBOpenHelper(Context context) {
        super(context,
                DataProviderContract.CALLLOG_TABLE_NAME,
                null,
                DataProviderContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_CALLLOG_TABLE_SQL);
    }

    private void dropTables(SQLiteDatabase db) {

        // If the table doesn't exist, don't throw an error
        db.execSQL("DROP TABLE IF EXISTS " + DataProviderContract.CALLLOG_TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all the existing data");

        dropTables(db);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG,
                "Downgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all the existing data");

        dropTables(db);

        onCreate(db);
    }
}
