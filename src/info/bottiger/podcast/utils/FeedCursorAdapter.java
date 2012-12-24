package info.bottiger.podcast.utils;

import android.widget.SimpleCursorAdapter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.app.Activity;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import info.bottiger.podcast.PlayerActivity;
import info.bottiger.podcast.PodcastBaseFragment;
import info.bottiger.podcast.R;
import info.bottiger.podcast.RecentItemFragment;
import info.bottiger.podcast.SwipeActivity;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.service.PodcastDownloadManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;

public class FeedCursorAdapter extends SimpleCursorAdapter {

	public static final int ICON_DEFAULT_ID = -1;

	public interface FieldHandler {
		public void setViewValue(FeedCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId);
	}

	public static class TextFieldHandler implements FieldHandler {
		public void setViewValue(FeedCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId) {
			// Normal text column, just display what's in the database
			String text = cursor.getString(fromColumnId);

			if (text == null) {
				text = "";
			}

			if (v instanceof TextView) {
				adapter.setViewText((TextView) v, text);
			} else if (v instanceof ImageView) {
				adapter.setViewImage((ImageView) v, text);
			}
		}
	}

	public static class IconFieldHandler implements FieldHandler {
		public IconFieldHandler(HashMap<Integer, Integer> iconMap) {
		}

		public IconFieldHandler() {
		}

		public void setViewValue(FeedCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId) {
			adapter.setViewImage3((ImageView) v, cursor.getString(fromColumnId));
		}
	}

	/*
	 * public static class IconFieldHandler implements FieldHandler {
	 * HashMap<Integer, Integer> mIconMap; public
	 * IconFieldHandler(HashMap<Integer,Integer> iconMap) { mIconMap = iconMap;
	 * } public void setViewValue(IconCursorAdapter adapter, Cursor cursor, View
	 * v, int fromColumnId) { //The status column gets displayed as our icon int
	 * status = cursor.getInt(fromColumnId);
	 * 
	 * Integer iconI = mIconMap.get(status); if (iconI==null) iconI =
	 * mIconMap.get(ICON_DEFAULT_ID); //look for default value in map int icon =
	 * (iconI!=null)? iconI.intValue(): R.drawable.status_unknown; //Use this
	 * icon when not in map and no map default. //This allows going back to a
	 * previous version after data has been //added in a new version with
	 * additional status codes. adapter.setViewImage2((ImageView) v, icon); } }
	 */
	public final static FieldHandler defaultTextFieldHandler = new TextFieldHandler();

	protected int[] mFrom2;
	protected int[] mTo2;
	protected FieldHandler[] mFieldHandlers;

	private static final int TYPE_COLLAPS = 0;
	private static final int TYPE_EXPAND = 1;
	private static final int TYPE_MAX_COUNT = 2;

