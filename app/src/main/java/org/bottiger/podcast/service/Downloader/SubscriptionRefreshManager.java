package org.bottiger.podcast.service.Downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.images.RequestManager;
import org.bottiger.podcast.parser.syndication.handler.FeedHandler;
import org.bottiger.podcast.parser.syndication.handler.UnsupportedFeedtypeException;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.xml.sax.SAXException;

import java.io.IOException;

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

    private Context mContext;
    private static RequestQueue sRequestQueue;

    private RequestQueue.RequestFinishedListener<StringRequest> mFinishedListener = new RequestQueue.RequestFinishedListener<StringRequest>() {
        @Override
        public void onRequestFinished(Request<StringRequest> request) {
            return;
        }
    };

    public SubscriptionRefreshManager(@NonNull Context argContext) {
        mContext = argContext;
        RequestManager.initIfNeeded(mContext);

        if (sRequestQueue == null)
            sRequestQueue = RequestManager.getRequestQueue();
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

    public void refresh(Subscription subscription, IDownloadCompleteCallback argCallback) {
        Log.d(DEBUG_KEY, "refresh subscription: " + subscription + " (null => all)");


        if (EpisodeDownloadManager.updateConnectStatus(mContext) != EpisodeDownloadManager.NETWORK_STATE.OK) {
            Log.d(DEBUG_KEY, "refresh aborted, no network");
            return;
        }

        EpisodeDownloadManager.isDownloading = true;

        if (subscription != null) {
            addSubscriptionToQueue(subscription, sRequestQueue, argCallback);
        } else {
            populateQueue(mContext, argCallback);
        }

        sRequestQueue.addRequestFinishedListener(mFinishedListener);

        Log.d(DEBUG_KEY, "starting refresh using Volley");
        sRequestQueue.start();
    }

    private void addSubscriptionToQueue(@NonNull Subscription argSubscription, RequestQueue requestQueue, IDownloadCompleteCallback argCallback) {
        Log.d(DEBUG_KEY, "Adding to queue: " + argSubscription);

        if (argSubscription == null) {
            VendorCrashReporter.report(ACRA_KEY, "subscription=null");
            return;
        }

        String subscriptionUrl = argSubscription.getUrl();

        if (TextUtils.isEmpty(subscriptionUrl)) {
            VendorCrashReporter.report(ACRA_KEY, "subscription.url=empty");
            return;
        }

        StringResponseListener responseListener = new StringResponseListener(mContext
                .getContentResolver(), argCallback, argSubscription);

        StringRequest request = new StringRequest(subscriptionUrl,
                responseListener,
                getFailureListener());

        request.setRetryPolicy(mRetryPolicy);

        // Add the request to Volley
        requestQueue.add(request);
        Log.d(DEBUG_KEY, "Added to queue successfully: " + argSubscription);
    }

    private static class StringResponseListener implements Response.Listener<String> {

		FeedHandler feedHandler = new FeedHandler();
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
            Log.d(DEBUG_KEY, "Volley response from: " + subscription);
            //new ParseFeedTask().execute(response);

            try {
                Log.d(DEBUG_KEY, "Parsing: " + subscription);
                feedHandler.parseFeed(contentResolver, subscription,
                        response.replace("ï»¿", "")); // Byte Order Mark
            } catch (SAXException e) {
                Log.d(DEBUG_KEY, "Parsing EXCEPTION: " + subscription);
                VendorCrashReporter.handleException(e);
                VendorCrashReporter.report("subscription1", subscription.getUrl());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(DEBUG_KEY, "Parsing EXCEPTION: " + subscription);
                VendorCrashReporter.handleException(e);
                VendorCrashReporter.report("subscription2", subscription.getUrl());
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                Log.d(DEBUG_KEY, "Parsing EXCEPTION: " + subscription);
                VendorCrashReporter.handleException(e);
                VendorCrashReporter.report("subscription3", subscription.getUrl());
                e.printStackTrace();
            } catch (UnsupportedFeedtypeException e) {
                Log.d(DEBUG_KEY, "Parsing EXCEPTION: " + subscription);
                VendorCrashReporter.handleException(e);
                VendorCrashReporter.report("subscription4", subscription.getUrl());
                e.printStackTrace();
            }

            if (callback != null) {
                Log.d(DEBUG_KEY, "Parsing callback for: " + subscription);
                callback.complete(true);
            }
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

                addSubscriptionToQueue(sub, sRequestQueue, argCallback);
                subscriptionsAdded++;
            }
        } finally {
            subscriptionCursor.close();
        }

        Log.d(DEBUG_KEY, "populateQueue added: " + subscriptionsAdded);
        return subscriptionsAdded;
    }
}

