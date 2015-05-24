package org.bottiger.podcast.provider;

import java.io.File;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.bottiger.podcast.listeners.DownloadProgressObservable;
import org.bottiger.podcast.service.DownloadStatus;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.utils.PodcastLog;
import org.bottiger.podcast.utils.SDCardManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

public class FeedItem implements IEpisode, Comparable<FeedItem> {

	public static final int MAX_DOWNLOAD_FAIL = 5;

	private final PodcastLog log = PodcastLog.getLog(getClass());
	private static ItemLruCache cache = null;

	private DatabaseHelper mBulkUpdater = null;

	/*
	 * Let's document these retared fields! They are totally impossible to guess
	 * the meaning of.
	 */

	/**
	 * Unique ID
	 */
	public long id;

	/**
	 * Unique ID of the file on the remote server
	 */
	public String remote_id;

	/**
	 * URL of the episode:
	 * http://podcast.dr.dk/P1/p1debat/2013/p1debat_1301171220.mp3
	 */
	public String url;

	/**
	 * Title of the episode
	 */
	public String title;

	/**
	 * Name of Publisher
	 */
	public String author;

	/**
	 * Date Published
	 */
	public String date;

	/**
	 * Episode description in text
	 */
	public String content;

	/**
	 * Also an URL
	 */
	@Deprecated
	public String resource;

	/**
	 * Duration as String hh:mm:ss or mm:ss 02:23:34
	 */
	public String duration_string;

	/**
	 * Duration in milliseconds
	 */
	public long duration_ms;

	/**
	 * URL to relevant episode image
	 */
	public String image;

	/**
	 * Unique ID of the subscription the episode belongs to
	 */
	public long sub_id;

	/**
	 * Total size of the episode in bytes
	 */
	public long filesize;

	/**
	 * Size of the file on disk in bytes
	 */
	public long chunkFilesize;

	/**
	 * Filename of the episode on disk. sn209.mp3
	 */
	private String filename;

	/**
	 * Episode number.
	 */
	public int episodeNumber;

	/**
	 * Download reference ID as returned by
	 * http://developer.android.com/reference
	 * /android/app/DownloadManager.html#enqueue
	 * (android.app.DownloadManager.Request)
	 */
	private long downloadReferenceID;

	/**
	 * Flags for filtering downloaded items
	 */
	private Boolean isDownloaded;

	/**
	 * Last position during playback in ms Should match seekTo(int)
	 * http://developer
	 * .android.com/reference/android/media/MediaPlayer.html#seekTo(int)
	 */
	public int offset;

	/**
	 * Deprecated status from before forking
	 */
	@Deprecated
	public int status;

	@Deprecated
	public long failcount;
	// failcount is currently used for two purposes:
	// 1. counts the number of times we fail to download, and
	// when we exceed a predefined max, we pause the download.
	// 2. when an item is in the extended_player, failcount is used as
	// the order of the item in the list.

	/**
	 * Have to listened to this episode yet?
	 */
	public int listened;

	/**
	 * Priority in the playlist
	 */
	public int priority;

	/**
	 * Filesize as reported by the RSS feed
	 */
	public long length;

	/**
	 * The time the record in the database was updated the last time. measured
	 * in: System.currentTimeMillis()
	 */
	public long lastUpdate;

	/**
	 * The URI of the podcast episode
	 */
	@Deprecated
	public String uri;

	/**
	 * Title of the parent subscription
	 */
	public String sub_title;

	@Deprecated
	public long created;

	public String type;

	@Deprecated
	private long m_date;

	static String[] DATE_FORMATS = { "EEE, dd MMM yyyy HH:mm:ss Z",
			"EEE, d MMM yy HH:mm z", "EEE, d MMM yyyy HH:mm:ss z",
			"EEE, d MMM yyyy HH:mm z", "d MMM yy HH:mm z",
			"d MMM yy HH:mm:ss z", "d MMM yyyy HH:mm z",
			"d MMM yyyy HH:mm:ss z", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss",
			"EEE,dd MMM yyyy HH:mm:ss Z", };

