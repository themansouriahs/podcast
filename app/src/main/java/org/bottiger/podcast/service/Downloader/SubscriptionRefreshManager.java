package org.bottiger.podcast.service.Downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
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
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by apl on 26-03-2015.
 */
public class SubscriptionRefreshManager {

    private static final String ACRA_KEY = "SubscriptionRefreshManager";
    public static final String DEBUG_KEY = "SubscriptionRefresh";


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

    public void refreshAll() {
        Log.d(DEBUG_KEY, "refreshAll()");
        refresh(null, null);
    }

    public void refresh(@Nullable ISubscription subscription, @Nullable IDownloadCompleteCallback argCallback) {
        Log.d(DEBUG_KEY, "refresh subscription: " + subscription + " (null => all)");


        if (!EpisodeDownloadManager.canPerform(EpisodeDownloadManager.ACTION.REFRESH_SUBSCRIPTION, mContext)) {
            Log.d(DEBUG_KEY, "refresh aborted, not allowed"); // NoI18N
            return;
        }

        EpisodeDownloadManager.isDownloading = true;

        if (subscription != null) {
            addSubscriptionToQueue(mContext, subscription, argCallback);
        } else {
            populateQueue(mContext, argCallback);
        }
    }

    private void addSubscriptionToQueue(@NonNull final Context argContext, @NonNull final ISubscription argSubscription, @Nullable final IDownloadCompleteCallback argCallback) {
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

        final IDownloadCompleteCallback wrappedCallback = new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean argSucces, ISubscription argSubscription) {
                if (argSucces && EpisodeDownloadManager.canPerform(EpisodeDownloadManager.ACTION.DOWNLOAD_AUTOMATICALLY, argContext)) {
                    boolean startDownload = false;
                    Date tenMinutesAgo = new Date(System.currentTimeMillis() - (10 * 60 * 1000));
                    for (IEpisode episode : argSubscription.getEpisodes()) {
                        if (episode instanceof FeedItem) {
                            Date lastUpdate = new Date(((FeedItem)episode).getLastUpdate());
                            if (lastUpdate.after(tenMinutesAgo)) {
                                EpisodeDownloadManager.addItemToQueue(episode, EpisodeDownloadManager.QUEUE_POSITION.FIRST);
                                startDownload = true;
                            }
                        }
                    }

                    if (startDownload)
                        EpisodeDownloadManager.startDownload(argContext);
                }

                if (argCallback != null)
                    argCallback.complete(argSucces, argSubscription);
            }
        };

        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(subscriptionUrl)
                .build();

        mDecodeThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                com.squareup.okhttp.Response response = null;
                ISubscription parsedSubscription = null;
                try {
                    response = client.newCall(request).execute();

                    if (response != null && response.isSuccessful()) {
                        String feedString = response.body().string();
                        parsedSubscription = processResponse(argContext, argSubscription, feedString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final ISubscription finalSubscription = parsedSubscription != null ? parsedSubscription : null;

                if (wrappedCallback != null) {

                    Handler mainHandler = new Handler(argContext.getMainLooper());
                    Runnable myRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Log.d(DEBUG_KEY, "Parsing callback for: " + argSubscription);
                            wrappedCallback.complete(finalSubscription != null, finalSubscription);
                        }
                    };
                    mainHandler.post(myRunnable);
                }
            }
        });
    }

    private int populateQueue(@NonNull Context argContext, @Nullable IDownloadCompleteCallback argCallback) {
        Log.d(DEBUG_KEY, "populateQueue");

        Cursor subscriptionCursor = null;
        int subscriptionsAdded = 0;

        try {
            subscriptionCursor = SubscriptionLoader.allAsCursor(argContext
                    .getContentResolver());

            while (subscriptionCursor.moveToNext()) {

                Subscription sub = SubscriptionLoader.getByCursor(subscriptionCursor);

                addSubscriptionToQueue(argContext, sub, argCallback);
                subscriptionsAdded++;
            }
        } finally {
            subscriptionCursor.close();
        }

        Log.d(DEBUG_KEY, "populateQueue added: " + subscriptionsAdded);
        return subscriptionsAdded;
    }

    private static ISubscription processResponse(Context argContext, @NonNull ISubscription subscription, String argResponse) {
        Log.d(DEBUG_KEY, "Volley response from: " + subscription);
        //new ParseFeedTask().onSucces(response);

        ISubscription parsedSubscription = null;

        try {
            Log.d(DEBUG_KEY, "Parsing: " + subscription);
            parsedSubscription = feedHandler.parseFeed(argContext.getContentResolver(), subscription,
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

        return parsedSubscription;
    }
}

