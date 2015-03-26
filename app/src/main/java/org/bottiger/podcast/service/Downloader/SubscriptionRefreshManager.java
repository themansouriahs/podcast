package org.bottiger.podcast.service.Downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.images.RequestManager;
import org.bottiger.podcast.parser.JSONFeedParserWrapper;
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

    private static Context mContext;

    public SubscriptionRefreshManager(@NonNull Context argContext) {
        mContext = argContext;
    }

    public static void start_update(final Context context) {
        start_update(context, null, null);
    }

    public static void start_update(final Context context,
                                    Subscription subscription, IDownloadCompleteCallback argCallback) {

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

    private static void addSubscriptionToQueue(@NonNull Subscription argSubscription, RequestQueue requestQueue, IDownloadCompleteCallback argCallback) {

        if (argSubscription == null) {
            VendorCrashReporter.report("addSubscriptionToQueue", "subscription=null");
            return;
        }

        StringRequest jr = new StringRequest(argSubscription.getUrl(),
                new MyStringResponseListener(mContext
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

    static class MyStringResponseListener implements Response.Listener<String> {

		static FeedHandler feedHandler = new FeedHandler();
		Subscription subscription;
		ContentResolver contentResolver;
		final JSONFeedParserWrapper feedParser = null;
        IDownloadCompleteCallback callback;

		public MyStringResponseListener(ContentResolver contentResolver,
                                        IDownloadCompleteCallback argCallback, @NonNull Subscription subscription) {
			this.subscription = subscription;
			this.contentResolver = contentResolver;
            this.callback = argCallback;
		}

		@Override
		public void onResponse(String response) {
			// volleyResultParser.execute(response);

            new ParseFeedTask().execute(response);
			//decrementProcessCount();
		}

        private class ParseFeedTask extends AsyncTask<String, Void, Void> {
            protected Void doInBackground(String... responses) {

                Subscription sub = null;
                String response = responses[0];

                try {
                    sub = feedHandler.parseFeed(contentResolver, subscription,
                            response.replace("ï»¿", "")); // Byte Order Mark
                } catch (SAXException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (UnsupportedFeedtypeException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                return null;
            }

            protected void onPostExecute(Void result) {
                EpisodeDownloadManager.decrementProcessCount();
                if (callback != null) {
                    callback.complete(true);
                }
            }
        }


    }
}

