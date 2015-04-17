package org.bottiger.podcast.provider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.service.Downloader.SubscriptionRefreshManager;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.PodcastLog;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.support.v7.graphics.Palette;

import javax.annotation.Nullable;

public class Subscription extends AbstractItem implements ISubscription, PaletteListener {

	public static final String TYPE_RSS2 = "rss";
	public static final String TYPE_RSS091 = "rss";
	public static final String TYPE_ATOM1 = "atom";

	private final PodcastLog log = PodcastLog.getLog(getClass());

	public final static int ADD_SUCCESS = 0;
	public final static int ADD_FAIL_DUP = -1;
	public final static int ADD_FAIL_UNSUCCESS = -2;

	public final static int STATUS_SUBSCRIBED = 1;
	public final static int STATUS_UNSUBSCRIBED = 2;

	public final static String UNSUBSCRIBED = "unsubscribed";

	private static SubscriptionLruCache cache = null;

    private boolean mIsDirty = false;

	public long id;
	public String title;

	/**
	 * Link to a website or similar
	 */
	public String link;
	public String comment;

	public String url;
	public String description;
	public String imageURL;
	public String sync_id;
	public long status;
	public long lastUpdated;
	public long lastItemUpdated;
	public long fail_count;
	public long auto_download;

    private int mPrimaryColor;
    private int mPrimaryTintColor;
    private int mSecondaryColor;

	public static void view(Activity act, long channel_id) {
		Uri uri = ContentUris.withAppendedId(SubscriptionColumns.URI,
				channel_id);
		// Subscription channel = Subscription.getById(act.getContentResolver(),
		// channel_id);
		act.startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}

	public static void viewEpisodes(Activity act, long channel_id) {
		Uri uri = ContentUris.withAppendedId(SubscriptionColumns.URI,
				channel_id);
		act.startActivity(new Intent(Intent.ACTION_EDIT, uri));
	}

	public static Cursor allAsCursor(ContentResolver context) {
		return context.query(SubscriptionColumns.URI,
				SubscriptionColumns.ALL_COLUMNS, null, null, null);
	}

	public static LinkedList<Subscription> allAsList(ContentResolver context) {
		LinkedList<Subscription> subscriptions = new LinkedList<Subscription>();
        Cursor cursor = null;
        try {
            cursor = allAsCursor(context);

            while (cursor.moveToNext()) {
                subscriptions.add(Subscription.getByCursor(cursor));
            }
        } finally {
            cursor.close();
        }
		return subscriptions;
	}

