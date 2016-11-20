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
import android.content.ContentValues;
import android.database.Cursor;
import android.database.ContentObserver;
import android.provider.MediaStore;


import java.io.File;
public class MediaLibrary  {

	public static final String TABLE_SONGS                = "songs";
	public static final String TABLE_ALBUMS               = "albums";
	public static final String TABLE_CONTRIBUTORS         = "contributors";
	public static final String TABLE_CONTRIBUTORS_SONGS   = "contributors_songs";
	public static final String TABLE_GENRES               = "genres";
	public static final String TABLE_GENRES_SONGS         = "genres_songs";
	public static final String TABLE_PLAYLISTS            = "playlists";
	public static final String TABLE_PLAYLISTS_SONGS      = "playlists_songs";
	public static final String VIEW_ARTISTS               = "_artists";
	public static final String VIEW_ALBUMS_ARTISTS        = "_albums_artists";
	public static final String VIEW_SONGS_ALBUMS_ARTISTS  = "_songs_albums_artists";

	private static MediaLibraryBackend sBackend;

	private static MediaScanner sScanner;

	private static MediaLibraryBackend getBackend(Context context) {
		if (sBackend == null) {
			// -> unlikely
//			synchronized(sLock) {
				if (sBackend == null) {
					sBackend = new MediaLibraryBackend(context);

					sScanner = new MediaScanner(sBackend);
					File dir = new File("/storage");
					sScanner.startScan(dir);
				}
//			}
		}
		return sBackend;
	}


	/**
	 * Perform a media query on the database, returns a cursor
	 *
	 * @param context the context to use
	 * @param table the table to query, one of MediaLibrary.TABLE_*
	 * @param projection the columns to returns in this query
	 * @param selection the selection (WHERE) to use
	 * @param selectionArgs arguments for the selection
	 * @param orderBy how the result should be sorted
	 */
	public static Cursor queryLibrary(Context context, String table, String[] projection, String selection, String[] selectionArgs, String orderBy) {
		return getBackend(context).query(false, table, projection, selection, selectionArgs, null, null, orderBy, null);
	}

	/**
	 * Removes a single song from the database
	 *
	 * @param context the context to use
	 * @param id the song id to delete
	 */
	public static void removeSong(Context context, long id) {
		getBackend(context).delete(TABLE_SONGS, SongColumns._ID+"="+id, null);
	}

	/**
	 * Updates the play or skipcount of a song
	 *
	 * @param context the context to use
	 * @param id the song id to update
	 * @param boolean true if song was played, false if skipped
	 */
	public static void updateSongPlayCounts(Context context, long id, boolean played) {
		final String column = played ? MediaLibrary.SongColumns.PLAYCOUNT : MediaLibrary.SongColumns.SKIPCOUNT;
		ContentValues v = new ContentValues();
		v.put(column, column+" + 1");
		getBackend(context).update(MediaLibrary.TABLE_SONGS, v, MediaLibrary.SongColumns._ID+"="+id, null, false);
	}

	/**
	 * Registers a new content observer for the media library
	 *
	 * @param context the context to use
	 * @param observer the content observer we are going to call on changes
	 */
	public static void registerContentObserver(Context context, ContentObserver observer) {
		getBackend(context).registerContentObserver(observer);
	}

	/**
	 * Returns true if we are currently scanning for media
	 */
	public static boolean isScannerRunning(Context context) {
		// FIXME: IMPLEMENT THIS
		return false;
	}

	/**
	 * Returns the 'key' of given string used for sorting and searching
	 *
	 * @param name the string to convert
	 * @return the the key of given name
	 */
	public static String keyFor(String name) {
		return MediaStore.Audio.keyFor(name);
	}

