package info.bottiger.podcast.provider;

import info.bottiger.podcast.PlayerActivity;
import info.bottiger.podcast.R;
import info.bottiger.podcast.service.PodcastDownloadManager;
import info.bottiger.podcast.utils.FileUtils;
import info.bottiger.podcast.utils.Log;
import info.bottiger.podcast.utils.SDCardManager;
import info.bottiger.podcast.utils.StrUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.util.LruCache;
import android.widget.Toast;

public class FeedItem implements Comparable<FeedItem>, WithIcon {

	public static final int MAX_DOWNLOAD_FAIL = 5;

	private final Log log = Log.getLog(getClass());
	private static ItemLruCache cache = null;

	/*
	 * Let's document these retared fields! They are totally impossible to guess
	 * the meaning of.
	 */

	/*
	 * Unique ID
	 */
	public long id;

	/*
	 * URL of the episode:
	 * http://podcast.dr.dk/P1/p1debat/2013/p1debat_1301171220.mp3
	 */
	public String url;

	/*
	 * Title of the episode
	 */
	public String title;

	/*
	 * Name of Publisher
	 */
	public String author;

	/*
	 * Date Published
	 */
	public String date;

	/*
	 * Episode description in text
	 */
	public String content;

	/*
	 * 
	 */
	public String resource;

	/*
	 * Duration as String hh:mm:ss or mm:ss 02:23:34
	 */
	public String duration;

	/*
	 * URL to relevant episode image
	 */
	public String image;

	/*
	 * Unique ID of the subscription the episode belongs to
	 */
	public long sub_id;

	/*
	 * Total size of the episode in bytes
	 */
	public long filesize;

	/*
	 * Size of the file on disk in bytes
	 */
	public long chunkFilesize;

	/*
	 * Filename of the episode on disk. sn209.mp3
	 */
	private String pathname;

	/*
	 * Last position during playback in ms Should match seekTo(int)
	 * http://developer
	 * .android.com/reference/android/media/MediaPlayer.html#seekTo(int)
	 */
	public int offset;

	/*
	 * Deprecated status from before forking
	 */
	@Deprecated
	public int status;

	@Deprecated
	public long failcount;
	// failcount is currently used for two purposes:
	// 1. counts the number of times we fail to download, and
	// when we exceed a predefined max, we pause the download.
	// 2. when an item is in the player, failcount is used as
	// the order of the item in the list.

	/*
	 * Have to listened to this episode yet?
	 */
	public int listened;

	@Deprecated
	public long length;

	/*
	 * The time the record in the database was updated the last time. measured
	 * in: System.currentTimeMillis()
	 */
	public long update;

	/*
	 * The URI of the podcast episode
	 */
	@Deprecated
	public String uri;

	/*
	 * Title of the parent subscription
	 */
	public String sub_title;

	@Deprecated
	public long created;

	@Deprecated
	public String type;

	@Deprecated
	private long m_date;

	static String[] DATE_FORMATS = { "EEE, dd MMM yyyy HH:mm:ss Z",
			"EEE, d MMM yy HH:mm z", "EEE, d MMM yyyy HH:mm:ss z",
			"EEE, d MMM yyyy HH:mm z", "d MMM yy HH:mm z",
			"d MMM yy HH:mm:ss z", "d MMM yyyy HH:mm z",
			"d MMM yyyy HH:mm:ss z", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss",
			"EEE,dd MMM yyyy HH:mm:ss Z", };

	static String default_format = "EEE, dd MMM yyyy HH:mm:ss Z";