	public static String default_format = "yyyy-MM-dd HH:mm:ss Z";

	public static void view(Activity act, long item_id) {
		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, item_id);
		act.startActivity(new Intent(Intent.ACTION_EDIT, uri));
	}

	public static Cursor allAsCursor(ContentResolver context,
			Subscription subscription, String oldestDate) {
		String strID = String.valueOf(subscription.getId());
		if (oldestDate != null) {
			return context.query(ItemColumns.URI, ItemColumns.ALL_COLUMNS,
					ItemColumns.SUBS_ID + "=? AND " + ItemColumns.DATE + ">?",
					new String[] { strID, oldestDate }, null);
		}
		return context.query(ItemColumns.URI, ItemColumns.ALL_COLUMNS,
				ItemColumns.SUBS_ID + "=?", new String[] { strID }, null);
	}

	public static HashMap<String, FeedItem> allAsList(ContentResolver context,
			Subscription subscription, String oldestDate) {
		HashMap<String, FeedItem> item = new HashMap<String, FeedItem>();
		Cursor cursor = allAsCursor(context, subscription, oldestDate);

		while (cursor.moveToNext()) {
			FeedItem currentItem = FeedItem.getByCursor(cursor);
			item.put(currentItem.url, currentItem);
		}
		cursor.close();
		return item;
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
	
	public static FeedItem[] getByURL(ContentResolver contentResolver, String[] urls) {
		return getByURL(contentResolver, urls, null);
	}

	public static FeedItem getByURL(ContentResolver contentResolver, String url) {
		return getByURL(contentResolver, new String[]{url}, null)[0];
	}

	public static FeedItem[] getByURL(ContentResolver contentResolver,
			String[] urls, FeedItem cachedFeedItem) {
		FeedItem[] items = new FeedItem[urls.length];
		Cursor cursor = null;

		/*
		 * Build a query with the correct amount of ?,?,?
		 */
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(ItemColumns.URL + " IN (");
		for (int i = 1; i <= urls.length; i++) {
			queryBuilder.append("\"" + urls[i-1] + "\"");
			if (i != urls.length)
				queryBuilder.append(", ");
		}
		queryBuilder.append(")");
		String where = queryBuilder.toString();
		//String where = ItemColumns.URL + " = ?";
		
		try {
			cursor = contentResolver.query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null); //urls

			int j = 0;
			cursor.moveToPosition(-1);
			while (cursor.moveToNext()) {
				items[j] = FeedItem.getByCursor(cursor, cachedFeedItem);
				j++;
			}
			cursor.close();
		} catch (ArrayIndexOutOfBoundsException e) {
            items[1] = null;
        } finally {
			if (cursor != null)
				cursor.close();
		}
		return items;

	}

	public static FeedItem getByDownloadReference(
			ContentResolver contentResolver, long id) {
		FeedItem item = null;
		Cursor cursor = null;

		String where = ItemColumns.DOWNLOAD_REFERENCE + " = " + id;
		try {
			cursor = contentResolver.query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
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
		FeedItem item = fetchFromCursor(cursor, null);
		return item;
	}

	public static FeedItem getByCursor(Cursor cursor, FeedItem cachedFeedItem) {
		FeedItem item = fetchFromCursor(cursor, cachedFeedItem);
		return item;
	}

	public static FeedItem getMostRecent(ContentResolver context) {
		return getBySQL(context, "1==1", "_id DESC");
	}

	public FeedItem() {
		reset();
	}

	public void reset() {
		url = null;
		title = null;
		author = null;
		date = null;
		content = null;
		resource = null;
		duration_string = null;
		filename = null;
		uri = null;
		type = null;
		image = null;
		remote_id = null;

		id = -1;
		offset = -1;
		status = -1;
		failcount = -1;
		length = -1;
		lastUpdate = -1;
		listened = -1;
		priority = -1;
		filesize = -1;
		chunkFilesize = -1;
		downloadReferenceID = -1;
		episodeNumber = -1;
		isDownloaded = null;
		duration_ms = -1;

		created = -1;
		sub_title = null;
		sub_id = -1;

		m_date = -1;

	}

	public void setOffset(ContentResolver contentResolver, long i) {
		offset = (int) i;
        if (contentResolver != null) {
            lastUpdate = -1;
            update(contentResolver);
        }

	}

    public ContentValues getContentValues(Boolean silent) {
        ContentValues cv = new ContentValues();

        if (title != null)
            cv.put(ItemColumns.TITLE, title);
        if (content != null)
            cv.put(ItemColumns.CONTENT, content);
        if (filename != null)
            cv.put(ItemColumns.PATHNAME, filename);
        if (date != null)
            cv.put(ItemColumns.DATE, date);
        if (sub_id > 0)
            cv.put(ItemColumns.SUBS_ID, sub_id);

        if (url != null) {
            cv.put(ItemColumns.URL, url);
            cv.put(ItemColumns.RESOURCE, url);
        }
        if (remote_id != null)
            cv.put(ItemColumns.REMOTE_ID, remote_id);
        if (filesize >= 0)
            cv.put(ItemColumns.FILESIZE, filesize);
        if (downloadReferenceID >= 0)
            cv.put(ItemColumns.DOWNLOAD_REFERENCE, downloadReferenceID);
        if (episodeNumber >= 0)
            cv.put(ItemColumns.EPISODE_NUMBER, episodeNumber);
        if (chunkFilesize >= 0)
            cv.put(ItemColumns.LENGTH, length);
        if (duration_ms >= 0)
            cv.put(ItemColumns.DURATION_MS, duration_ms);
        if (chunkFilesize >= 0)
            cv.put(ItemColumns.CHUNK_FILESIZE, chunkFilesize);
        if (offset >= 0)
            cv.put(ItemColumns.OFFSET, offset);

        if (!silent) {
            lastUpdate = Long.valueOf(System.currentTimeMillis());
        }
        if (lastUpdate > 0)
            cv.put(ItemColumns.LAST_UPDATE, lastUpdate);

        if (listened >= 0)
            cv.put(ItemColumns.LISTENED, listened);

		cv.put(ItemColumns.PRIORITY, priority);

        if (image != null)
            cv.put(ItemColumns.IMAGE_URL, image);

        if (isDownloaded != null)
            cv.put(ItemColumns.IS_DOWNLOADED, isDownloaded);

        return cv;
    }

	/**
	 * Batch update
	 */
	public ContentProviderOperation update(ContentResolver contentResolver,
			boolean batchUpdate, boolean silent) {

		log.debug("item update start");

		ContentProviderOperation contentUpdate = null;

		initCache();
		if (this.id > 0)
			cache.remove(this.id);

		ContentValues cv = getContentValues(silent);

		// BaseColumns._ID + "=" + id
		String condition = ItemColumns.URL + "='" + url + "'";
		// String condition = ItemColumns.URL + "='?' AND "+ ItemColumns.URL +
		// "='?'"; // BUG!
		// http://code.google.com/p/android/issues/detail?id=56062
		if (batchUpdate) {
			contentUpdate = ContentProviderOperation.newUpdate(ItemColumns.URI)
					.withValues(cv).withSelection(condition, null) // new
																	// String[]{url})
					.withYieldAllowed(true).build();
		} else {
			int numUpdatedRows = contentResolver.update(ItemColumns.URI, cv,
					condition, null);
			if (numUpdatedRows == 1) {
                log.debug("update OK");
            } else if (numUpdatedRows == 0) {
				log.debug("update NOT OK. Insert instead");
				contentResolver.insert(ItemColumns.URI, cv);
			} else {
                throw new IllegalStateException("Never update more than one row here");
            }
		}
		return contentUpdate;
	}

	/**
	 * Update the FeedItem in the database
	 */
	public void silentUpdate(ContentResolver contentResolver) {
		update(contentResolver, false, true);
	}

	/**
	 * Update the FeedItem in the database
	 */
	public void update(ContentResolver contentResolver) {
		update(contentResolver, false, false);
	}

	public Uri insert(ContentResolver contentResolver) {
		log.debug("item insert start");
		try {

			ContentValues cv = new ContentValues();
			if (filename != null)
				cv.put(ItemColumns.PATHNAME, filename);
			if (remote_id != null)
				cv.put(ItemColumns.REMOTE_ID, remote_id);
			if (offset >= 0)
				cv.put(ItemColumns.OFFSET, offset);
			if (lastUpdate >= 0)
				cv.put(ItemColumns.LAST_UPDATE, lastUpdate);

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
			if (downloadReferenceID >= 0)
				cv.put(ItemColumns.DOWNLOAD_REFERENCE, downloadReferenceID);

			if (isDownloaded != null)
				cv.put(ItemColumns.IS_DOWNLOADED, isDownloaded);

			if (episodeNumber >= 0)
				cv.put(ItemColumns.EPISODE_NUMBER, episodeNumber);
			if (length >= 0)
				cv.put(ItemColumns.LENGTH, length);
			if (duration_string != null) {
				// Log.w("ITEM","  duration: " + duration);
				cv.put(ItemColumns.DURATION, duration_string);
			}
			if (duration_ms > 0) {
				cv.put(ItemColumns.DURATION_MS, duration_ms);
			}
			if (sub_title != null) {
				cv.put(ItemColumns.SUB_TITLE, sub_title);
			}
			if (listened >= 0)
				cv.put(ItemColumns.LISTENED, listened);

			if (priority >= 0)
				cv.put(ItemColumns.PRIORITY, priority);

			return contentResolver.insert(ItemColumns.URI, cv);

		} finally {
		}
	}

	/**
	 * @see http://docs.oracle.com/javase/6/docs/api/java/lang/String.html#compareTo
	 *      %28java.lang.String%29
	 * @return True of the current FeedItem is newer than the supplied argument
	 */
	public boolean newerThan(FeedItem item) {
		int comparator = this.getDate().compareTo(item.getDate());
		return comparator > 0;
	}

	@Deprecated
	public long getLongDate() {
		return m_date;
	}

	/**
	 * @return the PublishingDate as default_format = "yyyy-MM-dd HH:mm:ss Z"
	 */
	public String getDate() {
		// log.debug(" getDate() start");
		return this.date;
	}

    /**
     *   i.e "yyyy-MM-dd hh:mm:ss"
     */
    public String getDate(@NonNull Context argContext) {
        java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(argContext);

        long time;

        try {
            time = getDateTime().getTime();
        } catch (NullPointerException npe) {
            return "";
        }

        return dateFormat.format(time);
    }

    private Date mDate = null;
    public static SimpleDateFormat sdf = new SimpleDateFormat(default_format, Locale.US);
    public Date getDateTime() {

        if (mDate == null) {
            try {
                mDate = sdf.parse(date);
            } catch (ParseException pe) {
                throw new IllegalArgumentException("Datestring must be parsable");
            }
        }
        return mDate;
    }

	@Deprecated
	private long parse() {
		long l = 0;
		try {
			return sdf.parse(date)
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
		return fetchFromCursor(cursor, null);
	}

	private static FeedItem fetchFromCursor(Cursor cursor,
			FeedItem cachedFeedItem) {

		FeedItem item;
		if (cachedFeedItem != null) {
			cachedFeedItem.reset();
			item = cachedFeedItem;
		} else
			item = new FeedItem();

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
		item.filename = cursor.getString(cursor
				.getColumnIndex(ItemColumns.PATHNAME));
		item.remote_id = cursor.getString(cursor
				.getColumnIndex(ItemColumns.REMOTE_ID));
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
		item.length = cursor.getLong(cursor.getColumnIndex(ItemColumns.LENGTH));
		item.chunkFilesize = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.CHUNK_FILESIZE));
		item.downloadReferenceID = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.DOWNLOAD_REFERENCE));

		int intVal = cursor.getInt(cursor
				.getColumnIndex(ItemColumns.IS_DOWNLOADED));
		item.isDownloaded = intVal == 1;

		item.episodeNumber = cursor.getInt(cursor
				.getColumnIndex(ItemColumns.EPISODE_NUMBER));
		item.duration_string = cursor.getString(cursor
				.getColumnIndex(ItemColumns.DURATION));
		item.duration_ms = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.DURATION_MS));
		item.lastUpdate = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.LAST_UPDATE));
		item.sub_title = cursor.getString(cursor
				.getColumnIndex(ItemColumns.SUB_TITLE));
		item.sub_id = cursor
				.getLong(cursor.getColumnIndex(ItemColumns.SUBS_ID));
		item.listened = cursor.getInt(cursor
				.getColumnIndex(ItemColumns.LISTENED));
		item.priority = cursor.getInt(cursor
				.getColumnIndex(ItemColumns.PRIORITY));

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

	/**
	 * Writes the currentstatus of the FeedItem with the giving ID to the
	 * textView argument
	 *
	 */
	public String getStatus(DownloadManager downloadManager) {
		DownloadStatus downloadStatus = EpisodeDownloadManager.getStatus(this);
		String statusText = "";
		switch (downloadStatus) {
		case PENDING:
			statusText = "waiting";
			break;
		case DOWNLOADING:
			statusText = "Downloading: " + Integer.toString(getProgress(downloadManager));
			break;
		case DONE:
			statusText = "Downloaded";
			break;
		case ERROR:
			statusText = "Error";
			break;
		default:
			statusText = "";
		}
		return statusText;
	}

	/**
	 * Get the current download progress as a int .
	 * 
	 * @return download status in percent
	 */
	public int getProgress(DownloadManager downloadManager) {
		assert downloadManager != null;
		long percent = 0;

		// FIXME This is run one time for each textview. It should only be run
		// once with all the reference ID's
		Query query = new Query();
		query.setFilterById(getDownloadReferenceID());
		Cursor c = downloadManager.query(query);

        if (c == null) {
            return 100;
        }

		c.moveToFirst();
		while (c.isAfterLast() == false) {
			int cursorBytesSoFarIndex = c
					.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
			int cursorBytesTotalIndex = c
					.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

			long bytesSoFar = c.getInt(cursorBytesSoFarIndex);
			long bytesTotal = c.getInt(cursorBytesTotalIndex);

			percent = bytesSoFar * 100 / bytesTotal;

			c.moveToNext();
		}

        int percentInt = (int)percent;

        if (lastProgress > 0) {
            if (percentInt == lastProgress) {
                iterWithoutChange++;
            } else {
                Log.d("FeedItem", iterWithoutChange + " iterations without change in progress");
                iterWithoutChange = 0;
            }
        }

        lastProgress = percentInt;
		return percentInt;
	}
    private int iterWithoutChange = 0;
    private int lastProgress = -1;

	/**
	 * Deletes the downloaded file and updates the data in the database
	 * 
	 * @param contentResolver
	 * @return True of the file was deleted succesfully
	 */
	public boolean delFile(ContentResolver contentResolver) {

		if (SDCardManager.getSDCardStatus()) {
			try {
				downloadReferenceID = -1;
				setDownloaded(false);
				update(contentResolver);
				File file = new File(getAbsolutePath());
				if (file.exists() && file.delete()) {
                    DownloadProgressObservable.deleteEpisode(this);
					return true;
				}
			} catch (Exception e) {
				log.warn("del file failed : " + getAbsolutePath() + "  " + e);
			}
		}

		return false;

	}

	public void prepareDownload(ContentResolver context) {
		if (getAbsolutePath().equals("") || getAbsolutePath().equals("0")) {
			String filenameFromURL = resource.substring(resource
					.lastIndexOf("/") + 1);
			filename = this.sub_id + "_" + filenameFromURL;
		}
		update(context);
	}

	public void downloadSuccess(ContentResolver contentResolver) {
		filesize = getCurrentFileSize();
		update(contentResolver);
	}

	public void endDownload(ContentResolver context) {
		lastUpdate = Long.valueOf(System.currentTimeMillis());
		update(context);
	}

	public long getCurrentFileSize() {
		String file = getAbsolutePath();
		if (file != null)
			return new File(file).length();
		return 0;
	}

	public String getFilename() {
		if (filename == null || filename.equals("")) {
			MessageDigest m;
			try {
				m = MessageDigest.getInstance("MD5");
				m.reset();
				m.update(this.getURL().getBytes());
				byte[] digest = m.digest();
				BigInteger bigInt = new BigInteger(1, digest);
				String hashtext = bigInt.toString(16);
				return hashtext;
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return filename;
	}

	/**
	 * @param filename
	 *            the filename to set
	 */
	public void setFilename(String filename) {
		// Remove non ascii characters
		//this.filename = filename.replaceAll("[^\\x00-\\x7F]", "");
        this.filename = filename.replaceAll("[^(\\x41-\\x5A|\\x61-\\x7A|\\x2D|\\x2E|\\x30-\\x39|\\x5F)]", ""); // only a-z A-Z 0-9 .-_ http://www.asciitable.com/
	}

	public String getAbsolutePath() {
		return SDCardManager.pathFromFilename(this);
	}

	public String getAbsoluteTmpPath() {
		return SDCardManager.pathTmpFromFilename(this);
	}

    // in ms
	public void setPosition(ContentResolver contentResolver, long pos) {
		this.offset = (int) pos;
		update(contentResolver);
	}

	@Override
	public int compareTo(FeedItem another) {
		return another.date.compareTo(date);
		/*
		 * if (this.update > another.update) return 1; else if (this.update <
		 * another.update) return -1;
		 * 
		 * return 0;
		 */
	}
	
	public void setURL(String url) {
		this.url = url;
	}

	public String getURL() {
		String itemURL = "";
		if (url != null && url.length() > 1)
			itemURL = url;
		else if (resource != null && resource.length() > 1)
			itemURL = resource;
		return itemURL;
	}

	public long getFilesize() {
		return filesize;
	}

	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * 
	 * @return whether the item is downloaded to the phone
	 */
	public boolean isDownloaded() {
        try {
            if (this.isDownloaded!=null)
                return this.isDownloaded;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

		File f = new File(getAbsolutePath());

        if (f == null)
            return false;

		if (f.exists())
			return true;

		isDownloaded = false;
		return isDownloaded;
	}

	/**
	 * @return true of the podcast has been listened too
	 */
	public boolean isMarkedAsListened() {
		return this.listened == 1;
	}

	/**
	 * @param isDownloaded
	 *            the isDownloaded to set
	 */
	public void setDownloaded(boolean isDownloaded) {
		this.isDownloaded = isDownloaded;
	}

	/**
	 * Mark the episode as listened
	 */
	public void markAsListened() {
		markAsListened(1);
	}
	
	/**
	 * If the episode has been listened to
	 */
	public boolean isListened() {
		return this.listened == 1;
	}

	/**
	 * @params 1 for listened. 0 for unlistened. -1 for undefined
	 */
	public void markAsListened(int hasBeenListened) {
		this.listened = hasBeenListened;
	}

	/**
     * The duration of the mp3 (or whatever) in milliseconds.
	 * @return the duration in ms. -1 if unknown
	 */
	public long getDuration() {
        return duration_ms;
        /*
		if (duration_ms > 0) {
			if ("".equals(duration_string)) {
				duration_ms = StrUtils.parseTimeToSeconds(duration_string);
			}
			return duration_ms;
		}

		if (isDownloaded()) {
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			try {
				String path = getAbsolutePath();
				retriever.setDataSource(path);
			} catch (RuntimeException e) {
				e.printStackTrace();
				return this.length;
			}
			String mediaID = retriever
					.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			if (mediaID != null) {
                duration_ms = Long.parseLong(mediaID);
                return duration_ms;
            } else {
                return -1;
            }
		} else {
			if (this.duration_string == null || this.duration_string.equals(""))
				return this.length;
			else {
				// String offsetString = StrUtils.getTimeFromOffset(this.offset,
				// this.length, this.duration);
				return StrUtils.parseTimeToSeconds(duration_string);
			}
		}
		*/
	}

    public void setDuration(long argDurationMs) {
        setDuration(argDurationMs, true);
    }

    public void setDuration(long argDurationMs, boolean argOverride) {
        if (!argOverride) {
            if (duration_ms > 0)
                return;
        }

        this.duration_ms = argDurationMs;
    }

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
            return true;
        }
		if (obj == null) {
            return false;
        }
        boolean isInstanceof = obj instanceof FeedItem;
		if (!isInstanceof) {
            return false;
        }

        FeedItem other = (FeedItem) obj;
		if (id != other.id) {
            return false;
        }

		return true;
	}

	/**
	 * Clear the cache
	 */
	public static void clearCache() {
		if (cache != null) {
			cache.evictAll();
		}
	}

	private static void initCache() {
		if (cache == null) {
			int memoryClass = 5 * 1024 * 1024; // FIXME use getMemoryClass()
			cache = new ItemLruCache(memoryClass);
		}
	}

    public Subscription getSubscription(@NonNull Context argContext) {
        return SubscriptionLoader.getById(argContext.getContentResolver(), sub_id);
    }

    @Override
    public long getOffset() {
        return offset;
    }

    public void removeFromPlaylist(@NonNull ContentResolver argContentResolver) {
        priority = -1;
        update(argContentResolver);
    }

    /**
	 * Caching class for keeping items in memory
	 */
	private static class ItemLruCache extends LruCache<Long, FeedItem> {

		public ItemLruCache(int maxSize) {
			super(maxSize);
		}

	}

    @Nullable
	@Override
	public String getArtwork(@NonNull Context context) {
		String imageURL;

        if (!TextUtils.isEmpty(image))
            return image;

        Subscription subscription = getSubscription(context);

        if (subscription == null)
            return null;

		imageURL = subscription.imageURL;

        if (TextUtils.isEmpty(imageURL))
            return null;

		return imageURL;
	}

	public long getId() {
		return id;
	}

	/**
	 * Put the current FeedItem after the argument in the playlist. If the given
	 * FeedItem is null the current FeedItem becomes the first item in the
	 * playlist
	 */
	public void setPriority(IEpisode precedingItem, Context context) {
		priority = precedingItem == null ? 1 : precedingItem.getPriority() + 1;
		increateHigherPriorities(precedingItem, context);
		update(context.getContentResolver());
	}

	public void setTopPriority(Context context) {
		setPriority(null, context);
	}

	public void trackEnded(ContentResolver contentResolver) {
		priority = 0;
		markAsListened();
		update(contentResolver);
	}

	@Override
	public void setPriority(int argPriority) {
		priority = argPriority;
	}

	/**
	 * Increase the priority of all items in the playlist after the current item
	 * 
	 * @param precedingItem
	 * @param context
	 */
	private void increateHigherPriorities(IEpisode precedingItem,
			Context context) {
		String currentTime = String.valueOf(System.currentTimeMillis());
		String updateLastUpdate = ", " + ItemColumns.LAST_UPDATE + "="
				+ currentTime + " ";

		PodcastOpenHelper helper = PodcastOpenHelper.getInstance(context);//new PodcastOpenHelper(context);
		SQLiteDatabase db = helper.getWritableDatabase();
		String action = "UPDATE " + ItemColumns.TABLE_NAME + " SET ";
		String value = ItemColumns.PRIORITY + "=" + ItemColumns.PRIORITY + "+1"
				+ updateLastUpdate;
		String where = "WHERE " + ItemColumns.PRIORITY + ">=" + this.priority
				+ " AND " + ItemColumns.PRIORITY + "<> 0 AND "
				+ ItemColumns._ID + "<> " + this.id;
		String sql = action + value + where;

		String actionCurrent = "UPDATE " + ItemColumns.TABLE_NAME + " SET ";
		String valueCurrent = ItemColumns.PRIORITY + "=" + this.priority
				+ updateLastUpdate;
		String whereCurrent = "WHERE " + ItemColumns._ID + "==" + this.id;
		String sqlCurrent = actionCurrent + valueCurrent + whereCurrent;

		db.beginTransaction();
		try {
			db.execSQL(sql);
			db.execSQL(sqlCurrent);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * 
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * @return the downloadReferenceID
	 */
	public long getDownloadReferenceID() {
		return downloadReferenceID;
	}

	/**
	 * @param downloadReferenceID
	 *            the downloadReferenceID to set
	 */
	public void setDownloadReferenceID(long downloadReferenceID) {
		this.downloadReferenceID = downloadReferenceID;
	}

	/**
	 * @return the episodeNumber
	 */
	public int getEpisodeNumber() {
		return episodeNumber;
	}

	/**
	 * @param episodeNumber
	 *            the episodeNumber to set
	 */
	public void setEpisodeNumber(int episodeNumber) {
		this.episodeNumber = episodeNumber;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
        return title != null ? title : "";
	}

    @Override
    public URL getUrl() {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null; // should never happen
    }

    @Override
    public String getDescription() {
        return content;
    }

    /**
     * @return the title of the episode. Most feeds format the title in the feed as: 'FeedTittle: EpisodeTitle'. This method returns the episodetitle
     */
    public String getShortTitle() {
        String separator = ":";
        String longTitle = getTitle();
        if (TextUtils.isEmpty(longTitle))
            return longTitle;

        int index = longTitle.indexOf(separator);

        if (index < 0)
            return longTitle;

        String sub = longTitle.substring(index+1, longTitle.length());

        String shortTitle = sub.trim();

        if (TextUtils.isEmpty(shortTitle))
            return longTitle;

        return shortTitle;
    }

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

    @Override
    public void setUrl(@NonNull URL argUrl) {

    }

    @Override
    public void setArtwork(@NonNull URL argUrl) {

    }

    public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Get the time this item was last updated
	 * 
	 * @return
	 */
	public long getLastUpdate() {
		return this.lastUpdate;
	}

	public long lastModificationDate() {
		return this.lastUpdate;
	}

	public void queue(Context context) {

		String sqlQueue = "update " + ItemColumns.TABLE_NAME + " set "
				+ ItemColumns.PRIORITY + " = (select max("
				+ ItemColumns.PRIORITY + ") from " + ItemColumns.TABLE_NAME
				+ ")+1 where " + ItemColumns._ID + " =" + this.id;

		DatabaseHelper dbHelper = new DatabaseHelper();
		dbHelper.executeSQL(context, sqlQueue);
	}

	public void setFeed(Subscription feed) {
		this.sub_id = feed.getId();
	}

	public Object getMedia() {
		return null;
	}

    private static final SimpleDateFormat sFormat = new SimpleDateFormat(default_format, Locale.US);
	public void setPubDate(Date parseRFC822Date) {
        try {
            this.date = sFormat.format(parseRFC822Date);
        } catch (NullPointerException npe) {
            //Log.e("Date parsing error:" , "Could not parse: " + parseRFC822Date.toString());
            this.date = sFormat.format(new Date());
        }
	}

	public void setDescription(String content2) {
		this.content = content2;
	}

	public void setLink(String href) {
		this.url = href;
	}
}
