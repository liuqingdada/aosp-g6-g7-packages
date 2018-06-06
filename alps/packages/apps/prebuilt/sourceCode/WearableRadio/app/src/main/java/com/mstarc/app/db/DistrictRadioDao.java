package com.mstarc.app.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.mstarc.app.radio.Radio;
import java.util.ArrayList;
import java.util.List;

public class DistrictRadioDao
{
	private DBOpenHelper helper;
	private SQLiteDatabase db;

	public DistrictRadioDao(Context context)
	{
		helper = new DBOpenHelper(context);
	}

	public void insert(String radioName, int radioId)
	{
		db = helper.getWritableDatabase();
		String sql = "INSERT INTO districtradio(radio_id,radio_name) VALUES(" + radioId + ",'" + radioName + "')";
		db.execSQL(sql);
		db.close();
	}

	public void dropTableDistrictRadio()
	{
		db = helper.getWritableDatabase();
		String dropSql = "drop table districtradio";
		String createSql = "CREATE TABLE IF NOT EXISTS districtradio(id INTEGER PRIMARY KEY AUTOINCREMENT, radio_id INTEGER,radio_name varchar(50))";
		db.execSQL(dropSql);
		db.execSQL(createSql);
		db.close();
	}

	/**
	 * 数据上传成功就删除
	 */
	public void delete()
	{
		db = helper.getWritableDatabase();
		String sql = "delete from districtradio";
		db.execSQL(sql);
		db.close();
	}

	public List<Radio> getDIstrictRadioList()
	{
		List<Radio> districtRadioList = new ArrayList<Radio>();
		db = helper.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT * from districtradio", null);
		if (cursor == null || cursor.getCount() == 0)
			return districtRadioList;
		if (cursor.moveToFirst())
		{
			do
			{
				Radio radio = new Radio(cursor.getInt(cursor.getColumnIndex("radio_id")), cursor.getString(cursor.getColumnIndex("radio_name")));
				districtRadioList.add(radio);
			} while (cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return districtRadioList;
	}
}