	public static Subscription getBySQL(ContentResolver context, String where,
			String order) {
		Subscription sub = null;
		Cursor cursor = null;

		try {
			cursor = context.query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, where, null, order);
			if (cursor.moveToFirst()) {
				sub = Subscription.getByCursor(cursor);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return sub;
	}

	public static Subscription getByUrl(ContentResolver contentResolver,
			String url) {
		Cursor cursor = null;
		try {
			cursor = contentResolver.query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, SubscriptionColumns.URL
							+ "=?", new String[] { url }, null);
			if (cursor.moveToFirst()) {
				Subscription sub = new Subscription();
				sub = fetchFromCursor(sub, cursor);
				cursor.close();
				return sub;
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (cursor != null)
				cursor.close();
		}

		return null;

	}

	public static Subscription getByCursor(Cursor cursor) {
		// if (cursor.moveToFirst() == false)
		// return null;
		Subscription sub = new Subscription();
		sub = fetchFromCursor(sub, cursor);
		return sub;
	}

    public static Subscription getFirst(ContentResolver context) {

        Cursor cursor = null;
        Subscription sub = null;

        initCache();

        try {
            cursor = context.query(SubscriptionColumns.URI,
                    SubscriptionColumns.ALL_COLUMNS, "1", null, null);
            if (cursor.moveToFirst()) {
                sub = new Subscription();
                sub = fetchFromCursor(sub, cursor);
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (cursor != null)
                cursor.close();
        }

        return sub;
    }

	public static Subscription getById(ContentResolver context, long id) {
		Cursor cursor = null;
		Subscription sub = null;

		initCache();

		// Return item directly if cached
		synchronized (cache) {
			sub = cache.get(id);
			if (sub != null) {
				return sub;
			}
		}

		try {
			String where = BaseColumns._ID + " = " + id;

			cursor = context.query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, where, null, null);
			if (cursor.moveToFirst()) {
				sub = new Subscription();
				sub = fetchFromCursor(sub, cursor);
				cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (cursor != null)
				cursor.close();
		}

		return sub;

	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public void reset() {
		id = -1;
		title = null;
		url = null;
		link = null;
		comment = "";
		description = null;
		imageURL = null;
		lastUpdated = -1;
		fail_count = -1;
		lastItemUpdated = -1;
		auto_download = -1;
		sync_id = null;
		status = -1;
        mPrimaryColor = -1;
        mPrimaryTintColor = -1;
        mSecondaryColor = -1;
	}

	public Subscription() {
		reset();
	}

	public Subscription(String url_link) {
		reset();
		url = url_link;
		title = url_link;
		link = url_link;
	}

	public void unsubscribe(Context context) {
		// Unsubscribe from local database
		this.status = STATUS_UNSUBSCRIBED;
		update(context.getContentResolver());
		deleteEpisodes(context);
	}

	private boolean deleteEpisodes(Context context) {
		String where = ItemColumns.SUBS_ID + " = ?";
		String[] selectionArgs = { String.valueOf(id) };
		int deletedRows = context.getContentResolver().delete(ItemColumns.URI,
				where, selectionArgs);
		if (deletedRows > 1) {
			return true;
		} else
			return false;
	}

	public int subscribe(@NonNull final Context context) {
		Subscription sub = Subscription.getByUrl(context.getContentResolver(),
				url);

        if (sub == null) {
            ContentValues cv = new ContentValues();
            cv.put(SubscriptionColumns.TITLE, title);
            cv.put(SubscriptionColumns.URL, url);
            cv.put(SubscriptionColumns.LINK, link);
            cv.put(SubscriptionColumns.LAST_UPDATED, 0L);
            cv.put(SubscriptionColumns.COMMENT, comment);
            cv.put(SubscriptionColumns.DESCRIPTION, description);
            cv.put(SubscriptionColumns.IMAGE_URL, imageURL);
            cv.put(SubscriptionColumns.REMOTE_ID, sync_id);
            cv.put(SubscriptionColumns.STATUS, STATUS_SUBSCRIBED);
            Uri uri = context.getContentResolver().insert(SubscriptionColumns.URI,
                    cv);

            if (uri == null) {
                VendorCrashReporter.report("SubscriptionFailed", "" + url);
                return ADD_FAIL_UNSUCCESS;
            }
        } else {
            // Update the current subscription
            sub.status = STATUS_SUBSCRIBED;
            sub.update(context.getContentResolver());
        }

        if (sub == null) {
            sub = Subscription.getByUrl(context.getContentResolver(), url);

            if (sub == null) {
                return ADD_FAIL_UNSUCCESS;
            }
        }

        SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.SUBSCRIBE_TO_FEED);
        sub.refresh(context);

		return ADD_SUCCESS;

	}

	public void delete(ContentResolver context) {
		Uri uri = ContentUris.withAppendedId(SubscriptionColumns.URI, id);
		context.delete(uri, null, null);
	}

	public LinkedList<FeedItem> getFeedItems(ContentResolver contentResolver) {
		LinkedList<FeedItem> episodes = new LinkedList<FeedItem>();
		Cursor itemCursor = contentResolver.query(ItemColumns.URI,
				ItemColumns.ALL_COLUMNS, ItemColumns.SUBS_ID + "==" + this.id,
				null, null);
		for (boolean more = itemCursor.moveToFirst(); more; more = itemCursor
				.moveToNext()) {
			episodes.add(FeedItem.getByCursor(itemCursor));
		}

		return episodes;
	}

    public void refresh(@NonNull final Context argContext) {
        SoundWaves.sSubscriptionRefreshManager.refresh(this, new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean succes) {
                update(argContext.getContentResolver());
            }
        });
    }

