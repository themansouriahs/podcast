package org.bottiger.podcast.service.Downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v7.util.SortedList;
import android.text.TextUtils;
import android.util.Log;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.datastructures.EpisodeList;
import org.bottiger.podcast.parser.FeedParser;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.StorageUtils;
import org.bottiger.podcast.utils.okhttp.UserAgentInterceptor;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by apl on 26-03-2015.
 */
public class SubscriptionRefreshManager {

    private static final String ACRA_KEY = "SubscriptionRefreshManager";
    public static final String TAG = "SubscriptionRefresh";

    @NonNull
    private final OkHttpClient mOkClient;

    @NonNull
    private final Handler mainHandler;

    @NonNull
    private final FeedParser mFeedParser = new FeedParser();

    @NonNull
    private Context mContext;

    public SubscriptionRefreshManager(@NonNull Context argContext) {
        mContext = argContext;
        mainHandler = new Handler(argContext.getMainLooper());

        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.interceptors().add(new UserAgentInterceptor(argContext));
        mOkClient = okHttpBuilder.build();
    }

    public void refreshAll() {
        Log.d(TAG, "refreshAll()");
        refresh(null, null);
    }

    @WorkerThread
    public Subscription refreshSync(@NonNull Subscription argSubscription) throws IOException {
        final Request request = getRequest(argSubscription);
        Response response = mOkClient.newCall(request).execute();
        handleHttpResponse(mContext, argSubscription, response, null);
        return argSubscription;
    }

    public void refresh(@Nullable ISubscription argSubscription, @Nullable IDownloadCompleteCallback argCallback) {
        Log.d(TAG, "refresh subscription: " + argSubscription + " (null => all)");

        if (!StorageUtils.canPerform(SoundWavesDownloadManager.ACTION_REFRESH_SUBSCRIPTION, mContext, argSubscription)) {
            Log.d(TAG, "refresh aborted, not allowed"); // NoI18N
            return;
        }


        if (argSubscription != null) {
            addSubscriptionToQueue(mContext, argSubscription, argCallback);
        } else {
            addAllSubscriptionsToQueue(mContext, argCallback);
        }
    }

