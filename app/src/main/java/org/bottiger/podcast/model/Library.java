package org.bottiger.podcast.model;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.util.Log;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
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
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by aplb on 11-10-2015.
 */
public class Library {

    @NonNull private Context mContext;
    @NonNull private SqlBrite mSqlBrite = SqlBrite.create();
    @NonNull private BriteDatabase mDb;
    @NonNull private LibraryPersistency mLibraryPersistency;

    private final ReentrantLock mLock = new ReentrantLock();

    @NonNull
    private ArrayList<IEpisode> mEpisodes = new ArrayList<>();

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

        //loadPlaylist(PlayerService.getInstance().getPlaylist());
        loadSubscriptions();
    }

    public void addEpisode(FeedItem argEpisode) {
        mLock.lock();
        try {
            if (argEpisode == null)
                return;

            if (mEpisodes.contains(argEpisode))
                return;

            mEpisodes.add(argEpisode);
        } finally {
            mLock.unlock();
        }
    }

    public void addSubscription(Subscription argSubscription) {
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
        } finally {
            mLock.unlock();
        }
    }

    public void removeSubscription(Subscription argSubscription) {
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

    public Subscription getSubscription(@NonNull String argUrl) {
        return mSubscriptionUrlLUT.get(argUrl);
    }

    public ISubscription getSubscription(@NonNull Long argId) {
        return mSubscriptionIdLUT.get(argId);
    }

    public SortedList<IEpisode> newEpisodeSortedList(SortedList.Callback<IEpisode> argCallback) {
        SortedList<IEpisode> sortedList = new SortedList<>(IEpisode.class, argCallback);
        sortedList.addAll(mEpisodes);
        return sortedList;
    }

    public boolean containsSubscription(@Nullable ISubscription argSubscription) {
        if (argSubscription == null)
            return false;

        return mSubscriptionUrlLUT.containsKey(argSubscription.getURLString());
    }

    private void invalidate() {
        return;
    }

    private String getAllSubscriptions() {
        return "SELECT * FROM " + SubscriptionColumns.TABLE_NAME;
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
                        episode = FeedItem.fetchFromCursor(cursor, null);
                        addEpisode(episode);
                        argPlaylist.setItem(counter, episode);
                        counter++;
                    }
                    argPlaylist.notifyPlaylistChanged();
                } finally {
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
                    cursor.close();
                }
            }
        });
    }

    public void loadEpisodes(@NonNull final Subscription argSubscription) {
        Observable<SqlBrite.Query> episodes = mDb.createQuery(ItemColumns.TABLE_NAME, getAllEpisodes(argSubscription));
        episodes
                .observeOn(Schedulers.io())
                .subscribe(new Action1<SqlBrite.Query>() {
                    @Override
                    public void call(SqlBrite.Query query) {
                        Cursor cursor = null;
                        //mEpisodes.clear();

                /*
                HashMap<Long, Subscription> subscriptionMap = new HashMap<>(mActiveSubscriptions.size());
                Subscription subscription;
                for (int i = 0; i < mActiveSubscriptions.size(); i++) {
                    subscription = (Subscription) mActiveSubscriptions.get(i);
                    subscriptionMap.put(subscription.getId(), subscription);
                }*/

                        long start = 0;

                        int counter = 0;
                        try {
                            cursor = query.run();

                            start = System.currentTimeMillis();

                            FeedItem[] emptyItems = new FeedItem[1];
                            int count = cursor.getCount();
                            if (count > 0) {
                                emptyItems = new FeedItem[cursor.getCount()];

                                for (int i = 0; i < emptyItems.length; i++) {
                                    //long start = System.currentTimeMillis();
                                    emptyItems[i] = new FeedItem();
                                    //long end = System.currentTimeMillis();
                                    //Log.d("allocateEpisode", i + ": " + (end-start));
                                }
                            }

                            while (cursor.moveToNext()) {
                                //long start = System.currentTimeMillis();
                                FeedItem item = FeedItem.fetchFromCursor(cursor, emptyItems[counter]);
                                //long end1 = System.currentTimeMillis();

                                //subscription = subscriptionMap.get(item.sub_id);
                                //long end2 = System.currentTimeMillis();
                                //if (subscription != null) {
                                argSubscription.addEpisode(item);
                                addEpisode(item);
                                //}

                                counter++;
                                //long end3 = System.currentTimeMillis();
                                //Log.d("loadEpisode", "1: " + (end1-start) + " ms. 2: " + (end2-start) + " ms. 3: " + (end3-start));
                            }

                            argSubscription.setIsLoaded(true);
                        } finally {
                            cursor.close();
                            long end = System.currentTimeMillis();
                            Log.d("loadAllEpisodes", "1: " + (end - start) + " ms");
                        }
                    }
                });
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
                        } else {
                            subscription.setStatus(Subscription.STATUS_SUBSCRIBED);
                        }
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
    }

    private int compareSubscriptions(ISubscription s1, ISubscription s2) {
        return s1.getTitle().compareTo(s2.getTitle());
    }

    private void notifySubscriptionChanged(long argId, @SubscriptionChanged.Action int argAction) {
        SoundWaves.getRxBus().send(new SubscriptionChanged(argId, argAction));
    }
}
