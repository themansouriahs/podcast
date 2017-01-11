package org.bottiger.podcast.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Patterns;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.datastructures.EpisodeList;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.provider.base.BasePodcastSubscription;
import org.bottiger.podcast.utils.BitMaskUtils;
import org.bottiger.podcast.utils.PlaybackSpeed;
import org.bottiger.podcast.utils.PreferenceHelper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.annotation.Nullable;

import io.requery.android.database.sqlite.SQLiteDatabase;

public class Subscription extends BasePodcastSubscription {

	private static final String TAG = Subscription.class.getSimpleName();

	private SharedPreferences mSharedPreferences = null;

	public static final int SHOW_EPISODE_DESCRIPTION_SET = 1;
	public static final int SHOW_EPISODE_DESCRIPTION = (1 << 1);
	public static final int ADD_NEW_TO_PLAYLIST_SET = (1 << 2);
	public static final int ADD_NEW_TO_PLAYLIST = (1 << 3);
	private static final int DOWNLOAD_NEW_EPISODES_SET = (1 << 4);
	private static final int DOWNLOAD_NEW_EPISODES = (1 << 5);
	private static final int DELETE_AFTER_PLAYBACK_SET = (1 << 6);
	private static final int DELETE_AFTER_PLAYBACK = (1 << 7);
	private static final int LIST_OLDEST_FIRST_SET = (1 << 8);
	private static final int LIST_OLDEST_FIRST = (1 << 9);
	private static final int PLAYBACK_SPEED_SET = (1 << 10);
	private static final int PLAYBACK_SPEED_1BIT = (1 << 11);
	private static final int PLAYBACK_SPEED_2BIT = (1 << 12);
	private static final int PLAYBACK_SPEED_3BIT = (1 << 13);
	private static final int PLAYBACK_SPEED_4BIT = (1 << 14);
	private static final int AUTHENTICATION_NEEDED_SET = (1 << 15);
	private static final int AUTHENTICATION_NEEDED = (1 << 16);
	private static final int AUTHENTICATION_WORKING = (1 << 17);
	private static final int SKIP_INTRO_SET = (1 << 18);
	private static final int SKIP_INTRO = (1 << 19);
	private static final int NOTIFY_ON_NEW_SET = (1 << 20);
	private static final int NOTIFY_ON_NEW = (1 << 21);
	private static final int PIN_AT_TOP_SET = (1 << 22);
	private static final int PIN_AT_TOP = (1 << 23);

	private final int mOldestFirstID = R.string.pref_list_oldest_first_key;
	private final int mDeleteAfterPlaybackID = R.string.pref_delete_when_finished_key;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({STATUS_SUBSCRIBED, STATUS_UNSUBSCRIBED})
	public @interface Subscribed {}
	public static final int STATUS_SUBSCRIBED = 1;
	public static final int STATUS_UNSUBSCRIBED = 2;

    /**
     * See SubscriptionColumns for documentation
     */
	public String comment;
	public String sync_id;
	public long status;
	public long lastUpdated;
	public long lastItemUpdated;
	public long fail_count;
	@Deprecated public long auto_download;

	public int new_episodes_cache;
	private int episode_count_cache;

	/**
	 * Settings is a bitmasked int with various settings
	 */
	private int mSettings;

	public String getLink() {
		return mLink;
	}

	public void reset() {
		id = -1;
		mTitle = null;
		mUrlString = null;
		mLink = null;
		comment = "";
		mDescription = null;
		mImageURL = null;
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
		new_episodes_cache = -1;
		episode_count_cache = -1;
	}

	public Subscription(@NonNull SharedPreferences argSharedPreferences) {
		reset();
		mSharedPreferences = argSharedPreferences;
		init();
	}

	public Subscription(@NonNull SharedPreferences argSharedPreferences, String url_link) {
		this(argSharedPreferences);
		mUrlString = url_link;
		mTitle = url_link;
		mLink = url_link;
		init();
	}

