package org.bottiger.podcast.webservices.datastore.gpodder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v7.util.SortedList;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.okhttp.UserAgentInterceptor;
import org.bottiger.podcast.webservices.datastore.CallbackWrapper;
import org.bottiger.podcast.webservices.datastore.IWebservice;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GActionsList;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GDevice;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GEpisodeAction;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GSubscription;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.SubscriptionChanges;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.UpdatedUrls;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Nonnegative;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by Arvid on 8/23/2015.
 */
public class GPodderAPI implements IWebservice {

    private static final String TAG = "GPodderAPI";

    // This seems to mess up my model since I use the url as a primary key
    private static final boolean RESPECT_GPODDER_URL_SANITIZER = false;

    private IGPodderAPI api;
    private String mUsername;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SYNCHRONIZATION_OK, AUTHENTICATION_ERROR, SERVER_ERROR})
    public @interface SynchronizationResult {
    }

    public static final int SYNCHRONIZATION_OK = 1;
    public static final int AUTHENTICATION_ERROR = 2;
    public static final int SERVER_ERROR = 3;

    private boolean mAuthenticated = false;

    private final Callback mDummyCallback = new Callback<String>() {
        @Override
        public void onResponse(Call<String> call, Response<String> response) {
            Log.d(TAG, response.toString());
        }

        @Override
        public void onFailure(Call<String> call, Throwable error) {
            Log.d(TAG, error.toString());
        }
    };

    public GPodderAPI(@NonNull String argServer) {
        this(argServer, null, null, null);
    }

    public GPodderAPI(@NonNull String argServer, @Nullable String argUsername, @Nullable String argPassword) {
        this(argServer, argUsername, argPassword, null);
    }

    public GPodderAPI(@NonNull String argServer, @Nullable String argUsername, @Nullable String argPassword, @Nullable Callback argCallback) {

        mUsername = argUsername;

        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.interceptors().add(new UserAgentInterceptor(SoundWaves.getAppContext()));
        client.interceptors().add(new ApiRequestInterceptor(argUsername, argPassword));

        api = new Retrofit.Builder()
                .baseUrl(argServer)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client.build()) // The default client didn't handle well responses like 401
                .build()
                .create(IGPodderAPI.class);

        if (mUsername != null) {
            authenticate(null, argCallback);
        }
    }

    /**
     * TODO: api.updateDeviceData always fail
     * TODO: sometime failes: if (!updatedUrlsResponse.isSuccess()); return SERVER_ERROR;
     *
     * @param argContext
     * @param argLocalSubscriptions
     * @return
     * @throws IOException
     */
    public
    @SynchronizationResult
    int synchronize(@NonNull Context argContext,
                    @NonNull LongSparseArray<Subscription> argLocalSubscriptions) throws IOException {

        Response response = api.login(mUsername).execute();
        if (!response.isSuccessful())
            return AUTHENTICATION_ERROR;

        GDevice device = new GDevice();
        device.caption = GPodderUtils.getDeviceCaption(argContext);
        device.type = GPodderUtils.getDeviceCaption(argContext);
        Response responseDevice = api.updateDeviceData(mUsername, GPodderUtils.getDeviceCaption(argContext), device).execute();
        //if (!responseDevice.isSuccess())
        //    return SERVER_ERROR;

        String key = argContext.getResources().getString(R.string.gpodder_last_sync_key);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
        long lastSync = prefs.getLong(key, 0);

        long timestamp;
        @SynchronizationResult int returnValue = SYNCHRONIZATION_OK;


        /**
         * Fetch changes from the server
         */
        Set<String> subscribedUrls = getSubscribedUrls(argLocalSubscriptions);

        Call<SubscriptionChanges> subscriptionsChanges = api.getDeviceSubscriptionsChanges(mUsername, GPodderUtils.getDeviceCaption(argContext), lastSync); // lastCall
        Response<SubscriptionChanges> subscriptionsChangesResponse = subscriptionsChanges.execute();

        if (!subscriptionsChangesResponse.isSuccessful())
            return SERVER_ERROR;

        SubscriptionChanges subscriptionsChangesResult = subscriptionsChangesResponse.body();
        List<String> added = subscriptionsChangesResult.add;
        List<String> removed = subscriptionsChangesResult.remove;

        added.removeAll(subscribedUrls);

        String newUrl;
        Subscription changedSubscription;
        for (int i = 0; i < added.size(); i++) {
            newUrl = added.get(i);
            /*
            changedSubscription = new Subscription(newUrl);
            changedSubscription.subscribe(argContext);
            */
            SoundWaves.getAppContext(argContext).getLibraryInstance().subscribe(newUrl);
        }

        /**
         * Upload changes from the client to gpodder
         */
        String[] gPodderDeviceSubscriptions;
        Set<String> gPodderSubscriptionsSet = getSet();
        try {
            gPodderDeviceSubscriptions = getDeviceSubscriptionsAsStrings(argContext);
        } catch (IOException ioe) {
            return SERVER_ERROR;
        }

        for (int i = 0; i < gPodderDeviceSubscriptions.length; i++) {
            gPodderSubscriptionsSet.add(gPodderDeviceSubscriptions[i]);
        }
        gPodderDeviceSubscriptions = null;

        // FIXME: Fuck, at the moment we do not have a "subscribed_at" timesstamp on Subscriptions.
        // We just upload all of them
        SubscriptionChanges localSubscriptionsChanges = new SubscriptionChanges();
        String url;
        for (int i = 0; i < argLocalSubscriptions.size(); i++) {
            long arrayKey = argLocalSubscriptions.keyAt(i);
            Subscription subscription = argLocalSubscriptions.get(arrayKey);

            url = subscription.getURLString();

            if (removed.contains(url)) {
                if (subscription.IsSubscribed()) {
                    SoundWaves.getAppContext(argContext).getLibraryInstance().unsubscribe(subscription.getURLString(), "GPodder:Unsubscribe");
                } else {
                    Log.w(TAG, "gPodder removed a subscription we are not subscribed to: " + url); // NoI18N
                    VendorCrashReporter.report("Removed unknown subscription", url); // NoI18N
                }
            }


            if (subscription.IsSubscribed() && !gPodderSubscriptionsSet.contains(url)) {
                localSubscriptionsChanges.add.add(url);
            }

            if (subscription.IsSubscribed()) {
                //localSubscriptionsChanges.remove.add(url);
                gPodderSubscriptionsSet.remove(url);
            }
        }

        // We are not subscribed to the URL's there are left
        localSubscriptionsChanges.remove.addAll(gPodderSubscriptionsSet);

        Call<UpdatedUrls> updatedUrlsCall = api.uploadDeviceSubscriptionsChanges(localSubscriptionsChanges, mUsername, GPodderUtils.getDeviceCaption(argContext));
        Response<UpdatedUrls> updatedUrlsResponse = updatedUrlsCall.execute();

        if (!updatedUrlsResponse.isSuccessful())
            return SERVER_ERROR;

        UpdatedUrls updatedUrls = updatedUrlsResponse.body();

        if (RESPECT_GPODDER_URL_SANITIZER) {
            updateLocalUrls(argContext, argLocalSubscriptions, updatedUrls.getUpdatedUrls());
        }

        timestamp = updatedUrls.getTimestamp();

        if (timestamp > 0) {
            prefs.edit().putLong(key, timestamp).commit();
        }


        /**
         * Sync Episode actions
         */
        //String where = String.format("%s>%d", ItemColumns.LAST_UPDATE, lastSync * 1000);
        //FeedItem[] items = FeedCursorLoader.asCursor(argContext.getContentResolver(), where);
        ArrayList<IEpisode> allItems = SoundWaves.getAppContext(argContext).getLibraryInstance().getEpisodes();
        List<FeedItem> recentlySyncedItems = new LinkedList<>();
        IEpisode episode;
        FeedItem eitem;
        for (int i = 0; i < allItems.size(); i++) {
            episode = allItems.get(i);
            if (episode instanceof FeedItem) {
                eitem = (FeedItem) episode;
                if (eitem.lastModificationDate() > lastSync * 1000) {
                    recentlySyncedItems.add(eitem);
                }
            }
        }

        FeedItem[] items = recentlySyncedItems.toArray(new FeedItem[recentlySyncedItems.size()]);

        Call<GActionsList> actionList = api.getEpisodeActions(mUsername, null, null, lastSync, true);
        Response<GActionsList> actionListResponse = actionList.execute();

        if (!actionListResponse.isSuccessful())
            return SERVER_ERROR;

        GEpisodeAction[] remoteActions = actionListResponse.body().getActions();

        if (items != null) {
            List<GEpisodeAction> actions = new LinkedList<>();
            GEpisodeAction action;
            for (int i = 0; i < items.length; i++) {
                FeedItem item = items[i];
                action = new GEpisodeAction();

                action.podcast = getSubscriptionUrlForEpisode(argLocalSubscriptions, item);
                action.episode = item.getURL();
                action.device = GPodderUtils.getDeviceCaption(argContext);

                if (TextUtils.isEmpty(action.podcast)) {
                    continue;
                }

                try {
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                    df.setTimeZone(TimeZone.getDefault());
                    String nowAsISO = df.format(item.getDateTime());
                    action.timestamp = nowAsISO;
                } catch (Exception e) {
                    continue;
                }

                if (item.getOffset() > 0) {
                    action.action = GEpisodeAction.PLAY;
                } else {
                    continue;
                }
                /*
                else if (item.isDownloaded()) {
                    action.action = GEpisodeAction.DOWNLOAD;
                } else {
                    action.action = GEpisodeAction.NEW;
                }
                */

                if (action.action == GEpisodeAction.PLAY) {
                    // we currently can not support action.started
                    action.started = 0;

                    action.position = (item.getOffset()/1000);

                    long duration = item.getDuration();
                    if (duration > 0) {
                        action.total = duration;

                        if (item.isListened()) {
                            action.position = duration;
                        }
                    }
                }

                actions.add(action);

                if (remoteActions != null) {
                    for (int j = 0; j < remoteActions.length; j++) {
                        if (remoteActions[j].episode == action.episode) {
                            remoteActions[j] = null;
                            break;
                        }
                    }
                }
            }

            Call<UpdatedUrls> uploadEpisodesCall = api.uploadEpisodeActions(actions.toArray(new GEpisodeAction[actions.size()]), mUsername);
            Response<UpdatedUrls> uploadEpisodesResponse = uploadEpisodesCall.execute();

            if (!uploadEpisodesResponse.isSuccessful()) {
                return SERVER_ERROR;
            }
        }

        if (remoteActions != null) {
            GEpisodeAction action;
            IEpisode item;
            ContentResolver resolver = argContext.getContentResolver();
            for (int i = 0; i < remoteActions.length; i++) {
                action = remoteActions[i];
                if (action != null && action.action == GEpisodeAction.PLAY) {
                    //item = FeedItem.getByURL(resolver, action.episode);
                    item = SoundWaves.getAppContext(argContext).getLibraryInstance().getEpisode(action.episode);
                    if (item != null) {
                        item.setOffset(resolver, action.position*1000);
                    }
                }
            }
        }

        return returnValue;
    }

    public
    @SynchronizationResult
    int initialSynchronization(@NonNull Context argContext, @NonNull LongSparseArray<Subscription> argLocalSubscriptions) throws IOException {

        String[] deviceSubscriptions;
        try {
            deviceSubscriptions = getDeviceSubscriptionsAsStrings(argContext);
        } catch (IOException ioe) {
            return SERVER_ERROR;
        }

        int s = deviceSubscriptions.length;
        GSubscription[] hack = new GSubscription[deviceSubscriptions.length];
        for (int i = 0; i < s; i++) {
            hack[i] = new GSubscription();
            hack[i].setUrl(deviceSubscriptions[i]);
        }

        GSubscription[] fetchedSubscriptions = hack;
        List<GSubscription> newSubscriptions = new LinkedList<>();

        Map<String, Subscription> localSubscriptionMap;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            localSubscriptionMap = new ArrayMap<>(argLocalSubscriptions.size());
        } else {
            localSubscriptionMap = new HashMap<>(argLocalSubscriptions.size());
        }

        Subscription value;
        for (int i = 0; i < argLocalSubscriptions.size(); i++) {
            long arrayKey = argLocalSubscriptions.keyAt(i);
            ISubscription subscription = argLocalSubscriptions.get(arrayKey);

            value = argLocalSubscriptions.get(arrayKey);
            String key = value.getURLString();
            localSubscriptionMap.put(key, value);
        }

        String url;
        GSubscription gSubscription;
        for (int i = 0; i < fetchedSubscriptions.length; i++) {
            gSubscription = fetchedSubscriptions[i];
            url = gSubscription.getUrl();

            // If the remote collection contains subscriptions we don't know about
            // we store them
            if (!localSubscriptionMap.containsKey(url)) {
                newSubscriptions.add(gSubscription);
            }
        }

        // Subscribe to all the new subscriptions
        Subscription newLocalSubscription;
        for (int i = 0; i < newSubscriptions.size(); i++) {
            gSubscription = newSubscriptions.get(i);
            /*
            newLocalSubscription = new Subscription(gSubscription.getUrl());
            newLocalSubscription.subscribe(argContext);
            */
            SoundWaves.getAppContext(argContext).getLibraryInstance().subscribe(gSubscription.getUrl());
        }


        /**
         * Upload local subscriptions to gpodder
         */
        List<String> localSubscriptionUrls = new LinkedList<>();
        for (int i = 0; i < argLocalSubscriptions.size(); i++) {
            newLocalSubscription = argLocalSubscriptions.valueAt(i);
            localSubscriptionUrls.add(newLocalSubscription.getURLString());
        }
        Response response = api.uploadDeviceSubscriptions(localSubscriptionUrls, mUsername, GPodderUtils.getDeviceCaption(argContext)).execute();

        if (!response.isSuccessful()) {
            return SERVER_ERROR;
        }

        return SYNCHRONIZATION_OK;
    }

    public Call<List<GSubscription>> search(@NonNull final String argSearchTerm, @Nullable final IWebservice.ICallback<List<GSubscription>> argICallback) {
        Call<List<GSubscription>> result = api.search(argSearchTerm);
        result.enqueue(new Callback<List<GSubscription>>() {
            @Override
            public void onResponse(Call<List<GSubscription>> argCall, Response<List<GSubscription>> response) {

                if (argICallback == null)
                    return;

                argICallback.onResponse(argCall, response);
            }

            @Override
            public void onFailure(Call<List<GSubscription>> call, Throwable t) {
                Log.d(TAG, t.toString());
            }
        });

        return result;
    }

    public void uploadSubscriptions(@NonNull final Context argContext,
                                    final LongSparseArray<ISubscription> argSubscriptions) {
        uploadSubscriptions(argContext, argSubscriptions, null);
    }

    public void uploadSubscriptions(@NonNull final Context argContext,
                                    final LongSparseArray<ISubscription> argSubscriptions,
                                    @Nullable final ICallback argCallback) {

        if (mAuthenticated) {
            uploadSubscriptionsInternal(argContext, argSubscriptions, argCallback);
            return;
        }

        authenticate(argCallback, new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                uploadSubscriptionsInternal(argContext, argSubscriptions, argCallback);
            }

            @Override
            public void onFailure(Call call, Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    private void uploadSubscriptionsInternal(@NonNull Context argContext, final LongSparseArray<ISubscription> argSubscriptions, @Nullable ICallback argCalback) {

        //String test = "feeds.twit.tv/brickhouse.xml\nleoville.tv/podcasts/twit.xml";
        //String opml = OPMLImportExport.toOPML(argSubscriptions);

        List<String> subscriptionList = new LinkedList<>();
        long key;
        ISubscription feed;

        for (int i = 0; i < argSubscriptions.size(); i++) {
            key = argSubscriptions.keyAt(i);
            feed = argSubscriptions.get(key);
            subscriptionList.add(feed.getURLString());
        }

        CallbackWrapper<ResponseBody> callback = new CallbackWrapper(argCalback, mDummyCallback);

        api.uploadDeviceSubscriptions(subscriptionList, mUsername, GPodderUtils.getDeviceCaption(argContext)).enqueue(callback);
    }

    public void getDeviceSubscriptions(@NonNull Context argContext) {

        if (mAuthenticated) {
            //getDeviceSubscriptions(argContext);
            return;
        }

        api.getDeviceSubscriptions(mUsername, GPodderUtils.getDeviceCaption(argContext)).enqueue(new Callback<String[]>() {
            @Override
            public void onResponse(Call<String[]> call, Response<String[]> response) {
                Log.d(TAG, call.toString());
            }

            @Override
            public void onFailure(Call<String[]> call, Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    public void getAllSubscriptions() {

        if (mAuthenticated) {
            //getAllSubscriptions();
            return;
        }

        api.getSubscriptions(mUsername).enqueue(new Callback<List<GSubscription>>() {
            @Override
            public void onResponse(Call<List<GSubscription>> call, Response<List<GSubscription>> argRetrofit) {
                Log.d(TAG, call.toString());
            }

            @Override
            public void onFailure(Call<List<GSubscription>> call, Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    public void getDeviceSubscriptionsChanges(@NonNull Context argContext,  long argSince) {

        if (mAuthenticated) {
            //getDeviceSubscriptionsChanges(argContext, argSince);
            return;
        }

        api.getDeviceSubscriptionsChanges(mUsername, GPodderUtils.getDeviceCaption(argContext), argSince).enqueue(new Callback<SubscriptionChanges>() {
            @Override
            public void onResponse(Call<SubscriptionChanges> call, Response<SubscriptionChanges> response) {
                Log.d(TAG, call.toString());
            }

            @Override
            public void onFailure(Call<SubscriptionChanges> call, Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    private void uploadSubscriptionChangesInternal(@NonNull Context argContext,
                                                   LongSparseArray<ISubscription> argAdded,
                                                   LongSparseArray<ISubscription> argRemoved) {

        SubscriptionChanges changes = new SubscriptionChanges();

        ISubscription subscription;

        for (int i = 0; i < argAdded.size(); i++) {
            subscription = argAdded.valueAt(i);
            changes.add.add(subscription.getURLString());
        }

        for (int i = 0; i < argRemoved.size(); i++) {
            subscription = argRemoved.valueAt(i);
            changes.remove.add(subscription.getURLString());
        }

        api.uploadDeviceSubscriptionsChanges(changes, mUsername, GPodderUtils.getDeviceCaption(argContext)).enqueue(new Callback<UpdatedUrls>() {
            @Override
            public void onResponse(Call<UpdatedUrls> call, Response<UpdatedUrls> response) {
                Log.d(TAG, call.toString());
                //UpdatedUrls updatedUrls = new Gson
            }

            @Override
            public void onFailure(Call<UpdatedUrls> call, Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    public void authenticate(@Nullable ICallback argICallback) {
        authenticate(argICallback, null);
    }

    public Call<List<GSubscription>> getTopList(int amount, @Nullable String argTag, @Nullable ICallback<List<GSubscription>> argCallback) {
        Call<List<GSubscription>> call;

        if (TextUtils.isEmpty(argTag)) {
            call = api.getPodcastToplist(amount);
        } else {
            call = api.getPodcastForTag(argTag, amount);
        }

        CallbackWrapper callbackWrapper = new CallbackWrapper(argCallback, mDummyCallback);
        call.enqueue(callbackWrapper);

        return call;
    }

    public boolean authenticateSync() throws IOException {

        // Fetch data from: http://gpodder.net/clientconfig.json

        AuthenticationCallback callback = new AuthenticationCallback(null, null);
        Response response = api.login(mUsername).execute();

        if (response.isSuccessful()) {
            return true;
        }

        return response.isSuccessful();
    }

    private void authenticate(@Nullable ICallback argICallback, @Nullable Callback argCallback) {

        // Fetch data from: http://gpodder.net/clientconfig.json

        AuthenticationCallback callback = new AuthenticationCallback(argICallback, argCallback);
        api.login(mUsername).enqueue(callback);
    }

    private class AuthenticationCallback extends CallbackWrapper {

        public AuthenticationCallback(@Nullable ICallback argICallback, @Nullable Callback argCallback) {
            super(argICallback, argCallback);
        }

        @Override
        public void onResponse(Call call, Response response) {
            mAuthenticated = true;
            super.onResponse(call, response);
        }

        @Override
        public void onFailure(Call call, Throwable error) {
            mAuthenticated = false;
            /*
            Response response = error.getResponse();

            switch (response.getStatus()) {
                case 401: {
                    // 401 Unauthorized
                    break;
                }
                case 400: {
                    // If the client provides a cookie, but for a different username than the one given
                    break;
                }
            }
            */
            Log.d(TAG, error.toString());
            super.onFailure(call, error);
        }

    }

    /*
    private void storeAuthenticationCookie(Response argResponse) {
        com.squareup.okhttp.Headers headers = argResponse.headers();
        int numHeaders = headers.size();

        String name, value;
        for (int i = 0; i < numHeaders; i++) {
            name = headers.name(i);
            value = headers.value(i);
            if (name.equals("Set-Cookie") && name.startsWith("sessionid")) {
                ApiRequestInterceptor.cookie = value.split(";")[0];
            }
        }
    }
    */

    @NonNull
    private static Set<String> getSubscribedUrls(@NonNull LongSparseArray<Subscription> argLocalSubscriptions) {
        Set<String> urls = getSet();

        for (int i = 0; i < argLocalSubscriptions.size(); i++) {
            long arrayKey = argLocalSubscriptions.keyAt(i);
            // get the object by the key.
            ISubscription subscription = argLocalSubscriptions.get(arrayKey);
            if (subscription.IsSubscribed())
                urls.add(subscription.getURLString());
        }

        return urls;
    }

    private static Set<String> getSet() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return new ArraySet<>();
        } else {
            return new HashSet<>();
        }
    }

    private String[] getDeviceSubscriptionsAsStrings(@NonNull Context argContext) throws IOException {
        Call<String[]> deviceSubscriptions = api.getDeviceSubscriptions(mUsername, GPodderUtils.getDeviceCaption(argContext));
        Response<String[]> deviceSubscriptionsResponse = deviceSubscriptions.execute();

        if (!deviceSubscriptionsResponse.isSuccessful())
            throw new IOException("Could not contact server"); // NoI18N

        return deviceSubscriptionsResponse.body();
    }

    private void updateLocalUrls(@NonNull Context argContext, LongSparseArray<Subscription> argLocalSubscriptions, Set<Pair<String, String>> updatedUrls) {
        for (int i = 0; i < argLocalSubscriptions.size(); i++) {
            long arrayKey = argLocalSubscriptions.keyAt(i);
            // get the object by the key.
            Subscription subscription = argLocalSubscriptions.get(arrayKey);
            String url = subscription.getURLString();

            //if (updatedUrls.)
            for (Pair<String, String> p : updatedUrls) {
                if (p.first.equals(url)) {
                    subscription.updateUrl(argContext, p.second);
                    continue;
                }
            }
        }
    }

    private String getSubscriptionUrlForEpisode(LongSparseArray<Subscription> argLocalSubscriptions, FeedItem item) {
        if (argLocalSubscriptions == null || item == null)
            return "";

        for (int i = 0; i < argLocalSubscriptions.size(); i++) {
            long key = argLocalSubscriptions.keyAt(i);
            Subscription subscription = argLocalSubscriptions.get(key);
            if (subscription.getId() == item.sub_id) {
                return subscription.getURLString();
            }
        }

        return "";
    }

}