	public static void view(Activity act, long item_id) {
		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, item_id);
		act.startActivity(new Intent(Intent.ACTION_EDIT, uri));
	}

	@Deprecated
	public static FeedItem getBySQL(ContentResolver context, String where,
			String order) {
		FeedItem item = null;
		Cursor cursor = null;

		try {
			cursor = context.query(ItemColumns.URI, ItemColumns.ALL_COLUMNS,
					where, null, order);
			if (cursor.moveToFirst()) {
				item = FeedItem.getByCursor(cursor);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return item;

	}

	public static FeedItem getById(ContentResolver contentResolver, long id) {
		Cursor cursor = null;
		FeedItem item = null;

		initCache();

		// Return item directly if cached
		synchronized (cache) {
			item = cache.get(id);
			if (item != null) {
				return item;
			}
		}

		try {
			String where = BaseColumns._ID + " = " + id;

			cursor = contentResolver.query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			if (cursor.moveToFirst()) {
				item = fetchFromCursor(cursor);

				cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {

			if (cursor != null)
				cursor.close();

		}

		return item;
	}

	public static FeedItem getByCursor(Cursor cursor) {
		FeedItem item = fetchFromCursor(cursor);
		return item;
	}

	public FeedItem() {
		url = null;
		title = null;
		author = null;
		date = null;
		content = null;
		resource = null;
		duration = null;
		pathname = null;
		uri = null;
		type = null;
		image = null;

		id = -1;
		offset = -1;
		status = -1;
		failcount = -1;
		length = -1;
		update = -1;
		listened = -1;
		filesize = -1;
		chunkFilesize = -1;

		created = -1;
		sub_title = null;
		sub_id = -1;

		m_date = -1;

	}

	public void updateOffset(ContentResolver context, long i) {
		offset = (int) i;
		update = -1;
		update(context);

	}

	/*
	 * Update the FeedItem in the database
	 */
	public void update(ContentResolver context) {
		log.debug("item update start");

		initCache();
		if (this.id > 0)
			cache.remove(this.id);

		try {

			ContentValues cv = new ContentValues();
			if (pathname != null)
				cv.put(ItemColumns.PATHNAME, pathname);
			if (filesize >= 0)
				cv.put(ItemColumns.FILESIZE, filesize);
			if (chunkFilesize >= 0)
				cv.put(ItemColumns.CHUNK_FILESIZE, chunkFilesize);
			if (offset >= 0)
				cv.put(ItemColumns.OFFSET, offset);
			if (update >= 0) {
				update = Long.valueOf(System.currentTimeMillis());
				cv.put(ItemColumns.LAST_UPDATE, update);
			}
			if (listened >= 0)
				cv.put(ItemColumns.LISTENED, listened);

			context.update(ItemColumns.URI, cv, BaseColumns._ID + "=" + id,
					null);

			log.debug("update OK");
		} finally {
		}
	}

	public Uri insert(ContentResolver context) {
		log.debug("item insert start");
		try {

			ContentValues cv = new ContentValues();
			if (pathname != null)
				cv.put(ItemColumns.PATHNAME, pathname);
			if (offset >= 0)
				cv.put(ItemColumns.OFFSET, offset);
			if (update >= 0)
				cv.put(ItemColumns.LAST_UPDATE, update);

			if (sub_id >= 0)
				cv.put(ItemColumns.SUBS_ID, sub_id);
			if (url != null)
				cv.put(ItemColumns.URL, url);
			if (image != null)
				cv.put(ItemColumns.IMAGE_URL, image);
			if (title != null)
				cv.put(ItemColumns.TITLE, title);

			if (author != null)
				cv.put(ItemColumns.AUTHOR, author);
			if (date != null)
				cv.put(ItemColumns.DATE, date);
			if (content != null)
				cv.put(ItemColumns.CONTENT, content);
			if (resource != null)
				cv.put(ItemColumns.RESOURCE, resource);
			if (filesize >= 0)
				cv.put(ItemColumns.FILESIZE, filesize);
			if (duration != null) {
				// Log.w("ITEM","  duration: " + duration);
				cv.put(ItemColumns.DURATION, duration);
			}
			if (sub_title != null) {
				cv.put(ItemColumns.SUB_TITLE, sub_title);
			}
			if (listened >= 0)
				cv.put(ItemColumns.LISTENED, listened);

			return context.insert(ItemColumns.URI, cv);

		} finally {
		}
	}

	@Deprecated
	public long getDate() {
		// log.debug(" getDate() start");

		if (m_date < 0) {
			m_date = parse();
			// log.debug(" getDate() end " + default_format);

		}

		return m_date;

	}

	@Deprecated
	private long parse() {
		long l = 0;
		try {
			return new SimpleDateFormat(default_format, Locale.US).parse(date)
					.getTime();
		} catch (ParseException e) {
			log.debug(" first fail");
		}

		for (String format : DATE_FORMATS) {
			try {
				l = new SimpleDateFormat(format, Locale.US).parse(date)
						.getTime();
				default_format = format;
				return l;
			} catch (ParseException e) {
			}
		}
		log.warn("cannot parser date: " + date);
		return 0L;
	}

	private static FeedItem fetchFromCursor(Cursor cursor) {
		FeedItem item = new FeedItem();

		item.id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

		// Return item directly if cached
		initCache();
		synchronized (cache) {
			FeedItem cacheItem = cache.get(item.id);
			if (cacheItem != null && cacheItem.title != "") { // FIXME
																// cacheItem.title
																// != ""
				item = cacheItem;
				return item;
			}
		}

		int idx = cursor.getColumnIndex(ItemColumns.RESOURCE);
		item.resource = cursor.getString(idx);
		item.pathname = cursor.getString(cursor
				.getColumnIndex(ItemColumns.PATHNAME));
		item.offset = cursor.getInt(cursor.getColumnIndex(ItemColumns.OFFSET));
		item.url = cursor.getString(cursor.getColumnIndex(ItemColumns.URL));
		item.image = cursor.getString(cursor
				.getColumnIndex(ItemColumns.IMAGE_URL));
		item.title = cursor.getString(cursor.getColumnIndex(ItemColumns.TITLE));
		item.author = cursor.getString(cursor
				.getColumnIndex(ItemColumns.AUTHOR));
		item.date = cursor.getString(cursor.getColumnIndex(ItemColumns.DATE));
		item.content = cursor.getString(cursor
				.getColumnIndex(ItemColumns.CONTENT));
		item.filesize = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.FILESIZE));
		item.chunkFilesize = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.CHUNK_FILESIZE));
		item.duration = cursor.getString(cursor
				.getColumnIndex(ItemColumns.DURATION));
		item.update = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.LAST_UPDATE));
		item.sub_title = cursor.getString(cursor
				.getColumnIndex(ItemColumns.SUB_TITLE));
		item.sub_id = cursor
				.getLong(cursor.getColumnIndex(ItemColumns.SUBS_ID));
		item.listened = cursor.getInt(cursor
				.getColumnIndex(ItemColumns.LISTENED));

		// if item was not cached we put it in the cache
		synchronized (cache) {
			cache.put(item.id, item);
		}

		return item;
	}

	@Override
	public String toString() {
		return "Feed: " + title;
	}

	public void delFile(ContentResolver contentResolver) {

		if (SDCardManager.getSDCardStatus()) {
			try {
				File file = new File(getPathname());

				boolean deleted = true;
				if (file.exists()) {
					deleted = file.delete();
				}
				if (deleted) {
					update(contentResolver);
				}
			} catch (Exception e) {
				log.warn("del file failed : " + getPathname() + "  " + e);

			}
		}

	}

	public void prepareDownload(ContentResolver context) {
		if (getPathname().equals("") || getPathname().equals("0")) {
			String filename = resource.substring(resource.lastIndexOf("/") + 1);
			pathname = this.sub_id + "_" + filename + ".mp3";
		}
		update(context);
	}

	public void downloadSuccess(ContentResolver contentResolver) {
		filesize = getCurrentFileSize();
		update(contentResolver);
	}

	public void endDownload(ContentResolver context) {
		update = Long.valueOf(System.currentTimeMillis());
		update(context);
	}

	public long getCurrentFileSize() {
		String filename = getPathname();
		if (filename != null)
			return new File(filename).length();
		return 0;
	}

	public String getPathname() {
		return SDCardManager.pathFromFilename(this.pathname);
	}

	public void setPosition(ContentResolver contentResolver, long pos) {
		this.offset = (int) pos;
		update(contentResolver);
	}

	@Override
	public int compareTo(FeedItem another) {
		if (this.update > another.update)
			return 1;
		else if (this.update < another.update)
			return -1;

		return 0;
	}

	public String getURL() {
		String itemURL = "";
		if (resource.length() > 1)
			itemURL = resource;
		else if (url.length() > 1)
			itemURL = url;
		return itemURL;
	}

	public boolean isDownloaded() {
		/* refactor when it works */
		String path = getPathname();
		if (path == null || path == "")
			return false;

		File localFile = new File(path);
		long localFilesize = localFile.length();

		if (filesize == 0)
			return localFilesize > 0;
		else
			return localFilesize >= filesize && localFilesize > 0;
	}

	/*
	 * @return the duration of the mp3 (or whatever) in milliseconds.
	 */
	public long getDuration() {
		if (isDownloaded()) {
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			try {
				retriever.setDataSource(getPathname());
			} catch (RuntimeException e) {
				e.printStackTrace();
				return this.length;
			}
			return Long
					.parseLong(retriever
							.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		} else {
			if (this.duration.equals(""))
				return this.length;
			else {
				// String offsetString = StrUtils.getTimeFromOffset(this.offset,
				// this.length, this.duration);
				return StrUtils.parseTimeToSeconds(duration);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FeedItem))
			return false;
		FeedItem other = (FeedItem) obj;
		if (id != other.id)
			return false;
		return true;
	}

	private static void initCache() {
		if (cache == null) {
			int memoryClass = 16 * 1024 * 1024; // FIXME use getMemoryClass()
			cache = new ItemLruCache(memoryClass);
		}
	}

	/*
	 * Caching class for keeping items in memory
	 */
	private static class ItemLruCache extends LruCache<Long, FeedItem> {

		public ItemLruCache(int maxSize) {
			super(maxSize);
		}

	}

	@Override
	public String getImageURL(Context context) {
		String imageURL = null;
		if (!image.equals("")) {
			imageURL = image;
		} else {
			Subscription subscription = Subscription.getById(
					context.getContentResolver(), sub_id);
			imageURL = subscription.imageURL;

		}
		return imageURL;
	}

	@Override
	public long getId() {
		return id;
	}
}
