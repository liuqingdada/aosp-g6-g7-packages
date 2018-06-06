package com.mstarc.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 存放地方台
 */
public class DBOpenHelper extends SQLiteOpenHelper
{
	public static int VERSION = 5;
	private static final String DB_NAME = "district_radio.db";
	public DBOpenHelper(Context context)
	{
		super(context, DB_NAME, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE IF NOT EXISTS districtradio(id INTEGER PRIMARY KEY AUTOINCREMENT, radio_id INTEGER,radio_name varchar(50))");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("drop table districtradio");
		db.execSQL("CREATE TABLE IF NOT EXISTS districtradio(id INTEGER PRIMARY KEY AUTOINCREMENT, radio_id INTEGER,radio_name varchar(50))");
	}
}
