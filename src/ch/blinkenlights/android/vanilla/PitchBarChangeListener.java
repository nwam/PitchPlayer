package ch.blinkenlights.android.vanilla;

import android.content.Context;
import android.widget.SeekBar;

/**
 * Created by nwam on 20/12/16.
 */
public class PitchBarChangeListener implements SeekBar.OnSeekBarChangeListener {

    Context context;


    public PitchBarChangeListener(Context c){
        context = c;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        PlaybackService playbackService = PlaybackService.get(context);
        playbackService.mMediaPlayer.updatePitch(progress);

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}