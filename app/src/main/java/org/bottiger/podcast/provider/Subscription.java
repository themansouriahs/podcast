package org.bottiger.podcast.provider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.utils.ColorExtractor;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.Log;

import javax.annotation.Nullable;

public class Subscription implements ISubscription, PaletteListener {

	private static final String TAG = "Subscription";

	public final static int ADD_SUCCESS = 0;
	public final static int ADD_FAIL_UNSUCCESS = -2;

	public final static int STATUS_SUBSCRIBED = 1;
	public final static int STATUS_UNSUBSCRIBED = 2;

    private boolean mIsDirty = false;

    /**
     * See SubscriptionColumns for documentation
     */
	public long id;
	public String title;
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

    private final ArrayList<IEpisode> mEpisodes = new ArrayList<>();


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
		Subscription sub = SubscriptionLoader.getByUrl(context.getContentResolver(),
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
            sub = SubscriptionLoader.getByUrl(context.getContentResolver(), url);

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

    public ArrayList<IEpisode> getEpisodes() {
        return mEpisodes;
    }

	public ArrayList<IEpisode> getEpisodes(@NonNull ContentResolver contentResolver) {

		LinkedList<FeedItem> episodes = new LinkedList<>();
		Cursor itemCursor = contentResolver.query(ItemColumns.URI,
				ItemColumns.ALL_COLUMNS, ItemColumns.SUBS_ID + "==" + this.id,
				null, null);
		for (boolean more = itemCursor.moveToFirst(); more; more = itemCursor
				.moveToNext()) {
			episodes.add(FeedItem.getByCursor(itemCursor));
		}

        mEpisodes.clear();
        mEpisodes.addAll(episodes);

		return mEpisodes;
	}


    private class RefreshSyncTask extends AsyncTask<Context, Void, Void> {
        protected Void doInBackground(Context... contexts) {
            refresh(contexts[0]);
            return null;
        }
    }


    public void refreshAsync(@NonNull final Context argContext) {
        new RefreshSyncTask().execute(argContext);
    }

    public void refresh(@NonNull final Context argContext) {
        SoundWaves.sSubscriptionRefreshManager.refresh(this, new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean succes, ISubscription subscription) {
                update(argContext.getContentResolver());
            }
        });
    }

    public ContentProviderOperation update(ContentResolver contentResolver) {
        return update(contentResolver, false, false);
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
				Log.d(TAG, "update OK");
			else {
				Log.d(TAG, "update NOT OK. Insert instead");
				contentResolver.insert(SubscriptionColumns.URI, cv);
			}
		}

		return contentUpdate;
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
    public TYPE getType() {
        return TYPE.DEFAULT;
    }

    @Override
    public String getPaletteUrl() {
        return getImageURL();
    }

    public URL getURL() {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

    @NonNull
    @Override
    public String getURLString() {
        return getURL() == null ? "" : getURL().toString();
    }

	public long getId() {
		return id;
	}

	@Deprecated
	public String getArtwork(Context context) {
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

	public void setLastItemUpdated(long argTimestamp) {
		this.lastItemUpdated = argTimestamp;
	}

	public long getLastItemUpdated() {
		return this.lastItemUpdated;
	}

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

	public void setType(String typeRss2) {
		// TODO Auto-generated method stub

	}

	public void setDescription(String content) {
		this.description = content;
	}
}
