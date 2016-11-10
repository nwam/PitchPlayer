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
import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;
import java.util.ArrayList;

public class MediaLibraryBackend extends SQLiteOpenHelper {

	private static final boolean DEBUG = true;

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
	 * SQL Schema of `songs' table
	 */
	private static final String DATABASE_CREATE_SONGS = "CREATE TABLE "+ MediaLibrary.TABLE_SONGS + " ("
	  + MediaLibrary.SongColumns._ID          +" INTEGER PRIMARY KEY, "
	  + MediaLibrary.SongColumns.TITLE        +" TEXT NOT NULL, "
	  + MediaLibrary.SongColumns.TITLE_SORT   +" VARCHAR(64) NOT NULL, "
	  + MediaLibrary.SongColumns.SONG_NUMBER  +" INTEGER, "
	  + MediaLibrary.SongColumns.ALBUM_ID     +" INTEGER NOT NULL, "
	  + MediaLibrary.SongColumns.PLAYCOUNT    +" INTEGER NOT NULL DEFAULT 0, "
	  + MediaLibrary.SongColumns.SKIPCOUNT    +" INTEGER NOT NULL DEFAULT 0, "
	  + MediaLibrary.SongColumns.DURATION     +" INTEGER NOT NULL, "
	  + MediaLibrary.SongColumns.PATH         +" VARCHAR(4096) NOT NULL "
	  + ");";

	/**
	 * SQL Schema of `albums' table
	 */
	private static final String DATABASE_CREATE_ALBUMS = "CREATE TABLE "+ MediaLibrary.TABLE_ALBUMS + " ("
	  + MediaLibrary.AlbumColumns._ID               +" INTEGER PRIMARY KEY, "
	  + MediaLibrary.AlbumColumns.ALBUM             +" TEXT NOT NULL, "
	  + MediaLibrary.AlbumColumns.ALBUM_SORT        +" VARCHAR(64) NOT NULL, "
	  + MediaLibrary.AlbumColumns.SONG_COUNT        +" INTEGER, "
	  + MediaLibrary.AlbumColumns.DISC_NUMBER       +" INTEGER, "
	  + MediaLibrary.AlbumColumns.DISC_COUNT        +" INTEGER, "
	  + MediaLibrary.AlbumColumns.PRIMARY_ARTIST_ID +" INTEGER NOT NULL DEFAULT 0"
	  + ");";

	/**
	 * SQL Schema of `contributors' table
	 */
	private static final String DATABASE_CREATE_CONTRIBUTORS = "CREATE TABLE "+ MediaLibrary.TABLE_CONTRIBUTORS + " ("
	  + MediaLibrary.ContributorColumns._ID               +" INTEGER PRIMARY KEY, "
	  + MediaLibrary.ContributorColumns._CONTRIBUTOR      +" TEXT NOT NULL, "
	  + MediaLibrary.ContributorColumns._CONTRIBUTOR_SORT +" TEXT NOT NULL "
	  + ");";

	/**
	 * SQL Schema of 'contributors<->songs' table
	 */
	private static final String DATABASE_CREATE_CONTRIBUTORS_SONGS = "CREATE TABLE "+ MediaLibrary.TABLE_CONTRIBUTORS_SONGS+ " ("
	  + MediaLibrary.ContributorSongColumns.ROLE             +" INTEGER, "
	  + MediaLibrary.ContributorSongColumns._CONTRIBUTOR_ID  +" INTEGER, "
	  + MediaLibrary.ContributorSongColumns.SONG_ID          +" INTEGER, "
	  + "PRIMARY KEY("+MediaLibrary.ContributorSongColumns.ROLE+","
	                  +MediaLibrary.ContributorSongColumns._CONTRIBUTOR_ID+","
	                  +MediaLibrary.ContributorSongColumns.SONG_ID+") "
	  + ");"
	  + "CREATE INDEX idx_contributors_songs ON "+MediaLibrary.TABLE_CONTRIBUTORS_SONGS
	  +" ("+MediaLibrary.ContributorSongColumns.SONG_ID+", "+MediaLibrary.ContributorSongColumns.ROLE+")"
	  +";";

