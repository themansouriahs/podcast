package org.bottiger.podcast.provider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.utils.BitMaskUtils;
import org.bottiger.podcast.utils.ColorExtractor;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.util.Patterns;
import android.webkit.URLUtil;

import javax.annotation.Nullable;

public class Subscription implements ISubscription, PaletteListener {

	private static final String TAG = "Subscription";

	private static SharedPreferences sSharedPreferences = null;

	private static final int SHOW_EPISODE_DESCRIPTION_SET = 1;
	private static final int SHOW_EPISODE_DESCRIPTION = (1 << 1);
	private static final int ADD_NEW_TO_PLAYLIST_SET = (1 << 2);
	private static final int ADD_NEW_TO_PLAYLIST = (1 << 3);
	private static final int DOWNLOAD_NEW_EPISODES_SET = (1 << 4);
	private static final int DOWNLOAD_NEW_EPISODES = (1 << 5);
	private static final int DELETE_AFTER_PLAYBACK_SET = (1 << 6);
	private static final int DELETE_AFTER_PLAYBACK = (1 << 7);
	private static final int LIST_OLDEST_FIRST_SET = (1 << 8);
	private static final int LIST_OLDEST_FIRST = (1 << 9);

	private final int mOldestFirstID = R.string.pref_list_oldest_first_key;
	private final int mDeleteAfterPlaybackID = R.string.pref_delete_when_finished_key;
	private final int mAutoDownloadID = R.string.pref_download_on_update_key;
	private final int mPlaylistSubscriptionsID = R.string.pref_playlist_subscriptions_key;

	public final static int ADD_SUCCESS = 0;
	public final static int ADD_FAIL_UNSUCCESS = -2;

	public final static int STATUS_SUBSCRIBED = 1;
	public final static int STATUS_UNSUBSCRIBED = 2;

    private boolean mIsDirty = false;
	private boolean mIsRefreshing = false;

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

	/**
	 * Settings is a bitmasked int with various settings
	 */
	private int mSettings;

    private final ArrayList<IEpisode> mEpisodes = new ArrayList<>();

	public void setLink(@NonNull String argLink) {
		this.link = argLink;
	}

	public String getLink() {
		return link;
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
		mSettings = -1;
	}

