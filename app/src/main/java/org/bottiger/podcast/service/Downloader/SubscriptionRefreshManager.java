package org.bottiger.podcast.service.Downloader;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.images.RequestManager;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.DownloadCompleteCallback;

/**
 * Created by apl on 26-03-2015.
 */
public class SubscriptionRefreshManager {

    private static Context mContext;

    public SubscriptionRefreshManager(@NonNull Context argContext) {
        mContext = argContext;
    }

    public static void start_update(final Context context) {
        start_update(context, null, null);
    }

    public static void start_update(final Context context,
                                    Subscription subscription, DownloadCompleteCallback argCallback) {

        if (EpisodeDownloadManager.updateConnectStatus(context) == EpisodeDownloadManager.NO_CONNECT)
            return;

        EpisodeDownloadManager.isDownloading = true;

        mContext = context;

        // FIXME
        // Perhaps we should do this in the background in the future
        RequestManager.initIfNeeded(context);
        RequestQueue requestQueue = RequestManager.getRequestQueue();

        Cursor subscriptionCursor;

        if (subscription == null) {
            subscriptionCursor = Subscription.allAsCursor(context
                    .getContentResolver());

            while (subscriptionCursor.moveToNext()) {

                Subscription sub = Subscription.getByCursor(subscriptionCursor);

                if (subscription == null || sub.equals(subscription)) {

                    addSubscriptionToQueue(sub, requestQueue, argCallback);
    /*
    StringRequest jr = new StringRequest(sub.getUrl(),
    new MyStringResponseListener(context
    .getContentResolver(), argCallback, sub),
    createGetFailureListener());

    int MY_SOCKET_TIMEOUT_MS = 300000;
    DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
    MY_SOCKET_TIMEOUT_MS,
    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    jr.setRetryPolicy(retryPolicy);

    // Add the request to Volley
    requestQueue.add(jr);
    processCounts.incrementAndGet();
    */

                }
            }
        } else {
            addSubscriptionToQueue(subscription, requestQueue, argCallback);
        }

        requestQueue.start();
    }

    private static void addSubscriptionToQueue(@NonNull Subscription argSubscription, RequestQueue requestQueue, DownloadCompleteCallback argCallback) {

        if (argSubscription == null) {
            VendorCrashReporter.report("addSubscriptionToQueue", "subscription=null");
            return;
        }

        StringRequest jr = new StringRequest(argSubscription.getUrl(),
                new EpisodeDownloadManager.MyStringResponseListener(mContext
                        .getContentResolver(), argCallback, argSubscription),
                EpisodeDownloadManager.createGetFailureListener());

        int MY_SOCKET_TIMEOUT_MS = 300000;
        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jr.setRetryPolicy(retryPolicy);

        // Add the request to Volley
        requestQueue.add(jr);
        EpisodeDownloadManager.processCounts.incrementAndGet();
    }
}

