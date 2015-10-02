package org.bottiger.podcast.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v4.util.LongSparseArray;
import android.support.v4.util.LruCache;
import android.util.SparseArray;

import java.util.LinkedList;

/**
 * Created by apl on 22-05-2015.
 */
public class SubscriptionLoader {

    private static SubscriptionLruCache cache = null;

    private static void initCache() {
        if (cache == null) {
            int memoryClass = 16 * 1024 * 1024; // FIXME use getMemoryClass()
            cache = new SubscriptionLoader.SubscriptionLruCache(memoryClass);
        }
    }

    public static Cursor allAsCursor(ContentResolver context) {
        return context.query(SubscriptionColumns.URI,
                SubscriptionColumns.ALL_COLUMNS, null, null, null);
    }

    @Deprecated
    public static LinkedList<Subscription> allAsList(ContentResolver context) {
        LinkedList<Subscription> subscriptions = new LinkedList<>();
        Cursor cursor = null;
        try {
            cursor = allAsCursor(context);

            while (cursor.moveToNext()) {
                subscriptions.add(getByCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return subscriptions;
    }

    public static LongSparseArray<ISubscription> asList(ContentResolver context, boolean argIncludeUnsubscribed) {
        LongSparseArray<ISubscription> subscriptions = new LongSparseArray<>();
        Cursor cursor = null;
        try {
            cursor = allAsCursor(context);

            long key;
            Subscription subscription;
            while (cursor.moveToNext()) {
                subscription = getByCursor(cursor);
                if (argIncludeUnsubscribed || subscription.IsSubscribed()) {
                    key = subscription.getId();
                    subscriptions.append(key, subscription);
                }
            }
        } finally {
            cursor.close();
        }
        return subscriptions;
    }

    public static Subscription getByUrl(ContentResolver contentResolver,
                                        String url) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(SubscriptionColumns.URI,
                    SubscriptionColumns.ALL_COLUMNS, SubscriptionColumns.URL
                            + "=?", new String[]{url}, null);
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

    public static Subscription fetchFromCursor(Subscription sub, Cursor cursor) {
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

        sub.setPrimaryColor(cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.PRIMARY_COLOR)));
        sub.setPrimaryTintColor(cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.PRIMARY_TINT_COLOR)));
        sub.setSecondaryColor(cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.SECONDARY_COLOR)));

        // if item was not cached we put it in the cache
        synchronized (cache) {
            cache.put(sub.id, sub);
        }

        return sub;
    }


    static class SubscriptionLruCache extends
            LruCache<Long, Subscription> {

		public SubscriptionLruCache(int maxSize) {
			super(maxSize);
		}

	}
}