	/**
	 * Batch update
	 */
	public ContentProviderOperation update(ContentResolver contentResolver,
			boolean batchUpdate, boolean silent) {

		ContentProviderOperation contentUpdate = null;
		ContentValues cv = new ContentValues();

		if (title != null)
			cv.put(SubscriptionColumns.TITLE, title);
		if (url != null)
			cv.put(SubscriptionColumns.URL, url);
		if (imageURL != null)
			cv.put(SubscriptionColumns.IMAGE_URL, imageURL);
		if (description != null)
			cv.put(SubscriptionColumns.DESCRIPTION, description);

		if (fail_count <= 0 && !silent) {
			lastUpdated = Long.valueOf(System.currentTimeMillis());
		} else {
			lastUpdated = 0;
		}
		cv.put(SubscriptionColumns.LAST_UPDATED, lastUpdated);

		if (fail_count >= 0)
			cv.put(SubscriptionColumns.FAIL_COUNT, fail_count);

		if (lastItemUpdated >= 0)
			cv.put(SubscriptionColumns.LAST_ITEM_UPDATED, lastItemUpdated);

		if (auto_download >= 0)
			cv.put(SubscriptionColumns.AUTO_DOWNLOAD, auto_download);

		cv.put(SubscriptionColumns.REMOTE_ID, sync_id);

		if (status >= 0)
			cv.put(SubscriptionColumns.STATUS, status);

        if (mPrimaryColor != -1)
            cv.put(SubscriptionColumns.PRIMARY_COLOR, mPrimaryColor);

        if (mPrimaryTintColor != -1)
            cv.put(SubscriptionColumns.PRIMARY_TINT_COLOR, mPrimaryTintColor);

        if (mSecondaryColor != -1)
            cv.put(SubscriptionColumns.SECONDARY_COLOR, mSecondaryColor);

		// cv.put(SubscriptionColumns.COMMENT, "valuehejeh");

		// BaseColumns._ID + "=" + id
		String condition = SubscriptionColumns.URL + "='" + url + "'";
		if (batchUpdate) {
			contentUpdate = ContentProviderOperation
					.newUpdate(SubscriptionColumns.URI).withValues(cv)
					.withSelection(condition, null).withYieldAllowed(true)
					.build();
		} else {
			int numUpdatedRows = contentResolver.update(
					SubscriptionColumns.URI, cv, condition, null);
			if (numUpdatedRows == 1)
				log.debug("update OK");
			else {
				log.debug("update NOT OK. Insert instead");
				contentResolver.insert(SubscriptionColumns.URI, cv);
			}
		}

		return contentUpdate;
	}