	/**
	 * SQL Schema of `genres' table
	 */
	private static final String DATABASE_CREATE_GENRES = "CREATE TABLE "+ MediaLibrary.TABLE_GENRES + " ("
	  + MediaLibrary.GenreColumns._ID         +" INTEGER PRIMARY KEY, "
	  + MediaLibrary.GenreColumns._GENRE      +" TEXT NOT NULL, "
	  + MediaLibrary.GenreColumns._GENRE_SORT +" TEXT NOT NULL "
	  + ");";

	/**
	 * SQL Schema of 'genres<->songs' table
	 */
	private static final String DATABASE_CREATE_GENRES_SONGS = "CREATE TABLE "+ MediaLibrary.TABLE_GENRES_SONGS+ " ("
	  + MediaLibrary.GenreSongColumns._GENRE_ID  +" INTEGER, "
	  + MediaLibrary.GenreSongColumns.SONG_ID    +" INTEGER, "
	  + "PRIMARY KEY("+MediaLibrary.GenreSongColumns._GENRE_ID+","
	                  +MediaLibrary.GenreSongColumns.SONG_ID+") "
	  + ");";

	/**
	 * Additional columns to select for artist info
	 */
	private static final String VIEW_ARTIST_SELECT = "_artist."+MediaLibrary.ContributorColumns._CONTRIBUTOR+" AS "+MediaLibrary.ContributorColumns.ARTIST
	                                               +",_artist."+MediaLibrary.ContributorColumns._CONTRIBUTOR_SORT+" AS "+MediaLibrary.ContributorColumns.ARTIST_SORT
	                                               +",_artist."+MediaLibrary.ContributorColumns._ID+" AS "+MediaLibrary.ContributorColumns.ARTIST_ID;

	/**
	 * View which includes song, album and artist information
	 */
	private static final String VIEW_CREATE_SONGS_ALBUMS_ARTISTS = "CREATE VIEW "+ MediaLibrary.VIEW_SONGS_ALBUMS_ARTISTS+ " AS "
	  + "SELECT *, " + VIEW_ARTIST_SELECT + " FROM " + MediaLibrary.TABLE_SONGS
	  +" LEFT JOIN "+MediaLibrary.TABLE_ALBUMS+" ON "+MediaLibrary.TABLE_SONGS+"."+MediaLibrary.SongColumns.ALBUM_ID+" = "+MediaLibrary.TABLE_ALBUMS+"."+MediaLibrary.AlbumColumns._ID
	  +" LEFT JOIN "+MediaLibrary.TABLE_CONTRIBUTORS_SONGS+" ON "+MediaLibrary.TABLE_CONTRIBUTORS_SONGS+"."+MediaLibrary.ContributorSongColumns.ROLE+"=0 "
	  +" AND "+MediaLibrary.TABLE_CONTRIBUTORS_SONGS+"."+MediaLibrary.ContributorSongColumns.SONG_ID+" = "+MediaLibrary.TABLE_SONGS+"."+MediaLibrary.SongColumns._ID
	  +" LEFT JOIN "+MediaLibrary.TABLE_CONTRIBUTORS+" AS _artist ON _artist."+MediaLibrary.ContributorColumns._ID+" = "+MediaLibrary.TABLE_CONTRIBUTORS_SONGS+"."+MediaLibrary.ContributorSongColumns._CONTRIBUTOR_ID
	  +" ;";

	/**
	 * View which includes album and artist information
	 */
	private static final String VIEW_CREATE_ALBUMS_ARTISTS = "CREATE VIEW "+ MediaLibrary.VIEW_ALBUMS_ARTISTS+ " AS "
	  + "SELECT *, " + VIEW_ARTIST_SELECT + " FROM " + MediaLibrary.TABLE_ALBUMS
	  +" LEFT JOIN "+MediaLibrary.TABLE_CONTRIBUTORS+" AS _artist"
	  +" ON _artist."+MediaLibrary.ContributorColumns._ID+" = "+MediaLibrary.TABLE_ALBUMS+"."+MediaLibrary.AlbumColumns.PRIMARY_ARTIST_ID
	  +" ;";

