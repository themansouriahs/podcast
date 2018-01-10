package org.bottiger.podcast.model;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.util.ArrayMap;
import android.support.v7.util.SortedList;
import android.text.TextUtils;
import android.util.Log;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.cloud.EventLogger;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.events.EpisodeChanged;
import org.bottiger.podcast.model.events.ItemChanged;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.notification.NewEpisodesNotification;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.PersistedSubscription;
import org.bottiger.podcast.provider.PodcastOpenHelper;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.provider.base.BaseEpisode;
import org.bottiger.podcast.utils.PreferenceHelper;
import org.bottiger.podcast.utils.StrUtils;
import org.bottiger.podcast.utils.rxbus.RxBasicSubscriber;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by aplb on 11-10-2015.
 */
public class Library {

    private static final String TAG = Library.class.getSimpleName();
    private static final int BACKPREASURE_BUFFER_SIZE = 10000;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DATE_NEW_FIRST, DATE_OLD_FIRST, NOT_SET, ALPHABETICALLY,
            ALPHABETICALLY_REVERSE, LAST_UPDATE, NEW_EPISODES, SCORE})
    public @interface SortOrder {}
    public static final int DATE_NEW_FIRST         = 0;
    public static final int DATE_OLD_FIRST         = 1;
    public static final int NOT_SET                = 2;
    public static final int ALPHABETICALLY         = 3;
    public static final int ALPHABETICALLY_REVERSE = 4;
    public static final int LAST_UPDATE            = 5;
    public static final int NEW_EPISODES           = 6;
    public static final int SCORE                  = 7;

    @NonNull private Context mContext;
    @NonNull private LibraryPersistency mLibraryPersistency;

    @Library.SortOrder
    private int mSubscriptionSortOrder = Library.ALPHABETICALLY;

    private final ReentrantLock mEpisodeLock = new ReentrantLock();
    private final ReentrantLock mSubscriptionLock = new ReentrantLock();

    private final NewEpisodesNotification mNewEpisodesNotification = new NewEpisodesNotification();

    @NonNull
    private final ArrayList<IEpisode> mEpisodes = new ArrayList<>();
    @NonNull
    private final ArrayMap<String, IEpisode> mEpisodesUrlLUT = new ArrayMap<>();
    @NonNull
    private final ArrayMap<Long, FeedItem> mEpisodesIdLUT = new ArrayMap<>();

    @NonNull
    @Deprecated
    public PublishSubject<Subscription> mSubscriptionsChangeObservable = PublishSubject.create();

    @NonNull
    public io.reactivex.subjects.PublishSubject<Subscription> mSubscriptionsChangePublisher = io.reactivex.subjects.PublishSubject.create();

    @NonNull
    private final List<Subscription> mActiveSubscriptions = new LinkedList<>();

    @NonNull
    private final MutableLiveData<List<Subscription>> mActiveLiveSubscriptions = new MutableLiveData<>();
    @NonNull
    private final ArrayMap<String, Subscription> mLiveSubscriptionUrlLUT = new ArrayMap<>();
    @NonNull
    private final ArrayMap<String, Subscription> mSubscriptionUrlLUT = new ArrayMap<>();
    @NonNull
    private final ArrayMap<Long, Subscription> mSubscriptionIdLUT = new ArrayMap<>();


    public Library(@NonNull Context argContext) {
        mContext = argContext.getApplicationContext();

        mLibraryPersistency = new LibraryPersistency(argContext, this);

        mActiveLiveSubscriptions.setValue(mActiveSubscriptions);

        mSubscriptionSortOrder = PreferenceHelper.getIntegerPreferenceValue(
                mContext,
                R.string.pref_subscription_sort_order_key,
                Integer.valueOf(mSubscriptionSortOrder));

        loadSubscriptions();

        SoundWaves.getRxBus()
                .toObserverable()
                .onBackpressureBuffer(BACKPREASURE_BUFFER_SIZE)
                .ofType(ItemChanged.class)
                .subscribe(new Action1<ItemChanged>() {
                    @Override
                    public void call(ItemChanged itemChangedEvent) {
                        if (itemChangedEvent instanceof EpisodeChanged) {
                            handleChangedEvent((EpisodeChanged) itemChangedEvent);
                            return;
                        }

                        if (itemChangedEvent instanceof SubscriptionChanged) {
                            handleChangedEvent((SubscriptionChanged) itemChangedEvent);
                            return;
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.handleException(throwable);
                        Log.wtf(TAG, "Missing back pressure. Should not happen anymore :(");
                    }
                });

        SoundWaves.getRxBus2()
                .toFlowableCommon()
                .ofType(ItemChanged.class)
                .subscribe(new RxBasicSubscriber<ItemChanged>() {
                    @Override
                    public void onNext(ItemChanged changedEvent) {
                        if (changedEvent instanceof EpisodeChanged) {
                            handleChangedEvent((EpisodeChanged) changedEvent);
                            return;
                        }

                        if (changedEvent instanceof SubscriptionChanged) {
                            handleChangedEvent((SubscriptionChanged) changedEvent);
                            return;
                        }
                    }
                });
    }

    private void handleChangedEvent(SubscriptionChanged argSubscriptionChanged) {
        Subscription subscription = getSubscription(argSubscriptionChanged.getId());

        if (subscription == null || TextUtils.isEmpty(subscription.getURLString())) {
            if (argSubscriptionChanged.getAction() == SubscriptionChanged.LOADED) {
                return; // it's ok. The Subscription is marked as loaded before added to the model.
            }

            Log.wtf("Subscription empty!", "id: " + argSubscriptionChanged.getId());
            return;
        }

        try {
            mSubscriptionLock.lock();
            if (!subscription.IsSubscribed() &&
                mActiveSubscriptions.indexOf(subscription) != -1) {
                    Log.e("Unsubscribing", "from: " + subscription.getTitle() + ", tag:" + argSubscriptionChanged.getTag()); // NoI18N
                    mActiveSubscriptions.remove(subscription);
                    mActiveLiveSubscriptions.postValue(mActiveSubscriptions);
                    mSubscriptionsChangePublisher.onNext(subscription);
            }
        } finally {
            mSubscriptionLock.unlock();
        }

        switch (argSubscriptionChanged.getAction()) {
            case SubscriptionChanged.CHANGED:
            case SubscriptionChanged.REMOVED: {
                updateSubscription(subscription);
                break;
            }
            case SubscriptionChanged.ADDED:
            case SubscriptionChanged.SUBSCRIBED:
            case SubscriptionChanged.LOADED:
                break;
        }
    }

    private void handleChangedEvent(EpisodeChanged argEpisodeChanged) {
        @EpisodeChanged.Action int action = argEpisodeChanged.getAction();

        switch (action) {
            case EpisodeChanged.DOWNLOADED:
            case EpisodeChanged.DOWNLOAD_PROGRESS:
            case EpisodeChanged.FILE_DELETED:
                break;
            case EpisodeChanged.PARSED: {
                if (mEpisodesUrlLUT.containsKey(argEpisodeChanged.getUrl()))
                    return;

                break;
            }
            case EpisodeChanged.ADDED:
            case EpisodeChanged.REMOVED:
            case EpisodeChanged.CHANGED:
            case EpisodeChanged.PLAYING_PROGRESS: {
                IEpisode episode = getEpisode(argEpisodeChanged.getId());
                if (episode != null)
                    updateEpisode(episode);
                break;
            }
        }
    }

    /**
     *
     * @param argSubscription
     * @return
     */
    public boolean addEpisodes(@NonNull Subscription argSubscription) {
        mEpisodeLock.lock();

        LinkedList<IEpisode> episodes = argSubscription.getEpisodes().getFilteredList();
        LinkedList<String> keys = new LinkedList<>();
        LinkedList<FeedItem> unpersistedEpisodes = new LinkedList<>();

        for (int i = 0; i < episodes.size(); i++) {
            keys.add(i, getKey(episodes.get(i)));
        }

        try {
            if (mEpisodesUrlLUT.containsAll(keys)) {
                return false;
            }

            for (int i = 0; i < episodes.size(); i++) {
                FeedItem episode = (FeedItem) episodes.get(i);
                if (!mEpisodesUrlLUT.containsKey(keys.get(i))) {
                    mEpisodes.add(episode);
                    mEpisodesUrlLUT.put(keys.get(i), episode);

                    if (!episode.isPersisted()) {
                        unpersistedEpisodes.add(episode);
                    }
                }
            }

            //long start = System.currentTimeMillis();
            mLibraryPersistency.insert(mContext, unpersistedEpisodes);
            //long end = System.currentTimeMillis();
            //Log.d(TAG, "insert time: " + (end-start) + " ms (#" + unpersistedEpisodes.size() + ")");

            mNewEpisodesNotification.show(mContext, unpersistedEpisodes);

        } finally {
            mEpisodeLock.unlock();
        }

        return true;
    }

    public void clearNewEpisodeNotification() {
        Log.d(TAG, "clear new notification");
        mNewEpisodesNotification.removeNotification(mContext);
    }

    /*
        Return true if the episode was added
     */
    public boolean addEpisode(@Nullable IEpisode argEpisode) {
        return addEpisodeInternal(argEpisode, false);
    }

    /**
     * Adds an episode to the Library. If the Episode already exists in the Library it will be updated.
     *
     * @param argEpisode The Episode
     * @param argSilent Do not senda notification about the event
     * @return True if the episode was added. False it was already there.
     */
    private boolean addEpisodeInternal(@Nullable IEpisode argEpisode, boolean argSilent) {

        if (argEpisode == null)
            return false;

        boolean isFeedItem = argEpisode instanceof FeedItem;
        FeedItem item = isFeedItem ? (FeedItem)argEpisode : null;

        mEpisodeLock.lock();
        try {
            // If the item is a feedItem it should belong to a subscription.
            // If it does not belong to a subscription yet (i.e. we are parsing the subscription, maybe for the first time)
            // We do not add it to the library yet.
            if (isFeedItem && item.sub_id < 0)
                return false;

            String episodeUrl = argEpisode.getURL();
            boolean libraryContainsEpisode = mEpisodesUrlLUT.containsKey(episodeUrl);
            IEpisode libraryEpisode = libraryContainsEpisode ? mEpisodesUrlLUT.get(episodeUrl) : item;

            Subscription subscription = null;
            if (item != null) {
                subscription = item.getSubscription(mContext);

                if (subscription != null) {
                    subscription.addEpisode(libraryEpisode, true);
                }
            }

            if (libraryContainsEpisode) {
                // FIXME we should update the content of the model episode
                return false;
            }

            mEpisodes.add(argEpisode);
            mEpisodesUrlLUT.put(argEpisode.getURL(), argEpisode);

            if (isFeedItem) {
                boolean updatedEpisode = false;

                //long start = System.currentTimeMillis();
                if (!item.isPersisted()) {
                    updateEpisode(item);
                    updatedEpisode = true;
                }
                //long end = System.currentTimeMillis();
                //Log.d(TAG, "insert time: " + (end-start) + " ms");

                mEpisodesIdLUT.put(item.getId(), item);

                if (subscription != null && updatedEpisode) {
                    IEpisode episode = subscription.getEpisodes().getNewest();
                    if (episode != null) {
                        subscription.setLastUpdated(episode.getCreatedAt().getTime());
                    }
                }
            }
        } finally {
            mEpisodeLock.unlock();
        }

        return true;
    }

    @WorkerThread
    public void addSubscription(@Nullable Subscription argSubscription) {
        addSubscriptionInternal(argSubscription, false);
    }

    @WorkerThread
    private void addSubscriptionInternal(@Nullable Subscription argSubscription, boolean argSilent) {
        mSubscriptionLock.lock();
        try {
            if (argSubscription == null)
                return;

            if (mSubscriptionIdLUT.containsKey(argSubscription.getId())) {
                // The ID LUT is not cleared when a subscription is removed
                if (mSubscriptionUrlLUT.containsKey(argSubscription.getUrl()))
                    return;
            }

            mLiveSubscriptionUrlLUT.put(argSubscription.getUrl(), argSubscription);
            mSubscriptionUrlLUT.put(argSubscription.getUrl(), argSubscription);
            mSubscriptionIdLUT.put(argSubscription.getId(), argSubscription);

            if (mActiveSubscriptions.indexOf(argSubscription) == -1 &&
                    argSubscription.IsSubscribed()) {
                mActiveSubscriptions.add(argSubscription);
                mActiveLiveSubscriptions.postValue(mActiveSubscriptions);
            }

            if (!argSilent)
                mSubscriptionsChangePublisher.onNext(argSubscription);
        } finally {
            mSubscriptionLock.unlock();
        }
    }

    public void removeSubscription(@Nullable Subscription argSubscription) {
        mSubscriptionLock.lock();
        try {
            if (argSubscription == null)
                return;

            mEpisodeLock.lock();
            try {
                for (Iterator<IEpisode> iterator = mEpisodes.iterator(); iterator.hasNext();) {
                    IEpisode episode = iterator.next();
                    boolean doRemove = episode.getSubscription(mContext).equals(argSubscription);
                    if (doRemove) {
                        if (episode instanceof FeedItem) {
                            FeedItem feedItem = (FeedItem) episode;
                            mEpisodesIdLUT.remove(feedItem.getId());
                        }

                        mEpisodesUrlLUT.remove(episode.getURL());
                        // Remove the current element from the iterator and the list.
                        iterator.remove();
                    }
                }

            } finally {
                mEpisodeLock.unlock();
            }

            VendorCrashReporter.report("remove", argSubscription.getUrl());

            //mSubscriptionIdLUT.remove(argSubscription.getId());
            mSubscriptionUrlLUT.remove(argSubscription.getUrl());
            mActiveSubscriptions.remove(argSubscription);
            mActiveLiveSubscriptions.postValue(mActiveSubscriptions);

            argSubscription.unsubscribe("unsubscribe");
        } finally {
            mSubscriptionLock.unlock();
        }

        mSubscriptionsChangePublisher.onNext(argSubscription);
    }

    private void clearSubscriptions() {
        mSubscriptionLock.lock();
        try {
            mActiveSubscriptions.clear();
            mSubscriptionUrlLUT.clear();
            mSubscriptionIdLUT.clear();
            mActiveLiveSubscriptions.postValue(mActiveSubscriptions);
        } finally {
            mSubscriptionLock.unlock();
        }
    }

    public static Subscription getByCursor(@NonNull Cursor cursor, @NonNull SharedPreferences argSharedPreferences) {
        Subscription subscription = new Subscription(argSharedPreferences);

        subscription = SubscriptionLoader.fetchFromCursor(subscription, cursor);
        return subscription;
    }

    @Deprecated
    public List<Subscription> getSubscriptions() {
        return mActiveSubscriptions;
    }

    @NonNull
    public LiveData<List<Subscription>> getLiveSubscriptions() {
        return mActiveLiveSubscriptions;
    }

    @Nullable
    public Subscription getSubscription(@NonNull String argUrl) {
        return mSubscriptionUrlLUT.get(argUrl);
    }

    @Nullable
    public LiveData<ISubscription> getLiveSubscription(@NonNull String argUrl) {
        return getSubscription(argUrl);
    }

    public int getSubscriptionCount() {
        return  mActiveSubscriptions.size();
    }

    @Nullable
    public Subscription getSubscription(@NonNull Long argId, boolean doLookup) {
        if (mSubscriptionIdLUT.containsKey(argId)) {
            return mSubscriptionIdLUT.get(argId);
        }

        if (doLookup) {
            loadSubscriptionsInternalSync(false);
        }

        return mSubscriptionIdLUT.get(argId);
    }

    @Nullable
    public Subscription getSubscription(@NonNull Long argId) {
        return getSubscription(argId, false);
    }

    @NonNull
    public ArrayList<IEpisode> getEpisodes() {
        return mEpisodes;
    }

    @Nullable
    public IEpisode getEpisode(@NonNull String argUrl, boolean doLookup) {

        IEpisode episode = mEpisodesUrlLUT.get(argUrl);

        if (episode != null || !doLookup) {
            return episode;
        }

        String query = getSingleEpisodes(argUrl);
        Cursor cursor = PodcastOpenHelper.runQuery(mContext, query);
        episode = LibraryPersistency.fetchEpisodeFromCursor(cursor, null);

        return episode;
    }

    @Nullable
    public IEpisode getEpisode(@NonNull String argUrl) {
        return getEpisode(argUrl, false);
    }

    @Nullable
    public FeedItem getEpisode(long argId) {
        if (mEpisodesIdLUT.containsKey(argId)) {
            return mEpisodesIdLUT.get(argId);
        }

        return null;
    }

    public SortedList<IEpisode> newEpisodeSortedList(SortedList.Callback<IEpisode> argCallback) {
        SortedList<IEpisode> sortedList = new SortedList<>(IEpisode.class, argCallback);

        try {
            mEpisodeLock.lock();
            for (int i = 0; i < mEpisodes.size(); i++) {
                sortedList.add(mEpisodes.get(i));
            }
        } finally {
            mEpisodeLock.unlock();
        }

        //sortedList.addAll(mEpisodes);
        return sortedList;
    }

    public boolean containsSubscription(@Nullable ISubscription argSubscription) {
        if (argSubscription == null)
            return false;

        ISubscription subscription = mSubscriptionUrlLUT.get(argSubscription.getURLString());
        if (subscription == null)
            return false;

        return subscription.IsSubscribed();
    }

    public boolean containsEpisode(@Nullable IEpisode argEpisode) {
        if (argEpisode == null)
            return false;

        return mEpisodes.contains(argEpisode);
    }

    private void invalidate() {
        return;
    }

    /**
     * Return a timestamp which can be used to determine if an episode is new.
     * @return
     */
    public static long episodeNewThreshold() {
        int newThresholdInDays = 6;

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR,-newThresholdInDays);
        Date threshold= cal.getTime();
        return threshold.getTime();
    }

    /**
     * Get all the subscriptions from the database.
     *
     * @return
     */
    private String getAllSubscriptions() {
        long thresholdTimestamp = episodeNewThreshold();

        StringBuilder builder = new StringBuilder(200);
        builder.append("SELECT ");
        builder.append("*, ");
        builder.append("(SELECT count(");
        builder.append(ItemColumns.TABLE_NAME + "." + ItemColumns._ID);
        builder.append(") ");
        builder.append("FROM " + ItemColumns.TABLE_NAME + " ");
        builder.append("WHERE ");
        builder.append(ItemColumns.TABLE_NAME + "." + ItemColumns.SUBS_ID + " == ");
        builder.append(SubscriptionColumns.TABLE_NAME + "." + SubscriptionColumns._ID);
        builder.append(" AND ");
        builder.append(ItemColumns.TABLE_NAME + "." + ItemColumns.PUB_DATE + ">" + thresholdTimestamp + ") ");
        builder.append("AS " + SubscriptionColumns.NEW_EPISODES + " ");
        builder.append("FROM " + SubscriptionColumns.TABLE_NAME + " ");
        //builder.append("WHERE " + SubscriptionColumns.STATUS + "==" + Subscription.STATUS_SUBSCRIBED);

        return builder.toString();
    }

    private String getSingleEpisodes(@NonNull String argUrl) {
        return "SELECT * FROM " + ItemColumns.TABLE_NAME + " WHERE " + ItemColumns.URL + "==" + argUrl;
    }

    private String getAllEpisodes(@NonNull Subscription argSubscription) {
        return "SELECT * FROM " + ItemColumns.TABLE_NAME + " WHERE " + ItemColumns.SUBS_ID + "==" + argSubscription.getId();
    }

    private String getPlaylistEpisodes(@NonNull Playlist argPlaylist) {
        return "SELECT * FROM " + ItemColumns.TABLE_NAME + " WHERE " +
                argPlaylist.getWhere() + " ORDER BY " +
                argPlaylist.getOrder();
    }

    @WorkerThread
    public void loadPlaylistSync(@NonNull final Playlist argPlaylist) {
        String query = getPlaylistEpisodes(argPlaylist);
        loadPlaylistInternal(query, argPlaylist);
    }

    public rx.Subscription loadPlaylist(@NonNull final Playlist argPlaylist) {
        String query = getPlaylistEpisodes(argPlaylist);

        return Observable.just(query)
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Action1<String>() {
            @Override
            public void call(String query) {
                loadPlaylistInternal(query, argPlaylist);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                VendorCrashReporter.report("subscribeError" , throwable.toString());
                Log.d(TAG, "error: " + throwable.toString());
            }
        });
    }

    @WorkerThread
    private void loadPlaylistInternal(@NonNull String query, @NonNull  Playlist argPlaylist) {

        Cursor cursor = null;

        mEpisodeLock.lock();
        try {
            if (argPlaylist.isLoaded())
                return;

            cursor = PodcastOpenHelper.runQuery(Library.this.mContext, query);

            addEpisodes(cursor, null);

            // Populate the playlist from the library
            argPlaylist.populatePlaylist();

            argPlaylist.setIsLoaded(true);
        } finally {
            if (cursor != null)
                cursor.close();
            mEpisodeLock.unlock();
        }
    }

    private int addEpisodes(@NonNull Cursor argCursor, @Nullable FeedItem[] emptyItems) {
        int counter = 0;
        boolean wasAdded = false;
        while (argCursor.moveToNext()) {
            final FeedItem item;
            if (emptyItems != null) {
                item = LibraryPersistency.fetchEpisodeFromCursor(argCursor, emptyItems[counter]);
            } else {
                item = LibraryPersistency.fetchEpisodeFromCursor(argCursor, null);
            }
            wasAdded = addEpisode(item);

            if (wasAdded) {
                counter++;
            }
        }

        return counter;
    }

    private Single<List<Subscription>> mLoadedSubscriptionsObservable;
    public Single<List<Subscription>> getLoadedSubscriptions() {
        if (mLoadedSubscriptionsObservable == null) {
            mLoadedSubscriptionsObservable = io.reactivex.Observable.create((ObservableOnSubscribe<List<Subscription>>) e -> {
                loadSubscriptionsInternalSync(true);
                e.onNext(mActiveSubscriptions);
                e.onComplete();
            }).replay(1).autoConnect().firstOrError();
        }

        return mLoadedSubscriptionsObservable;
    }

    @MainThread
    private void loadSubscriptions() {
        Observable.just(1)
                .subscribeOn(Schedulers.io())
                .map(integer -> {
                    loadSubscriptionsInternalSync(true);
                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    // This is done in the adapter instead.
                    // The reason for this is that the adapter nows the size of the views
                    //preloadImages();
                });
    }

    public void loadSubscriptionsInternalSync(boolean doReload) {
        Cursor cursor = null;

        if (doReload) {
            clearSubscriptions();
        }

        Subscription subscription = null;
        try {
            cursor = PodcastOpenHelper.runQuery(Library.this.mContext, getAllSubscriptions());

            while (cursor.moveToNext()) {
                subscription = getByCursor(cursor, PreferenceManager.getDefaultSharedPreferences(mContext));

                if (!TextUtils.isEmpty(subscription.getUrl())) {
                    addSubscriptionInternal(subscription, true);
                } else {
                    subscription = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            if (subscription != null)
                mSubscriptionsChangePublisher.onNext(subscription);
            if(cursor != null)
                cursor.close();

        }
    }

    public void loadEpisodes(@NonNull final Subscription argSubscription) {
        if (argSubscription.IsLoaded())
            return;

        String query = getAllEpisodes(argSubscription);
        Observable.just(query)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String query) {
                        loadEpisodesSync(argSubscription, query);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.d(TAG, "error: " + throwable.toString());
                    }
                });
    }

    @WorkerThread
    public void loadEpisodesSync(@NonNull final Subscription argSubscription, @Nullable String argQuery) {

        synchronized (argSubscription) {

            if (argSubscription.IsLoaded())
                return;

            Cursor cursor = null;

            long start = System.currentTimeMillis();
            try {

                if (argQuery == null)
                    argQuery = getAllEpisodes(argSubscription);

                //argQuery = "update " + SubscriptionColumns.TABLE_NAME + " set " + SubscriptionColumns.STATUS + "=" + Subscription.STATUS_SUBSCRIBED;

                cursor = PodcastOpenHelper.runQuery(Library.this.mContext, argQuery);

                start = System.currentTimeMillis();

                FeedItem[] emptyItems = new FeedItem[1];
                int count = cursor.getCount();
                if (count > 0) {
                    emptyItems = new FeedItem[cursor.getCount()];

                    for (int i = 0; i < emptyItems.length; i++) {
                        emptyItems[i] = new FeedItem();
                    }
                }

                argSubscription.setIsRefreshing(true);

                addEpisodes(cursor, emptyItems);

                argSubscription.setIsRefreshing(false);
                argSubscription.setIsLoaded(true);
            } finally {
                if (cursor != null)
                    cursor.close();
                long end = System.currentTimeMillis();
                Log.d("loadAllEpisodes", "1: " + (end - start) + " ms");
            }
        }
    }

    public boolean IsSubscribed(String argUrl) {
        if (!StrUtils.isValidUrl(argUrl))
            return false;

        Subscription subscription = mSubscriptionUrlLUT.get(argUrl);

        return subscription != null && subscription.IsSubscribed();
    }

    public void unsubscribe(String argUrl, String argTag) {
        if (!StrUtils.isValidUrl(argUrl))
            return;

        Subscription subscription = mSubscriptionUrlLUT.get(argUrl);

        if (subscription == null)
            return;

        updateSubscription(subscription);
        removeSubscription(subscription);

        EventLogger.postEvent(mContext, EventLogger.UNSUBSCRIBE_PODCAST, null, argUrl, null);
    }

    @Deprecated
    public void subscribe(String argUrl) {
        if (!StrUtils.isValidUrl(argUrl))
            return;

        try {
            SlimSubscription slimSubscription = new SlimSubscription("", new URL(argUrl), "");
            subscribe(slimSubscription);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(@NonNull ISubscription argSubscription) {
        Observable
                .just(argSubscription)
                .map(new Func1<ISubscription, Subscription>() {
                    @Override
                    public Subscription call(ISubscription argSubscription) {

                        Subscription subscription;

                        mSubscriptionLock.lock();
                        try {
                            String key = getKey(argSubscription);
                            subscription = mSubscriptionUrlLUT.containsKey(key) ?
                                    mSubscriptionUrlLUT.get(key) :
                                    new Subscription(PreferenceManager.getDefaultSharedPreferences(mContext),
                                            argSubscription);

                            subscription.subscribe("Subscribe:from:Library.subscribe");
                            updateSubscription(subscription);

                            mSubscriptionUrlLUT.put(key, subscription);
                            mSubscriptionIdLUT.put(subscription.getId(), subscription);
                            mActiveSubscriptions.add(subscription);
                            mActiveLiveSubscriptions.postValue(mActiveSubscriptions);
                        } finally {
                            mSubscriptionLock.unlock();
                        }

                        EventLogger.postEvent(mContext, EventLogger.SUBSCRIBE_PODCAST, null, argSubscription.getURLString(), null);
                        mSubscriptionsChangePublisher.onNext(subscription);

                        SoundWaves.getAppContext(mContext).getRefreshManager().refresh(subscription, null);

                        return subscription;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Subscription>() {

                    Subscription mSubscription;

                    @Override
                    public void onCompleted() {
                        //SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.SUBSCRIBE_TO_FEED);
                        //notifySubscriptionChanged(mSubscription.getId(), SubscriptionChanged.ADDED);
                        addSubscription(mSubscription);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("OnError", e.getMessage());
                        VendorCrashReporter.report("OnError", e.getStackTrace().toString());
                    }

                    @Override
                    public void onNext(Subscription subscription) {
                        mSubscription = subscription;
                    }
                });
    }

    public void updateSubscription(Subscription argSubscription) {
        mLibraryPersistency.persist(argSubscription);

        // Update new episodes
        SortedList<IEpisode> episodes = argSubscription.getEpisodes();
        for (int i = 0; i < episodes.size(); i++) {
            IEpisode episode = episodes.get(i);
            if (!containsEpisode(episode)) {
                updateEpisode(episode);
            }
        }
    }

    public @LibraryPersistency.PersistencyResult
    int updateEpisode(@NonNull IEpisode argEpisode) {
        if (argEpisode instanceof FeedItem) {
            return updateEpisode((FeedItem) argEpisode);
        }

        return LibraryPersistency.ERROR;
    }

    private @LibraryPersistency.PersistencyResult
    int updateEpisode(@NonNull FeedItem argEpisode) {
        addEpisode(argEpisode);
        return mLibraryPersistency.persist(argEpisode);
    }

    private int compareSubscriptions(ISubscription s1, ISubscription s2) {

        if (s1.isPinned() != s2.isPinned()) {
            return Boolean.valueOf(s2.isPinned()).compareTo(s1.isPinned());
        }

        switch (mSubscriptionSortOrder) {

            case ALPHABETICALLY_REVERSE: {
                return compareTitle(s2, s1);
            }
            case LAST_UPDATE: {
                return compareDates(s2, s1); // largest first
            }
            case NEW_EPISODES: {
                int order = compareNewEpisodes(s2, s1); // largest first
                if (order == 0) {
                    return compareTitle(s1, s2);
                }

                return order;
            }
            case SCORE: {
                int order = compareNewScore(s2, s1); // largest first
                if (order == 0) {
                    return compareTitle(s1, s2);
                }

                return order;
            }
            case ALPHABETICALLY:
            case NOT_SET:
            case DATE_OLD_FIRST:
            case DATE_NEW_FIRST:
                break;
        }

        // Default
        //return s1.getTitle().compareTo(s2.getTitle());
        return compareTitle(s1, s2);
    }

    private static int compareDates(@Nullable ISubscription s1, @Nullable ISubscription s2) {
        Long episode1 = s1 != null ? s1.getLastUpdate() : 0;
        Long episode2 = s2 != null ? s2.getLastUpdate() : 0;

        return episode1.compareTo(episode2);
    }

    private static int compareNewScore(@Nullable ISubscription s1, @Nullable ISubscription s2) {
        Integer episode1 = s1 instanceof PersistedSubscription ? ((PersistedSubscription)s1).getScore() : 0;
        Integer episode2 = s2 instanceof PersistedSubscription ? ((PersistedSubscription)s2).getScore() : 0;

        return episode1.compareTo(episode2);
    }

    private static int compareNewEpisodes(@Nullable ISubscription s1, @Nullable ISubscription s2) {
        Integer episode1 = s1 != null ? s1.getNewEpisodes() : 0;
        Integer episode2 = s2 != null ? s2.getNewEpisodes() : 0;

        return episode1.compareTo(episode2);
    }

    private static int compareTitle(@Nullable ISubscription s1, @Nullable ISubscription s2) {
        String title1 = s1 != null ? s1.getTitle() : "";
        String title2 = s2 != null ? s2.getTitle() : "";

        return title1.compareTo(title2);
    }

    private void notifySubscriptionChanged(final long argId, @SubscriptionChanged.Action final int argAction, @Nullable String argTag) {
        if (TextUtils.isEmpty(argTag))
            argTag = "NoTag";

        SoundWaves.getRxBus2().send(new SubscriptionChanged(argId, argAction, argTag));
    }

    private static String getKey(@NonNull ISubscription argSubscription) {
        return argSubscription.getURLString();
    }

    private static String getKey(@NonNull IEpisode argEpisode) {
        return argEpisode.getURL();
    }

    public void setSubscriptionOrder(@SortOrder int argSortOrder) {
        PreferenceHelper.setIntegerPreferenceValue(mContext, R.string.pref_subscription_sort_order_key, argSortOrder);
        mSubscriptionSortOrder = argSortOrder;

        resetOrder();
    }

    public @SortOrder int getSubscriptionOrder() {
        return mSubscriptionSortOrder;
    }

    public void resetOrder() {
        //mActiveSubscriptions.beginBatchedUpdates();
        Subscription[] subscriptionsTmp = new Subscription[mActiveSubscriptions.size()];
        Subscription subTmp;

        for (int i = 0; i < subscriptionsTmp.length; i++) {
            subscriptionsTmp[i] = mActiveSubscriptions.get(i);
        }

        mActiveSubscriptions.clear();

        for (int i = 0; i < subscriptionsTmp.length; i++) {
            subTmp = subscriptionsTmp[i];
            mActiveSubscriptions.add(subTmp);
        }

        //mActiveSubscriptions.endBatchedUpdates();
        mActiveLiveSubscriptions.postValue(mActiveSubscriptions);
    }

    /**
     *
     *
     * @param argID
     * @param argEpisode
     */
    protected void setEpisodeId(@NonNull Long argID, @NonNull FeedItem argEpisode) {
        mEpisodesIdLUT.put(argID, argEpisode);
    }
}
