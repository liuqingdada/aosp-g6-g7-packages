package com.mstarc.wearablephone.bluetooth;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by wangxinzhi on 17-4-30.
 */

public class BTCallLogProvider extends ContentProvider {
    private SQLiteOpenHelper mHelper;
    private static final UriMatcher sUriMatcher;
    public static final int CALLLOG_QUERY = 1;
    public static final int INVALID_URI = -1;
    public static final String LIMIT_PARAM_KEY = "limit";
    public static final String OFFSET_PARAM_KEY = "offset";
    public static final String DEFAULT_SORT_ORDER = "date DESC";


    static {

        // Creates an object that associates content URIs with numeric codes
        sUriMatcher = new UriMatcher(0);
        sUriMatcher.addURI(
                DataProviderContract.AUTHORITY,
                DataProviderContract.CALLLOG_TABLE_NAME,
                CALLLOG_QUERY);

    }

    @Override
    public boolean onCreate() {
        mHelper = new DBOpenHelper(getContext());
        return true;
    }

    private int getIntParam(Uri uri, String key, int defaultValue) {
        String valueString = uri.getQueryParameter(key);
        if (valueString == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(valueString);
        } catch (NumberFormatException e) {
            String msg = "Integer required for " + key + " parameter but value '" + valueString +
                    "' was found instead.";
            throw new IllegalArgumentException(msg, e);
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        // Decodes the content URI and maps it to a code
        switch (sUriMatcher.match(uri)) {

            // If the query is for a picture URL
            case CALLLOG_QUERY:
                // Does the query against a read-only version of the database
                final int limit = getIntParam(uri, LIMIT_PARAM_KEY, 0);
                final int offset = getIntParam(uri, OFFSET_PARAM_KEY, 0);
                String limitClause = null;
                if (limit > 0) {
                    limitClause = offset + "," + limit;
                }

                final Cursor returnCursor = db.query(DataProviderContract.CALLLOG_TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder,
                        limitClause);
                // Sets the ContentResolver to watch this content URI for data changes
                returnCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return returnCursor;

            case INVALID_URI:

                throw new IllegalArgumentException("Query -- Invalid URI:" + uri);
        }

        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        // Decode the URI to choose which action to take
        switch (sUriMatcher.match(uri)) {

            // For the modification date table
            case CALLLOG_QUERY:

                // Creates a writeable database or gets one from cache
                SQLiteDatabase localSQLiteDatabase = mHelper.getWritableDatabase();

                // Inserts the row into the table and returns the new row's _id value
                long id = localSQLiteDatabase.insert(
                        DataProviderContract.CALLLOG_TABLE_NAME,
                        null,
                        values
                );

                // If the insert succeeded, notify a change and return the new row's content URI.
                if (-1 != id) {
                    getContext().getContentResolver().notifyChange(uri, null);
                    return Uri.withAppendedPath(uri, Long.toString(id));
                } else {

                    throw new SQLiteException("Insert error:" + uri);
                }
            case INVALID_URI:

                throw new IllegalArgumentException("Insert: Invalid URI" + uri);
        }

        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {

            // For the modification date table
            case CALLLOG_QUERY:
                count = db.delete(DataProviderContract.CALLLOG_TABLE_NAME, selection, selectionArgs);
                db.close();
                break;
            case INVALID_URI:
                throw new IllegalArgumentException("Insert: Invalid URI" + uri);
        }

        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {

            // For the modification date table
            case CALLLOG_QUERY:
                count = db.update(DataProviderContract.CALLLOG_TABLE_NAME, values, selection, selectionArgs);
                db.close();
                break;
            case INVALID_URI:
                throw new IllegalArgumentException("Insert: Invalid URI" + uri);
        }
        return count;
    }


}
