package com.mstarc.wechat.wearwechat.dao;

import android.provider.BaseColumns;

public class MessageContract
{
  public static abstract class MessageEntry
    implements BaseColumns
  {
    public static final String COLUMN_CLIENT_MSG_ID = "client_msg_id";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_CREATE_TIME = "create_time";
    public static final String COLUMN_FROM_MEMBER_NICK_NAME = "from_member_nickname";
    public static final String COLUMN_FROM_MEMBER_USER_NAME = "from_member_username";
    public static final String COLUMN_FROM_NICK_NAME = "from_nickname";
    public static final String COLUMN_FROM_USER_NAME = "from_username";
    public static final String COLUMN_MSG_ID = "msg_id";
    public static final String COLUMN_MSG_TYPE = "msg_type";
    public static final String COLUMN_TO_NICK_NAME = "to_nickname";
    public static final String COLUMN_TO_USER_NAME = "to_username";
    public static final String COLUMN_VOICE_LENGTH = "voice_length";
    public static final String TABLE_NAME = "message";
  }
}

