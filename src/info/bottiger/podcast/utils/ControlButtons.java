package info.bottiger.podcast.utils;

import info.bottiger.podcast.R;
import info.bottiger.podcast.service.PlayerService;
import info.bottiger.podcast.utils.ControlButtons.Holder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;

public class ControlButtons {
	
    public static class Holder {
    	public ImageButton playPauseButton;
        public ImageButton stopButton;
        public ImageButton infoButton;
        public ImageButton downloadButton;
        public ImageButton queueButton;
        public SeekBar seekbar;
    }

	public static void setListener(Holder viewHolder, final PlayerService playerServiceBinder, final long id) {
		final ImageButton b = (ImageButton)viewHolder.playPauseButton;
		b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (playerServiceBinder.isPlaying()) {
                	playerServiceBinder.pause();
                	b.setContentDescription("Play");
                	b.setImageResource(R.drawable.play);
                } else {
                	playerServiceBinder.play(id);
                	b.setImageResource(R.drawable.pause);
                	b.setContentDescription("Pause");
                }
            }
        });
		
		viewHolder.stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	playerServiceBinder.stop();
            }
        });
		
		viewHolder.infoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
		
		viewHolder.downloadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
		
		viewHolder.queueButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
	}

}
