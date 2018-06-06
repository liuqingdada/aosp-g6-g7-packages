package com.mstarc.music.wearablemusic.data;

import android.net.Uri;

/**
 * description
 * <p/>
 * Created by andyding on 2017/6/5.
 */

public class MusicDataContract {

    public static final String COMMON_COLUMN_ID = "_id";
    public static final String COMMON_MUSIC_ID = "fileId";
    public static final String COMMON_MUSIC_FILE_NAME = "filename";
    public static final String COMMON_DOWNLOAD_URL = "downurl";
    public static final String COMMON_SAVE_URL = "saveurl";
    public static final String COMMON_MUSIC_SINGER = "singer";
    public static final String COMMON_MUSIC_NAME = "songname";
    public static final String COMMON_MUSIC_ICON = "icon";
    public static final String COMMON_MUSIC_TIME = "time";
    public static final String COMMON_MUSIC_SIZE = "size";

    public final static String CONTENT_AUTHORITY = "com.mstarc.music.database";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * A list of possible paths that will be appended to the base URI for each of the different
     * tables.
     */

    public static class Music {
        public static final String TABLE_NAME = "music";
        public final static Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, TABLE_NAME);

        public static class Column {
            public static String _ID = COMMON_COLUMN_ID;
            public static String MUSIC_ID = COMMON_MUSIC_ID;
            public static String MUSIC_FILE_NAME = COMMON_MUSIC_FILE_NAME;
            public static String DOWNLOAD_URL = COMMON_DOWNLOAD_URL;
            public static String SAVE_URL = COMMON_SAVE_URL;
            public static String MUSIC_SINGER = COMMON_MUSIC_SINGER;
            public static String MUSIC_NAME = COMMON_MUSIC_NAME;
            public static String MUSIC_ICON = COMMON_MUSIC_ICON;
            public static String MUSIC_TIME = COMMON_MUSIC_TIME;
            public static String MUSIC_SIZE = COMMON_MUSIC_SIZE;
        }
    }
}
