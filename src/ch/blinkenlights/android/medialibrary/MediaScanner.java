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

import ch.blinkenlights.bastp.Bastp;
import android.content.ContentValues;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Vector;

public class MediaScanner  {

	public static void scanSingleDirectory(MediaLibraryBackend backend, String dirpath) {
		File dir = new File(dirpath);
		if (dir.isDirectory() == false)
			return;

		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				Log.v("VanillaMusic", "+ "+file);
				addSingleFile(backend, file);
			}
		}
	}

	public static void addSingleFile(MediaLibraryBackend backend, File file) {
		String path     = file.getAbsolutePath();
		HashMap tags    = (new Bastp()).getTags(path);

		String title = (tags.containsKey("TITLE") ? (String)((Vector)tags.get("TITLE")).get(0) : "Untitled");
		String album = (tags.containsKey("ALBUM") ? (String)((Vector)tags.get("ALBUM")).get(0) : "No Album");
		String artist = (tags.containsKey("ARTIST") ? (String)((Vector)tags.get("ARTIST")).get(0) : "Unknown Artist");

		long trackId = hash63(path);
		long albumId = hash63(album);
		long artistId = hash63(artist);

		ContentValues v = new ContentValues();
		v.put(MediaLibrary.TrackColumns._ID,        trackId);
		v.put(MediaLibrary.TrackColumns.TITLE,      title);
		v.put(MediaLibrary.TrackColumns.TITLE_SORT, title);
		v.put(MediaLibrary.TrackColumns.ALBUM_ID,   albumId);
		v.put(MediaLibrary.TrackColumns.PATH,       path);
		backend.insert(MediaLibrary.TABLE_TRACKS, null, v);

		v.clear();
		v.put(MediaLibrary.AlbumColumns._ID,            albumId);
		v.put(MediaLibrary.AlbumColumns.ALBUM,          album);
		v.put(MediaLibrary.AlbumColumns.ALBUM_SORT,     album);
		v.put(MediaLibrary.AlbumColumns.CONTRIBUTOR_ID, artistId);
		backend.insert(MediaLibrary.TABLE_ALBUMS, null, v);

		v.clear();
		v.put(MediaLibrary.ContributorColumns._ID,              artistId);
		v.put(MediaLibrary.ContributorColumns.CONTRIBUTOR,      artist);
		v.put(MediaLibrary.ContributorColumns.CONTRIBUTOR_SORT, artist);
		backend.insert(MediaLibrary.TABLE_CONTRIBUTORS, null, v);

		v.clear();
		v.put(MediaLibrary.ContributorTrackColumns.CONTRIBUTOR_ID, artistId);
		v.put(MediaLibrary.ContributorTrackColumns.TRACK_ID,       trackId);
		v.put(MediaLibrary.ContributorTrackColumns.ROLE,           0);
		backend.insert(MediaLibrary.TABLE_CONTRIBUTORS_TRACKS, null, v);
		Log.v("VanillaMusic", "Inserted "+path);
	}


	/**
	 * Simple 63 bit hash function for strings
	 */
	private static long hash63(String str) {
		long hash = 0;
		int len = str.length();
		for (int i = 0; i < len ; i++) {
			hash = 31*hash + str.charAt(i);
		}
		return (hash < 0 ? hash*-1 : hash);
	}


}

