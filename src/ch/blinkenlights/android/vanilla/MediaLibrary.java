/*
 * Copyright (C) 2016 Adrian Ulrich <adrian@blinkenlights.ch>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>. 
 */

package ch.blinkenlights.android.vanilla;

import android.content.Context;
import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;

public class MediaLibrary extends SQLiteOpenHelper {

	/**
	 * SQL constants and CREATE TABLE statements used by
	 * this java class
	 */
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "media-library.db";
	private static final String TABLE_TRACKS = "tracks";
	private static final String DATABASE_CREATE = "CREATE TABLE "+TABLE_TRACKS + " ("
	  + "`id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT, "
	  + "`label` TEXT, "
	  + "`label_sort` VARCHAR(64) "
	  + ";";

	/**
	* @desc Constructor for the playcounts helper
	*/
	public MediaLibrary(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * @desc Called when database does not exist
	 */
	@Override
	public void onCreate(SQLiteDatabase dbh) {
		dbh.execSQL(DATABASE_CREATE);
		//dbh.execSQL(INDEX_UNIQUE_CREATE);
		//dbh.execSQL(INDEX_TYPE_CREATE);
	}

	/**
	 * @desc Called when the existing database
	 * schema is outdated
	 */
	@Override
	public void onUpgrade(SQLiteDatabase dbh, int oldVersion, int newVersion) {
	}

}
