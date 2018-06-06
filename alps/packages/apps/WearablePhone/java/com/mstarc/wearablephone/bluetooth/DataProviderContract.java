package com.mstarc.wearablephone.bluetooth;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by wangxinzhi on 17-4-30.
 */

public class DataProviderContract implements BaseColumns {

    private DataProviderContract() {
    }

    // The URI scheme used for content URIs
    public static final String SCHEME = "content";

    // The provider's authority
    public static final String AUTHORITY = "com.mstarc.wearablephone.bt";

    public static final String CALLLOG_TABLE_NAME = "CallLog";


    /**
     * The DataProvider content URI
     */
    public static final Uri CONTENT_URI = Uri.parse(SCHEME + "://" + AUTHORITY+"/"+CALLLOG_TABLE_NAME);

    public static final String ROW_ID = BaseColumns._ID;

    /**
     * Picture table content URI
     */
    public static final Uri CALLLOG_TABLE_CONTENTURI =
            Uri.withAppendedPath(CONTENT_URI, CALLLOG_TABLE_NAME);

    /**
     * Picture table thumbnail URL column name
     */

    public static final String DEFAULT_SORT_ORDER = "date DESC";

    public static final String TYPE = "type";

    /**
     * Call log type for incoming calls.
     */
    public static final int INCOMING_TYPE = 1;
    /**
     * Call log type for outgoing calls.
     */
    public static final int OUTGOING_TYPE = 2;
    /**
     * Call log type for missed calls.
     */
    public static final int MISSED_TYPE = 3;
    /** Call log type for voicemails. */
    /**
     * Call log type for calls rejected by direct user action.
     */
    public static final int REJECTED_TYPE = 4;
    /**
     * Call log type for calls blocked automatically.
     */
    public static final int BLOCKED_TYPE = 5;

    public static final String NUMBER = "number";

    public static final String NAME = "name";
    public static final String DATE = "date";

    public static final int DATABASE_VERSION = 1;
}
