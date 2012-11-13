package info.bottiger.podcast.utils;

import info.bottiger.podcast.PodcastBaseFragment;
import info.bottiger.podcast.R;
import info.bottiger.podcast.RecentItemFragment;
import info.bottiger.podcast.SwipeActivity;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.service.PlayerService;
import info.bottiger.podcast.service.PodcastService;
import info.bottiger.podcast.utils.ControlButtons.Holder;
import android.content.ContentResolver;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ControlButtons {
	
	public static RecentItemFragment fragment = null;
		
    public static class Holder {
    	public ImageButton playPauseButton;
        public ImageButton stopButton;
        public ImageButton infoButton;
        public ImageButton downloadButton;
        public ImageButton queueButton;
        public TextView currentTime;
        public TextView timeSlash;
        public TextView duration;
        public TextView filesize;
        public SeekBar seekbar;
    }
    
    public static void setCurrentTime(final Holder viewHolder, final long id) {
    	
    }

    public static void setListener(final PodcastService podcastServiceConnection, final Holder viewHolder, final long id) {
    	
    	fragment.queueNextRefresh(1);
    	
    	final ImageButton playPauseButton = (ImageButton)viewHolder.playPauseButton;
  
		playPauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if (viewHolder.seekbar != null) fragment.setProgressBar(viewHolder.seekbar);
            	if (viewHolder.currentTime != null) 
            		fragment.setCurrentTime(viewHolder.currentTime);
            	
            	if (viewHolder.duration != null) fragment.setDuration(viewHolder.duration);
                if (fragment.mPlayerServiceBinder.isPlaying()) {
                	playPauseButton.setContentDescription("Play");
                	playPauseButton.setImageResource(R.drawable.play);
                	fragment.mPlayerServiceBinder.pause();
                } else {
                	playPauseButton.setImageResource(R.drawable.pause);
                	playPauseButton.setContentDescription("Pause");
                	fragment.mPlayerServiceBinder.play(id);
                }
            }
        });
		
		viewHolder.stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	playPauseButton.setContentDescription("Play");
            	playPauseButton.setImageResource(R.drawable.play);
            	fragment.mPlayerServiceBinder.stop();
            }
        });
		
		viewHolder.infoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
		
		viewHolder.downloadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	ContentResolver resolver = fragment.getActivity().getContentResolver();
            	FeedItem item = FeedItem.getById(resolver, id);
            	
            	if (item.isDownloaded()) {
            		// Delete file
            		item.delFile(resolver);
            		viewHolder.downloadButton.setImageResource(R.drawable.download);
            		viewHolder.downloadButton.setContentDescription("Download");
            	} else {
            		// Download file
            		FilesizeUpdater.put(fragment.getActivity(), item.id, viewHolder.filesize);
            		podcastServiceConnection.downloadItem(fragment.getActivity().getContentResolver(), item);
            		viewHolder.downloadButton.setImageResource(R.drawable.trash);
            		viewHolder.downloadButton.setContentDescription("Trash");
            	}
            }
        });
		
		viewHolder.queueButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
		
        if (viewHolder.seekbar instanceof SeekBar) {
        	viewHolder.seekbar.setOnSeekBarChangeListener(fragment.mSeekListener);
        }
        viewHolder.seekbar.setMax(1000); 
	}
    
	public static void setPlayerListeners(View listView, View playerView, long id) {

		ControlButtons.Holder viewHolder = new ControlButtons.Holder();
		viewHolder.currentTime = (TextView) playerView.findViewById(R.id.current_position);
		
		viewHolder.playPauseButton = (ImageButton) playerView.findViewById(R.id.play_toggle);
		viewHolder.stopButton = (ImageButton) playerView.findViewById(R.id.stop);
		viewHolder.infoButton = (ImageButton) playerView.findViewById(R.id.info);
		viewHolder.downloadButton = (ImageButton) playerView.findViewById(R.id.download);
		viewHolder.queueButton = (ImageButton) playerView.findViewById(R.id.queue);
		viewHolder.seekbar = (SeekBar) playerView.findViewById(R.id.progress);
		viewHolder.currentTime = (TextView) listView.findViewById(R.id.current_position);
		viewHolder.duration = (TextView) listView.findViewById(R.id.duration);
		viewHolder.filesize = (TextView) listView.findViewById(R.id.filesize);
		
		ControlButtons.setListener(SwipeActivity.mServiceBinder, viewHolder, id);
	}
	
	public static void setPlayerListeners(View playerView, long id) {
		//View view = getChildByID(id);

		ControlButtons.Holder viewHolder = new ControlButtons.Holder();
		viewHolder.currentTime = (TextView) playerView.findViewById(R.id.current_position);
		
		
		viewHolder.playPauseButton = (ImageButton) playerView.findViewById(R.id.play_toggle);
		viewHolder.stopButton = (ImageButton) playerView.findViewById(R.id.stop);
		viewHolder.infoButton = (ImageButton) playerView.findViewById(R.id.info);
		viewHolder.downloadButton = (ImageButton) playerView.findViewById(R.id.download);
		viewHolder.queueButton = (ImageButton) playerView.findViewById(R.id.queue);
		viewHolder.seekbar = (SeekBar) playerView.findViewById(R.id.progress);
		
		ControlButtons.setListener(SwipeActivity.mServiceBinder, viewHolder, id);
			
	}

}
