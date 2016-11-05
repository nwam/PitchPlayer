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

	public static final String TRACKS_TABLE = "tracks";
	public static final String ALBUMS_TABLE = "albums";

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
	 * @param table the table to query, one of MediaLibrary.*_TABLE
	 * @param projection the columns to returns in this query
	 * @param selection the selection (WHERE) to use
	 * @param selectionArgs arguments for the selection
	 * @param sortOrder how the result should be sorted
	 */
	public static Cursor queryLibrary(Context context, String table, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MediaLibraryBackend be = getBackend(context);
		return null;
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
		public static final String _ID = "id";
		/**
		 * The title of this track
		 */
		public static final String LABEL = "label";
		/**
		 * The sortable title of this track
		 */
		public static final String LABEL_SORT = "label_sort";
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
		public static final String LABEL = TrackColumns.LABEL;
		/**
		 * The sortable title of this album
		 */
		public static final String LABEL_SORT = TrackColumns.LABEL_SORT;
		/**
		 * The disc number of this album
		 */
		public static final String DISC_NUMBER = "disc_num";
		/**
		 * The total amount of discs
		 */
		public static final String DISC_COUNT = "disc_count";
		/**
		 * The contributor / artist reference for this album
		 */
		public static final String CONTRIBUTOR = "contributor";
	}

}
