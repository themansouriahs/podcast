package info.bottiger.podcast.utils;

import info.bottiger.podcast.PodcastBaseFragment;
import info.bottiger.podcast.R;
import info.bottiger.podcast.RecentItemFragment;
import info.bottiger.podcast.MainActivity;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.service.PodcastDownloadManager;
import info.bottiger.podcast.service.PodcastService;
import android.content.ContentResolver;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ControlButtons {

	public static final int MAX_SEEKBAR_VALUE = 1000;
	private static long mLastSeekEventTime;
	private static boolean mFromTouch;

	private static TextView mCurrentTime;

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

	public static void setCurrentTime(final Holder viewHolder, final long id) {

	}

	public static void setListener(
			final PodcastService podcastServiceConnection,
			final Holder viewHolder, final long id) {

		final ImageButton playPauseButton = viewHolder.playPauseButton;
		final ImageView playImage = viewHolder.image;
		final ContentResolver resolver = fragment.getActivity()
				.getContentResolver();
		final FeedItem item = FeedItem.getById(resolver, id);
		final ThemeHelper themeHelper = new ThemeHelper(fragment.getActivity());
		
		playPauseButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playPause(id, viewHolder, themeHelper, playPauseButton);
			}
		});
		
		playImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playPause(id, viewHolder, themeHelper, playPauseButton);
			}
		});

		viewHolder.stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				playPauseButton.setContentDescription("Play");
				playPauseButton.setImageResource(themeHelper
						.getAttr(R.attr.play_icon));
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

	public static void setPlayerListeners(View listView, View playerView,
			long id) {

		ControlButtons.Holder viewHolder = new ControlButtons.Holder();
		viewHolder.currentTime = (TextView) playerView
				.findViewById(R.id.current_position);

		viewHolder.playPauseButton = (ImageButton) playerView
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
		viewHolder.image = (ImageView) listView.findViewById(R.id.list_image);

		ControlButtons.setListener(MainActivity.mPodcastServiceBinder,
				viewHolder, id);
	}

	public static void setPlayerListeners(View playerView, long id) {
		// View view = getChildByID(id);

		ControlButtons.Holder viewHolder = new ControlButtons.Holder();
		viewHolder.currentTime = (TextView) playerView
				.findViewById(R.id.current_position);

		viewHolder.playPauseButton = (ImageButton) playerView
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

		ControlButtons.setListener(MainActivity.mPodcastServiceBinder,
				viewHolder, id);

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

		}

	}
	
	private static void playPause(long id, Holder viewHolder, ThemeHelper themeHelper, ImageButton playPauseButton) {
		if (viewHolder.seekbar != null)
			fragment.setProgressBar(viewHolder.seekbar);
		if (viewHolder.currentTime != null)
			fragment.setCurrentTime(viewHolder.currentTime);

		if (viewHolder.duration != null)
			fragment.setDuration(viewHolder.duration);
		if (PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
			playPauseButton.setContentDescription("Play");
			playPauseButton.setImageResource(themeHelper
					.getAttr(R.attr.play_icon));
			PodcastBaseFragment.mPlayerServiceBinder.pause();
		} else {
			playPauseButton.setImageResource(themeHelper
					.getAttr(R.attr.pause_icon));
			playPauseButton.setContentDescription("Pause");
			PodcastBaseFragment.mPlayerServiceBinder.play(id);
			PodcastBaseFragment.queueNextRefresh();
		}
	}

}