	private ArrayList<FeedItem> mData = new ArrayList<FeedItem>();
	private LayoutInflater mInflater;
	private Context mContext;
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
		handlers[fromColumns.length - 1] = new IconFieldHandler(iconMap);
		return handlers;
	}

	// Legacy constructor
	public FeedCursorAdapter(Context context, int layout, Cursor cursor,
			String[] fromColumns, int[] to, HashMap<Integer, Integer> iconMap) {
		this(context, layout, cursor, fromColumns, to, defaultFieldHandlers(
				fromColumns, iconMap));
	}

	public FeedCursorAdapter(Context context, int layout, Cursor cursor,
			String[] fromColumns, int[] to, FieldHandler[] fieldHandlers) {
		this(context, null, layout, cursor,
				fromColumns, to, fieldHandlers);
	}
	
	// Newer constructor allows custom FieldHandlers.
	// Would be better to bundle fromColumn/to/fieldHandler for each field and
	// pass a single array
	// of those objects, but the overhead of defining that value class in Java
	// is not worth it.
	// If this were a Scala program, that would be a one-line case class.
	public FeedCursorAdapter(Context context, PodcastBaseFragment fragment, int layout, Cursor cursor,
			String[] fromColumns, int[] to, FieldHandler[] fieldHandlers) {
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

		View v;
		Cursor itemCursor = (Cursor) getItem(position);
		
		if (!itemCursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}
		
		if (convertView == null) {
			v = newView(mContext, itemCursor, parent);
		} else {
			v = convertView;
		}
		
		bindView(v, mContext, itemCursor);

		Long itemID = itemCursor.getLong(itemCursor.getColumnIndex(ItemColumns._ID));

		FeedItem feedItem = FeedItem.getById(mContext.getContentResolver(), itemID);
		
		int pathIndex = itemCursor.getColumnIndex(ItemColumns.PATHNAME);
		// int itemOffset =
		// item.getInt(item.getColumnIndex(ItemColumns.OFFSET));
		
		long playingID;
		try {
			playingID = PodcastBaseFragment.mPlayerServiceBinder.getCurrentItem().id;
		} catch (Exception e) {
			playingID = 0;
		}

		if (mExpandedItemID.contains(itemID) || itemID == playingID) {
			ViewStub stub = (ViewStub) v.findViewById(R.id.stub);
			if (stub != null) {
				stub.inflate();
			}
			
			View playerView = v.findViewById(R.id.stub_player);
			playerView.setVisibility(View.VISIBLE);
			
			TextView timeSlash = (TextView)v.findViewById(R.id.time_slash);
			timeSlash.setText("/");
			timeSlash.setVisibility(View.VISIBLE);
			
			TextView currentTime = (TextView)v.findViewById(R.id.current_position);
			currentTime.setText(StrUtils.formatTime(feedItem));
			
			SeekBar sb = (SeekBar)playerView.findViewById(R.id.progress);
			sb.setMax(1000);
			
			PlayerActivity.setProgressBar(sb, feedItem);

			ControlButtons.setPlayerListeners(v, playerView, itemID);

			if (feedItem.isDownloaded()) {
				ImageButton downloadButton = (ImageButton) playerView.findViewById(R.id.download);
				downloadButton.setImageResource(R.drawable.trash);
			}
			
			if (PodcastBaseFragment.mPlayerServiceBinder.isInitialized()) {
				if (itemID == PodcastBaseFragment.mPlayerServiceBinder
						.getCurrentItem().id) {
					if (PodcastBaseFragment.mPlayerServiceBinder.isPlaying()) {
						ImageButton playPauseButton = (ImageButton) v
							.findViewById(R.id.play_toggle);
						playPauseButton.setImageResource(R.drawable.pause);
						
						//SeekBar sb = (SeekBar) v.findViewById(R.id.progress); 
						TextView tv = (TextView) v.findViewById(R.id.current_position); 
						mFragment.setProgressBar(sb);
						mFragment.setCurrentTime(tv);
					} else {
						//ImageButton playPauseButton = (ImageButton) v
						//		.findViewById(R.id.play_toggle);
						//playPauseButton.setImageResource(R.drawable.play);
					}
				}
				

			}
			
		} else {
			View playerView = v.findViewById(R.id.stub_player);
			if (playerView != null) {
				playerView.setVisibility(View.GONE);
			}
		}

		return v;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder viewHolder = new ViewHolder();
		View v = null;

		Long cursorID = cursor.getLong(cursor.getColumnIndex("_id"));
		boolean doExpand = mExpandedItemID.contains(cursorID);

		v = mInflater.inflate(R.layout.list_item, null);

		viewHolder.textViewTitle = (TextView) v.findViewById(R.id.title);
		viewHolder.textViewSubTitle = (TextView) v.findViewById(R.id.podcast);
		viewHolder.textViewFileSize = (TextView) v.findViewById(R.id.filesize);
		viewHolder.imageView = (ImageView) v.findViewById(R.id.list_image);
		viewHolder.textViewCurrentTime = (TextView) v
				.findViewById(R.id.current_position);
		viewHolder.textViewDuration = (TextView) v.findViewById(R.id.duration);
		viewHolder.textViewSlash = (TextView) v.findViewById(R.id.time_slash);
		viewHolder.textFileSize = (TextView) v.findViewById(R.id.filesize);

		v.setTag(viewHolder);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();

		int titleIndex = cursor.getColumnIndex(ItemColumns.TITLE);
		String title = cursor.getString(titleIndex);

		int idIndex = cursor.getColumnIndex(ItemColumns._ID);
		long id = cursor.getLong(idIndex);

		int subtitleIndex = cursor.getColumnIndex(ItemColumns.SUB_TITLE);
		int imageIndex = cursor.getColumnIndex(ItemColumns.IMAGE_URL);

		int durationIndex = cursor.getColumnIndex(ItemColumns.DURATION);
		int offsetIndex = cursor.getColumnIndex(ItemColumns.OFFSET);
		int lengthIndex = cursor.getColumnIndex(ItemColumns.LENGTH);

		
		int filePathIndex = cursor.getColumnIndex(ItemColumns.PATHNAME);
		String filePath;
		
		PodcastDownloadManager.DownloadStatus ds = null;
		
		try {
			filePath = cursor.getString(filePathIndex);
		} catch (Exception e) {
			filePath = "";
		}

		FeedItem item = null;
		
		try {
			item = FeedItem.getByCursor(cursor);
		} catch (IllegalStateException e) {
			//Subscription sub = Subscription.getByCursor(cursor);
		}
			
		int statusIndex = cursor.getColumnIndex(ItemColumns.STATUS);
		
		if (item != null) {
			// FeedItem.getById(context.getContentResolver(), id)
			ds = PodcastDownloadManager.getStatus(item);
			FilesizeUpdater.put(mContext, id, holder.textViewFileSize);
			writeStatus(id, holder.textViewFileSize, ds);
		}

		
		int filesize = 0;
		/*
		try {
			int filesizeIndex = cursor
					.getColumnIndexOrThrow(ItemColumns.FILESIZE);
			filesize = cursor.getInt(filesizeIndex);
			
			if (filesize > 0)
				holder.textViewFileSize.setText(filesize / 1024 / 1024 + " MB");
		} catch (IllegalArgumentException e) {

		}
		*/

		holder.textViewTitle.setText(title);

		if (durationIndex > 0) {
			String duration = cursor.getString(durationIndex);
			int offset = cursor.getInt(offsetIndex);

			holder.textViewDuration.setText(duration);

			// if (offset > 0 || status == ItemColumns.ITEM_STATUS_PLAYING_NOW )
			// {

			// if (status != ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			if (offset > 0 && filesize > 0) {
				String offsetText = StrUtils.formatTime((float)offset/(float)filesize, duration);
				holder.textViewCurrentTime.setText(offsetText);
				holder.textViewSlash.setText("/");
				holder.textViewSlash.setVisibility(View.VISIBLE);
			} else {
				holder.textViewCurrentTime.setText("");
				holder.textViewSlash.setVisibility(View.GONE);
			}
		}

		if (subtitleIndex > 0)
			holder.textViewSubTitle.setText(cursor.getString(subtitleIndex));

		
		/* Calculate the imageOath */
		String imageURL;
		String fullPath = SDCardManager.pathFromFilename(filePath);
		if (filePath != null && filePath.length() > 0 && new File(fullPath).exists() && ds == PodcastDownloadManager.DownloadStatus.DONE) {
			//holder.imageView.setImageBitmap(cover);
			FileOutputStream out;
			String thumbnailPath = thumbnailCacheURL(cursor.getString(idIndex));
			File thumbnail = new File(thumbnailPath);
			String urlPrefix = "file://";
			if (!thumbnail.exists()) {
			try {
				MediaMetadataRetriever mmr = new MediaMetadataRetriever();
				mmr.setDataSource(fullPath);
				byte[] ba = mmr.getEmbeddedPicture();
				Bitmap cover = BitmapFactory.decodeByteArray(ba, 0, ba.length);
				out = new FileOutputStream(thumbnailPath);
				cover.compress(Bitmap.CompressFormat.PNG, 90, out);
				imageURL = urlPrefix + thumbnailPath;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				imageURL = urlPrefix + cursor.getString(imageIndex);
			} catch (RuntimeException e) {
				e.printStackTrace();
				imageURL = cursor.getString(imageIndex);
			}
			} else {
				imageURL = urlPrefix + thumbnailPath;
			}
		} else {
			//this.setViewImage3(holder.imageView, cursor.getString(imageIndex));
			imageURL = cursor.getString(imageIndex);
		}
		
		File cacheDir = SDCardManager.getCacheDir();
		DisplayImageOptions options = new DisplayImageOptions.Builder()
		.showStubImage(R.drawable.channel_big_pic)
        .cacheInMemory()
        .cacheOnDisc()
        .build();
		ImageLoader imageLoader = ImageLoader.getInstance();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
			.memoryCacheExtraOptions(480, 800) // max width, max height
			.threadPoolSize(5)
			.offOutOfMemoryHandling()
			.memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // You can pass your own memory cache implementation
			.discCache(new UnlimitedDiscCache(cacheDir)) // You can pass your own disc cache implementation
            .discCacheFileNameGenerator(new HashCodeFileNameGenerator())
            .imageDownloader(new URLConnectionImageDownloader(5 * 1000, 20 * 1000)) // connectTimeout (5 s), readTimeout (20 s)
            .tasksProcessingOrder(QueueProcessingType.FIFO)
            .defaultDisplayImageOptions(options)
            .build();
		// Initialize ImageLoader with configuration. Do it once.
		imageLoader.init(config);
		// Load and display image asynchronously
		//imageLoader.displayImage(cursor.getString(imageIndex), holder.imageView);
		if (imageURL != null && !imageURL.equals(""))
			imageLoader.displayImage(imageURL, holder.imageView);
	}

	public void showItem(Long id) {
		if (!mExpandedItemID.isEmpty())
			mExpandedItemID.remove(mExpandedItemID.first()); // HACK: only show
																// one expanded
																// at the time
		mExpandedItemID.add(id);
	}

	public void toggleItem(Cursor item) {
		Long itemID = item.getLong(item.getColumnIndex(ItemColumns._ID));
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

	public static class ViewHolder {
		public ImageView imageView;
		public TextView textViewTitle;
		public TextView textViewSubTitle;
		public TextView textViewDuration;
		public TextView textViewFileSize;
		public TextView textViewCurrentTime;
		public TextView textViewSlash;
		public TextView textFileSize;
	}
	
	private void writeStatus(long id, TextView tv, PodcastDownloadManager.DownloadStatus ds) {
		String statusText = "";
		switch (ds) {
		case PENDING:
			statusText = "waiting";
			break;
		case DOWNLOADING:
			statusText = "downloading";
			break;
		case DONE:
			statusText = "Done";
			break;
		case ERROR:
			statusText = "Error";
			break;
		default:
			statusText = "";
		}
		tv.setText(statusText);
	}

	private void setViewImage3(ImageView v, String imageURL) {
		// https://github.com/koush/UrlImageViewHelper#readme
		int cacheTime = 60000 * 60 * 24 * 31; // in ms
		UrlImageViewHelper.loadUrlDrawable(v.getContext(), imageURL);
		UrlImageViewHelper.setUrlDrawable(v, imageURL,
				R.drawable.generic_podcast, cacheTime);
	}

	private Long itemID(int position) {
		Object item = getItem(position);
		if (item instanceof FeedItem) {
			FeedItem feedItem = (FeedItem) item;
			return Long.valueOf(feedItem.id);
		} else
			return new Long(1); // FIXME
	}
	
	private String thumbnailCacheURL(String StringID) {
		//Long id = Long.parseLong(StringID);
		String thumbURL = SDCardManager.getThumbnailCacheDir() + "/" + StringID + ".png";
		return thumbURL;
	}

}
