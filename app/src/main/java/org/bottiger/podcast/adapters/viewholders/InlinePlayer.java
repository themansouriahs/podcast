package org.bottiger.podcast.adapters.viewholders;

import org.bottiger.podcast.R;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.PlayerButtonView;
import org.bottiger.podcast.views.PlayerLinearLayout;

import android.view.View;
import android.view.ViewStub;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

/** PlaylistViewHolder for holding the expanded extended_player */
public class InlinePlayer {
	public ViewStub stub;
	public PlayerLinearLayout playerLinearLayout;
	public TextView timeSlash;
	public SeekBar seekbar;
	public TextView currentTime;
	public TextView duration;
	public TextView filesize;
	public ToggleButton playPauseButton;
	public PlayerButtonView previousButton;
	public DownloadButtonView downloadButton;
	public PlayerButtonView bookmarkButton;
	public PlayerButtonView queueButton;
	
	private static InlinePlayer currentEpisodePlayerViewHolder = null;
	private static InlinePlayer secondaryEpisodePlayerViewHolder = null;
	
	public static InlinePlayer getViewHolder(View view) {
		return getViewHolder(view, null) ;
	}
	
	public static InlinePlayer getViewHolder(View view, InlinePlayer viewHolder) {
		
		if (viewHolder == null)
			viewHolder = new InlinePlayer();

		viewHolder.timeSlash = (TextView) view.findViewById(R.id.time_slash);
		viewHolder.currentTime = (TextView) view
				.findViewById(R.id.current_position);
		viewHolder.seekbar = (SeekBar) view.findViewById(R.id.player_progress);

        viewHolder.queueButton = (PlayerButtonView) view.findViewById(R.id.queue);
		viewHolder.previousButton = (PlayerButtonView) view.findViewById(R.id.previous);
		viewHolder.downloadButton = (DownloadButtonView) view
				.findViewById(R.id.download);
		viewHolder.bookmarkButton = (PlayerButtonView) view.findViewById(R.id.bookmark);
		
		// listview
		viewHolder.queueButton = (PlayerButtonView) view.findViewById(R.id.queue);
		viewHolder.duration = (TextView) view.findViewById(R.id.duration);
		viewHolder.filesize = (TextView) view.findViewById(R.id.filesize);

		return viewHolder;
	}


	public static InlinePlayer getCurrentEpisodePlayerViewHolder() {
		return currentEpisodePlayerViewHolder;
	}

	public static InlinePlayer getSecondaryEpisodePlayerViewHolder() {
		return secondaryEpisodePlayerViewHolder;
	}
}
