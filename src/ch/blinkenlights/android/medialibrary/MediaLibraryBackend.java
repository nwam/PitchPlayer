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
	private static final String DATABASE_CREATE_TRACKS = "CREATE TABLE "+ MediaLibrary.TABLE_TRACKS + " ("
	  + MediaLibrary.TrackColumns._ID        +" INTEGER PRIMARY KEY, "
	  + MediaLibrary.TrackColumns.TITLE      +" TEXT NOT NULL, "
	  + MediaLibrary.TrackColumns.TITLE_SORT +" VARCHAR(64) NOT NULL, "
	  + MediaLibrary.TrackColumns.ALBUM_ID   +" INTEGER NOT NULL, "
	  + MediaLibrary.TrackColumns.PLAYCOUNT  +" INTEGER NOT NULL DEFAULT 0, "
	  + MediaLibrary.TrackColumns.SKIPCOUNT  +" INTEGER NOT NULL DEFAULT 0, "
	  + MediaLibrary.TrackColumns.PATH       +" VARCHAR(4096) NOT NULL "
	  + ");";

	/**
	 * SQL Schema of `albums' table
	 */
	private static final String DATABASE_CREATE_ALBUMS = "CREATE TABLE "+ MediaLibrary.TABLE_ALBUMS + " ("
	  + MediaLibrary.AlbumColumns._ID            +" INTEGER PRIMARY KEY, "
	  + MediaLibrary.AlbumColumns.ALBUM          +" TEXT NOT NULL, "
	  + MediaLibrary.AlbumColumns.ALBUM_SORT     +" VARCHAR(64) NOT NULL, "
	  + MediaLibrary.AlbumColumns.DISC_NUMBER    +" INTEGER, "
	  + MediaLibrary.AlbumColumns.DISC_COUNT     +" INTEGER, "
	  + MediaLibrary.AlbumColumns.CONTRIBUTOR_ID +" INTEGER NOT NULL DEFAULT 0"
	  + ");";

	/**
	 * SQL Schema of `contributors' table
	 */
	private static final String DATABASE_CREATE_CONTRIBUTORS = "CREATE TABLE "+ MediaLibrary.TABLE_CONTRIBUTORS + " ("
	  + MediaLibrary.ContributorColumns._ID              +" INTEGER PRIMARY KEY, "
	  + MediaLibrary.ContributorColumns.CONTRIBUTOR      +" TEXT NOT NULL, "
	  + MediaLibrary.ContributorColumns.CONTRIBUTOR_SORT +" TEXT NOT NULL "
	  + ");";

	/**
	 * SQL Schema of 'contributors' table
	 */
	private static final String DATABASE_CREATE_CONTRIBUTORS_TRACKS = "CREATE TABLE "+ MediaLibrary.TABLE_CONTRIBUTORS_TRACKS+ " ("
	  + MediaLibrary.ContributorTrackColumns.ROLE           +" INTEGER, "
	  + MediaLibrary.ContributorTrackColumns.CONTRIBUTOR_ID +" INTEGER, "
	  + MediaLibrary.ContributorTrackColumns.TRACK_ID       +" INTEGER, "
	  + "PRIMARY KEY("+MediaLibrary.ContributorTrackColumns.ROLE+","
	                  +MediaLibrary.ContributorTrackColumns.CONTRIBUTOR_ID+","
	                  +MediaLibrary.ContributorTrackColumns.TRACK_ID+") "
	  + ");";

	/**
	 * View which includes track, album and artist information
	 */
	private static final String VIEW_CREATE_TRACKS_ALBUMS_ARTISTS = "CREATE VIEW "+ MediaLibrary.VIEW_TRACKS_ALBUMS_ARTISTS+ " AS "
	  + "SELECT * FROM "+MediaLibrary.TABLE_TRACKS
	  +" LEFT JOIN "+MediaLibrary.TABLE_ALBUMS+" ON "+MediaLibrary.TABLE_TRACKS+"."+MediaLibrary.TrackColumns._ID+" = "+MediaLibrary.TABLE_ALBUMS+"."+MediaLibrary.AlbumColumns._ID
	  +" LEFT JOIN "+MediaLibrary.TABLE_CONTRIBUTORS_TRACKS+" ON "+MediaLibrary.TABLE_CONTRIBUTORS_TRACKS+"."+MediaLibrary.ContributorTrackColumns.ROLE+"=0 "
	  +" AND "+MediaLibrary.TABLE_CONTRIBUTORS_TRACKS+"."+MediaLibrary.ContributorTrackColumns.TRACK_ID+" = "+MediaLibrary.TABLE_TRACKS+"."+MediaLibrary.TrackColumns._ID
	  +" LEFT JOIN "+MediaLibrary.TABLE_CONTRIBUTORS+" ON "+MediaLibrary.TABLE_CONTRIBUTORS+"."+MediaLibrary.ContributorColumns._ID+" = "+MediaLibrary.TABLE_CONTRIBUTORS_TRACKS+"."+MediaLibrary.ContributorTrackColumns.CONTRIBUTOR_ID
	  +" ;";

	/**
	 * View which includes album and artist information
	 */
	private static final String VIEW_CREATE_ALBUMS_ARTISTS = "CREATE VIEW "+ MediaLibrary.VIEW_ALBUMS_ARTISTS+ " AS "
	  + "SELECT * FROM "+MediaLibrary.TABLE_ALBUMS
	  +" LEFT JOIN "+MediaLibrary.TABLE_CONTRIBUTORS
	  +" ON "+MediaLibrary.TABLE_CONTRIBUTORS+"."+MediaLibrary.ContributorColumns._ID+" = "+MediaLibrary.TABLE_ALBUMS+"."+MediaLibrary.AlbumColumns.CONTRIBUTOR_ID
	  +" ;";


	/**
	* @desc Constructor for the MediaLibraryBackend helper
	*/
	public MediaLibraryBackend(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * Called when database does not exist
	 */
	@Override
	public void onCreate(SQLiteDatabase dbh) {
		dbh.execSQL(DATABASE_CREATE_TRACKS);
		dbh.execSQL(DATABASE_CREATE_ALBUMS);
		dbh.execSQL(DATABASE_CREATE_CONTRIBUTORS);
		dbh.execSQL(DATABASE_CREATE_CONTRIBUTORS_TRACKS);
		dbh.execSQL(VIEW_CREATE_TRACKS_ALBUMS_ARTISTS);
		dbh.execSQL(VIEW_CREATE_ALBUMS_ARTISTS);
		//dbh.execSQL(INDEX_UNIQUE_CREATE);
		//dbh.execSQL(INDEX_TYPE_CREATE);
	}

	/**
	 * Called when the existing database
	 * schema is outdated
	 */
	@Override
	public void onUpgrade(SQLiteDatabase dbh, int oldVersion, int newVersion) {
	}


	/**
	 * Wrappr for SQLiteDatabase.query() function
	 */
	public Cursor query (boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return getReadableDatabase().query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	/**
	 * Populate database with fake data
	 */
	protected void pushDebugData() {
		SQLiteDatabase dbh = getWritableDatabase();
		dbh.execSQL("INSERT INTO contributors VALUES(NULL, 'Hans Wurst', 'Hans Wurst');");

		for(int i=0; i<64;i++) {
			dbh.execSQL("INSERT INTO "+MediaLibrary.TABLE_TRACKS+" VALUES(NULL, 'foobar"+i+"', 'fbx', 2, 0, 0, '/example/song');");
			dbh.execSQL("INSERT INTO "+MediaLibrary.TABLE_ALBUMS+" VALUES(NULL, 'album "+i+"', 'abx', 0, 0, 1);");
			dbh.execSQL("INSERT INTO "+MediaLibrary.TABLE_CONTRIBUTORS_TRACKS+" VALUES(0, 1, "+i+");");
			Log.v(TAG, "Inserting fake song "+i);
		}
		dbh.close();
	}
}
