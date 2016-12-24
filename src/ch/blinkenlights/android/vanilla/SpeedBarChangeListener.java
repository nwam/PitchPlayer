package ch.blinkenlights.android.vanilla;

import android.content.Context;
import android.drm.DrmStore;
import android.widget.SeekBar;

/**
 * Created by nwam on 21/12/16.
 */
public class SpeedBarChangeListener implements SeekBar.OnSeekBarChangeListener{
    PlaybackService playbackService;
    boolean playing;

    public SpeedBarChangeListener(Context c){
        playbackService = PlaybackService.get(c);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (playing) {
            playbackService.mMediaPlayer.updateSpeed(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        playing = playbackService.isPlaying();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (!playing){
            playbackService.mMediaPlayer.updateSpeed(seekBar.getProgress());
            playbackService.mMediaPlayer.pause();
        }
    }
}
