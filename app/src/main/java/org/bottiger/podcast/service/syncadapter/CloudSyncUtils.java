package org.bottiger.podcast.service.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.SubscriptionColumns;

/**
 * Created by aplb on 14-09-2015.
 */
public class CloudSyncUtils {

    private static final String TAG = "CloudSyncUtils";

    /*
     * SyncAdapter
     */
    // Constants
    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "org.bottiger.podcast.provider.PodcastProvider";
    // An account type, in the form of a domain name
    // See authenticator.xml
    public static final String ACCOUNT_TYPE = "com.soundwavesapp.sync";
    // The account name
    public static final String ACCOUNT = "Cloud sync";
    // Instance fields

    // Sync interval constants
    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 180L; // 3 hours
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;

    @Nullable
    Account mAccount;

    @TargetApi(23)
    public boolean CreateSyncAccount(@NonNull Context argContext) {
        // Create the account type and default account
        Account newAccount = new Account(
                ACCOUNT, ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) argContext.getSystemService(
                        argContext.ACCOUNT_SERVICE);

        ContentResolver mResolver = argContext.getContentResolver();
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
        } else {
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
            //return false;
        }

        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        /*


         * Request the sync for the default account, authority, and
         * manual sync settings
         */
        //mResolver.setIsSyncable(newAccount, AUTHORITY, 1);
        mResolver.setSyncAutomatically(newAccount, AUTHORITY, true);
        //mResolver.requestSync(newAccount, AUTHORITY, settingsBundle);

        /*
         * Turn on periodic syncing
         */
        ContentResolver.addPeriodicSync(
                newAccount,
                AUTHORITY,
                Bundle.EMPTY,
                SYNC_INTERVAL);


        return true;
    }

    private void registerContentObserver(@NonNull ContentResolver argContenResolver) {
        // Construct a URI that points to the content provider data table
        Uri mUri = new Uri.Builder()
                .scheme(SubscriptionColumns.URI.getScheme())
                .authority(AUTHORITY)
                .path(SubscriptionColumns.TABLE_NAME)
                .build();

        // Get a handler for the main thread
        Handler mainHandler = new Handler(Looper.getMainLooper());

        /*
         * Create a content observer object.
}                * Its code does not mutatevider, so set
         * selfChange to "false"
         */
        TableObserver observer = new TableObserver(mainHandler);
		/*
         * Register the observer for the data table. The table's path
         * and any of its subpaths trigger the observer.
         */
        argContenResolver.registerContentObserver(mUri, true, observer);
    }

    public class TableObserver extends ContentObserver {
        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public TableObserver(Handler handler) {
            super(handler);
        }
        /*
         * Define a method that's called when data in the
         * observed content provider changes.
         * This method signature is provided for compatibility with
         * older platforms.
         */
        @Override
        public void onChange(boolean selfChange) {
         /*
          * Invoke the method signature available as of
          * Android platform version 4.1, with a null URI.
          */
            onChange(selfChange, null);
        }
        /*
         * Define a method that's called when data in the
         * observed content provider changes.
         */
        @Override
        public void onChange(boolean selfChange, Uri changeUri) {
            /*
             * Ask the framework to run your sync adapter.
             * To maintain backward compatibility, assume that
             * changeUri is null.
             */
            Bundle extras = new Bundle();
            ContentResolver.requestSync(mAccount, AUTHORITY, extras);
        }

    }

    public static boolean startCloudSync(@NonNull Context argContext) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);

        String key = argContext.getResources().getString(R.string.pref_cloud_support_key);
        boolean defaultValue = !BuildConfig.PRIVATE_MODE;

        boolean enableClouldSupport = prefs.getBoolean(key, defaultValue);

        if (!enableClouldSupport) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            try {
                CloudSyncUtils cloudSyncUtils = new CloudSyncUtils();
                cloudSyncUtils.CreateSyncAccount(argContext);
            } catch (Exception e) {
                Log.w(TAG, "Failed to start sync adapter. Exception: " + e.toString()); // NoI18N
                return false;
            }
        }

        return true;
    }
}
