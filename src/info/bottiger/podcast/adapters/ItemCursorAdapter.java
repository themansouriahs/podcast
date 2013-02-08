package info.bottiger.podcast.adapters;

import info.bottiger.podcast.PlayerActivity;
import info.bottiger.podcast.PodcastBaseFragment;
import info.bottiger.podcast.R;
import info.bottiger.podcast.listeners.DownloadProgressListener;
import info.bottiger.podcast.provider.BitmapProvider;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.provider.WithIcon;
import info.bottiger.podcast.service.DownloadStatus;
import info.bottiger.podcast.service.PodcastDownloadManager;
import info.bottiger.podcast.utils.ControlButtons;
import info.bottiger.podcast.utils.FilesizeUpdater;
import info.bottiger.podcast.utils.SDCardManager;
import info.bottiger.podcast.utils.StrUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;


public class ItemCursorAdapter extends AbstractPodcastAdapter {

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

	// Create a set of FieldHandlers for methods calling using the legacy
	// constructor
	private static FieldHandler[] defaultFieldHandlers(String[] fromColumns,
			HashMap<Integer, Integer> iconMap) {
		FieldHandler[] handlers = new FieldHandler[fromColumns.length];
		for (int i = 0; i < handlers.length - 1; i++) {
			handlers[i] = defaultTextFieldHandler;
		}
		handlers[fromColumns.length - 1] = new FieldHandler.IconFieldHandler(iconMap);
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

		bindView(listViewItem, mContext, itemCursor);

		Long itemID = itemCursor.getLong(itemCursor
				.getColumnIndex(BaseColumns._ID));

		FeedItem feedItem = FeedItem.getById(mContext.getContentResolver(),
				itemID);

		int pathIndex = itemCursor.getColumnIndex(ItemColumns.PATHNAME);
		// int itemOffset =
		// item.getInt(item.getColumnIndex(ItemColumns.OFFSET));

		long playingID;
		try {
			playingID = PodcastBaseFragment.mPlayerServiceBinder
					.getCurrentItem().id;
		} catch (Exception e) {
			playingID = 0;
		}

		if (mExpandedItemID.contains(itemID) || itemID == playingID) {
			ViewStub stub = (ViewStub) listViewItem.findViewById(R.id.stub);
			if (stub != null) {
				stub.inflate();
			}

			View playerView = listViewItem.findViewById(R.id.stub_player);
			playerView.setVisibility(View.VISIBLE);

			TextView timeSlash = (TextView) listViewItem
					.findViewById(R.id.time_slash);
			timeSlash.setText("/");
			timeSlash.setVisibility(View.VISIBLE);

			TextView currentTime = (TextView) listViewItem
					.findViewById(R.id.current_position);
			currentTime.setText(StrUtils.formatTime(feedItem));

			SeekBar sb = (SeekBar) playerView.findViewById(R.id.progress);
			sb.setMax(1000);

			PlayerActivity.setProgressBar(sb, feedItem);

			ControlButtons.setPlayerListeners(listViewItem, playerView, itemID);

			if (feedItem.isDownloaded()) {
				ImageButton downloadButton = (ImageButton) playerView
						.findViewById(R.id.download);
				downloadButton.setImageResource(R.drawable.ic_action_delete);
			}

			if (PodcastBaseFragment.mPlayerServiceBinder.isInitialized()) {
				if (itemID == PodcastBaseFragment.mPlayerServiceBinder
						.getCurrentItem().id) {
					if (PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
						ImageButton playPauseButton = (ImageButton) listViewItem
								.findViewById(R.id.play_toggle);
						playPauseButton.setImageResource(R.drawable.av_pause);

						// SeekBar sb = (SeekBar) v.findViewById(R.id.progress);
						TextView tv = (TextView) listViewItem
								.findViewById(R.id.current_position);
						mFragment.setProgressBar(sb);
						mFragment.setCurrentTime(tv);
					} else {
						// ImageButton playPauseButton = (ImageButton) v
						// .findViewById(R.id.play_toggle);
						// playPauseButton.setImageResource(R.drawable.play);
					}
				}

			}

		} else {
			View playerView = listViewItem.findViewById(R.id.stub_player);
			if (playerView != null) {
				playerView.setVisibility(View.GONE);
			}
		}

		return listViewItem;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View view = mInflater.inflate(R.layout.list_item, null);

		view.setTag(R.id.list_image, view.findViewById(R.id.list_image));
		view.setTag(R.id.title, view.findViewById(R.id.title));
		view.setTag(R.id.podcast, view.findViewById(R.id.podcast));
		view.setTag(R.id.duration, view.findViewById(R.id.duration));
		view.setTag(R.id.current_position,
				view.findViewById(R.id.current_position));
		view.setTag(R.id.time_slash, view.findViewById(R.id.time_slash));
		view.setTag(R.id.filesize, view.findViewById(R.id.filesize));
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		FeedItem item = null;
		try {
			item = FeedItem.getByCursor(cursor);
		} catch (IllegalStateException e) {
		}

		Subscription sub = null;
		try {
			sub = Subscription.getByCursor(cursor);
		} catch (IllegalStateException e) {
		}

		/*
		 * http://drasticp.blogspot.dk/2012/04/viewholder-is-dead.html
		 */
		ImageView icon = (ImageView) view.getTag(R.id.list_image);
		TextView mainTitle = (TextView) view.getTag(R.id.title);
		TextView subTitle = (TextView) view.getTag(R.id.podcast);
		TextView timeDuration = (TextView) view.getTag(R.id.duration);
		TextView currentPosition = (TextView) view
				.getTag(R.id.current_position);
		TextView slash = (TextView) view.getTag(R.id.time_slash);
		TextView fileSize = (TextView) view.getTag(R.id.filesize);

		DownloadStatus ds;
		if (item != null) {
			// FeedItem.getById(context.getContentResolver(), id)
			
			
			//ds = PodcastDownloadManager.getStatus(item);
			//FilesizeUpdater.put(mContext, item.getId(), fileSize);
			//writeStatus(item.getId(), fileSize, ds);
			
			
			DownloadProgressListener.registerTextView(context, fileSize, item);

			int filesize = 0;
			
			DownloadManager downloadManager = (DownloadManager) mContext
					.getSystemService(Context.DOWNLOAD_SERVICE);
			fileSize.setText(item.getStatus(downloadManager));

			if (item.title != null)
				mainTitle.setText(item.title);

			if (item.sub_title != null)
				subTitle.setText(item.sub_title);

			if (item.getDuration() > 0) {

				timeDuration.setText(item.duration_string);

				if (item.offset > 0 && filesize > 0) {
					String offsetText = StrUtils.formatTime((float) item.offset
							/ (float) filesize, item.duration_string);
					currentPosition.setText(offsetText);
					slash.setText("/");
					slash.setVisibility(View.VISIBLE);
				} else {
					currentPosition.setText("");
					slash.setVisibility(View.GONE);
				}
			}

			if (item.image != null && !item.image.equals("")) {
				ImageLoader imageLoader = getImageLoader(context);
				imageLoader.displayImage(item.image, icon);
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

}