    private void addSubscriptionToQueue(@NonNull final Context argContext,
                                        @NonNull final ISubscription argSubscription,
                                        @Nullable final IDownloadCompleteCallback argCallback) {
        Log.d(TAG, "Adding to queue: " + argSubscription);

        if (argSubscription == null) {
            VendorCrashReporter.report(ACRA_KEY, "subscription=null");
            return;
        }

        if (argSubscription.getURL() == null) {
            VendorCrashReporter.report(ACRA_KEY, "subscription.url=null");
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

        final Request request = getRequest(argSubscription);

        mOkClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                wrappedCallback.complete(false, argSubscription);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleHttpResponse(argContext, argSubscription, response, argCallback);
            }
        });
    }

    private int addAllSubscriptionsToQueue(@NonNull Context argContext, @Nullable IDownloadCompleteCallback argCallback) {
        Log.d(TAG, "addAllSubscriptionsToQueue");

        int subscriptionsAdded = 0;

        SortedList<Subscription> subscriptions = SoundWaves.getAppContext(argContext).getLibraryInstance().getSubscriptions();
        for (int i = 0; i < subscriptions.size(); i++) {
           addSubscriptionToQueue(argContext, subscriptions.get(i), argCallback);
           subscriptionsAdded++;
        }

        Log.d(TAG, "addAllSubscriptionsToQueue added: " + subscriptionsAdded);
        return subscriptionsAdded;
    }

    /**
     * Download new episodes from a subscription.
     *
     * @param argContext A Context
     * @param argSubscription The Subscription.
     */
    private void downloadNewEpisodeskCallback(final @NonNull Context argContext, @NonNull Subscription argSubscription) {
        if (StorageUtils.canPerform(SoundWavesDownloadManager.ACTION_DOWNLOAD_AUTOMATICALLY,
                argContext,
                argSubscription)) {

            Date tenMinutesAgo = new Date(System.currentTimeMillis() - (10 * 60 * 1000));

            LinkedList<IEpisode> episodes = argSubscription.getEpisodes().getUnfilteredList();
            int newEpisodeCount = Math.min(argSubscription.countNewEpisodes(), episodes.size());

            for (int i = 0; i < episodes.size(); i++) {
                FeedItem episode = (FeedItem)episodes.get(i);
                Date lastUpdate = new Date(episode.getLastUpdate());
                if (lastUpdate.after(tenMinutesAgo) && newEpisodeCount > 0) {
                    SoundWaves.getAppContext(argContext).getDownloadManager().addItemToQueue(episode, false, SoundWavesDownloadManager.LAST);
                    newEpisodeCount--;
                }
            }
        }
    }

    private static Request getRequest(@NonNull ISubscription argSubscription) {
        return new Request.Builder()
                .url(argSubscription.getURLString())
                .build();
    }

    private void handleHttpResponse(@NonNull Context argContext,
                                    @NonNull ISubscription argSubscription,
                                    @NonNull Response response,
                                    @Nullable final IDownloadCompleteCallback argCallback) {
        try {
            ensureLoadedSubscription(argContext, argSubscription);

            if (requiresAuthentication(response, argSubscription)) {
                return;
            }

            if (response.body() != null && response.isSuccessful()) {
                boolean success = true;
                try {
                    mFeedParser.parse(argSubscription, response.body().byteStream(), argContext);

                    downloadNewEpisodes(argContext, argSubscription);
                } catch (Exception exception) {
                    handleParingError(exception, argSubscription);
                    success = false;
                }

                Log.d(TAG, "Parsing callback for: " + argSubscription);
                if (argCallback != null)
                    argCallback.complete(success, argSubscription);

            }
        } catch (NullPointerException npe) {
            handleParingError(npe, argSubscription);
        }
    }

    private void downloadNewEpisodes(@NonNull Context argContext, @NonNull ISubscription argSubscription) {
        if (argSubscription instanceof Subscription) {
            downloadNewEpisodeskCallback(argContext, (Subscription)argSubscription);
        }
    }

    /**
     * Ensure the subscription and all of it's episodes are loaded before refreshing it.
     *
     * @param argContext A context
     * @param argSubscription The subscription
     */
    private static void ensureLoadedSubscription(@NonNull Context argContext, @NonNull ISubscription argSubscription) {
        if (argSubscription instanceof Subscription) {
            SoundWaves.getAppContext(argContext).getLibraryInstance().loadEpisodesSync((Subscription)argSubscription, null);
        }
    }

    /**
     *
     * @param response The HTTP Response
     * @param argSubscription The subscription being refreshed
     * @return true if the request was denied because of lacking authentication
     */
    private static boolean requiresAuthentication(@NonNull Response response, @NonNull ISubscription argSubscription) {
        if (response.code() == 401) { // 401 (Access Denied)
            if (argSubscription instanceof Subscription) {
                Subscription subscription = (Subscription) argSubscription;
                subscription.setRequiringAuthentication(true);
                subscription.setAuthenticationWorking(false);
            }
            return true;
        }

        return false;
    }

    /**
     * Logs and reports Feed parsing errors
     *
     * @param argExceiption The Exception from the parser
     * @param argSubscription The Subscription being parsed
     */
    private static void handleParingError(@NonNull Exception argExceiption, @NonNull ISubscription argSubscription) {
        Log.d(TAG, "Parsing error " + argExceiption.toString());

        String[] keys = new String[1];
        String[] values = new String[1];

        keys[0] = "url";
        values[0] = TextUtils.isEmpty(argSubscription.getURLString()) ? "No url" : argSubscription.getURLString(); // NoI18N
        VendorCrashReporter.handleException(argExceiption, keys, values);
    }
}

