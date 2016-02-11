package org.bottiger.podcast.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;

import java.util.Collection;
import java.util.Date;

/**
 * Created by aplb on 12-10-2015.
 */
public class LibraryPersistency {

    private static final String TAG = "LibraryPersistency";

    private ContentResolver mContentResolver;
    private Library mLibrary;

    public LibraryPersistency(@NonNull Context argContext, @NonNull Library argLibrary) {
        mContentResolver = argContext.getContentResolver();
        mLibrary = argLibrary;
    }

    public void persist(FeedItem argEpisode) {

        if (argEpisode.sub_id < 0) {
            Log.d("FeedItem", "Id less than 0");
            return;
        }

        ContentValues cv = getEpisodeContentValues(argEpisode, true);

        // BaseColumns._ID + "=" + id
        String condition = ItemColumns.URL + "='" + argEpisode.getURL() + "'";
        // String condition = ItemColumns.URL + "='?' AND "+ ItemColumns.URL +
        // "='?'"; // BUG!
        // http://code.google.com/p/android/issues/detail?id=56062

            int numUpdatedRows = mContentResolver.update(ItemColumns.URI, cv,
                    condition, null);
            if (numUpdatedRows == 1) {
                Log.d("FeedItem", "update OK");
            } else if (numUpdatedRows == 0) {
                Log.d("FeedItem", "update NOT OK. Insert instead");
                long createdAt = System.currentTimeMillis();
                cv.put(ItemColumns.CREATED, System.currentTimeMillis());
                argEpisode.created_at = createdAt;
                Uri result = mContentResolver.insert(ItemColumns.URI, cv);
                long newId = getId(result.toString());
                argEpisode.id = newId;
            } else {
                throw new IllegalStateException("Never update more than one row here");
            }
    }

    public void persist(Collection<IEpisode> argEpisodes) {

    }

    public ContentProviderOperation persist(@NonNull Subscription argSubscription) {
        return persist(argSubscription, false, false);
    }

    public ContentProviderOperation persist(@NonNull Subscription argSubscription,
                                            boolean batchUpdate,
                                            boolean silent) {

        ContentProviderOperation result = update(argSubscription, batchUpdate, silent);

        // unsubscribe
        if (!argSubscription.IsSubscribed()) {
            deleteEpisodes(argSubscription);
        }

        return result;
    }

    public ContentProviderOperation update(@NonNull Subscription argSubscription,
                                                  boolean batchUpdate,
                                                  boolean silent) {

        ContentProviderOperation contentUpdate = null;
        ContentValues cv = new ContentValues();

        cv.put(SubscriptionColumns.TITLE, argSubscription.getTitle());
        cv.put(SubscriptionColumns.URL, argSubscription.getURLString());
        cv.put(SubscriptionColumns.IMAGE_URL, argSubscription.getImageURL());
        cv.put(SubscriptionColumns.DESCRIPTION, argSubscription.description);

        if (!silent) {
            long lastUpdated = System.currentTimeMillis();
            cv.put(SubscriptionColumns.LAST_UPDATED, lastUpdated);
        }

        cv.put(SubscriptionColumns.LAST_ITEM_UPDATED, argSubscription.getLastItemUpdated());
        cv.put(SubscriptionColumns.STATUS, argSubscription.getStatus());
        cv.put(SubscriptionColumns.SETTINGS, argSubscription.getSettings());
        cv.put(SubscriptionColumns.PRIMARY_COLOR, argSubscription.getPrimaryColor());
        cv.put(SubscriptionColumns.PRIMARY_TINT_COLOR, argSubscription.getPrimaryTintColor());
        cv.put(SubscriptionColumns.SECONDARY_COLOR, argSubscription.getSecondaryColor());

        String condition = SubscriptionColumns.URL + "='" + argSubscription.getURLString() + "'";
        if (batchUpdate) {
            contentUpdate = ContentProviderOperation
                    .newUpdate(SubscriptionColumns.URI).withValues(cv)
                    .withSelection(condition, null).withYieldAllowed(true)
                    .build();
        } else {
            int status = cv.getAsInteger(SubscriptionColumns.STATUS);
            if (status == Subscription.STATUS_UNSUBSCRIBED) {
                String title = cv.getAsString(SubscriptionColumns.TITLE);
                Log.e("Unsubscribing", "from: " + title + ", stack:" + Log.getStackTraceString(new Exception()));
            }

            int numUpdatedRows = mContentResolver.update(
                    SubscriptionColumns.URI, cv, condition, null);
            if (numUpdatedRows == 1)
                Log.d(TAG, "update OK");
            else {
                Log.d(TAG, "update NOT OK. Insert instead");
                Uri uri = mContentResolver.insert(SubscriptionColumns.URI, cv);

                if (uri == null) {
                    VendorCrashReporter.report("Insert Subscription Failed", "" + argSubscription.getURLString());
                } else {
                    long id = Long.parseLong(uri.toString().replaceAll("[^0-9]", ""));
                    argSubscription.setId(id);
                    argSubscription.notifyEpisodeAdded();
                    SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.SUBSCRIBE_TO_FEED);
                }
            }
        }

