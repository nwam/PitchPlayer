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
import android.database.Cursor;

public class MediaLibrary  {

	public static final String TABLE_TRACKS               = "tracks";
	public static final String TABLE_ALBUMS               = "albums";
	public static final String TABLE_CONTRIBUTORS         = "contributors";
	public static final String TABLE_CONTRIBUTORS_TRACKS  = "contributors_tracks";
	public static final String VIEW_TRACKS_ALBUMS_ARTISTS = "_tracks_albums_artists";
	public static final String VIEW_ALBUMS_ARTISTS        = "_albums_artists";

	private static MediaLibraryBackend sBackend;

	private static MediaLibraryBackend getBackend(Context context) {
		if (sBackend == null) {
			// -> unlikely
//			synchronized(sLock) {
				if (sBackend == null)
					sBackend = new MediaLibraryBackend(context);
					sBackend.pushDebugData();
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
	 * Returns true if there are no songs in the database
	 */
	public static boolean isLibraryEmpty(Context context) {
		// FIXME: IMPLEMENT THIS
		return true;
	}

	/**
	 * Returns true if we are currently scanning for media
	 */
	public static boolean isScannerRunning(Context context) {
		// FIXME: IMPLEMENT THIS
		return false;
	}

	// Columns of Track entries
	public interface TrackColumns {
		/**
		 * The id of this track in the database
		 */
		public static final String _ID = "_id";
		/**
		 * The title of this track
		 */
		public static final String TITLE = "title";
		/**
		 * The sortable title of this track
		 */
		public static final String TITLE_SORT = "title_sort";
		/**
		 * The album where this track belongs to
		 */
		public static final String ALBUM_ID = "album_id";
		/**
		 * How often the track was played
		 */
		public static final String PLAYCOUNT = "playcount";
		/**
		 * How often the track was skipped
		 */
		public static final String SKIPCOUNT = "skipcount";
		/**
		 * The path to the music file
		 */
		public static final String PATH = "path";
	}

	// Columns of Album entries
	public interface AlbumColumns {
		/**
		 * The id of this album in the database
		 */
		public static final String _ID = TrackColumns._ID;
		/**
		 * The title of this album
		 */
		public static final String ALBUM = "album";
		/**
		 * The sortable title of this album
		 */
		public static final String ALBUM_SORT = "album_sort";
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
		public static final String CONTRIBUTOR_ID = "contributor_id";
	}

	// Columns of Contributors entries
	public interface ContributorColumns {
		/**
		 * The id of this contributor
		 */
		public static final String _ID = TrackColumns._ID;
		/**
		 * The name of this contributor
		 */
		public static final String CONTRIBUTOR = "contributor";
		/**
		 * The sortable title of this contributor
		 */
		public static final String CONTRIBUTOR_SORT = "contributor_sort";
	}

	// Tracks <-> Contributor mapping
	public interface ContributorTrackColumns {
		/**
		 * The role of this entry
		 */
		public static final String ROLE = "role";
		/**
		 * the contirbutor id this maps to
		 */
		public static final String CONTRIBUTOR_ID = "contributor_id";
		/**
		 * the track this maps to
		 */
		public static final String TRACK_ID = "track_id";
	}
}
