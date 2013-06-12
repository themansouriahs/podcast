package org.bottiger.podcast.adapters.viewholders;

import org.bottiger.podcast.R;

import android.view.View;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

/** ViewHolder for holding the expanded player */
public class InlinePlayer {
	public ViewStub stub;
	public View playerView;
	public TextView timeSlash;
	public SeekBar seekbar;
	public TextView currentTime;
	public TextView duration;
	public TextView filesize;
	public ImageButton playPauseButton;
	public ImageButton stopButton;
	public ImageButton downloadButton;
	public ImageButton infoButton;
	public ImageButton queueButton;
	
	private static InlinePlayer currentEpisodePlayerViewHolder = null;
	private static InlinePlayer secondaryEpisodePlayerViewHolder = null;
	
	public static InlinePlayer getViewHolder(View view) {
		return getViewHolder(view, null) ;
	}
	
	public static InlinePlayer getViewHolder(View view, InlinePlayer viewHolder) {
		
		if (viewHolder == null)
			viewHolder = new InlinePlayer();

		viewHolder.stub = (ViewStub) view.findViewById(R.id.stub);
		viewHolder.timeSlash = (TextView) view.findViewById(R.id.time_slash);
		viewHolder.currentTime = (TextView) view
				.findViewById(R.id.current_position);
		viewHolder.seekbar = (SeekBar) view.findViewById(R.id.progress);
		viewHolder.playPauseButton = (ImageButton) view
				.findViewById(R.id.play_toggle);
		viewHolder.stopButton = (ImageButton) view.findViewById(R.id.stop);
		viewHolder.downloadButton = (ImageButton) view
				.findViewById(R.id.download);
		viewHolder.infoButton = (ImageButton) view.findViewById(R.id.info);
		
		// listview
		viewHolder.queueButton = (ImageButton) view.findViewById(R.id.queue);
		viewHolder.duration = (TextView) view.findViewById(R.id.duration);
		viewHolder.filesize = (TextView) view.findViewById(R.id.filesize);
		
		return viewHolder;
	}

	public static InlinePlayer getCurrentEpisodePlayerViewHolder() {
		return currentEpisodePlayerViewHolder;
	}

	public static void setCurrentEpisodePlayerViewHolder(
			InlinePlayer currentEpisodePlayerViewHolder) {
		InlinePlayer.currentEpisodePlayerViewHolder = currentEpisodePlayerViewHolder;
	}

	public static InlinePlayer getSecondaryEpisodePlayerViewHolder() {
		return secondaryEpisodePlayerViewHolder;
	}

	public static void setSecondaryEpisodePlayerViewHolder(
			InlinePlayer secondaryEpisodePlayerViewHolder) {
		InlinePlayer.secondaryEpisodePlayerViewHolder = secondaryEpisodePlayerViewHolder;
	}
}
