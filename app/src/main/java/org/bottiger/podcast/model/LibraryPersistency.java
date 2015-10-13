package org.bottiger.podcast.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;

import java.util.Collection;

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

    public void persist(IEpisode argEpisode) {

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
            long lastUpdated = Long.valueOf(System.currentTimeMillis());
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
                    SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.SUBSCRIBE_TO_FEED);
                }
            }
        }

        return contentUpdate;
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
}
