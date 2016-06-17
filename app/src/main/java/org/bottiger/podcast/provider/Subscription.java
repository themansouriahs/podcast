package org.bottiger.podcast.provider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.model.datastructures.EpisodeList;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.utils.BitMaskUtils;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.PlaybackSpeed;

import android.content.Context;
import android.content.SharedPreferences;
import io.requery.android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.support.v7.util.SortedList;
import android.text.TextUtils;
import android.util.Patterns;

import javax.annotation.Nullable;

public class Subscription extends BaseSubscription implements PaletteListener {

	private static final String TAG = "Subscription";

	private static SharedPreferences sSharedPreferences = null;

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

	private final int mOldestFirstID = R.string.pref_list_oldest_first_key;
	private final int mDeleteAfterPlaybackID = R.string.pref_delete_when_finished_key;


	@Retention(RetentionPolicy.SOURCE)
	@IntDef({STATUS_SUBSCRIBED, STATUS_UNSUBSCRIBED})
	public @interface Subscribed {}
	public static final int STATUS_SUBSCRIBED = 1;
	public static final int STATUS_UNSUBSCRIBED = 2;

    private boolean mIsDirty = false;
	private boolean mIsLoaded = false;
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
	public int new_episodes;
	@Deprecated public long auto_download;
    private int mPrimaryColor;
    private int mPrimaryTintColor;
    private int mSecondaryColor;

	/**
	 * Settings is a bitmasked int with various settings
	 */
	private int mSettings;

