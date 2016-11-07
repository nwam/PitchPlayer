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
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Vector;

public class MediaScanner  {


	public static void scanSingleDirectory(MediaLibraryBackend backend, File dir) {
		if (dir.isDirectory() == false)
			return;

		File[] dirents = dir.listFiles();
		if (dirents == null)
			return;

		for (File file : dirents) {
			if (file.isFile()) {
				Log.v("VanillaMusic", "MediaScanner: inspecting file "+file);
				addSingleFile(backend, file);
			}
			else if (file.isDirectory()) {
				Log.v("VanillaMusic", "MediaScanner: scanning subdir "+file);
				scanSingleDirectory(backend, file);
			}
		}
	}

	public static void addSingleFile(MediaLibraryBackend backend, File file) {
		String path  = file.getAbsolutePath();
		long songId  = hash63(path);

		HashMap tags = (new Bastp()).getTags(path);
		if (tags.containsKey("type") == false)
			return; // no tags found

Log.v("VanillaMusic", "> Found mime "+((String)tags.get("type")));

		if (backend.isSongExisting(songId)) {
			Log.v("VanillaMusic", "Skipping already known song with id "+songId);
			return;
		}

		MediaMetadataRetriever data = new MediaMetadataRetriever();
		try {
			data.setDataSource(path);
		} catch (Exception e) {
				Log.w("VanillaMusic", "Failed to extract metadata from " + path);
		}

		String duration = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		if (duration == null)
			return; // not a supported media file!


		String title = (tags.containsKey("TITLE") ? (String)((Vector)tags.get("TITLE")).get(0) : "Untitled");
		String album = (tags.containsKey("ALBUM") ? (String)((Vector)tags.get("ALBUM")).get(0) : "No Album");
		String artist = (tags.containsKey("ARTIST") ? (String)((Vector)tags.get("ARTIST")).get(0) : "Unknown Artist");

		String composer = "Composer and "+artist;

		long albumId = hash63(album);
		long artistId = hash63(artist);
		long composerId = hash63(composer);

		ContentValues v = new ContentValues();
		v.put(MediaLibrary.SongColumns._ID,        songId);
		v.put(MediaLibrary.SongColumns.TITLE,      title);
		v.put(MediaLibrary.SongColumns.TITLE_SORT, title);
		v.put(MediaLibrary.SongColumns.ALBUM_ID,   albumId);
		v.put(MediaLibrary.SongColumns.DURATION,   duration);
		v.put(MediaLibrary.SongColumns.PATH,       path);
		backend.insert(MediaLibrary.TABLE_SONGS, null, v);

		v.clear();
		v.put(MediaLibrary.AlbumColumns._ID,            albumId);
		v.put(MediaLibrary.AlbumColumns.ALBUM,          album);
		v.put(MediaLibrary.AlbumColumns.ALBUM_SORT,     album);
		v.put(MediaLibrary.AlbumColumns.PRIMARY_ARTIST_ID, artistId);
		backend.insert(MediaLibrary.TABLE_ALBUMS, null, v);

		v.clear();
		v.put(MediaLibrary.ContributorColumns._ID,              artistId);
		v.put(MediaLibrary.ContributorColumns._CONTRIBUTOR,      artist);
		v.put(MediaLibrary.ContributorColumns._CONTRIBUTOR_SORT, artist);
		backend.insert(MediaLibrary.TABLE_CONTRIBUTORS, null, v);

		v.clear();
		v.put(MediaLibrary.ContributorSongColumns._CONTRIBUTOR_ID, artistId);
		v.put(MediaLibrary.ContributorSongColumns.SONG_ID,       songId);
		v.put(MediaLibrary.ContributorSongColumns.ROLE,           0);
		backend.insert(MediaLibrary.TABLE_CONTRIBUTORS_SONGS, null, v);

		v.clear();
		v.put(MediaLibrary.ContributorColumns._ID,              composerId);
		v.put(MediaLibrary.ContributorColumns._CONTRIBUTOR,      composer);
		v.put(MediaLibrary.ContributorColumns._CONTRIBUTOR_SORT, composer);
		backend.insert(MediaLibrary.TABLE_CONTRIBUTORS, null, v);

		v.clear();
		v.put(MediaLibrary.ContributorSongColumns._CONTRIBUTOR_ID, composerId);
		v.put(MediaLibrary.ContributorSongColumns.SONG_ID,       songId);
		v.put(MediaLibrary.ContributorSongColumns.ROLE,           1);
		backend.insert(MediaLibrary.TABLE_CONTRIBUTORS_SONGS, null, v);
		Log.v("VanillaMusic", "MediaScanner: inserted "+path);
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

