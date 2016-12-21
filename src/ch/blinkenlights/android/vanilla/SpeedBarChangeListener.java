package ch.blinkenlights.android.vanilla;

import android.content.Context;
import android.widget.SeekBar;

/**
 * Created by nwam on 21/12/16.
 */
public class SpeedBarChangeListener implements SeekBar.OnSeekBarChangeListener{
    Context context;


    public SpeedBarChangeListener(Context c){
        context = c;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        PlaybackService playbackService = PlaybackService.get(context);
        playbackService.mMediaPlayer.updateSpeed(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