	public Subscription(@NonNull SharedPreferences argSharedPreferences, @NonNull ISubscription argSlimSubscription) {
		this(argSharedPreferences);
		mUrlString = argSlimSubscription.getURLString();
		mTitle = argSlimSubscription.getTitle();
		mImageURL = argSlimSubscription.getImageURL();
		init();
	}

	@Override
	protected void init() {
		super.init();
		mEpisodes = new EpisodeList(IEpisode.class, mEpisodesListCallback);
		setIsRefreshing(true);
	}

	private boolean updateEpisodeData(@NonNull IEpisode argCurrnetEpisode, @NonNull IEpisode argNewEpisode) {
		argCurrnetEpisode.setPubDate(argNewEpisode.getDateTime());
		if (argNewEpisode.getFilesize() > 0)
			argCurrnetEpisode.setFilesize(argNewEpisode.getFilesize());

		return true;
	}

	public boolean addEpisode(@NonNull IEpisode argEpisode, boolean argSilent) {
		if (contains(argEpisode)) {

			IEpisode modelEpisode = getMatchingEpisode(argEpisode);
			updateEpisodeData(modelEpisode, argEpisode);

			return false;
		}

		mEpisodes.add(argEpisode);
		
		if (!argSilent)
			notifyEpisodeAdded(true);

		return true;
	}

