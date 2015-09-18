package org.bottiger.podcast.service.Downloader;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.parser.syndication.handler.FeedHandler;
import org.bottiger.podcast.parser.syndication.handler.UnsupportedFeedtypeException;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by apl on 26-03-2015.
 */
public class SubscriptionRefreshManager {

    private static final String ACRA_KEY = "SubscriptionRefreshManager";
    public static final String TAG = "SubscriptionRefresh";

    private static FeedHandler mFeedHandler = new FeedHandler();
    final OkHttpClient mOkClient = new OkHttpClient();
    final Handler mainHandler;

    private Context mContext;

    public SubscriptionRefreshManager(@NonNull Context argContext) {
        mContext = argContext;
        mainHandler = new Handler(argContext.getMainLooper());
    }

    public void refreshAll() {
        Log.d(TAG, "refreshAll()");
        refresh(null, null);
    }

    public void refresh(@Nullable ISubscription argSubscription, @Nullable IDownloadCompleteCallback argCallback) {
        Log.d(TAG, "refresh subscription: " + argSubscription + " (null => all)");

        if (!EpisodeDownloadManager.canPerform(EpisodeDownloadManager.ACTION_REFRESH_SUBSCRIPTION, mContext, argSubscription)) {
            Log.d(TAG, "refresh aborted, not allowed"); // NoI18N
            return;
        }


        if (argSubscription != null) {
            addSubscriptionToQueue(mContext, argSubscription, argCallback);
        } else {
            addAllSubscriptionsToQueue(mContext, argCallback);
        }
    }

    private void addSubscriptionToQueue(@NonNull final Context argContext, @NonNull final ISubscription argSubscription, @Nullable final IDownloadCompleteCallback argCallback) {
        Log.d(TAG, "Adding to queue: " + argSubscription);

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
            public void complete(final boolean argSucces, final ISubscription argSubscription) {
                downloadNewEpisodeskCallback(argContext, argSubscription);

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (argCallback != null)
                            argCallback.complete(argSucces, argSubscription);
                    }
                };
                mainHandler.post(myRunnable);
            }
        };

        argSubscription.setIsRefreshing(true);

        final Request request = new Request.Builder()
                .url(subscriptionUrl)
                .build();

        mOkClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                wrappedCallback.complete(false, argSubscription);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                ISubscription parsedSubscription = null;
                try {
                    if (response != null && response.body() != null && response.isSuccessful()) {
                        String feedString = response.body().string();
                        parsedSubscription = processResponse(argContext, argSubscription, feedString);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final ISubscription finalSubscription = parsedSubscription != null ? parsedSubscription : null;

                Log.d(TAG, "Parsing callback for: " + argSubscription);
                wrappedCallback.complete(finalSubscription != null, finalSubscription);

                argSubscription.setIsRefreshing(false);
            }
        });
    }

    private int addAllSubscriptionsToQueue(@NonNull Context argContext, @Nullable IDownloadCompleteCallback argCallback) {
        Log.d(TAG, "addAllSubscriptionsToQueue");

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

        Log.d(TAG, "addAllSubscriptionsToQueue added: " + subscriptionsAdded);
        return subscriptionsAdded;
    }

    private static ISubscription processResponse(Context argContext, @NonNull ISubscription subscription, String argResponse) {
        Log.v(TAG, "Http response from: " + subscription);

        ISubscription parsedSubscription = null;

        try {
            Log.d(TAG, "Parsing: " + subscription);
            parsedSubscription = mFeedHandler.parseFeed(argContext.getContentResolver(), subscription,
                    argResponse.replace("ï»¿", "")); // Byte Order Mark
        } catch (SAXException e) {
            Log.d(TAG, "Parsing EXCEPTION: " + subscription);
            VendorCrashReporter.handleException(e);
            VendorCrashReporter.report("subscription1", subscription.getURL().toString());
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Parsing EXCEPTION: " + subscription);
            VendorCrashReporter.handleException(e);
            VendorCrashReporter.report("subscription2", subscription.getURL().toString());
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            Log.d(TAG, "Parsing EXCEPTION: " + subscription);
            VendorCrashReporter.handleException(e);
            VendorCrashReporter.report("subscription3", subscription.getURL().toString());
            e.printStackTrace();
        } catch (UnsupportedFeedtypeException e) {
            Log.d(TAG, "Parsing EXCEPTION: " + subscription);
            VendorCrashReporter.handleException(e);
            VendorCrashReporter.report("subscription4", subscription.getURL().toString());
            e.printStackTrace();
        }

        return parsedSubscription;
    }

    private void downloadNewEpisodeskCallback(@NonNull Context argContext, @NonNull ISubscription argSubscription) {
        if (EpisodeDownloadManager.canPerform(EpisodeDownloadManager.ACTION_DOWNLOAD_AUTOMATICALLY,
                argContext,
                argSubscription)) {
            boolean startDownload = false;
            Date tenMinutesAgo = new Date(System.currentTimeMillis() - (10 * 60 * 1000));

            ArrayList<? extends IEpisode> episodes = argSubscription.getEpisodes();
            for (int i = 0; i < episodes.size(); i++) {
                IEpisode episode = episodes.get(i);
                if (episode instanceof FeedItem) {
                    Date lastUpdate = new Date(((FeedItem)episode).getLastUpdate());
                    if (lastUpdate.after(tenMinutesAgo)) {
                        EpisodeDownloadManager.addItemToQueue(episode, EpisodeDownloadManager.LAST);
                        startDownload = true;
                    }
                }
            }

            if (startDownload) {
                EpisodeDownloadManager.startDownload(argContext);
            }
        }
    }
}

