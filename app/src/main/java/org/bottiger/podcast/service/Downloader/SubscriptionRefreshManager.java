package org.bottiger.podcast.service.Downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.DefaultRetryPolicy;
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

    private DefaultRetryPolicy mRetryPolicy = new DefaultRetryPolicy(
                                                            MY_SOCKET_TIMEOUT_MS,
                                                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

    private Context mContext;
    private RequestQueue mRequestQueue;

    public SubscriptionRefreshManager(@NonNull Context argContext) {
        mContext = argContext;
        RequestManager.initIfNeeded(mContext);
        mRequestQueue = RequestManager.getRequestQueue();
    }

    Response.ErrorListener getFailureListener() {
		return new Response.ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError error) { // Handle error
				if (error instanceof com.android.volley.ServerError) {

				} else {
					error.printStackTrace();
					int i = 5;
					i = i + i;
				}
			}
		};
	}

    public void refreshALl() {
        refresh(null, null);
    }

    public void refresh(Subscription subscription, IDownloadCompleteCallback argCallback) {

        if (EpisodeDownloadManager.updateConnectStatus(mContext) == EpisodeDownloadManager.NO_CONNECT)
            return;

        EpisodeDownloadManager.isDownloading = true;

        if (subscription != null) {
            addSubscriptionToQueue(subscription, mRequestQueue, argCallback);
        } else {
            populateQueue(mContext, argCallback);
        }

        mRequestQueue.start();
    }

    private void addSubscriptionToQueue(@NonNull Subscription argSubscription, RequestQueue requestQueue, IDownloadCompleteCallback argCallback) {

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
    }

    private class StringResponseListener implements Response.Listener<String> {

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
            new ParseFeedTask().execute(response);
		}

        private class ParseFeedTask extends AsyncTask<String, Void, Void> {
            protected Void doInBackground(String... responses) {

                String response = responses[0];

                try {
                    feedHandler.parseFeed(contentResolver, subscription,
                            response.replace("ï»¿", "")); // Byte Order Mark
                } catch (SAXException e) {
                    VendorCrashReporter.handleException(e);
                    e.printStackTrace();
                } catch (IOException e) {
                    VendorCrashReporter.handleException(e);
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    VendorCrashReporter.handleException(e);
                    e.printStackTrace();
                } catch (UnsupportedFeedtypeException e) {
                    VendorCrashReporter.handleException(e);
                    e.printStackTrace();
                }

                return null;
            }

            protected void onPostExecute(Void result) {
                if (callback != null) {
                    callback.complete(true);
                }
            }
        }


    }

    private int populateQueue(@NonNull Context argContext, @Nullable IDownloadCompleteCallback argCallback) {
        Cursor subscriptionCursor;
        int subscriptionsAdded = 0;

        subscriptionCursor = Subscription.allAsCursor(argContext
                .getContentResolver());

        while (subscriptionCursor.moveToNext()) {

            Subscription sub = Subscription.getByCursor(subscriptionCursor);

            addSubscriptionToQueue(sub, mRequestQueue, argCallback);
            subscriptionsAdded++;
        }

        return subscriptionsAdded;
    }
}