	public boolean addEpisode(@NonNull IEpisode argEpisode) {
		return addEpisode(argEpisode, false);
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

		// I currently doesn't test that the new mUrlString can be parsed and contains content

		// Ok, the mUrlString should be valid
		// generate the SQL
		String sqlUpdateSubscription = String.format("UPDATE %s SET %s=%s WHERE %s=%s LIMIT 1",
								SubscriptionColumns.TABLE_NAME,
								SubscriptionColumns.URL,
								url.toString(),
								SubscriptionColumns.URL,
								this.mUrlString);
		/*
		String sqlUpdateItems = String.format("UPDATE %s SET %s=%s WHERE %s=%s LIMIT 1",
								ItemColumns.TABLE_NAME,
								ItemColumns.URL,
								mUrlString.toString(),
								ItemColumns.URL,
								this.mUrlString);;
		*/
		PodcastOpenHelper helper = PodcastOpenHelper.getInstance(argContext);
		SQLiteDatabase db = helper.getWritableDatabase();

		db.beginTransaction();
		try {
			db.execSQL(sqlUpdateSubscription);
			//db.execSQL(sqlUpdateItems);
			/*
			this.mUrlString = mUrlString.toString();
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
		VendorCrashReporter.report("updateURL failed", "Unable to change subscription mUrlString to: " + argNewUrl); // NoI18N
	}

	@Override
    public void setPrimaryColor(int argColor) {
		super.setPrimaryColor(argColor);
		notifyPropertyChanged(null);
    }

	@Override
    public void setPrimaryTintColor(int argColor) {
		super.setPrimaryTintColor(argColor);
		notifyPropertyChanged(null);
    }

	@Override
    public void setSecondaryColor(int argColor) {
		super.setSecondaryColor(argColor);
		notifyPropertyChanged(null);
    }

	@Override
	public String toString() {
		return "Subscription: " + this.mTitle + " (" + this.mUrlString + ")";
	}

    public boolean IsDirty() {
        return mIsDirty;
    }

	public boolean IsLoaded() {
		return mIsLoaded;
	}

	public void setIsLoaded(boolean argIsLoaded) {
		mIsLoaded = argIsLoaded;
		countEpisodes();
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
		if (mIsRefreshing == argIsRefreshing)
			return;

		if (!argIsRefreshing) {
			// This calls a lot of notifyPropertyChanged().
			// We should call it before setting mIsRefreshing to false.
			countEpisodes();
		}

		mIsRefreshing = argIsRefreshing;

		if (!argIsRefreshing) {

			if (mIsDirty) {
				mIsDirty = false;
				notifyPropertyChanged(SubscriptionChanged.CHANGED, "updatedDuringRefresh");
			}

			notifyPropertyChanged(SubscriptionChanged.LOADED, "loaded");
		}
	}

	@Override
    public @Type int getType() {
        return ISubscription.DEFAULT;
    }

    @Override
    public String getPaletteUrl() {
        return getImageURL();
    }

    @NonNull
	public URL getURL() {
		try {
			return new URL(mUrlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}

    @NonNull
    @Override
    public String getURLString() {
        return mUrlString;
    }

	@Deprecated
	public String getArtwork(Context context) {
		return getImageURL();
	}

	public String getImageURL() {
		return mImageURL;
	}

	public String getUrl() {
		return mUrlString;
	}

	public void setLastItemUpdated(long argTimestamp) {
		if (lastItemUpdated == argTimestamp)
			return;

		this.lastItemUpdated = argTimestamp;
		notifyPropertyChanged(null);
	}

	public long getLastItemUpdated() {
		return this.lastItemUpdated;
	}

	public long lastModificationDate() {
		return lastUpdated;
	}

	public void setLastUpdated(long argTimestamp) {
		if (lastUpdated >= argTimestamp)
			return;

		this.lastUpdated = argTimestamp;
		notifyPropertyChanged(null);
	}

	public long getLastUpdate() {
		return this.lastUpdated;
	}

	public int getStatus() {
		if (!IsSubscribed())
			return STATUS_UNSUBSCRIBED;

		return STATUS_SUBSCRIBED;
	}

    public void subscribe(@NonNull String argTag) {
		super.subscribe();
        setStatus(STATUS_SUBSCRIBED, argTag);
    }

    public void unsubscribe(@NonNull String argTag) {
		VendorCrashReporter.report("Unsubscribe" , argTag + "mTitle: " + mTitle + " mUrlString: " + mUrlString);
        setStatus(STATUS_UNSUBSCRIBED, argTag);
    }

	private void setStatus(@Subscribed int argStatus, @NonNull String argTag) {
		if (status == argStatus)
			return;

		status = argStatus;

		if (status == STATUS_SUBSCRIBED)
			notifyPropertyChanged(SubscriptionChanged.SUBSCRIBED, argTag);
		else {
			VendorCrashReporter.report("setstatus", argTag);
			notifyPropertyChanged(argTag);
		}
	}

	public int getSettings() {
		return mSettings;
	}

	public void setSettings(int argSettings) {
		mSettings = argSettings;
	}

	/**
	 * Should be run when the Subscription is loaded or refreshed.
	 *
	 * @return
     */
	private boolean countEpisodes() {
		int newCounter = 0;
		List<IEpisode> list = getEpisodes().getUnfilteredList();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isNew())
				newCounter++;
		}

		setEpisodeCount(list.size());
		setNewEpisodes(newCounter);
		return true;
	}

	@NonNull
	public Integer getNewEpisodes() {
		return new_episodes_cache <= 0 ? 0 : new_episodes_cache;
	}

	void setNewEpisodes(int argNewEpisodes) {
		if (new_episodes_cache == argNewEpisodes)
			return;

		new_episodes_cache = argNewEpisodes;
		notifyPropertyChanged(null);
	}

	void setEpisodeCount(int argEpisodeCount) {
		if (episode_count_cache == argEpisodeCount)
			return;

		episode_count_cache = argEpisodeCount;
		notifyPropertyChanged(null);
	}

	public int getEpisodeCount() {
		if (mIsLoaded)
			return mEpisodes.size();

		return episode_count_cache;
	}

	/**
	 * http://stackoverflow.com/questions/4549131/bitmask-question
	 *
	 * bitmask |= TRADEABLE; // Sets the flag using bitwise OR
	 * bitmask &= ~TRADEABLE; // Clears the flag using bitwise AND and NOT
	 * bitmask ^= TRADEABLE; // Toggles the flag using bitwise XOR
	 */
	private boolean appDefault = true;

