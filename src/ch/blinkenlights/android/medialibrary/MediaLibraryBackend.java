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

package ch.blinkenlights.android.medialibrary;

import android.content.Context;
import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;

public class MediaLibraryBackend extends SQLiteOpenHelper {

	/**
	 * SQL constants and CREATE TABLE statements used by
	 * this java class
	 */
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "media-library.db";

	/**
	 * The tag to use for log messages
	 */
	private static final String TAG = "VanillaMediaLibraryBackend";

	/**
	 * SQL Schema of `tracks' table
	 */
	private static final String DATABASE_CREATE = "CREATE TABLE "+ MediaLibrary.TRACKS_TABLE + " ("
	  + MediaLibrary.TrackColumns.TRACK_ID   +" INTEGER PRIMARY KEY, "
	  + MediaLibrary.TrackColumns.LABEL      +" TEXT NOT NULL, "
	  + MediaLibrary.TrackColumns.LABEL_SORT +" VARCHAR(64) NOT NULL, "
	  + MediaLibrary.TrackColumns.ALBUM_ID   +" INTEGER NOT NULL, "
	  + MediaLibrary.TrackColumns.PLAYCOUNT  +" INTEGER NOT NULL DEFAULT 0, "
	  + MediaLibrary.TrackColumns.SKIPCOUNT  +" INTEGER NOT NULL DEFAULT 0, "
	  + MediaLibrary.TrackColumns.PATH       +" VARCHAR(4096) NOT NULL "
	  + ");";

	/**
	* @desc Constructor for the MediaLibraryBackend helper
	*/
	public MediaLibraryBackend(Context context) {
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

	/**
	 * Populate database with fake data
	 */
	protected void pushDebugData() {
		SQLiteDatabase dbh = getWritableDatabase();
		for(int i=0; i<64;i++) {
			dbh.execSQL("INSERT INTO "+MediaLibrary.TRACKS_TABLE+" VALUES(NULL, 'foobar"+i+"', 'fbx', 0, 0, 0, '/example/song');");
			Log.v(TAG, "Inserting fake song "+i);
		}
		dbh.close();
	}
}