    private EpisodeList mEpisodes;
	private SortedList.Callback<IEpisode> mEpisodesListCallback = new SortedList.Callback<IEpisode>() {

		@Override
		public int compare(IEpisode o1, IEpisode o2) {

			if (o1 == null)
				return 1;

			if (o2 == null)
				return -1;

			Date dt1 = o1.getDateTime();

			if (dt1 == null)
				return 1;

			Date dt2 = o2.getDateTime();

			if (dt2 == null)
				return -1;

			return o2.getDateTime().compareTo(o1.getDateTime());
		}

		@Override
		public void onInserted(int position, int count) {

		}

		@Override
		public void onRemoved(int position, int count) {

		}

		@Override
		public void onMoved(int fromPosition, int toPosition) {

		}

		@Override
		public void onChanged(int position, int count) {

		}

		@Override
		public boolean areContentsTheSame(IEpisode oldItem, IEpisode newItem) {
			return false;
		}

		@Override
		public boolean areItemsTheSame(IEpisode item1, IEpisode item2) {
			return item1.equals(item2);
		}
	};

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
		new_episodes = -1;
	}

	public Subscription() {
		reset();
		if (sSharedPreferences == null) {
			sSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SoundWaves.getAppContext());
		}
		init();
	}

	public Subscription(String url_link) {
		this();
		url = url_link;
		title = url_link;
		link = url_link;
		init();
	}

	public Subscription(@NonNull ISubscription argSlimSubscription) {
		this();
		url = argSlimSubscription.getURLString();
		title = argSlimSubscription.getTitle();
		imageURL = argSlimSubscription.getImageURL();
		init();
	}

	private void init() {
		mEpisodes = new EpisodeList(IEpisode.class, mEpisodesListCallback);
	}

	public EpisodeList getEpisodes() {
		return mEpisodes;
	}

	public boolean contains(@NonNull IEpisode argEpisode) {
		return mEpisodes.indexOf(argEpisode) >= 0;
	}

	public boolean addEpisode(@NonNull IEpisode argEpisode, boolean argSilent) {
		if (contains(argEpisode))
			return false;

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

    public void setPrimaryColor(int argColor) {
		if (mPrimaryColor == argColor)
			return;

        mPrimaryColor = argColor;
		notifyPropertyChanged(null);
    }

    public void setPrimaryTintColor(int argColor) {
		if (mPrimaryTintColor == argColor)
			return;

        mPrimaryTintColor = argColor;
		notifyPropertyChanged(null);
    }

    public void setSecondaryColor(int argColor) {
		if (mSecondaryColor == argColor)
			return;

        mSecondaryColor = argColor;
		notifyPropertyChanged(null);
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
		notifyPropertyChanged(null);
    }

    public boolean IsDirty() {
        return mIsDirty;
    }

	public boolean IsLoaded() {
		return mIsLoaded;
	}

	public void setIsLoaded(boolean argIsLoaded) {
		mIsLoaded = argIsLoaded;
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

		mIsRefreshing = argIsRefreshing;

		if (!mIsRefreshing) {
			countNewEpisodes();
			notifyPropertyChanged(null);
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

	public void setId(long argId) {
		this.id = argId;
	}

	@Deprecated
	public String getArtwork(Context context) {
		return getImageURL();
	}

	public String getImageURL() {
		return imageURL;
	}

    public void setImageURL(String argUrl) {

		// FIXME tmp repair code. Remove in a future version
		if (argUrl == null)
			return;
		argUrl = argUrl.trim();

		if (imageURL != null && imageURL.equals(argUrl))
			return;

        imageURL = argUrl.trim();
		notifyPropertyChanged(null);
    }

	public String getUrl() {
		return url;
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
		if (status == STATUS_UNSUBSCRIBED)
			return STATUS_UNSUBSCRIBED;

		return STATUS_SUBSCRIBED;
	}

	public void setStatus(@Subscribed int argStatus, @NonNull String argTag) {
		if (status == argStatus)
			return;

		status = argStatus;

		if (status == STATUS_SUBSCRIBED)
			notifyPropertyChanged(SubscriptionChanged.SUBSCRIBED, argTag);
		else
			notifyPropertyChanged(argTag);
	}

	public int getSettings() {
		return mSettings;
	}

	public void setSettings(int argSettings) {
		mSettings = argSettings;
	}

	public int countNewEpisodes() {
		int newCounter = 0;
		List<IEpisode> list = getEpisodes().getUnfilteredList();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isNew())
				newCounter++;
		}

		new_episodes = newCounter;
		return getNewEpisodes();
	}

	public int getNewEpisodes() {
		return new_episodes <= 0 ? 0 : new_episodes;
	}

	public void setNewEpisodes(int argNewEpisodes) {
		new_episodes = argNewEpisodes;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void setTitle(String argTitle) {
		if (title != null && title.equals(argTitle))
			return;

		title = argTitle.trim();
		notifyPropertyChanged(null);
	}

	public void setURL(String argUrl) {
		if (url != null && url.equals(argUrl))
			return;

		url = argUrl.trim();
		notifyPropertyChanged(null);
	}

	public void setDescription(String content) {
		if (description != null && description.equals(content))
			return;

		description = content.trim();
		notifyPropertyChanged(null);
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

		notifyPropertyChanged(null);
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
			speedMap += 2<<-1;
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

	private boolean getApplicationValue(int argId, boolean argDefault) {
		String key = SoundWaves.getAppContext().getResources().getString(argId);
		return sSharedPreferences.getBoolean(key, argDefault);
	}

	public void notifyEpisodeAdded(boolean argSilent) {
		if (!mIsRefreshing || !argSilent)
			SoundWaves.getRxBus().send(new SubscriptionChanged(getId(), SubscriptionChanged.ADDED, null));
	}

	private void notifyPropertyChanged(@android.support.annotation.Nullable String argTag) {
		notifyPropertyChanged(SubscriptionChanged.CHANGED, argTag);
	}

	private void notifyPropertyChanged(@SubscriptionChanged.Action int event, @android.support.annotation.Nullable String argTag) {
		if (TextUtils.isEmpty(argTag))
			argTag = "NoTag";

		if (!mIsRefreshing)
			SoundWaves.getRxBus().send(new SubscriptionChanged(getId(), event, argTag));
	}

}
