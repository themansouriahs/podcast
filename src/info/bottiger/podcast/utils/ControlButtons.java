package info.bottiger.podcast.utils;

import info.bottiger.podcast.service.PlayerService;
import info.bottiger.podcast.utils.ControlButtons.Holder;
import android.view.View;
import android.widget.Button;

public class ControlButtons {
	
    public static class Holder {
    	public Button playPauseButton;
        public Button stopButton;
    }

	public static void setListener(Holder viewHolder, final PlayerService playerServiceBinder, final long id) {
		final Button b = (Button)viewHolder.playPauseButton;
		b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (playerServiceBinder.isPlaying()) {
                	playerServiceBinder.pause();
                	b.setText("Play");
                } else {
                	playerServiceBinder.play(id);
                	b.setText("Pause");
                }
            }
        });
		
		viewHolder.stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	playerServiceBinder.stop();
            }
        });
	}

}