	// Columns of Song entries
	public interface SongColumns {
		/**
		 * The id of this song in the database
		 */
		public static final String _ID = "_id";
		/**
		 * The title of this song
		 */
		public static final String TITLE = "title";
		/**
		 * The sortable title of this song
		 */
		public static final String TITLE_SORT = "title_sort";
		/**
		 * The position in the album of this song
		 */
		public static final String SONG_NUMBER = "song_num";
		/**
		 * The album where this song belongs to
		 */
		public static final String ALBUM_ID = "album_id";
		/**
		 * How often the song was played
		 */
		public static final String PLAYCOUNT = "playcount";
		/**
		 * How often the song was skipped
		 */
		public static final String SKIPCOUNT = "skipcount";
		/**
		 * The duration of this song
		 */
		public static final String DURATION = "duration";
		/**
		 * The path to the music file
		 */
		public static final String PATH = "path";
		/**
		 * The mtime of this item
		 */
		public static final String MTIME = "mtime";
	}

	// Columns of Album entries
	public interface AlbumColumns {
		/**
		 * The id of this album in the database
		 */
		public static final String _ID = SongColumns._ID;
		/**
		 * The title of this album
		 */
		public static final String ALBUM = "album";
		/**
		 * The sortable title of this album
		 */
		public static final String ALBUM_SORT = "album_sort";
		/**
		 * How many songs are on this album
		 */
		public static final String SONG_COUNT = "song_count";
		/**
		 * The disc number of this album
		 */
		public static final String DISC_NUMBER = "disc_num";
		/**
		 * The total amount of discs
		 */
		public static final String DISC_COUNT = "disc_count";
		/**
		 * The primary contributor / artist reference for this album
		 */
		public static final String PRIMARY_ARTIST_ID = "primary_artist_id";
		/**
		 * The year of this album
		 */
		public static final String YEAR = "year";
		/**
		 * The mtime of this item
		 */
		public static final String MTIME = "mtime";
	}

	// Columns of Contributors entries
	public interface ContributorColumns {
		/**
		 * The id of this contributor
		 */
		public static final String _ID = SongColumns._ID;
		/**
		 * The name of this contributor
		 */
		public static final String _CONTRIBUTOR = "_contributor";
		/**
		 * The sortable title of this contributor
		 */
		public static final String _CONTRIBUTOR_SORT = "_contributor_sort";
		/**
		 * The mtime of this item
		 */
		public static final String MTIME = "mtime";
		/**
		 * ONLY IN VIEWS - the artist
		 */
		public static final String ARTIST = "artist";
		/**
		 * ONLY IN VIEWS - the artist_sort key
		 */
		public static final String ARTIST_SORT = "artist_sort";
		/**
		 * ONLY IN VIEWS - the artist id
		 */
		public static final String ARTIST_ID = "artist_id";
	}

	// Songs <-> Contributor mapping
	public interface ContributorSongColumns {
		/**
		 * The role of this entry
		 */
		public static final String ROLE = "role";
		/**
		 * the contirbutor id this maps to
		 */
		public static final String _CONTRIBUTOR_ID = "_contributor_id";
		/**
		 * the song this maps to
		 */
		public static final String SONG_ID = "song_id";
	}

	// Columns of Genres entries
	public interface GenreColumns {
		/**
		 * The id of this genre
		 */
		public static final String _ID = SongColumns._ID;
		/**
		 * The name of this genre
		 */
		public static final String _GENRE = "_genre";
		/**
		 * The sortable title of this genre
		 */
		public static final String _GENRE_SORT = "_genre_sort";
	}

	// Songs <-> Contributor mapping
	public interface GenreSongColumns {
		/**
		 * the genre id this maps to
		 */
		public static final String _GENRE_ID = "_genre_id";
		/**
		 * the song this maps to
		 */
		public static final String SONG_ID = "song_id";
	}

	// Playlists
	public interface PlaylistColumns {
		/**
		 * The id of this playlist
		 */
		public static final String _ID = SongColumns._ID;
		/**
		 * The name of this playlist
		 */
		 public static final String NAME = "name";
	}

	// Song <-> Playlist mapping
	public interface PlaylistSongColumns {
		/**
		 * The ID of this entry
		 */
		public static final String _ID = SongColumns._ID;
		/**
		 * The playlist this entry belongs to
		 */
		public static final String PLAYLIST_ID = "playlist_id";
		/**
		 * The song this entry references to
		 */
		public static final String SONG_ID = "song_id";
		/**
		 * The order attribute
		 */
		public static final String POSITION = "position";
	}

}
