package org.bottiger.podcast.service.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.webservices.datastore.IWebservice;
import org.bottiger.podcast.webservices.datastore.gpodder.GPodderAPI;
import org.bottiger.podcast.webservices.datastore.gpodder.IGPodderAPI;

import java.io.IOException;

import retrofit.Response;

/**
 * Created by aplb on 14-09-2015.
 */
public class CloudSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "CloudSyncAdapter";

    public CloudSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public CloudSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String cloudSyncKey = getContext().getResources().getString(R.string.pref_cloud_support_key);
        boolean supportCloudSync = sharedPreferences.getBoolean(cloudSyncKey, !BuildConfig.PRIVATE_MODE);
        if (supportCloudSync) {
            performGPodderSync();
        }
    }

    private void performGPodderSync() {
        Log.d(TAG, "Performing gPodder synchronization");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String usernameKey = getContext().getResources().getString(R.string.pref_gpodder_username_key);
        String passwordKey = getContext().getResources().getString(R.string.pref_gpodder_password_key);
        String username = sharedPreferences.getString(usernameKey, "");
        String password = sharedPreferences.getString(passwordKey, "");

        GPodderAPI mGPodderAPI = new GPodderAPI(username, password);

        try {
            if (!mGPodderAPI.authenticateSync()) {
                return;
            }

            LongSparseArray<ISubscription> iSubscriptionLongSparseArray =
                    SubscriptionLoader.asList(getContext().getContentResolver());

            // FIXME Hack to convet types
            LongSparseArray<Subscription> subscriptionLongSparseArray = new LongSparseArray<>(iSubscriptionLongSparseArray.size());
            Subscription value;
            for(int i = 0; i < iSubscriptionLongSparseArray.size(); i++) {
                long arrayKey = iSubscriptionLongSparseArray.keyAt(i);
                ISubscription subscription = iSubscriptionLongSparseArray.get(arrayKey);

                subscriptionLongSparseArray.put(arrayKey, (Subscription)subscription);
            }

            mGPodderAPI.synchronize(getContext(), subscriptionLongSparseArray);
        } catch (IOException e) {
            VendorCrashReporter.handleException(e);
        }
    }
}
