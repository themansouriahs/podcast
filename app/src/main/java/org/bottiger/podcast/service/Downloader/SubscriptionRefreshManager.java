package org.bottiger.podcast.service.Downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.text.TextUtils;
import android.util.Log;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
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
    final OkHttpClient mOkClient;
    final Handler mainHandler;
    final FeedParser mFeedParser = new FeedParser();

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

    public Subscription refreshSync(@NonNull Context argContext, @NonNull Subscription argSubscription) throws IOException {
        final Request request = getRequest(argSubscription);
        Response response = mOkClient.newCall(request).execute();
        handleHttpResponse(argContext, argSubscription, response, null);
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

    private void downloadNewEpisodeskCallback(final @NonNull Context argContext, @NonNull ISubscription argSubscription) {
        if (StorageUtils.canPerform(SoundWavesDownloadManager.ACTION_DOWNLOAD_AUTOMATICALLY,
                argContext,
                argSubscription)) {

            if (!(argSubscription instanceof Subscription)) {
                return;
            }

            Subscription subscription = (Subscription)argSubscription;

            Date tenMinutesAgo = new Date(System.currentTimeMillis() - (10 * 60 * 1000));

            final PlayerService ps = PlayerService.getInstance();

            if (argSubscription instanceof Subscription) {
                SortedList<? extends IEpisode> episodes = argSubscription.getEpisodes();
                int newEpisodeCount = Math.min(subscription.countNewEpisodes(), episodes.size());

                for (int i = 0; i < episodes.size(); i++) {
                    IEpisode episode = episodes.get(i);
                    if (episode instanceof FeedItem) {
                        Date lastUpdate = new Date(((FeedItem) episode).getLastUpdate());
                        if (lastUpdate.after(tenMinutesAgo) && newEpisodeCount > 0) {
                            SoundWaves.getDownloadManager().addItemToQueue(episode, false, SoundWavesDownloadManager.LAST);
                            newEpisodeCount--;
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    private void postProcess(@NonNull Context argContext, @NonNull ISubscription argSubscription) {
        Log.d(SubscriptionRefreshManager.TAG, "Done Parsing: " + argSubscription);

        if (argSubscription instanceof Subscription) {
            Subscription subscription = (Subscription)argSubscription;
            SoundWaves.getAppContext(argContext).getLibraryInstance().updateSubscription(subscription);
            Log.d(SubscriptionRefreshManager.TAG, "Done updating database for: " + argSubscription);
            return;
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
        ISubscription parsedSubscription = null;
        try {
            if (argSubscription instanceof Subscription) {
                SoundWaves.getAppContext(argContext).getLibraryInstance().loadEpisodesSync((Subscription)argSubscription, null);
            }

            if (response.code() == 401) { // 401 (Access Denied)
                if (argSubscription instanceof Subscription) {
                    Subscription subscription = (Subscription) argSubscription;
                    subscription.setRequiringAuthentication(true);
                    subscription.setAuthenticationWorking(false);
                }
                return;
            }

            if (response.body() != null && response.isSuccessful()) {
                try {
                    parsedSubscription = mFeedParser.parse(argSubscription, response.body().byteStream(), argContext);

                    postProcess(argContext, argSubscription);
                    if (argSubscription instanceof Subscription) {
                        downloadNewEpisodeskCallback(argContext, argSubscription);
                    }
                } catch (XmlPullParserException e) {
                    Log.d(TAG, "Parsing error " + e.toString());

                    String[] keys = new String[1];
                    String[] values = new String[1];

                    keys[0] = "url";
                    values[0] = TextUtils.isEmpty(argSubscription.getURLString()) ? "No url" : argSubscription.getURLString(); // NoI18N
                    VendorCrashReporter.handleException(e, keys, values);
                } catch (Exception e) {
                    Log.d(TAG, "Parsing error " + e.toString());

                    String[] keys = new String[1];
                    String[] values = new String[1];

                    keys[0] = "url";
                    values[0] = TextUtils.isEmpty(argSubscription.getURLString()) ? "No url" : argSubscription.getURLString(); // NoI18N
                    VendorCrashReporter.handleException(e, keys, values);
                }

                final ISubscription finalSubscription = parsedSubscription != null ? argSubscription : null;

                Log.d(TAG, "Parsing callback for: " + argSubscription);
                if (argCallback != null && finalSubscription != null)
                    argCallback.complete(finalSubscription != null, finalSubscription);

            }
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        } finally {
            if (argSubscription != null)
                argSubscription.setIsRefreshing(false);
        }
    }
}