	public Subscription() {
		reset();
		if (sSharedPreferences == null) {
			sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SoundWaves.getAppContext());
		}
	}

	public Subscription(String url_link) {
		this();
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
			cv.put(SubscriptionColumns.SETTINGS, -1);
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

	public ArrayList<IEpisode> getEpisodes() {
		return mEpisodes;
	}

	public void addEpisode(@NonNull IEpisode argEpisode) {
		mEpisodes.add(argEpisode);
	}

	/**
	 * This method updates the URL of the subscription.
	 *
	 * @param argNewUrl
	 */
	public void updateUrl(@NonNull Context argContext, @Nullable String argNewUrl) {

		// We run a sanity check because we do not want to run the rest of the code without a context
		if (argContext == null) {
			throw new IllegalStateException("Context can not be null"); // NoI18N
		}

		URL url;
		try {
			url = new URL(argNewUrl);
		} catch (MalformedURLException e) {
			failUpdateUrl(argNewUrl);
			return;
		}

		if (Patterns.WEB_URL.matcher(url.toString()).matches()) {
			failUpdateUrl(url.toString());
			return;
		}

		// I currently doesn't test that the new url can be parsed and contains content

		// Ok, the url should be valid
		// generate the SQL
		String sqlUpdateSubscription = String.format("UPDATE %s SET %s=%s WHERE %s=%s LIMIT 1",
								SubscriptionColumns.TABLE_NAME,
								SubscriptionColumns.URL,
								url.toString(),
								SubscriptionColumns.URL,
								this.url);
		/*
		String sqlUpdateItems = String.format("UPDATE %s SET %s=%s WHERE %s=%s LIMIT 1",
								ItemColumns.TABLE_NAME,
								ItemColumns.URL,
								url.toString(),
								ItemColumns.URL,
								this.url);;
		*/
		PodcastOpenHelper helper = PodcastOpenHelper.getInstance(argContext);
		SQLiteDatabase db = helper.getWritableDatabase();

		db.beginTransaction();
		try {
			db.execSQL(sqlUpdateSubscription);
			//db.execSQL(sqlUpdateItems);
			/*
			this.url = url.toString();
			for (int i = 0; i < getEpisodes().size(); i++) {
				IEpisode episode = getEpisodes().get(0);
				if (episode instanceof FeedItem) {
					FeedItem feedItem = (FeedItem) episode;
					feedItem.sub
				}
			}*/
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private void failUpdateUrl(@Nullable String argNewUrl) {
		VendorCrashReporter.report("updateURL failed", "Unable to change subscription url to: " + argNewUrl); // NoI18N
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

		if (mSettings >= 0)
			cv.put(SubscriptionColumns.SETTINGS, mSettings);

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
		return "Subscription: " + this.title + " (" + this.url + ")";
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
	public boolean IsSubscribed() {
		return status == STATUS_SUBSCRIBED;
	}

	@Override
	public boolean IsRefreshing() {
		return mIsRefreshing;
	}

	@Override
	public void setIsRefreshing(boolean argIsRefreshing) {
		mIsRefreshing = argIsRefreshing;
	}

	@Override
    public @Type int getType() {
        return ISubscription.DEFAULT;
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


	/**
	 * http://stackoverflow.com/questions/4549131/bitmask-question
	 *
	 * bitmask |= TRADEABLE; // Sets the flag using bitwise OR
	 * bitmask &= ~TRADEABLE; // Clears the flag using bitwise AND and NOT
	 * bitmask ^= TRADEABLE; // Toggles the flag using bitwise XOR
	 */
	private boolean appDefault = true;

	public boolean isListOldestFirst() {
		if (!IsSettingEnabled(LIST_OLDEST_FIRST_SET))
			return getApplicationValue(mOldestFirstID, false);

		return IsSettingEnabled(LIST_OLDEST_FIRST);
	}

	public void setListOldestFirst(boolean listOldestFirst) {
        mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= LIST_OLDEST_FIRST_SET;

		if (listOldestFirst)
			mSettings |= LIST_OLDEST_FIRST;
		else
			mSettings &= ~LIST_OLDEST_FIRST;
	}

	public boolean isDeleteWhenListened() {
		if (!IsSettingEnabled(DELETE_AFTER_PLAYBACK_SET))
			return getApplicationValue(mDeleteAfterPlaybackID, false);

		return IsSettingEnabled(DELETE_AFTER_PLAYBACK);
	}

	public void setDeleteWhenListened(boolean deleteWhenListened) {
        mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= DELETE_AFTER_PLAYBACK_SET;

		if (deleteWhenListened)
			mSettings |= DELETE_AFTER_PLAYBACK;
		else
			mSettings &= ~DELETE_AFTER_PLAYBACK;
	}

	public boolean doDownloadNew(boolean argDefault) {
		if (!IsSettingEnabled(DOWNLOAD_NEW_EPISODES_SET))
			return argDefault;

		return IsSettingEnabled(DOWNLOAD_NEW_EPISODES);
	}

	public void setDownloadNew(boolean downloadNew) {
        mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= DOWNLOAD_NEW_EPISODES_SET;

		if (downloadNew)
			mSettings |= DOWNLOAD_NEW_EPISODES;
		else
			mSettings &= ~DOWNLOAD_NEW_EPISODES;
	}

	public boolean isAddNewToPlaylist() {
		if (!IsSettingEnabled(ADD_NEW_TO_PLAYLIST_SET))
			return true;

		return IsSettingEnabled(ADD_NEW_TO_PLAYLIST);
	}

	public void setAddNewToPlaylist(boolean addNewToPlaylist) {
        mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= ADD_NEW_TO_PLAYLIST_SET;

		if (addNewToPlaylist)
			mSettings |= ADD_NEW_TO_PLAYLIST;
		else
			mSettings &= ~ADD_NEW_TO_PLAYLIST;
	}

	public boolean isShowDescription() {
		if (!IsSettingEnabled(SHOW_EPISODE_DESCRIPTION_SET))
			return true;

		return IsSettingEnabled(SHOW_EPISODE_DESCRIPTION);
	}

	public void setShowDescription(boolean showDescription) {
        mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= SHOW_EPISODE_DESCRIPTION_SET;

		if (showDescription)
			mSettings |= SHOW_EPISODE_DESCRIPTION;
		else
			mSettings &= ~SHOW_EPISODE_DESCRIPTION;
	}

	private boolean IsSettingEnabled(int setting) {
		//return mSettings > 0 && ((setting & mSettings) != 0);
		return BitMaskUtils.IsBitSet(mSettings, setting);
	}

	private boolean getApplicationValue(int argId, boolean argDefault) {
		String key = SoundWaves.getAppContext().getResources().getString(argId);
		return sSharedPreferences.getBoolean(key, argDefault);
	}

}
