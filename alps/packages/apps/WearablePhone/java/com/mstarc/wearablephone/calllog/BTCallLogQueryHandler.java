/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mstarc.wearablephone.calllog;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.util.Log;

import com.android.contacts.common.database.NoNullCursorAsyncQueryHandler;
import com.google.common.collect.Lists;
import com.mstarc.wearablephone.bluetooth.DataProviderContract;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Handles asynchronous queries to the call log.
 */
public class BTCallLogQueryHandler extends NoNullCursorAsyncQueryHandler {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String TAG = "BTCallLogQueryHandler";
    private static final int NUM_LOGS_TO_DISPLAY = 1000;

    /**
     * The token for the query to fetch the old entries from the call log.
     */
    private static final int QUERY_BT_CALLLOG_TOKEN = 60;

    private final int mLogLimit;

    /**
     * Call type similar to Calls.INCOMING_TYPE used to specify all types instead of one particular
     * type.
     */
    public static final int CALL_TYPE_ALL = -1;

    private final WeakReference<Listener> mListener;

    /**
     * Simple handler that wraps background calls to catch
     * {@link SQLiteException}, such as when the disk is full.
     */
    protected class CatchingWorkerHandler extends WorkerHandler {
        public CatchingWorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                // Perform same query while catching any exceptions
                super.handleMessage(msg);
            } catch (SQLiteDiskIOException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            } catch (SQLiteFullException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            } catch (SQLiteDatabaseCorruptException e) {
                Log.w(TAG, "Exception on background worker thread", e);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "ContactsProvider not present on device", e);
            }
        }
    }

    @Override
    protected Handler createHandler(Looper looper) {
        // Provide our special handler that catches exceptions
        return new CatchingWorkerHandler(looper);
    }

    public BTCallLogQueryHandler(ContentResolver contentResolver, Listener listener) {
        this(contentResolver, listener, -1);
    }

    public BTCallLogQueryHandler(ContentResolver contentResolver, Listener listener, int limit) {
        super(contentResolver);
        mListener = new WeakReference<Listener>(listener);
        mLogLimit = limit;
    }

    /**
     * Fetches the list of calls from the call log for a given type.
     * This call ignores the new or old state.
     * <p>
     * It will asynchronously update the content of the list view when the fetch completes.
     */
    public void fetchCalls(int callType, long newerThan) {
        cancelFetch();
        fetchCalls(QUERY_BT_CALLLOG_TOKEN, callType, false /* newOnly */, newerThan);
    }

    public void fetchCalls(int callType) {
        fetchCalls(callType, 0);
    }

    /**
     * Fetches the list of calls in the call log.
     */
    private void fetchCalls(int token, int callType, boolean newOnly, long newerThan) {
        StringBuilder where = new StringBuilder();
        List<String> selectionArgs = Lists.newArrayList();

        if (callType > CALL_TYPE_ALL) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            // Add a clause to fetch only items of type voicemail.
            where.append(String.format("(%s = ?)", DataProviderContract.TYPE));
            // Add a clause to fetch only items newer than the requested date
            selectionArgs.add(Integer.toString(callType));
        }

        if (newerThan > 0) {
            if (where.length() > 0) {
                where.append(" AND ");
            }
            where.append(String.format("(%s > ?)", DataProviderContract.DATE));
            selectionArgs.add(Long.toString(newerThan));
        }
        final String selection = where.length() > 0 ? where.toString() : null;
        startQuery(token, null, DataProviderContract.CONTENT_URI,
                BTCallLogQuery._PROJECTION, selection, selectionArgs.toArray(EMPTY_STRING_ARRAY),
                Calls.DEFAULT_SORT_ORDER);
    }

    /**
     * Cancel any pending fetch request.
     */
    private void cancelFetch() {
        cancelOperation(QUERY_BT_CALLLOG_TOKEN);
    }


    @Override
    protected synchronized void onNotNullableQueryComplete(int token, Object cookie, Cursor cursor) {
        if (cursor == null) {
            return;
        }
        try {
            if (token == QUERY_BT_CALLLOG_TOKEN) {
                if (updateAdapterData(cursor)) {
                    cursor = null;
                }
            } else {
                Log.w(TAG, "Unknown query completed: ignoring: " + token);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Updates the adapter in the call log fragment to show the new cursor data.
     * Returns true if the listener took ownership of the cursor.
     */
    private boolean updateAdapterData(Cursor cursor) {
        final Listener listener = mListener.get();
        if (listener != null) {
            return listener.onBTCallsFetched(cursor);
        }
        return false;

    }

    /**
     * Listener to completion of various queries.
     */
    public interface Listener {
        /**
         * Called when {@link BTCallLogQueryHandler#fetchCalls(int)}complete.
         * Returns true if takes ownership of cursor.
         */
        boolean onBTCallsFetched(Cursor combinedCursor);
    }
}