        return contentUpdate;
    }

    private ContentValues getEpisodeContentValues(FeedItem argItem, Boolean silent) {
        ContentValues cv = new ContentValues();

        cv.put(ItemColumns.TITLE, argItem.getTitle());
        cv.put(ItemColumns.CONTENT, argItem.getDescription());
        cv.put(ItemColumns.PATHNAME, argItem.getFilename());
        cv.put(ItemColumns.SUBS_ID, argItem.getSubscriptionId());
        cv.put(ItemColumns.URL, argItem.getURL());
        cv.put(ItemColumns.FILESIZE, argItem.getFilesize());
        cv.put(ItemColumns.DURATION_MS, argItem.getDuration());
        cv.put(ItemColumns.OFFSET, argItem.getOffset());
        cv.put(ItemColumns.STATUS, argItem.getStatus());
        cv.put(ItemColumns.LISTENED, argItem.getListenedValue());
        cv.put(ItemColumns.PRIORITY, argItem.getPriority());
        cv.put(ItemColumns.IMAGE_URL, argItem.getArtwork());
        cv.put(ItemColumns.IS_DOWNLOADED, argItem.isDownloaded());

        // cv.put(ItemColumns.DATE, argItem.getDate());
        Date date = argItem.getDateTime();
        if (date != null) {
            long time = date.getTime();
            cv.put(ItemColumns.PUB_DATE, time);
        }

        if (!silent)
            cv.put(ItemColumns.LAST_UPDATE, System.currentTimeMillis());

        return cv;
    }

    public static FeedItem fetchEpisodeFromCursor(Cursor cursor,
                                                  FeedItem item)
    {
        if (item != null) {
            item.reset();
        } else
            item = new FeedItem();

        item.id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
        item.filename = cursor.getString(cursor.getColumnIndex(ItemColumns.PATHNAME));
        item.offset = cursor.getInt(cursor.getColumnIndex(ItemColumns.OFFSET));
        item.url = cursor.getString(cursor.getColumnIndex(ItemColumns.URL));
        item.image = cursor.getString(cursor.getColumnIndex(ItemColumns.IMAGE_URL));
        item.title = cursor.getString(cursor.getColumnIndex(ItemColumns.TITLE));
        item.author = cursor.getString(cursor.getColumnIndex(ItemColumns.AUTHOR));
        item.date = cursor.getString(cursor.getColumnIndex(ItemColumns.DATE));
        item.pub_date = cursor.getLong(cursor.getColumnIndex(ItemColumns.PUB_DATE));
        item.content = cursor.getString(cursor.getColumnIndex(ItemColumns.CONTENT));
        item.filesize = cursor.getLong(cursor.getColumnIndex(ItemColumns.FILESIZE));
        item.length = cursor.getLong(cursor.getColumnIndex(ItemColumns.LENGTH));
        int intVal = cursor.getInt(cursor.getColumnIndex(ItemColumns.IS_DOWNLOADED));
        item.isDownloaded = intVal == 1;

        item.duration_ms = cursor.getLong(cursor.getColumnIndex(ItemColumns.DURATION_MS));
        item.status = cursor.getInt(cursor.getColumnIndex(ItemColumns.STATUS));
        item.lastUpdate = cursor.getLong(cursor.getColumnIndex(ItemColumns.LAST_UPDATE));
        item.sub_title = cursor.getString(cursor.getColumnIndex(ItemColumns.SUB_TITLE));
        item.sub_id = cursor.getLong(cursor.getColumnIndex(ItemColumns.SUBS_ID));
        item.listened = cursor.getInt(cursor.getColumnIndex(ItemColumns.LISTENED));
        item.priority = cursor.getInt(cursor.getColumnIndex(ItemColumns.PRIORITY));
        item.created_at = cursor.getLong(cursor.getColumnIndex(ItemColumns.CREATED));

        return item;
    }

    private boolean deleteEpisodes(@NonNull Subscription argSubscription) {
        String where = ItemColumns.SUBS_ID + " = ?";
        String[] selectionArgs = { String.valueOf(argSubscription.getId()) };
        int deletedRows = mContentResolver.delete(ItemColumns.URI,
                where, selectionArgs);
        if (deletedRows > 1) {
            return true;
        } else
            return false;
    }

    private long getId(String argUri) {
        String s = argUri;
        int res = 0;
        int p = 1;
        int i = s.length()-1;
        while(i >= 0){
            int d = s.charAt(i) - '0';
            if (d>=0 && d<=9)
                res += d * p;
            else
                break;
            i--;
            p *= 10;
        }

        return res;
    }
}