	/**
	 * View which includes artist information
	 */
	private static final String VIEW_CREATE_ARTISTS = "CREATE VIEW "+ MediaLibrary.VIEW_ARTISTS+ " AS "
	  + "SELECT *, " + VIEW_ARTIST_SELECT + " FROM "+MediaLibrary.TABLE_CONTRIBUTORS+" AS _artist WHERE "+MediaLibrary.ContributorColumns._ID+" IN "
	  +" (SELECT "+MediaLibrary.ContributorSongColumns._CONTRIBUTOR_ID+" FROM "+MediaLibrary.TABLE_CONTRIBUTORS_SONGS
	  +" WHERE "+MediaLibrary.ContributorSongColumns.ROLE+"=0 GROUP BY "+MediaLibrary.ContributorSongColumns._CONTRIBUTOR_ID+")"
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
		dbh.execSQL(DATABASE_CREATE_SONGS);
		dbh.execSQL(DATABASE_CREATE_ALBUMS);
		dbh.execSQL(DATABASE_CREATE_CONTRIBUTORS);
		dbh.execSQL(DATABASE_CREATE_CONTRIBUTORS_SONGS);
		dbh.execSQL(DATABASE_CREATE_GENRES);
		dbh.execSQL(DATABASE_CREATE_GENRES_SONGS);
		dbh.execSQL(VIEW_CREATE_SONGS_ALBUMS_ARTISTS);
		dbh.execSQL(VIEW_CREATE_ALBUMS_ARTISTS);
		dbh.execSQL(VIEW_CREATE_ARTISTS);
	}

	/**
	 * Called when the existing database
	 * schema is outdated
	 */
	@Override
	public void onUpgrade(SQLiteDatabase dbh, int oldVersion, int newVersion) {
	}

	/**
	 * Returns true if given song id is already present in the library
	 */
	public boolean isSongExisting(long id) {
		long count = DatabaseUtils.queryNumEntries(getReadableDatabase(), MediaLibrary.TABLE_SONGS, MediaLibrary.SongColumns._ID+"=?", new String[]{""+id});
		return count != 0;
	}

	/**
	 * Wrapper for SQLiteDatabase.insert() function
	 */
	public long insert (String table, String nullColumnHack, ContentValues values) {
		return getWritableDatabase().insert(table, nullColumnHack, values);
	}

	/**
	 * Wrappr for SQLiteDatabase.query() function
	 */
	public Cursor query (boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {

		if (MediaLibrary.VIEW_SONGS_ALBUMS_ARTISTS.equals(table) && selection.matches("^artist_id=\\d+$")) {
			selection = "_id IN( SELECT song_id FROM contributors_songs WHERE contributors_songs.role=0 AND contributors_songs._contributor_id="+selection.substring(10)+")";
			Log.v("VanillaMusic", "++ FIXME: HACKY SQL OPTIMIZED");
		}


		if (DEBUG)
			debugQuery(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);

		return getReadableDatabase().query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}


	private void debugQuery(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		final String LT = "VanillaMusicSQL";
		Log.v(LT, "---- start query ---");
		Log.v(LT, "SELECT");
		for (String c : columns) {
			Log.v(LT, "   "+c);
		}
		Log.v(LT, "FROM "+table+" WHERE "+selection+" ");
		if (selectionArgs != null) {
			Log.v(LT, " /* with args: ");
			for (String a : selectionArgs) {
				Log.v(LT, a+", ");
			}
			Log.v(LT, " */");
		}
		Log.v(LT, " GROUP BY "+groupBy+" HAVING "+having+" ORDER BY "+orderBy+" LIMIT "+limit);

		Log.v(LT, "DBH = "+getReadableDatabase());

		Cursor dryRun = getReadableDatabase().query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
		long results = 0;
		long startAt = System.currentTimeMillis();
		if (dryRun != null) {
			while(dryRun.moveToNext()) {
				results++;
			}
		}
		dryRun.close();
		long tookMs = System.currentTimeMillis() - startAt;
		Log.v(LT, "--- finished in "+tookMs+" ms with count="+results);
	}

}
