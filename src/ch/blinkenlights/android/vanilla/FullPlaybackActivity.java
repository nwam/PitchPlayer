/*
 * Copyright (C) 2012 Christopher Eby <kreed@kreed.org>
 * Copyright (C) 2016 Adrian Ulrich <adrian@blinkenlights.ch>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ch.blinkenlights.android.vanilla;

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * The primary playback screen with playback controls and large cover display.
 */
public class FullPlaybackActivity extends SlidingPlaybackActivity
	implements View.OnLongClickListener,
		       CheckBox.OnCheckedChangeListener,
			   NumberPicker.OnValueChangeListener,
		       NumberPicker.Formatter
{
	public static final int DISPLAY_INFO_OVERLAP = 0;
	public static final int DISPLAY_INFO_BELOW = 1;
	public static final int DISPLAY_INFO_WIDGETS = 2;

	private TextView mOverlayText;
	private View mControlsTop;

	private TableLayout mInfoTable;
	private TextView mQueuePosView;

	private TextView mTitle;
	private TextView mAlbum;
	private TextView mArtist;

	private SeekBar mPitchBar;
	private NumberPicker mPitchPicker;
	private TextView mPitchText;
	private Button mPitchButton;
	private SeekBar mSpeedBar;
	private TextView mSpeedText;
	private Button mSpeedButton;
	private static final int MIN_SEMITONES = -24;
	private static final int MAX_SEMITONES = 24;

	private CheckBox mLooperCheckbox;
	private Button mLooperStartButton;
	private Button mLooperEndButton;
	private NumberPicker mLooperStartPicker;
	private NumberPicker mLooperEndPicker;

	/**
	 * True if the controls are visible (play, next, seek bar, etc).
	 */
	private boolean mControlsVisible;
	/**
	 * True if the extra info is visible.
	 */
	private boolean mExtraInfoVisible;

	/**
	 * The current display mode, which determines layout and cover render style.
	 */
	private int mDisplayMode;

	private Action mCoverPressAction;
	private Action mCoverLongPressAction;

	/**
	 * The currently playing song.
	 */
	private Song mCurrentSong;
	private MenuItem mFavorites;

	@Override
	public void onCreate(Bundle icicle)
	{
		ThemeHelper.setTheme(this, R.style.Playback);
		super.onCreate(icicle);

		setTitle(R.string.playback_view);

		SharedPreferences settings = PlaybackService.getSettings(this);
		int displayMode = Integer.parseInt(settings.getString(PrefKeys.DISPLAY_MODE, PrefDefaults.DISPLAY_MODE));
		mDisplayMode = displayMode;

		int layout = R.layout.full_playback;
		int coverStyle;

		switch (displayMode) {
		default:
			Log.w("VanillaMusic", "Invalid display mode given. Defaulting to widget mode.");
			// fall through
		case DISPLAY_INFO_WIDGETS:
			coverStyle = CoverBitmap.STYLE_NO_INFO;
			layout = R.layout.full_playback_alt;
			break;
		case DISPLAY_INFO_OVERLAP:
			coverStyle = CoverBitmap.STYLE_OVERLAPPING_BOX;
			break;
		case DISPLAY_INFO_BELOW:
			coverStyle = CoverBitmap.STYLE_INFO_BELOW;
			break;
		}

		setContentView(layout);

		CoverView coverView = (CoverView)findViewById(R.id.cover_view);
		coverView.setup(mLooper, this, coverStyle);
		coverView.setOnClickListener(this);
		coverView.setOnLongClickListener(this);
		mCoverView = coverView;

		TableLayout table = (TableLayout)findViewById(R.id.info_table);
		if (table != null) {
			table.setOnClickListener(this);
			table.setOnLongClickListener(this);
			mInfoTable = table;
		}

		mTitle = (TextView)findViewById(R.id.title);
		mAlbum = (TextView)findViewById(R.id.album);
		mArtist = (TextView)findViewById(R.id.artist);

		// to stop NumberPickers from gaining focus and opening the keyboard
		mTitle.setFocusableInTouchMode(true);
		mTitle.requestFocus();

		mControlsTop = findViewById(R.id.controls_top);
		mQueuePosView = (TextView)findViewById(R.id.queue_pos);

		mPitchBar = (SeekBar) findViewById(R.id.pitch_bar);
		mPitchButton = (Button) findViewById(R.id.pitch_button);
		mPitchPicker = (NumberPicker) findViewById(R.id.pitch_picker);
		mPitchText = (TextView) findViewById(R.id.pitch_text);
		mPitchPicker.setWrapSelectorWheel(false);

		mSpeedBar = (SeekBar) findViewById(R.id.speed_bar);
		mSpeedText = (TextView) findViewById(R.id.speed_text);
		mSpeedButton = (Button) findViewById(R.id.speed_button);

		mLooperCheckbox = (CheckBox) findViewById(R.id.looper_checkbox);
		mLooperStartButton = (Button) findViewById(R.id.looper_start_button);
		mLooperEndButton = (Button) findViewById(R.id.looper_end_button);
		mLooperStartPicker = (NumberPicker) findViewById(R.id.looper_start);
		mLooperStartPicker.setMinValue(0);
		mLooperStartPicker.setMaxValue(0);
		mLooperStartPicker.setWrapSelectorWheel(false);
		mLooperStartPicker.setOnValueChangedListener(this);
		mLooperStartPicker.setFormatter(this);
		mLooperEndPicker = (NumberPicker) findViewById(R.id.looper_end);
		mLooperEndPicker.setMinValue(0);
		mLooperEndPicker.setMaxValue(0);
		mLooperEndPicker.setWrapSelectorWheel(false);
		mLooperEndPicker.setOnValueChangedListener(this);
		mLooperEndPicker.setFormatter(this);

		mPitchPicker.setMinValue(0);
		mPitchPicker.setMaxValue(MAX_SEMITONES - MIN_SEMITONES);
		mPitchPicker.setFormatter(new NumberPicker.Formatter(){
			@Override
			public String format(int v){
				return Integer.toString(v + MIN_SEMITONES);
			}
		});

		bindControlButtons();

		setControlsVisible(settings.getBoolean(PrefKeys.VISIBLE_CONTROLS, PrefDefaults.VISIBLE_CONTROLS));
		setExtraInfoVisible(settings.getBoolean(PrefKeys.VISIBLE_EXTRA_INFO, PrefDefaults.VISIBLE_EXTRA_INFO));
	}

	@Override
	public void onStart()
	{
		super.onStart();

		SharedPreferences settings = PlaybackService.getSettings(this);
		if (mDisplayMode != Integer.parseInt(settings.getString(PrefKeys.DISPLAY_MODE, PrefDefaults.DISPLAY_MODE))) {
			finish();
			startActivity(new Intent(this, FullPlaybackActivity.class));
		}

		mCoverPressAction = Action.getAction(settings, PrefKeys.COVER_PRESS_ACTION, PrefDefaults.COVER_PRESS_ACTION);
		mCoverLongPressAction = Action.getAction(settings, PrefKeys.COVER_LONGPRESS_ACTION, PrefDefaults.COVER_LONGPRESS_ACTION);
	}

	/**
	 * Hide the message overlay, if it exists.
	 */
	private void hideMessageOverlay()
	{
		if (mOverlayText != null)
			mOverlayText.setVisibility(View.GONE);
	}

	/**
	 * Show some text in a message overlay.
	 *
	 * @param text Resource id of the text to show.
	 */
	private void showOverlayMessage(int text)
	{
		if (mOverlayText == null) {
			TextView view = new TextView(this);
			// This will be drawn on top of all other controls, so we flood this view
			// with a non-alpha color
			int[] colors = ThemeHelper.getDefaultCoverColors(this);
			view.setBackgroundColor(colors[0]); // background of default cover
			view.setGravity(Gravity.CENTER);
			view.setPadding(25, 25, 25, 25);
			// Make the view clickable so it eats touch events
			view.setClickable(true);
			view.setOnClickListener(this);
			addContentView(view,
					new ViewGroup.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.MATCH_PARENT));
			mOverlayText = view;
		} else {
			mOverlayText.setVisibility(View.VISIBLE);
		}

		mOverlayText.setText(text);
	}

	@Override
	protected void onStateChange(int state, int toggled)
	{
		super.onStateChange(state, toggled);

		if ((toggled & (PlaybackService.FLAG_NO_MEDIA|PlaybackService.FLAG_EMPTY_QUEUE)) != 0) {
			if ((state & PlaybackService.FLAG_NO_MEDIA) != 0) {
				showOverlayMessage(R.string.no_songs);
			} else if ((state & PlaybackService.FLAG_EMPTY_QUEUE) != 0) {
				showOverlayMessage(R.string.empty_queue);
			} else {
				hideMessageOverlay();
			}
		}

		if (mQueuePosView != null)
			updateQueuePosition();
	}

	@Override
	protected void onSongChange(Song song) {
		if (mTitle != null) {
			if (song == null) {
				mTitle.setText(null);
				mAlbum.setText(null);
				mArtist.setText(null);
			} else {
				mTitle.setText(song.title);
				mAlbum.setText(song.album);
				mArtist.setText(song.artist);
			}
			updateQueuePosition();
		}

		mLooperStartPicker.setMaxValue((int)song.duration/10);
		mLooperEndPicker.setMaxValue((int)song.duration/10);

		mCurrentSong = song;

		mHandler.sendEmptyMessage(MSG_LOAD_FAVOURITE_INFO);

		// All quick UI updates are done: Time to update the cover
		// and parse additional info
		if (mExtraInfoVisible) {
			mHandler.sendEmptyMessage(MSG_LOAD_EXTRA_INFO);
		}
		super.onSongChange(song);
	}

	/**
	 * Update the queue position display. mQueuePos must not be null.
	 */
	private void updateQueuePosition()
	{
		if (PlaybackService.finishAction(mState) == SongTimeline.FINISH_RANDOM) {
			// Not very useful in random mode; it will always show something
			// like 11/13 since the timeline is trimmed to 10 previous songs.
			// So just hide it.
			mQueuePosView.setText(null);
		} else {
			PlaybackService service = PlaybackService.get(this);
			mQueuePosView.setText((service.getTimelinePosition() + 1) + "/" + service.getTimelineLength());
		}
		mQueuePosView.requestLayout(); // ensure queue pos column has enough room
	}

	@Override
	public void onPositionInfoChanged()
	{
		if (mQueuePosView != null)
			mUiHandler.sendEmptyMessage(MSG_UPDATE_POSITION);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_DELETE, 30, R.string.delete);
		menu.add(0, MENU_ENQUEUE_ALBUM, 30, R.string.enqueue_current_album);
		menu.add(0, MENU_ENQUEUE_ARTIST, 30, R.string.enqueue_current_artist);
		menu.add(0, MENU_ENQUEUE_GENRE, 30, R.string.enqueue_current_genre);
		menu.add(0, MENU_ADD_TO_PLAYLIST, 30, R.string.add_to_playlist);
		menu.add(0, MENU_SHARE, 30, R.string.share);
		mFavorites = menu.add(0, MENU_SONG_FAVORITE, 0, R.string.add_to_favorites).setIcon(R.drawable.btn_rating_star_off_mtrl_alpha).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		// ensure that mFavorites is updated
		mHandler.sendEmptyMessage(MSG_LOAD_FAVOURITE_INFO);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		final Song song = mCurrentSong;

		switch (item.getItemId()) {
		case android.R.id.home:
		case MENU_LIBRARY:
			openLibrary(null);
			break;
		case MENU_ENQUEUE_ALBUM:
			PlaybackService.get(this).enqueueFromSong(song, MediaUtils.TYPE_ALBUM);
			break;
		case MENU_ENQUEUE_ARTIST:
			PlaybackService.get(this).enqueueFromSong(song, MediaUtils.TYPE_ARTIST);
			break;
		case MENU_ENQUEUE_GENRE:
			PlaybackService.get(this).enqueueFromSong(song, MediaUtils.TYPE_GENRE);
			break;
		case MENU_SONG_FAVORITE:
			long playlistId = Playlist.getFavoritesId(this, true);
			if (song != null) {
				PlaylistTask playlistTask = new PlaylistTask(playlistId, getString(R.string.playlist_favorites));
				playlistTask.audioIds = new ArrayList<Long>();
				playlistTask.audioIds.add(song.id);
				int action = Playlist.isInPlaylist(getContentResolver(), playlistId, song) ? MSG_REMOVE_FROM_PLAYLIST : MSG_ADD_TO_PLAYLIST;
				mHandler.sendMessage(mHandler.obtainMessage(action, playlistTask));
			}
			break;
		case MENU_ADD_TO_PLAYLIST:
			if (song != null) {
				Intent intent = new Intent();
				intent.putExtra("type", MediaUtils.TYPE_SONG);
				intent.putExtra("id", song.id);
				PlaylistDialog dialog = new PlaylistDialog(this, intent, null);
				dialog.show(getFragmentManager(), "PlaylistDialog");
			}
			break;
		case MENU_SHARE:
			if (song != null)
				MediaUtils.shareMedia(this, MediaUtils.TYPE_SONG, song.id);
			break;
		case MENU_DELETE:
			final PlaybackService playbackService = PlaybackService.get(this);
			final PlaybackActivity activity = this;

			if (song != null) {
				String delete_message = getString(R.string.delete_file, song.title);
				AlertDialog.Builder dialog = new AlertDialog.Builder(this);
				dialog.setTitle(R.string.delete);
				dialog
					.setMessage(delete_message)
					.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// MSG_DELETE expects an intent (usually called from listview)
							Intent intent = new Intent();
							intent.putExtra(LibraryAdapter.DATA_TYPE, MediaUtils.TYPE_SONG);
							intent.putExtra(LibraryAdapter.DATA_ID, song.id);
							mHandler.sendMessage(mHandler.obtainMessage(MSG_DELETE, intent));
						}
					})
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
						}
					});
				dialog.create().show();
			}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	@Override
	public boolean onSearchRequested()
	{
		openLibrary(null);
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			shiftCurrentSong(SongTimeline.SHIFT_NEXT_SONG);
			findViewById(R.id.next).requestFocus();
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			shiftCurrentSong(SongTimeline.SHIFT_PREVIOUS_SONG);
			findViewById(R.id.previous).requestFocus();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				setControlsVisible(!mControlsVisible);
				mHandler.sendEmptyMessage(MSG_SAVE_CONTROLS);
				return true;
			case KeyEvent.KEYCODE_BACK:
				if (mSlidingView.isHidden() == false) {
					mSlidingView.hideSlide();
					return true;
				}
		}

		return super.onKeyUp(keyCode, event);
	}

	/**
	 * Set the visibility of the controls views.
	 *
	 * @param visible True to show, false to hide
	 */
	private void setControlsVisible(boolean visible)
	{
		int mode = visible ? View.VISIBLE : View.GONE;
		mControlsTop.setVisibility(mode);
		mSlidingView.setVisibility(mode);
		mControlsVisible = visible;

		if (visible) {
			boolean ret = mPlayPauseButton.requestFocus();
			ret = mLooperCheckbox.requestFocus();
		}
	}

	/**
	 * Set the visibility of the extra metadata view.
	 *
	 * @param visible True to show, false to hide
	 */
	private void setExtraInfoVisible(boolean visible)
	{
		TableLayout table = mInfoTable;
		if (table == null)
			return;

		table.setColumnCollapsed(0, !visible);
		// Make title, album, and artist multi-line when extra info is visible
		boolean singleLine = !visible;
		for (int i = 0; i != 3; ++i) {
			TableRow row = (TableRow)table.getChildAt(i);
			((TextView)row.getChildAt(1)).setSingleLine(singleLine);
		}
		// toggle visibility of all but the first three rows (the title/artist/
		// album rows) and the last row (the seek bar)
		int visibility = visible ? View.VISIBLE : View.GONE;
		for (int i = table.getChildCount() - 1; --i != 2; ) {
			table.getChildAt(i).setVisibility(visibility);
		}
		mExtraInfoVisible = visible;
		if (visible && !mHandler.hasMessages(MSG_LOAD_EXTRA_INFO)) {
			mHandler.sendEmptyMessage(MSG_LOAD_EXTRA_INFO);
		}
	}

	/**
	 * Retrieve the extra metadata for the current song.
	 */
	private void loadExtraInfo()
	{
		Song song = mCurrentSong;

		if(song != null) {

			MediaMetadataRetriever data = new MediaMetadataRetriever();

			try {
				data.setDataSource(song.path);
			} catch (Exception e) {
				Log.w("VanillaMusic", "Failed to extract metadata from " + song.path);
			}

			String composer = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);
			if (composer == null)
				composer = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER);

			String year = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
			if (year == null || "0".equals(year)) {
				year = null;
			} else {
				int dash = year.indexOf('-');
				if (dash != -1)
					year = year.substring(0, dash);
			}

			StringBuilder sb = new StringBuilder(12);
			sb.append(decodeMimeType(data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)));
			String bitrate = data.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
			if (bitrate != null && bitrate.length() > 3) {
				sb.append(' ');
				sb.append(bitrate.substring(0, bitrate.length() - 3));
				sb.append("kbps");
			}

			BastpUtil.GainValues rg = PlaybackService.get(this).getReplayGainValues(song.path);

			data.release();
		}

		mUiHandler.sendEmptyMessage(MSG_COMMIT_INFO);
	}

	/**
	 * Decode the given mime type into a more human-friendly description.
	 */
	private static String decodeMimeType(String mime)
	{
		if ("audio/mpeg".equals(mime)) {
			return "MP3";
		} else if ("audio/mp4".equals(mime)) {
			return "AAC";
		} else if ("audio/vorbis".equals(mime)) {
			return "Ogg Vorbis";
		} else if ("application/ogg".equals(mime)) {
			return "Ogg Vorbis";
		} else if ("audio/flac".equals(mime)) {
			return "FLAC";
		}
		return mime;
	}

	/**
	 * Save the hidden_controls preference to storage.
	 */
	private static final int MSG_SAVE_CONTROLS = 10;
	/**
	 * Call {@link #loadExtraInfo()}.
	 */
	private static final int MSG_LOAD_EXTRA_INFO = 11;
	/**
	 * Pass obj to mExtraInfo.setText()
	 */
	private static final int MSG_COMMIT_INFO = 12;
	/**
	 * Calls {@link #updateQueuePosition()}.
	 */
	private static final int MSG_UPDATE_POSITION = 13;
	/**
	 * Check if passed song is a favorite
	 */
	private static final int MSG_LOAD_FAVOURITE_INFO = 14;
	/**
	 * Updates the favorites state
	 */
	private static final int MSG_COMMIT_FAVOURITE_INFO = 15;

	@Override
	public boolean handleMessage(Message message)
	{
		switch (message.what) {
		case MSG_SAVE_CONTROLS: {
			SharedPreferences.Editor editor = PlaybackService.getSettings(this).edit();
			editor.putBoolean(PrefKeys.VISIBLE_CONTROLS, mControlsVisible);
			editor.putBoolean(PrefKeys.VISIBLE_EXTRA_INFO, mExtraInfoVisible);
			editor.apply();
			break;
		}
		case MSG_LOAD_EXTRA_INFO:
			loadExtraInfo();
			break;
		case MSG_COMMIT_INFO: { // info now contains pitch controls
			PlaybackService playbackService = PlaybackService.get(this);

			mPitchBar.setOnSeekBarChangeListener(this);
			mPitchButton.setOnClickListener(this);
			mPitchPicker.setOnValueChangedListener(this);
			mSpeedBar.setOnSeekBarChangeListener(this);
			mSpeedButton.setOnClickListener(this);
			mLooperCheckbox.setOnCheckedChangeListener(this);
			mLooperStartButton.setOnClickListener(this);
			mLooperEndButton.setOnClickListener(this);

			mPitchBar.setProgress(playbackService.getPitchProgress());
			mPitchPicker.setValue(playbackService.getPitchSemitones() - MIN_SEMITONES);
			mPitchPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
			mPitchText.setText(getPitchText());
			mSpeedBar.setProgress(playbackService.getSpeedProgress());
			mSpeedText.setText(getSpeedText());
			mLooperCheckbox.setChecked(playbackService.mMediaPlayer.isLooperEnabled());
			mLooperStartPicker.setValue(playbackService.mMediaPlayer == null? 0 : playbackService.mMediaPlayer.getLooperStart());
			mLooperEndPicker.setValue(playbackService.mMediaPlayer == null? 0 : playbackService.mMediaPlayer.getLooperEnd());

			break;
		}
		case MSG_UPDATE_POSITION:
			updateQueuePosition();
			break;
		case MSG_NOTIFY_PLAYLIST_CHANGED: // triggers a fav-refresh
		case MSG_LOAD_FAVOURITE_INFO:
			if (mCurrentSong != null) {
				boolean found = Playlist.isInPlaylist(getContentResolver(), Playlist.getFavoritesId(this, false), mCurrentSong);
				mUiHandler.sendMessage(mUiHandler.obtainMessage(MSG_COMMIT_FAVOURITE_INFO, found));
			}
			break;
		case MSG_COMMIT_FAVOURITE_INFO:
			if (mFavorites != null) {
				boolean found = (boolean)message.obj;
				mFavorites.setIcon(found ? R.drawable.btn_rating_star_on_mtrl_alpha: R.drawable.btn_rating_star_off_mtrl_alpha);
				mFavorites.setTitle(found ? R.string.remove_from_favorites : R.string.add_to_favorites);
			}
			break;
		default:
			return super.handleMessage(message);
		}

		return true;
	}

	@Override
	protected void performAction(Action action) {
		switch (action) {
			case ToggleControls:
				setControlsVisible(!mControlsVisible);
				mHandler.sendEmptyMessage(MSG_SAVE_CONTROLS);
				break;
			case ShowQueue:
				mSlidingView.expandSlide();
				break;
			default:
				super.performAction(action);
		}
	}

	@Override
	public void onClick(View view){
		if (view == mOverlayText && (mState & PlaybackService.FLAG_EMPTY_QUEUE) != 0) {
			setState(PlaybackService.get(this).setFinishAction(SongTimeline.FINISH_RANDOM));
		} else if (view == mCoverView) {
			//performAction(mCoverPressAction);
			setExtraInfoVisible(!mExtraInfoVisible);
			mHandler.sendEmptyMessage(MSG_SAVE_CONTROLS);
		} else if (view.getId() == R.id.info_table) {
			openLibrary(mCurrentSong);
		} else if (view.getId() == R.id.pitch_button){
			mPitchBar.setProgress(1000);
			mPitchPicker.setValue(24);
		} else if (view.getId() == R.id.speed_button) {
			mSpeedBar.setProgress(1000);
		} else if (view.getId() == R.id.looper_start_button) {
			int currentPosition = PlaybackService.get(this).mMediaPlayer.getCurrentPosition()/10;
			mLooperStartPicker.setValue(currentPosition);
			onValueChange(mLooperStartPicker, mLooperStartPicker.getValue(), currentPosition);
		} else if (view.getId() == R.id.looper_end_button) {
			int currentPosition = PlaybackService.get(this).mMediaPlayer.getCurrentPosition()/10;
			mLooperEndPicker.setValue(currentPosition);
			onValueChange(mLooperEndPicker, mLooperEndPicker.getValue(), currentPosition);
		} else {
			super.onClick(view);
		}
	}

	@Override
	public boolean onLongClick(View view)
	{
		switch (view.getId()) {
		case R.id.cover_view:
			performAction(mCoverLongPressAction);
			break;
		case R.id.info_table:
			setExtraInfoVisible(!mExtraInfoVisible);
			mHandler.sendEmptyMessage(MSG_SAVE_CONTROLS);
			break;
		default:
			return false;
		}
		return true;
	}

	@Override
	public void onSlideFullyExpanded(boolean expanded) {
		super.onSlideFullyExpanded(expanded);
		if (expanded)
			setExtraInfoVisible(false);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked){
			PlaybackService.get(this).mMediaPlayer.enableLooper();
		} else {
			PlaybackService.get(this).mMediaPlayer.disableLooper();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		PlaybackService playbackService = PlaybackService.get(this);
		switch (seekBar.getId()) {
			case R.id.pitch_bar:
				playbackService.setPitchCents(progress);
				mPitchText.setText(getPitchText());
				break;
			case R.id.speed_bar:
				playbackService.setSpeed(progress);
				mSpeedText.setText(getSpeedText());
				break;
			default:
				super.onProgressChanged(seekBar, progress, fromUser);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		switch (seekBar.getId()) {
			case R.id.pitch_bar:
			case R.id.speed_bar:
				break;
			default:
				super.onStartTrackingTouch(seekBar);
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		switch (seekBar.getId()) {
			case R.id.pitch_bar:
				break;
			case R.id.speed_bar:
				PlaybackService playbackService = PlaybackService.get(this);
				if (!playbackService.isPlaying()) {
					playbackService.setSpeed(seekBar.getProgress());
					//playbackService.mMediaPlayer.pause();
				}
				break;
			default:
				super.onStopTrackingTouch(seekBar);
		}
	}

	@Override
	public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
		if(picker == mLooperStartPicker){
			PlaybackService.get(this).mMediaPlayer.setLooperStart(newVal*10);
			mLooperEndPicker.setMinValue(newVal);
		}else if(picker == mLooperEndPicker){
			PlaybackService.get(this).mMediaPlayer.setLooperEnd(newVal*10);
		}else if(picker == mPitchPicker){
			PlaybackService.get(this).setPitchSemitones(newVal + MIN_SEMITONES);
		}
	}

	// NumberPicker.Formatter
	@Override
	public String format(int value) {
		int minutes = value/100/60;
		int seconds = value/100%60;
		int fraction_of_a_second = value%100;
		return String.format("%d:%02d.%02d", minutes, seconds, fraction_of_a_second);

	}

	private String getPitchText(){
		float pitchChange = (PlaybackService.get(this).getPitchProgress()-1000.0f)/1000.0f;
		return String.format("%s%1.2f", pitchChange < 0 ? "" : "+", pitchChange);
	}

	private String getSpeedText(){
		float speedChange = (PlaybackService.get(this).getSpeed());
		return String.format("%1.2f", speedChange);
	}
}