	private static Subscription fetchFromCursor(Subscription sub, Cursor cursor) {
		// assert cursor.moveToFirst();
		// cursor.moveToFirst();
		sub.id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

		// Return item directly if cached
		initCache();
		synchronized (cache) {
			Subscription cacheSub = cache.get(sub.id);
			if (cacheSub != null && cacheSub.title != "") { // FIXME
															// cacheItem.title
															// != ""
				sub = cacheSub;
				return sub;
			}
		}

		int lastUpdatedIndex = cursor
				.getColumnIndex(SubscriptionColumns.LAST_UPDATED);
		int urlIndex = cursor.getColumnIndex(SubscriptionColumns.URL);

		String lastUpdatedString = cursor.getString(lastUpdatedIndex);
		sub.lastUpdated = Long.parseLong(lastUpdatedString);

		sub.title = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.TITLE));
		sub.url = cursor.getString(urlIndex);
		sub.imageURL = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.IMAGE_URL));
		sub.comment = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.COMMENT));
		sub.description = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.DESCRIPTION));
		sub.sync_id = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.REMOTE_ID));

		sub.auto_download = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.AUTO_DOWNLOAD));
		sub.lastItemUpdated = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.LAST_ITEM_UPDATED));
		sub.fail_count = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.FAIL_COUNT));

        sub.status = cursor.getLong(cursor
                .getColumnIndex(SubscriptionColumns.STATUS));

        sub.mPrimaryColor = cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.PRIMARY_COLOR));
        sub.mPrimaryTintColor = cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.PRIMARY_TINT_COLOR));
        sub.mSecondaryColor = cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.SECONDARY_COLOR));

		// if item was not cached we put it in the cache
		synchronized (cache) {
			cache.put(sub.id, sub);
		}

		return sub;
	}

    public void setPrimaryColor(int argColor) {
        mPrimaryColor = argColor;
    }

    public void setPrimaryTintColor(int argColor) {
        mPrimaryTintColor = argColor;
    }

    public void setSecondaryColor(int argColor) {
        mSecondaryColor = argColor;
    }

    public int getPrimaryColor() {
        return mPrimaryColor;
    }

    public int getPrimaryTintColor() {
        return mPrimaryTintColor;
    }

    public int getSecondaryColor() {
        return mSecondaryColor;
    }

	@Override
	public String toString() {
		return "Subscription: " + this.url;
	}

	private static void initCache() {
		if (cache == null) {
			int memoryClass = 16 * 1024 * 1024; // FIXME use getMemoryClass()
			cache = new SubscriptionLruCache(memoryClass);
		}
	}

    @Override
    public void onPaletteFound(@Nullable Palette argChangedPalette) {
        ColorExtractor extractor = new ColorExtractor(argChangedPalette);
        int newPrimaryColor = extractor.getPrimary();
        int newPrimaryTintColor = extractor.getPrimaryTint();
        int newSecondaryColor = extractor.getSecondary();

        if (newPrimaryColor != mPrimaryColor || newPrimaryTintColor != mPrimaryTintColor ||newSecondaryColor != mSecondaryColor) {
            mIsDirty = true;
        }

        mPrimaryColor     = newPrimaryColor;
        mPrimaryTintColor = newPrimaryTintColor;
        mSecondaryColor   = newSecondaryColor;
    }

    public boolean IsDirty() {
        return mIsDirty;
    }

    @Override
    public String getPaletteUrl() {
        return getImageURL();
    }

    private static class SubscriptionLruCache extends
			LruCache<Long, Subscription> {

		public SubscriptionLruCache(int maxSize) {
			super(maxSize);
		}

	}

	public URL getURL() {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	@Deprecated
	public String getImageURL(Context context) {
		return getImageURL();
	}

	public String getImageURL() {
		return imageURL;
	}

    public void setImageURL(String argUrl) {
        imageURL = argUrl;
    }

	public String getUrl() {
		return url;
	}

	/**
	 * Run whenever the subscription has been updated to google drive
	 */
	public void setDriveId(String fileID) {
		sync_id = fileID;
		// update(contentResolver);
	}

	@Override
	public long lastModificationDate() {
		return lastUpdated;
	}

	public long getLastUpdate() {
		return this.lastUpdated;
	}

	public int getStatus() {
		if (status == STATUS_UNSUBSCRIBED)
			return STATUS_UNSUBSCRIBED;

		return STATUS_SUBSCRIBED;
	}

	@Override
	public String getDriveId() {
		return sync_id;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	public void setURL(String url) {
		this.url = url;
	}

	@Override
	public String toJSON() {
		JSONObject json = new JSONObject();
		json.put("url", getURL().toString());
		json.put("last_update", getLastUpdate());
		json.put("title", getTitle());

		return json.toJSONString();
	}

	public static Subscription fromJSON(ContentResolver contentResolver,
			String json) {
		Subscription subscription = null;
		boolean updateItem = false;
		if (json != null) {
			String url = null;
			String title = null;
			String remote_id = null;
			Number lastUpdate = -1;

			Object rootObject = JSONValue.parse(json);
			JSONObject mainObject = (JSONObject) rootObject;
			if (mainObject != null) {
				url = mainObject.get("url").toString();
				if (mainObject.get("title") != null)
					title = mainObject.get("title").toString();
				if (mainObject.get("last_update") != null)
					lastUpdate = (Number) mainObject.get("last_update");
				if (mainObject.get("remote_id") != null)
					remote_id = mainObject.get("remote_id").toString();
			}

			subscription = Subscription.getByUrl(contentResolver, url);
			if (subscription == null) {
				subscription = new Subscription();
				updateItem = true;
			} else {
				updateItem = subscription.getLastUpdate() < lastUpdate
						.longValue();
			}

			if (url != null)
				subscription.url = url;
			if (title != null)
				subscription.title = title;
			if (remote_id != null)
				subscription.sync_id = remote_id;
			if (lastUpdate.longValue() > -1)
				subscription.lastUpdated = lastUpdate.longValue();

			if (updateItem)
				subscription.update(contentResolver);
		}

		return subscription;
	}

	public void setType(String typeRss2) {
		// TODO Auto-generated method stub

	}

	public void setDescription(String content) {
		this.description = content;
	}
}
