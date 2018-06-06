package com.mstarc.wearablemms.data;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.CursorAdapter;

/**
 * Created by Administrator on 2016/3/29.
 * 短信的异步查询
 */
public class SimpleQueryHander extends AsyncQueryHandler {
    public SimpleQueryHander(ContentResolver cr) {
        super(cr);
    }

    //查询完毕时调用
    //arg0、arg1:查询开始时携带的数据
    //arg2:查询结果
    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        super.onQueryComplete(token, cookie, cursor);
        if(cursor != null) {
            CursorUtils.printCursor(cursor);
        }

        if (cookie != null && cookie instanceof CursorAdapter){
            //查询得到的cursor，交给CursorAdapter，由它把cursor的内容显示至listView
            ((CursorAdapter) cookie).changeCursor(cursor);
        }
    }
}
