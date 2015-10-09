package org.bottiger.podcast.service.Downloader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.webkit.URLUtil;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.parser.FeedParser;
import org.bottiger.podcast.parser.FeedUpdater;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.service.PlayerService;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by apl on 26-03-2015.
 */
public class SubscriptionRefreshManager {

    private static final String ACRA_KEY = "SubscriptionRefreshManager";
    public static final String TAG = "SubscriptionRefresh";

    final OkHttpClient mOkClient = new OkHttpClient();
    final Handler mainHandler;
    final FeedParser mFeedParser = new FeedParser();

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

        if (!SoundWavesDownloadManager.canPerform(SoundWavesDownloadManager.ACTION_REFRESH_SUBSCRIPTION, mContext, argSubscription)) {
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

                        /**
                         * FIXME: https://github.com/square/okhttp/issues/1362
                         *
                         * This can (and will) fail with an out of memory exception when the response is too large.
                         */
                        //String str = response.body().string();

                        //InputStream feedString = new BOMInputStream(response.body().byteStream());
                        //Reader feedString = response.body().charStream();

                        //parsedSubscription = processResponse(argContext, argSubscription, feedString);
                        try {
                            parsedSubscription = mFeedParser.parse(argSubscription, response.body().byteStream());
                            //parsedSubscription.setURL(response.request().httpUrl().toString());

                            postProcess(argContext.getContentResolver(), argSubscription);
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

                        final ISubscription finalSubscription = parsedSubscription != null ? parsedSubscription : null;

                        Log.d(TAG, "Parsing callback for: " + argSubscription);
                        wrappedCallback.complete(finalSubscription != null, finalSubscription);

                    }
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                } finally {
                    if (argSubscription != null)
                        argSubscription.setIsRefreshing(false);
                }
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

    private void downloadNewEpisodeskCallback(final @NonNull Context argContext, @NonNull ISubscription argSubscription) {
        if (SoundWavesDownloadManager.canPerform(SoundWavesDownloadManager.ACTION_DOWNLOAD_AUTOMATICALLY,
                argContext,
                argSubscription)) {
            boolean startDownload = false;
            Date tenMinutesAgo = new Date(System.currentTimeMillis() - (10 * 60 * 1000));

            final PlayerService ps = SoundWaves.sBoundPlayerService;

            if (argSubscription instanceof Subscription) {
                ArrayList<? extends IEpisode> episodes = argSubscription.getEpisodes();
                for (int i = 0; i < episodes.size(); i++) {
                    IEpisode episode = episodes.get(i);
                    if (episode instanceof FeedItem) {
                        Date lastUpdate = new Date(((FeedItem) episode).getLastUpdate());
                        if (lastUpdate.after(tenMinutesAgo)) {

                            if (ps != null) {
                                ps.getDownloadManager().addItemToQueue(episode, SoundWavesDownloadManager.LAST);
                                startDownload = true;
                            }
                        }
                    }
                }
            }

            if (startDownload && ps != null) {
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        ps.getDownloadManager().startDownload();
                    }
                };
                mainHandler.post(myRunnable);
            }
        }
    }

    private void postProcess(@NonNull ContentResolver argContentResolver, @NonNull ISubscription argSubscription) {
        Log.d(SubscriptionRefreshManager.TAG, "Done Parsing: " + argSubscription);

        if (argSubscription instanceof Subscription) {
            Subscription subscription = (Subscription)argSubscription;
            FeedUpdater updater = new FeedUpdater(argContentResolver);
            updater.updateDatabase(subscription);
            ((Subscription)argSubscription).getEpisodes(argContentResolver);
            Log.d(SubscriptionRefreshManager.TAG, "Done updating database for: " + argSubscription);
            return;
        }

        if (argSubscription instanceof SlimSubscription) {
            SlimSubscription slimSubscription = (SlimSubscription) argSubscription;

            URL artwork = null;
            String artworkString = slimSubscription.getImageURL();
            if (Patterns.WEB_URL.matcher(artworkString).matches()) {
                try {
                    artwork = new URL(artworkString);
                } catch (MalformedURLException mue) {
                    artwork = null;
                }
            }

            /*
            ArrayList<SlimEpisode> slimEpisodes = new ArrayList<>();
            for (IEpisode episode : argSubscription.getEpisodes()) {
                SlimEpisode slimEpisode = EpisodeConverter.toSlim(episode);

                if (slimEpisode == null) {
                    continue;
                }

                if (artwork != null) {
                    slimEpisode.setArtwork(artwork);
                }

                if (slimEpisode != null) {
                    slimEpisodes.add(slimEpisode);
                }
            }
            slimSubscription.setEpisodes(slimEpisodes);4*/
            Log.d(SubscriptionRefreshManager.TAG, "Replacing the subscription with a populated SlimSubscription:");
        }
    }
}

