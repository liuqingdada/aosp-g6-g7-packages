package com.mstarc.wearablemms.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.InputStream;

/**
 * Created by Administrator on 2016/3/31.
 */
public class Sms {
    private String body;
    private long date;
    private int type;
    private int _id;
    private String address;

    public static Sms createFromCursor(Cursor cursor){
        Sms sms = new Sms();
        sms.setBody(cursor.getString(cursor.getColumnIndex("body")));
        sms.setDate(cursor.getLong(cursor.getColumnIndex("date")));
        sms.setType(cursor.getInt(cursor.getColumnIndex("type")));
        sms.set_id(cursor.getInt(cursor.getColumnIndex("_id")));
        sms.setAddress(cursor.getString(cursor.getColumnIndex("address")));
        return sms;
    }
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public void setAddress(String address){
        this.address = address;
    }
    public String getAddress(){
        return this.address;
    }
    public String getName(ContentResolver resolver){
        String name = getNameAddress(resolver,getAddress());
        return (name ==null)?getAddress():name;
    }
    private String getNameAddress(ContentResolver resolver, String address){
        String name = null;
        //把指定数据拼接在uri后面
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,address);
        Cursor cursor = resolver.query(uri,new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},null,null,null);
        if (cursor.moveToFirst()){
            name = cursor.getString(0);
            cursor.close();
        }
        return name;
    }

    //通过号码获取联系人头像
    public Bitmap getAvatarByAddress(ContentResolver resolver){
        Bitmap avatar = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,getAddress());
        Cursor cursor = resolver.query(uri,new String[]{ContactsContract.PhoneLookup._ID},null,null,null);
        if (cursor.moveToFirst()){
            String _id = cursor.getString(0);
            //获取联系人的照片
            InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(resolver, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,_id));
            avatar = BitmapFactory.decodeStream(is);
        }
        return avatar;
    }

    @Override
    public String toString() {
        return "Sms{" +
                "body='" + body + '\'' +
                ", date=" + date +
                ", type=" + type +
                ", _id=" + _id +
                ", address='" + address + '\'' +
                '}';
    }
}