	public boolean isRequiringAuthentication() {
		if (!IsSettingEnabled(AUTHENTICATION_NEEDED_SET))
			return false;

		return IsSettingEnabled(AUTHENTICATION_NEEDED);
	}

	public boolean isAuthenticationWorking() {
		if (!IsSettingEnabled(AUTHENTICATION_NEEDED_SET))
			return true;

		return IsSettingEnabled(AUTHENTICATION_WORKING);
	}

	public void setAuthenticationWorking(boolean argIsAuthenticationWorking) {
		mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= AUTHENTICATION_NEEDED_SET;

		if (argIsAuthenticationWorking)
			mSettings |= AUTHENTICATION_WORKING;
		else
			mSettings &= ~AUTHENTICATION_WORKING;

		notifyPropertyChanged(null);
	}

	public void setRequiringAuthentication(boolean argIsRequiringAuthentication) {
		mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= AUTHENTICATION_NEEDED_SET;

		if (argIsRequiringAuthentication)
			mSettings |= AUTHENTICATION_NEEDED;
		else
			mSettings &= ~AUTHENTICATION_NEEDED;

		notifyPropertyChanged(null);
	}

	public boolean doSkipIntro() {
		if (!IsSettingEnabled(SKIP_INTRO_SET))
			return false;

		return IsSettingEnabled(SKIP_INTRO);
	}

	public void setDoSkipIntro(boolean argSkipIntro) {
		mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= SKIP_INTRO_SET;

		if (argSkipIntro)
			mSettings |= SKIP_INTRO;
		else
			mSettings &= ~SKIP_INTRO;

		notifyPropertyChanged(null);
	}

	public boolean isPinned() {
		if (!IsSettingEnabled(PIN_AT_TOP_SET))
			return false;

		return IsSettingEnabled(PIN_AT_TOP);
	}

	public void setIsPinned(boolean argIsPinned) {
		mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= PIN_AT_TOP_SET;

		if (argIsPinned)
			mSettings |= PIN_AT_TOP;
		else
			mSettings &= ~PIN_AT_TOP;

		notifyPropertyChanged(null);
	}

	public boolean doNotifyOnNew(@NonNull Context argContext) {
		if (!IsSettingEnabled(NOTIFY_ON_NEW_SET))
			return PreferenceHelper.getBooleanPreferenceValue(argContext,
					R.string.pref_new_episode_notification_key,
					R.bool.pref_new_episode_notification_default);

		return IsSettingEnabled(NOTIFY_ON_NEW);
	}

	public void setDoNotifyOnNew(boolean argDoNotifyOnNew) {
		mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= NOTIFY_ON_NEW_SET;

		if (argDoNotifyOnNew)
			mSettings |= NOTIFY_ON_NEW;
		else
			mSettings &= ~NOTIFY_ON_NEW;

		notifyPropertyChanged(null);
	}

	public boolean isListOldestFirst(@NonNull Resources argResources) {
		if (!IsSettingEnabled(LIST_OLDEST_FIRST_SET))
			return getApplicationValue(argResources, mOldestFirstID, false);

		return IsSettingEnabled(LIST_OLDEST_FIRST);
	}

	public void setListOldestFirst(boolean listOldestFirst) {
        mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= LIST_OLDEST_FIRST_SET;

		if (listOldestFirst)
			mSettings |= LIST_OLDEST_FIRST;
		else
			mSettings &= ~LIST_OLDEST_FIRST;

		notifyPropertyChanged(null);
	}

	public boolean isDeleteWhenListened(@NonNull Resources argResources) {
		if (!IsSettingEnabled(DELETE_AFTER_PLAYBACK_SET))
			return getApplicationValue(argResources, mDeleteAfterPlaybackID, false);

		return IsSettingEnabled(DELETE_AFTER_PLAYBACK);
	}

	public void setDeleteWhenListened(boolean deleteWhenListened) {
        mSettings = mSettings < 0 ? 0 : mSettings;
		mSettings |= DELETE_AFTER_PLAYBACK_SET;

		if (deleteWhenListened)
			mSettings |= DELETE_AFTER_PLAYBACK;
		else
			mSettings &= ~DELETE_AFTER_PLAYBACK;

		notifyPropertyChanged(null);
	}

