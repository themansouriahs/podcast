package org.bottiger.podcast.adapters;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.PlayerActivity;
import org.bottiger.podcast.PlaylistDSLVFragment;
import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.viewholders.InlinePlayer;
import org.bottiger.podcast.images.ImageCacheManager;
import org.bottiger.podcast.listeners.DownloadProgressListener;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.DownloadStatus;
import org.bottiger.podcast.utils.ControlButtons;
import org.bottiger.podcast.utils.Playlist;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.ThemeHelper;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.BaseColumns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ItemCursorAdapter extends AbstractEpisodeCursorAdapter {
	
	private static final boolean COLOR_BACKGROUND = false;

	public static final int ICON_DEFAULT_ID = -1;

	public final static FieldHandler defaultTextFieldHandler = new FieldHandler.TextFieldHandler();

	protected int[] mFrom2;
	protected int[] mTo2;
	protected FieldHandler[] mFieldHandlers;

	private static final int TYPE_COLLAPS = 0;
	private static final int TYPE_EXPAND = 1;
	private static final int TYPE_MAX_COUNT = 2;

	private ArrayList<FeedItem> mData = new ArrayList<FeedItem>();
	private PodcastBaseFragment mFragment = null;

	private TreeSet<Number> mExpandedItemID = new TreeSet<Number>();

	private static DownloadManager mDownloadManager = null;

	private FeedItem mCurrentFeedItem = null;

	/** ViewHolder for holding a basiv listitem */
	static class EpisodeViewHolder {
		NetworkImageView icon;
		TextView mainTitle;
		TextView subTitle;
		TextView timeDuration;
		TextView currentPosition;
		TextView slash;
		TextView fileSize;
		ViewStub stub;
		View playerView;
	}

	// Create a set of FieldHandlers for methods calling using the legacy
	// constructor
	private static FieldHandler[] defaultFieldHandlers(String[] fromColumns,
			HashMap<Integer, Integer> iconMap) {
		FieldHandler[] handlers = new FieldHandler[fromColumns.length];
		for (int i = 0; i < handlers.length - 1; i++) {
			handlers[i] = defaultTextFieldHandler;
		}
		handlers[fromColumns.length - 1] = new FieldHandler.IconFieldHandler(
				iconMap);
		return handlers;
	}

	// Legacy constructor
	public ItemCursorAdapter(Context context, int layout, Cursor cursor,
			String[] fromColumns, int[] to, HashMap<Integer, Integer> iconMap) {
		this(context, layout, cursor, fromColumns, to, defaultFieldHandlers(
				fromColumns, iconMap));
	}

	public ItemCursorAdapter(Context context, int layout, Cursor cursor,
			String[] fromColumns, int[] to, FieldHandler[] fieldHandlers) {
		this(context, null, layout, cursor, fromColumns, to, fieldHandlers);
	}

	// Newer constructor allows custom FieldHandlers.
	// Would be better to bundle fromColumn/to/fieldHandler for each field and
	// pass a single array
	// of those objects, but the overhead of defining that value class in Java
	// is not worth it.
	// If this were a Scala program, that would be a one-line case class.
	public ItemCursorAdapter(Context context, PodcastBaseFragment fragment,
			int layout, Cursor cursor, String[] fromColumns, int[] to,
			FieldHandler[] fieldHandlers) {
		super(context, layout, cursor, fromColumns, to);

		mContext = context;
		mFragment = fragment;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (to.length < fromColumns.length) {
			mTo2 = new int[fromColumns.length];
			for (int i = 0; i < to.length; i++)
				mTo2[i] = to[i];
			mTo2[fromColumns.length - 1] = R.id.icon;
		} else
			mTo2 = to;
		mFieldHandlers = fieldHandlers;
		if (cursor != null) {
			int i;
			int count = fromColumns.length;
			if (mFrom2 == null || mFrom2.length != count) {
				mFrom2 = new int[count];
			}
			for (i = 0; i < count; i++) {
				mFrom2[i] = cursor.getColumnIndexOrThrow(fromColumns[i]);
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View listViewItem;
		EpisodeViewHolder holder;

		Cursor itemCursor = (Cursor) getItem(position);

		if (!itemCursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}

		if (convertView == null) {
			listViewItem = newView(mContext, itemCursor, parent);
		} else {
			listViewItem = convertView;
		}

		mCurrentFeedItem = FeedItem.getByCursor(itemCursor);

		bindView(listViewItem, mContext, itemCursor);

		getPlayerViewHolder(position, listViewItem, itemCursor, convertView,
				parent);
		// bindExandedPlayer(itemCursor, listViewItem, position);

		return listViewItem;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View view = mInflater.inflate(R.layout.episode_list, null);

		EpisodeViewHolder holder = new EpisodeViewHolder();
		holder.icon = (NetworkImageView) view.findViewById(R.id.list_image);
		// NetworkImageView icon = (NetworkImageView)
		// view.getTag(R.id.list_image);
		holder.mainTitle = (TextView) view.findViewById(R.id.title);
		holder.subTitle = (TextView) view.findViewById(R.id.podcast);
		holder.timeDuration = (TextView) view.findViewById(R.id.duration);
		holder.currentPosition = (TextView) view.getTag(R.id.current_position);
		holder.slash = (TextView) view.findViewById(R.id.time_slash);
		holder.fileSize = (TextView) view.findViewById(R.id.filesize);
		holder.stub = (ViewStub) view.findViewById(R.id.stub);
		holder.playerView = (View) view.findViewById(R.id.stub_player);
		view.setTag(holder);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		try {
			final FeedItem item = mCurrentFeedItem;

			EpisodeViewHolder holder = (EpisodeViewHolder) view.getTag();

			DownloadStatus ds;
			if (item != null) {

				DownloadProgressListener.registerTextView(context,
						holder.fileSize, item);

				holder.icon.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						PodcastBaseFragment.mPlayerServiceBinder.toggle(item
								.getId());// .play();
					}
				});

				int filesize = 0;

				if (mDownloadManager == null) {
					mDownloadManager = (DownloadManager) mContext
							.getSystemService(Context.DOWNLOAD_SERVICE);
				}

				holder.fileSize.setText(item.getStatus(mDownloadManager));

				// item.setPriority(null, mContext);

				if (item.title != null) {
					String title = item.title;
					int priority = item.getPriority();
					long lastUpdate = item.getLastUpdate();

					String preTitle;
					if (MainActivity.debugging)
						preTitle = "p:" + priority + " t:" + lastUpdate;
					else
						preTitle = String.valueOf(priority);
					
					if (priority > 0 || MainActivity.debugging) {
						title = preTitle + " # " + title;
					} 
					
					if (COLOR_BACKGROUND)
						colorBackground(view, priority);
					
					holder.mainTitle.setText(title);
				}

				if (item.sub_title != null)
					holder.subTitle.setText(item.sub_title);

				if (item.getDuration() > 0) {

					holder.timeDuration.setText(item.duration_string);

					if (item.offset > 0 && filesize > 0) {
						String offsetText = StrUtils.formatTime(
								(float) item.offset / (float) filesize,
								item.duration_string);
						holder.currentPosition.setText(offsetText);
						holder.slash.setText("/");
						holder.slash.setVisibility(View.VISIBLE);
					} else {
						if (holder.currentPosition != null) {
							holder.currentPosition.setText("");
							holder.slash.setVisibility(View.GONE);
						}
					}
				}

				if (false && (item.image != null && !item.image.equals(""))) {

					// ImageLoader imageLoader = getImageLoader(context);
					// imageLoader.displayImage(item.image, holder.icon);

					com.android.volley.toolbox.ImageLoader imageLoader = ImageCacheManager
							.getInstance().getImageLoader();
					holder.icon.setImageUrl(item.image, imageLoader);

				} else {
					holder.icon.setImageResource(R.drawable.generic_podcast);	
				}

			}

		} catch (IllegalStateException e) {
		}

	}

	public View getPlayerViewHolder(int position, View listViewItem,
			Cursor itemCursor, View convertView, ViewGroup parent) {

		View viewHolder = null;

		View playerView = listViewItem.findViewById(R.id.stub_player);

		// Update the playlsit to reflect the position of the item
		// Playlist.updatePosition(mCurrentFeedItem, position); FIXME
		boolean firstItem = position == 0;

		if (firstItem || mExpandedItemID.contains(mCurrentFeedItem.getId())) {

			InlinePlayer holder = firstItem ? InlinePlayer
					.getCurrentEpisodePlayerViewHolder() : InlinePlayer
					.getSecondaryEpisodePlayerViewHolder();

			if (holder == null || holder.playerView == null) {
				playerView = newPlayerView(mContext, listViewItem, parent,
						holder);
				holder = (InlinePlayer) playerView.getTag();

				EpisodeViewHolder episodeHolder = (EpisodeViewHolder) listViewItem
						.getTag();
				episodeHolder.playerView = playerView;
			}

			if (firstItem)
				InlinePlayer.setCurrentEpisodePlayerViewHolder(holder);
			else
				InlinePlayer.setSecondaryEpisodePlayerViewHolder(holder);

			bindExandedPlayer(mContext, mCurrentFeedItem, playerView, holder, position);
		} else {
			if (playerView != null) {
				playerView.setVisibility(View.GONE);
			}
		}

		return viewHolder;
	}

	public View newPlayerView(Context context, View view, View parent,
			InlinePlayer viewHolder) {

		ViewStub stub = (ViewStub) view.findViewById(R.id.stub);
		if (stub != null) {
			stub.inflate();
		}

		View playerView = view.findViewById(R.id.stub_player);

		viewHolder = InlinePlayer.getViewHolder(view, viewHolder);
		viewHolder.stub = stub;

		// viewHolder.playerView = (TextView)
		// view.findViewById(R.id.stub_player);
		/*
		 * viewHolder.timeSlash = (TextView) view.findViewById(R.id.time_slash);
		 * viewHolder.currentTime = (TextView) view
		 * .findViewById(R.id.current_position); viewHolder.seekbar = (SeekBar)
		 * view.findViewById(R.id.progress); viewHolder.playPauseButton =
		 * (ImageButton) view .findViewById(R.id.play_toggle);
		 * viewHolder.stopButton = (ImageButton) view.findViewById(R.id.stop);
		 * viewHolder.downloadButton = (ImageButton) view
		 * .findViewById(R.id.download); viewHolder.infoButton = (ImageButton)
		 * view.findViewById(R.id.info);
		 * 
		 * // listview viewHolder.queueButton = (ImageButton)
		 * view.findViewById(R.id.queue); viewHolder.duration = (TextView)
		 * view.findViewById(R.id.duration); viewHolder.filesize = (TextView)
		 * view.findViewById(R.id.filesize);
		 */

		playerView.setTag(viewHolder);

		return playerView;
	}

	/**
	 * Expands the StubView and creates the expandable player. This is done for
	 * the current playing episode and at most one other episode which the user
	 * is interacting with
	 * 
	 * @param feedItem
	 * @param playerView
	 * @param holder
	 * @param position
	 */
	public static void bindExandedPlayer(Context context, FeedItem feedItem, View playerView,
			InlinePlayer holder, int position) {

		ThemeHelper themeHelper = new ThemeHelper(context);

		playerView.setVisibility(View.VISIBLE);
		holder.timeSlash.setText("/");
		holder.timeSlash.setVisibility(View.VISIBLE);

		long playerPosition = 0;
		long playerDuration = 0;

		if (position == 0
				&& PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
			playerPosition = PodcastBaseFragment.mPlayerServiceBinder
					.position();
			playerDuration = PodcastBaseFragment.mPlayerServiceBinder
					.duration();
		} else {
			playerPosition = feedItem.offset;
			playerDuration = feedItem.getDuration();
		}

		holder.currentTime.setText(StrUtils.formatTime(playerPosition));

		holder.seekbar.setMax(ControlButtons.MAX_SEEKBAR_VALUE);

		// PlayerActivity.setProgressBar(sb, feedItem);
		long secondary = 0;
		if (feedItem.filesize != 0) {
			secondary = feedItem.isDownloaded() ? feedItem.getCurrentFileSize()
					: (feedItem.chunkFilesize / feedItem.filesize);
		}

		PlayerActivity.setProgressBar(holder.seekbar, playerDuration,
				playerPosition, secondary);

		ControlButtons.setPlayerListeners(holder, feedItem.getId());

		if (feedItem.isDownloaded()) {
			holder.downloadButton.setBackgroundResource(themeHelper
					.getAttr(R.attr.delete_icon));
		}

		if (PodcastBaseFragment.mPlayerServiceBinder.isInitialized()) {
			if (feedItem.getId() == PodcastBaseFragment.mPlayerServiceBinder
					.getCurrentItem().id) {
				if (PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
					PodcastBaseFragment.setCurrentTime(holder.currentTime);
					holder.playPauseButton.setBackgroundResource(themeHelper
							.getAttr(R.attr.pause_icon));
				}
			}

		}

	}

	public void showItem(Long id) {
		if (!mExpandedItemID.isEmpty())
			mExpandedItemID.remove(mExpandedItemID.first()); // HACK: only show
																// one expanded
																// at the time
		mExpandedItemID.add(id);
	}

	public void toggleItem(Cursor item) {
		Long itemID = item.getLong(item.getColumnIndex(BaseColumns._ID));
		toggleItem(itemID);
	}

	public void toggleItem(Long id) {
		if (mExpandedItemID.contains(id)) // ItemColumns._ID
			mExpandedItemID.remove(id);
		else {
			showItem(id);
		}
	}

	@Override
	public int getViewTypeCount() {
		return TYPE_MAX_COUNT;
	}

	@Override
	public int getItemViewType(int position) {
		return mExpandedItemID.contains(itemID(position)) ? TYPE_EXPAND
				: TYPE_COLLAPS;
	}

	private void setOverlay(View overlay, boolean isOn) {
		int opacity = isOn ? 150 : 0; // from 0 to 255
		overlay.setBackgroundColor(opacity * 0x1000000); // black with a
															// variable alpha
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.FILL_PARENT);
		params.gravity = Gravity.BOTTOM;
		overlay.setLayoutParams(params);
		// overlay.invalidate(); // update the view
	}

	/**
	 * Returns the ID of the item at the position
	 * 
	 * @param position
	 * @return ID of the FeedItem
	 */
	private Long itemID(int position) {
		Object item = getItem(position);
		if (item instanceof FeedItem) {
			FeedItem feedItem = (FeedItem) item;
			return Long.valueOf(feedItem.id);
		} else
			return new Long(1); // FIXME
	}
	
	/**
	 * Color the background of the playlist.
	 * This has performance issues.
	 */
	private void colorBackground(View view, int priority) {
		// Only draw the background if it is wrong.
		int defaultColor = mContext.getResources().getColor(
				R.color.default_background);
		int playlistColor = mContext.getResources().getColor(
				R.color.playlist_background);
		
		Drawable background = view.getBackground();
		int currentColor = (background != null) ? ((ColorDrawable)background).getColor() : defaultColor;
		int newColor = -1;
		
		if (priority > 0) {
			newColor = playlistColor;
		} else {
			newColor = defaultColor;
		}
		if (currentColor != newColor)
			view.setBackgroundColor(newColor);
	}

}
