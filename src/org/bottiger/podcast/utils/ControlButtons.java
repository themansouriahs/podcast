package org.bottiger.podcast.utils;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.RecentItemFragment;
import org.bottiger.podcast.adapters.viewholders.InlinePlayer;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.playlist.ReorderCursor;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.service.PodcastService;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ControlButtons {

	public static final int MAX_SEEKBAR_VALUE = 1000;
	private static long mLastSeekEventTime;
	private static boolean mFromTouch;

	private static TextView mCurrentTime;

	private static ThemeHelper themeHelper;

	public static RecentItemFragment fragment = null;

	public static class Holder {
		public ImageButton playPauseButton;
		public ImageButton stopButton;
		public ImageButton infoButton;
		public ImageButton downloadButton;
		public ImageButton queueButton;
		public ImageView image;
		public TextView currentTime;
		public TextView timeSlash;
		public TextView duration;
		public TextView filesize;
		public SeekBar seekbar;
	}

	public static void setThemeHelper(Context context) {
		themeHelper = new ThemeHelper(context);
	}

	public static void setListener(
			final PodcastService podcastServiceConnection,
			final InlinePlayer viewHolder, final PodcastBaseFragment fragment,
			final FeedItem item) {

		final ToggleButton playPauseButton = viewHolder.playPauseButton;
		// final ImageView playImage = viewHolder.image; FIXME
		final ContentResolver resolver = fragment.getActivity()
				.getContentResolver();
		final ThemeHelper themeHelper = new ThemeHelper(fragment.getActivity());

		playPauseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Debug.startMethodTracing("playpause");
				long start = System.nanoTime();
				playPause(item, fragment, viewHolder, themeHelper,
						playPauseButton);
				long end = System.nanoTime();
				long diff = end - start;
				long diff2 = diff * 2;
				Debug.stopMethodTracing();
				diff2 = diff2 - diff;
			}
		});

		viewHolder.stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playPauseButton.setContentDescription("Play");
				playPauseButton.setChecked(false);
				PodcastBaseFragment.mPlayerServiceBinder.stop();
			}
		});

		viewHolder.infoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});

		viewHolder.downloadButton
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {

						if (item.isDownloaded()) {
							// Delete file
							item.delFile(resolver);
							/*
							 * viewHolder.downloadButton
							 * .setBackgroundResource(themeHelper
							 * .getAttr(R.attr.download_icon));
							 */

							viewHolder.downloadButton
									.setImageResource(themeHelper
											.getAttr(R.attr.download_icon));
							viewHolder.downloadButton
									.setContentDescription("Download");
						} else {
							// Download file
							FilesizeUpdater.put(fragment.getActivity(),
									item.id, viewHolder.filesize);
							PodcastDownloadManager.addItemAndStartDownload(
									item, fragment.getActivity());
							// podcastServiceConnection.downloadItem(fragment
							// .getActivity().getContentResolver(), item);
							/*
							 * viewHolder.downloadButton
							 * .setBackgroundResource(themeHelper
							 * .getAttr(R.attr.delete_icon));
							 */
							viewHolder.downloadButton
									.setImageResource(themeHelper
											.getAttr(R.attr.delete_icon));
							viewHolder.downloadButton
									.setContentDescription("Trash");
						}
					}
				});

		viewHolder.queueButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});

		ControlButtons.mCurrentTime = viewHolder.currentTime;
		OnPlayerSeekBarChangeListener listener = new OnPlayerSeekBarChangeListener(
				item, resolver, viewHolder.currentTime, viewHolder.duration);
		// viewHolder.seekbar.setOnSeekBarChangeListener(mSeekListener);
		viewHolder.seekbar.setOnSeekBarChangeListener(listener);
		viewHolder.seekbar.setMax(MAX_SEEKBAR_VALUE);
	}

	public static void setPlayerListeners(InlinePlayer holder,
			PodcastBaseFragment fragment, FeedItem episode) {
		ControlButtons.setListener(MainActivity.mPodcastServiceBinder, holder,
				fragment, episode);
	}

	@Deprecated
	public static void setPlayerListeners(View listView, View playerView,
			FeedItem item) {

		InlinePlayer viewHolder = new InlinePlayer();
		viewHolder.currentTime = (TextView) playerView
				.findViewById(R.id.current_position);

		viewHolder.playPauseButton = (ToggleButton) playerView
				.findViewById(R.id.play_toggle);
		viewHolder.stopButton = (ImageButton) playerView
				.findViewById(R.id.stop);
		viewHolder.infoButton = (ImageButton) playerView
				.findViewById(R.id.info);
		viewHolder.downloadButton = (ImageButton) playerView
				.findViewById(R.id.download);
		viewHolder.queueButton = (ImageButton) playerView
				.findViewById(R.id.queue);
		viewHolder.seekbar = (SeekBar) playerView.findViewById(R.id.progress);
		viewHolder.currentTime = (TextView) listView
				.findViewById(R.id.current_position);
		viewHolder.duration = (TextView) listView.findViewById(R.id.duration);
		viewHolder.filesize = (TextView) listView.findViewById(R.id.filesize);
		// viewHolder.image = (ImageView)
		// listView.findViewById(R.id.list_image);

		ControlButtons.setListener(MainActivity.mPodcastServiceBinder,
				viewHolder, fragment, item);
	}

	private static class OnPlayerSeekBarChangeListener implements
			OnSeekBarChangeListener {

		ContentResolver contentResolver;
		TextView currentTimeView;
		TextView durationView;
		FeedItem item;

		public OnPlayerSeekBarChangeListener(FeedItem item,
				ContentResolver contentResolver, TextView tv, TextView dv) {
			this.contentResolver = contentResolver;
			this.item = item;
			this.currentTimeView = tv;
			this.durationView = dv;
		}

		@Override
		public void onStartTrackingTouch(SeekBar bar) {
			mLastSeekEventTime = 0;
			RecentItemFragment.setUpdateProgressbar(false);
		}

		@Override
		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			// if (!fromuser || (PodcastBaseFragment.mPlayerServiceBinder ==
			// null)) return;

			long now = SystemClock.elapsedRealtime();
			if ((now - mLastSeekEventTime) > 250 && durationView != null) {
				mLastSeekEventTime = now;

				float relativeProgress = progress / (float) MAX_SEEKBAR_VALUE;
				String duration = durationView.getText().toString();
				// long timeMs = duration * progress / 1000;
				// if (mCurrentTime != null)
				// mCurrentTime.setText(StrUtils.formatTime(timeMs));
				if (currentTimeView != null)
					currentTimeView.setText(StrUtils.formatTime(
							relativeProgress, duration));
			}

		}

		@Override
		public void onStopTrackingTouch(SeekBar bar) {
			// mPosOverride = -1;
			mFromTouch = false;
			long timeMs = item.getDuration() * bar.getProgress()
					/ MAX_SEEKBAR_VALUE;
			item.offset = (int) timeMs;
			item.update(contentResolver);
			try {
				if (PodcastBaseFragment.mPlayerServiceBinder.isInitialized()
						&& PodcastBaseFragment.mPlayerServiceBinder
								.getCurrentItem().equals(item))
					PodcastBaseFragment.mPlayerServiceBinder.seek(timeMs);
			} catch (Exception ex) {
			}
			// log.debug("mFromTouch = false; ");

			RecentItemFragment.setUpdateProgressbar(true);

		}

	}

	public static void playPauseToggle(FeedItem episode,
			PodcastBaseFragment fragment, ToggleButton button) {
		assert themeHelper != null;
		PlayerService ps = PodcastBaseFragment.mPlayerServiceBinder;
		boolean isPlaying = false;
		if (ps != null) {
			if (ps.isPlaying()) {
				ps.pause();
			} else {
				isPlaying = true;
				ps.play(episode.id);
				RecentItemFragment.queueNextRefresh();

				if (fragment != null) {
					// fragment.refreshView();
					Playlist playlist = new Playlist(fragment.getActivity());
					int episodePos = playlist.getPosition(episode);
					if (episodePos > 0) {
						playlist.move(episodePos, 0);
						//playlist.move(0, episodePos);
						
						CursorAdapter adapter = fragment.getAdapter(); 
						ReorderCursor cursor = (ReorderCursor) adapter.getCursor();
						
						ReorderCursor newCursor = new ReorderCursor(fragment.getActivity(), adapter,
								cursor, playlist.getReordering());
						
						
						cursor.reorderCursor(episodePos, 0);
						//adapter.notifyDataSetChanged();
						//cursor.drop(episodePos, 0);
						
						
						//adapter.swapCursor(newCursor);
							
						fragment.getListView().setSelection(0);
					}
				}
			}
		}
		button.setChecked(isPlaying);
	}

	private static void playPause(FeedItem episode,
			PodcastBaseFragment fragment, InlinePlayer viewHolder,
			ThemeHelper themeHelper, ToggleButton playPauseButton) {
		if (viewHolder.seekbar != null)
			fragment.setProgressBar(viewHolder.seekbar);

		if (viewHolder.currentTime != null)
			fragment.setCurrentTime(viewHolder.currentTime);

		if (viewHolder.duration != null)
			fragment.setDuration(viewHolder.duration);

		playPauseToggle(episode, fragment, playPauseButton);
	}

}