	public float getPlaybackSpeed() {
		float parsedSpeed = parsePlaybackSpeed();

		if (parsedSpeed == PlaybackSpeed.UNDEFINED)
			return PlaybackSpeed.DEFAULT;

		return parsedSpeed;
	}

	private float parsePlaybackSpeed() {
		if (!IsSettingEnabled(PLAYBACK_SPEED_SET))
			return PlaybackSpeed.UNDEFINED;

		int speedMap = 0;

		if (IsSettingEnabled(PLAYBACK_SPEED_1BIT)) {
			speedMap += 2<<2;
		}

		if (IsSettingEnabled(PLAYBACK_SPEED_2BIT)) {
			speedMap += 2<<1;
		}

		if (IsSettingEnabled(PLAYBACK_SPEED_3BIT)) {
			speedMap += 2<<0;
		}

		if (IsSettingEnabled(PLAYBACK_SPEED_4BIT)) {
			speedMap += 1;
		}

		float speed = PlaybackSpeed.toSpeed(speedMap);

		return speed;
	}

	public void setPlaybackSpeed(int argPlaybackSpeedTimesTen) {
		mSettings = mSettings < 0 ? 0 : mSettings;

		if (argPlaybackSpeedTimesTen > 0) {
			mSettings |= PLAYBACK_SPEED_SET;
		} else {
			mSettings &= ~PLAYBACK_SPEED_SET;
			notifyPropertyChanged(null);
			return;
		}

		int playbackSpeedHash = PlaybackSpeed.toMap(argPlaybackSpeedTimesTen);

		if (playbackSpeedHash >= 2<<2) {
			mSettings |= PLAYBACK_SPEED_1BIT;
			playbackSpeedHash -= 2<<2;
		} else {
			mSettings &= ~PLAYBACK_SPEED_1BIT;
		}

		if (playbackSpeedHash >= 2<<1) {
			mSettings |= PLAYBACK_SPEED_2BIT;
			playbackSpeedHash -= 2<<1;
		} else {
			mSettings &= ~PLAYBACK_SPEED_2BIT;
		}

		if (playbackSpeedHash >= 2<<0) {
			mSettings |= PLAYBACK_SPEED_3BIT;
			playbackSpeedHash -= 2<<0;
		} else {
			mSettings &= ~PLAYBACK_SPEED_3BIT;
		}

		if (playbackSpeedHash > 2<<-1) {
			mSettings |= PLAYBACK_SPEED_4BIT;
			playbackSpeedHash -= 2<<-1;
		} else {
			mSettings &= ~PLAYBACK_SPEED_4BIT;
		}

		notifyPropertyChanged(null);
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

		notifyPropertyChanged(null);
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

		notifyPropertyChanged(null);
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

		notifyPropertyChanged(null);
	}

	private boolean IsSettingEnabled(int setting) {
		return BitMaskUtils.IsBitSet(mSettings, setting);
	}

	private boolean getApplicationValue(@NonNull Resources argResources, int argId, boolean argDefault) {
		String key = argResources.getString(argId);
		return mSharedPreferences.getBoolean(key, argDefault);
	}

	public void notifyEpisodeAdded(boolean argSilent) {
		if (!mIsRefreshing || !argSilent)
			SoundWaves.getRxBus().send(new SubscriptionChanged(getId(), SubscriptionChanged.ADDED, null));
	}

	protected void notifyPropertyChanged(@SubscriptionChanged.Action int event, @android.support.annotation.Nullable String argTag) {
		if (TextUtils.isEmpty(argTag))
			argTag = "NoTag";

		if (!mIsRefreshing) {
			SoundWaves.getRxBus().send(new SubscriptionChanged(getId(), event, argTag));
		} else if (mIsLoaded) {
			mIsDirty = true;
		}
	}

}
