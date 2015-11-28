package org.bottiger.podcast.model;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.util.ArrayMap;
import android.support.v7.util.SortedList;
import android.util.Log;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.debug.SqliteCopy;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.events.DownloadProgress;
import org.bottiger.podcast.model.events.EpisodeChanged;
import org.bottiger.podcast.model.events.ItemChanged;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.PodcastOpenHelper;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.utils.StrUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final String TAG = "Library";

    @NonNull private Context mContext;
    @NonNull private SqlBrite mSqlBrite = SqlBrite.create();
    @NonNull private BriteDatabase mDb;
    @NonNull private LibraryPersistency mLibraryPersistency;

    private final ReentrantLock mLock = new ReentrantLock();

    @NonNull
    private ArrayList<IEpisode> mEpisodes = new ArrayList<>();
    @NonNull
    private ArrayMap<String, FeedItem> mEpisodesUrlLUT = new ArrayMap<>();
    @NonNull
    private ArrayMap<Long, FeedItem> mEpisodesIdLUT = new ArrayMap<>();

    @NonNull
    public PublishSubject<Subscription> mSubscriptionsChangeObservable = PublishSubject.create();

    @NonNull
    private SortedList<Subscription> mActiveSubscriptions;
    @NonNull
    private ArrayMap<String, Subscription> mSubscriptionUrlLUT = new ArrayMap<>();
    @NonNull
    private ArrayMap<Long, Subscription> mSubscriptionIdLUT = new ArrayMap<>();
    @NonNull
    private SortedList.Callback<Subscription> mSubscriptionsListCallback = new SortedList.Callback<Subscription>() {

        /**
         *
         * @param o1
         * @param o2
         * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
         */
        @Override
        public int compare(Subscription o1, Subscription o2) {
            return compareSubscriptions(o1, o2);
        }

        @Override
        public void onInserted(int position, int count) {
            notifySubscriptionChanged(mActiveSubscriptions.get(position).getId(), SubscriptionChanged.ADDED);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifySubscriptionChanged(mActiveSubscriptions.get(position).getId(), SubscriptionChanged.REMOVED);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {

        }

        @Override
        public void onChanged(int position, int count) {
            notifySubscriptionChanged(mActiveSubscriptions.get(position).getId(), SubscriptionChanged.CHANGED);
        }

        @Override
        public boolean areContentsTheSame(Subscription oldItem, Subscription newItem) {
            return false;
        }

        @Override
        public boolean areItemsTheSame(Subscription item1, Subscription item2) {
            return false;
        }
    };

    public Library(@NonNull Context argContext) {
        mContext = argContext;
        mDb = mSqlBrite.wrapDatabaseHelper(PodcastOpenHelper.getInstance(argContext));
        mLibraryPersistency = new LibraryPersistency(argContext, this);

        mActiveSubscriptions = new SortedList<>(Subscription.class, mSubscriptionsListCallback);

        loadSubscriptions();

        SoundWaves.getRxBus().toObserverable()
                .ofType(ItemChanged.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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
                        Log.w(TAG, "Error");
                    }
                });
    }

    public void handleChangedEvent(SubscriptionChanged argSubscriptionChanged) {
        Subscription subscription = getSubscription(argSubscriptionChanged.getId());
        updateSubscription(subscription);
    }

    public void handleChangedEvent(EpisodeChanged argEpisodeChanged) {
        @EpisodeChanged.Action int action = argEpisodeChanged.getAction();
        if (action == EpisodeChanged.PARSED) {
            if (mEpisodesUrlLUT.containsKey(argEpisodeChanged.getUrl()))
                return;
        }

        if (action == EpisodeChanged.CHANGED ||
            action == EpisodeChanged.ADDED ||
            action == EpisodeChanged.REMOVED) {
            IEpisode episode = getEpisode(argEpisodeChanged.getId());
            updateEpisode(episode);
        }
    }

    public void addEpisode(@Nullable IEpisode argEpisode) {
        mLock.lock();
        try {
        FeedItem item = null;
        if (argEpisode instanceof FeedItem)
            item = (FeedItem)argEpisode;

        if (item == null)
            return;


            // FIXME
            if (item.sub_id < 0)
                return;

            if (mEpisodesUrlLUT.containsKey(item.getURL())) {
                // FIXME we should update the content of the model episode
                return;
            }

            mEpisodes.add(argEpisode);
            mEpisodesUrlLUT.put(item.getURL(), item);
            mEpisodesIdLUT.put(item.getId(), item);

            if (!item.isPersisted()) {
                updateEpisode(item);
            }

            Subscription subscription = item.getSubscription();
            if (subscription != null) {
                subscription.addEpisode(item, true);
            }

        } finally {
            mLock.unlock();
        }
    }

    public void addSubscription(@Nullable Subscription argSubscription) {
        mLock.lock();
        try {
            if (argSubscription == null)
                return;

            if (mActiveSubscriptions.indexOf(argSubscription) == -1 && argSubscription.IsSubscribed()) {
                mActiveSubscriptions.add(argSubscription);
            }

            if (mSubscriptionIdLUT.containsKey(argSubscription.getId()))
                return;

            mSubscriptionUrlLUT.put(argSubscription.getUrl(), argSubscription);
            mSubscriptionIdLUT.put(argSubscription.getId(), argSubscription);

            mSubscriptionsChangeObservable.onNext(argSubscription);

        } finally {
            mLock.unlock();
        }
    }

    public void removeSubscription(@Nullable Subscription argSubscription) {
        mLock.lock();
        try {
            if (argSubscription == null)
                return;

            mSubscriptionIdLUT.remove(argSubscription);
            mSubscriptionUrlLUT.remove(argSubscription);
            mActiveSubscriptions.remove(argSubscription);
        } finally {
            mLock.unlock();
        }
    }

    private void clearSubscriptions() {
        mLock.lock();
        try {
            mActiveSubscriptions.clear();
            mSubscriptionUrlLUT.clear();
            mSubscriptionIdLUT.clear();
        } finally {
            mLock.unlock();
        }
    }

    public static Subscription getByCursor(Cursor cursor, Subscription argSubscription) {
        if (argSubscription == null) {
            argSubscription = new Subscription();
        }

        argSubscription = SubscriptionLoader.fetchFromCursor(argSubscription, cursor);
        return argSubscription;
    }

    public SortedList<Subscription> getSubscriptions() {
        return mActiveSubscriptions;
    }

    @Nullable
    public Subscription getSubscription(@NonNull String argUrl) {
        return mSubscriptionUrlLUT.get(argUrl);
    }

    @Nullable
    public Subscription getSubscription(@NonNull Long argId) {
        return mSubscriptionIdLUT.get(argId);
    }

    @Nullable
    public ArrayList<IEpisode> getEpisodes() {
        return mEpisodes;
    }

    @Nullable
    public FeedItem getEpisode(@NonNull String argUrl) {
        return mEpisodesUrlLUT.get(argUrl);
    }

    @Nullable
    public FeedItem getEpisode(long argId) {
        return mEpisodesIdLUT.get(argId);
    }

    public SortedList<IEpisode> newEpisodeSortedList(SortedList.Callback<IEpisode> argCallback) {
        SortedList<IEpisode> sortedList = new SortedList<>(IEpisode.class, argCallback);
        sortedList.addAll(mEpisodes);
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
     * Get all the subscriptions from the database.
     *
     * @return
     */
    private String getAllSubscriptions() {
        // SELECT *, (SELECT count(item._id) FROM item WHERE item.subs_id == subscriptions._id
        // AND item.pub_date>1445210057385) AS new_episodes FROM subscriptions

        int newThresholdInDays = 6;

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR,-newThresholdInDays);
        Date threshold= cal.getTime();
        long thresholdTimestamp = threshold.getTime();

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

        return builder.toString();
    }

    private String getAllEpisodes(@NonNull Subscription argSubscription) {
        return "SELECT * FROM " + ItemColumns.TABLE_NAME + " WHERE " + ItemColumns.SUBS_ID + "==" + argSubscription.getId();
    }

    private String getPlaylistEpisodes(@NonNull Playlist argPlaylist) {
        return "SELECT * FROM " + ItemColumns.TABLE_NAME + " WHERE " + argPlaylist.getWhere() + " ORDER BY " + argPlaylist.getOrder();
    }

    public void loadPlaylist(@NonNull final Playlist argPlaylist) {
        String query = getPlaylistEpisodes(argPlaylist);
        Observable<SqlBrite.Query> subscriptions = mDb.createQuery(SubscriptionColumns.TABLE_NAME, query);
        subscriptions.subscribe(new Action1<SqlBrite.Query>() {
            @Override
            public void call(SqlBrite.Query query) {
                Cursor cursor = null;
                int counter = 0;
                try {
                    cursor = query.run();
                    FeedItem episode;

                    while (cursor.moveToNext()) {
                        episode = LibraryPersistency.fetchEpisodeFromCursor(cursor, null);
                        addEpisode(episode);
                        argPlaylist.setItem(counter, episode);
                        counter++;
                    }
                    argPlaylist.notifyPlaylistChanged();
                } finally {
                    if (cursor != null)
                        cursor.close();
                }
            }
        });
    }

    public void loadSubscriptions() {
        Observable<SqlBrite.Query> subscriptions = mDb.createQuery(SubscriptionColumns.TABLE_NAME, getAllSubscriptions());
        subscriptions.subscribe(new Action1<SqlBrite.Query>() {
            @Override
            public void call(SqlBrite.Query query) {
                Cursor cursor = null;
                clearSubscriptions();
                try {
                    cursor = query.run();
                    Subscription subscription;

                    while (cursor.moveToNext()) {
                        subscription = getByCursor(cursor, null);
                        addSubscription(subscription);
                    }
                } finally {
                    if(cursor != null)
                        cursor.close();
                }
            }
        });
    }

    public void loadEpisodes(@NonNull final Subscription argSubscription) {
        if (argSubscription.IsLoaded())
            return;

        Observable<SqlBrite.Query> episodes = mDb.createQuery(ItemColumns.TABLE_NAME, getAllEpisodes(argSubscription));
        episodes
                .observeOn(Schedulers.io())
                .subscribe(new Action1<SqlBrite.Query>() {
                    @Override
                    public void call(SqlBrite.Query query) {
                        loadEpisodesSync(argSubscription, query);
                    }
                });
    }

    @WorkerThread
    public synchronized void loadEpisodesSync(@NonNull final Subscription argSubscription, @Nullable SqlBrite.Query argQuery) {
        if (argSubscription.IsLoaded())
            return;

        Cursor cursor = null;

        long start = 0;
        int counter = 0;
        try {

            if (argQuery == null) {
                cursor = PodcastOpenHelper.getInstance(mContext).getReadableDatabase().rawQuery(getAllEpisodes(argSubscription), new String[0]);
            } else {
                cursor = argQuery.run();
            }

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
            while (cursor.moveToNext()) {
                FeedItem item = LibraryPersistency.fetchEpisodeFromCursor(cursor, emptyItems[counter]);
                addEpisode(item);
                counter++;
            }
            argSubscription.setIsRefreshing(false);

            argSubscription.setIsLoaded(true);
        } finally {
            if (cursor != null)
                cursor.close();
            long end = System.currentTimeMillis();
            Log.d("loadAllEpisodes", "1: " + (end - start) + " ms");
        }
    }

    public boolean IsSubscribed(String argUrl) {
        if (!StrUtils.isValidUrl(argUrl))
            return false;

        Subscription subscription = mSubscriptionUrlLUT.get(argUrl);

        return subscription != null && subscription.IsSubscribed();
    }

    public void unsubscribe(String argUrl) {
        if (!StrUtils.isValidUrl(argUrl))
            return;

        Subscription subscription = mSubscriptionUrlLUT.get(argUrl);

        if (subscription == null)
            return;

        subscription.setStatus(Subscription.STATUS_UNSUBSCRIBED);
        updateSubscription(subscription);
        removeSubscription(subscription);
    }

    public void subscribe(String argUrl) {
        if (!StrUtils.isValidUrl(argUrl))
            return;

        Observable.just(argUrl).map(new Func1<String, Subscription>() {
                    @Override
                    public Subscription call(String argUrl) {

                        Subscription subscription;
                        subscription = mSubscriptionUrlLUT.get(argUrl);

                        if (subscription == null) {
                            subscription = new Subscription(argUrl);
                        }

                        subscription.setStatus(Subscription.STATUS_SUBSCRIBED);

                        try {
                            SoundWaves.sSubscriptionRefreshManager.refreshSync(mContext, subscription);
                        } catch (IOException e) {
                            VendorCrashReporter.handleException(e);
                            e.printStackTrace();
                            return null;
                        }
                        updateSubscription(subscription);

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

    public void updateEpisode(IEpisode argEpisode) {
        if (argEpisode instanceof FeedItem) {
            FeedItem item = (FeedItem) argEpisode;
            addEpisode(item);
            mLibraryPersistency.persist(item);
        }
    }

    private int compareSubscriptions(ISubscription s1, ISubscription s2) {
        return s1.getTitle().compareTo(s2.getTitle());
    }

    private void notifySubscriptionChanged(final long argId, @SubscriptionChanged.Action final int argAction) {
        SoundWaves.getRxBus().send(new SubscriptionChanged(argId, argAction));
        /*
        SoundWaves.getRxBus().toObserverable().sample(1, TimeUnit.SECONDS).doOnNext(new Action1<Object>() {
            @Override
            public void call(Object o) {
                new SubscriptionChanged(argId, argAction);
            }
        });
        */
    }
}
