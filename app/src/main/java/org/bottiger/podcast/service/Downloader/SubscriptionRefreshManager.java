package org.bottiger.podcast.service.Downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.images.RequestManager;
import org.bottiger.podcast.parser.syndication.handler.FeedHandler;
import org.bottiger.podcast.parser.syndication.handler.UnsupportedFeedtypeException;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by apl on 26-03-2015.
 */
public class SubscriptionRefreshManager {

    private static final int MY_SOCKET_TIMEOUT_MS = 300000; // 300 s
    private static final String ACRA_KEY = "SubscriptionRefreshManager";
    public static final String DEBUG_KEY = "SubscriptionRefresh";

    private DefaultRetryPolicy mRetryPolicy = new DefaultRetryPolicy(
                                                            MY_SOCKET_TIMEOUT_MS,
                                                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);


    private final BlockingQueue<Runnable> mDecodeWorkQueue = new LinkedBlockingQueue<Runnable>();;
    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    // Creates a thread pool manager
    private static ThreadPoolExecutor mDecodeThreadPool;

    private static FeedHandler feedHandler = new FeedHandler();

    private Context mContext;
    private static RequestQueue sRequestQueue;

    public SubscriptionRefreshManager(@NonNull Context argContext) {
        mContext = argContext;
        RequestManager.initIfNeeded(mContext);

        if (sRequestQueue == null)
            sRequestQueue = RequestManager.getRequestQueue();

        if (mDecodeThreadPool == null) {
            mDecodeThreadPool = new ThreadPoolExecutor(
                    16,       // Initial pool size
                    16,       // Max pool size
                    KEEP_ALIVE_TIME,
                    KEEP_ALIVE_TIME_UNIT,
                    mDecodeWorkQueue);
        }
    }

    Response.ErrorListener getFailureListener() {
		return new Response.ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError error) { // Handle error
                Log.e(DEBUG_KEY, "Volley error: " + error.toString());
                if (error instanceof com.android.volley.ServerError) {

				} else {
					error.printStackTrace();
					int i = 5;
					i = i + i;
				}
			}
		};
	}

    public void refreshAll() {
        Log.d(DEBUG_KEY, "refreshAll()");
        refresh(null, null);
    }

    public void refresh(ISubscription subscription, IDownloadCompleteCallback argCallback) {
        Log.d(DEBUG_KEY, "refresh subscription: " + subscription + " (null => all)");


        if (EpisodeDownloadManager.updateConnectStatus(mContext) != EpisodeDownloadManager.NETWORK_STATE.OK) {
            Log.d(DEBUG_KEY, "refresh aborted, no network");
            return;
        }

        EpisodeDownloadManager.isDownloading = true;

        if (subscription != null) {
            addSubscriptionToQueue(mContext, subscription, sRequestQueue, argCallback);
        } else {
            populateQueue(mContext, argCallback);
        }

        //sRequestQueue.addRequestFinishedListener(mFinishedListener);

        //Log.d(DEBUG_KEY, "starting refresh using Volley");
        //sRequestQueue.start();
    }

    private void addSubscriptionToQueue(@NonNull final Context argContext, @NonNull final ISubscription argSubscription, RequestQueue requestQueue, final IDownloadCompleteCallback argCallback) {
        Log.d(DEBUG_KEY, "Adding to queue: " + argSubscription);

        if (argSubscription == null) {
            VendorCrashReporter.report(ACRA_KEY, "subscription=null");
            return;
        }

        String subscriptionUrl = argSubscription.getURL().toString();

        if (TextUtils.isEmpty(subscriptionUrl)) {
            VendorCrashReporter.report(ACRA_KEY, "subscription.url=empty");
            return;
        }

        /*
        StringResponseListener responseListener = new StringResponseListener(mContext
                .getContentResolver(), argCallback, argSubscription);

        RequestFuture<String> future = RequestFuture.newFuture();

        StringRequest request = new StringRequest(subscriptionUrl,
                future,
                getFailureListener());

        request.setRetryPolicy(mRetryPolicy);

        // Add the request to Volley
        requestQueue.add(request);
        Log.d(DEBUG_KEY, "Added to queue successfully: " + argSubscription);
        */

        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(subscriptionUrl)
                .build();

        mDecodeThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                com.squareup.okhttp.Response response = null;
                try {
                    response = client.newCall(request).execute();

                    if (response != null && response.isSuccessful()) {
                        String feedString = response.body().string();
                        processResponse(argContext.getContentResolver(), argCallback, argSubscription, feedString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static class StringResponseListener implements Response.Listener<String> {

		Subscription subscription;
		ContentResolver contentResolver;
        IDownloadCompleteCallback callback;

		public StringResponseListener(ContentResolver contentResolver,
                                      IDownloadCompleteCallback argCallback, @NonNull Subscription subscription) {
			this.subscription = subscription;
			this.contentResolver = contentResolver;
            this.callback = argCallback;
		}

		@Override
		public void onResponse(String response) {
            processResponse(contentResolver, callback, subscription, response);
		}
    }

    private int populateQueue(@NonNull Context argContext, @Nullable IDownloadCompleteCallback argCallback) {
        Log.d(DEBUG_KEY, "populateQueue");

        Cursor subscriptionCursor = null;
        int subscriptionsAdded = 0;

        try {
            subscriptionCursor = Subscription.allAsCursor(argContext
                    .getContentResolver());

            while (subscriptionCursor.moveToNext()) {

                Subscription sub = Subscription.getByCursor(subscriptionCursor);

                addSubscriptionToQueue(argContext, sub, sRequestQueue, argCallback);
                subscriptionsAdded++;
            }
        } finally {
            subscriptionCursor.close();
        }

        Log.d(DEBUG_KEY, "populateQueue added: " + subscriptionsAdded);
        return subscriptionsAdded;
    }

    private static void processResponse(ContentResolver contentResolver,
                                        IDownloadCompleteCallback argCallback, @NonNull ISubscription subscription, String argResponse) {
        Log.d(DEBUG_KEY, "Volley response from: " + subscription);
        //new ParseFeedTask().execute(response);

        ISubscription parsedSubscription = null;

        try {
            Log.d(DEBUG_KEY, "Parsing: " + subscription);
            parsedSubscription = feedHandler.parseFeed(contentResolver, subscription,
                    argResponse.replace("ï»¿", "")); // Byte Order Mark
        } catch (SAXException e) {
            Log.d(DEBUG_KEY, "Parsing EXCEPTION: " + subscription);
            VendorCrashReporter.handleException(e);
            VendorCrashReporter.report("subscription1", subscription.getURL().toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(DEBUG_KEY, "Parsing EXCEPTION: " + subscription);
            VendorCrashReporter.handleException(e);
            VendorCrashReporter.report("subscription2", subscription.getURL().toString());
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            Log.d(DEBUG_KEY, "Parsing EXCEPTION: " + subscription);
            VendorCrashReporter.handleException(e);
            VendorCrashReporter.report("subscription3", subscription.getURL().toString());
            e.printStackTrace();
        } catch (UnsupportedFeedtypeException e) {
            Log.d(DEBUG_KEY, "Parsing EXCEPTION: " + subscription);
            VendorCrashReporter.handleException(e);
            VendorCrashReporter.report("subscription4", subscription.getURL().toString());
            e.printStackTrace();
        }

        if (argCallback != null) {
            Log.d(DEBUG_KEY, "Parsing callback for: " + subscription);
            argCallback.complete(true, parsedSubscription);
        }
    }
}

