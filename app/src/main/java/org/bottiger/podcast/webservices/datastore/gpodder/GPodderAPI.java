package org.bottiger.podcast.webservices.datastore.gpodder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;

import com.google.api.client.json.gson.GsonFactory;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.ResponseBody;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.webservices.datastore.CallbackWrapper;
import org.bottiger.podcast.webservices.datastore.IWebservice;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GSubscription;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.SubscriptionChanges;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.UpdatedUrls;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.Header;
import retrofit.http.Headers;

/**
 * Created by Arvid on 8/23/2015.
 */
public class GPodderAPI implements IWebservice {

    private static final String TAG = "GPodderAPI";
    private static final boolean RESPECT_GPODDER_URL_SANITIZER = true;

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
        public void onResponse(Response<String> response) {
            Log.d(TAG, response.toString());
        }

        @Override
        public void onFailure(Throwable error) {
            Log.d(TAG, error.toString());
        }
    };

    public GPodderAPI() {
        this(null, null, null);
    }

    public GPodderAPI(@Nullable String argUsername, @Nullable String argPassword) {
        this(argUsername, argPassword, null);
    }

    public GPodderAPI(@Nullable String argUsername, @Nullable String argPassword, @Nullable Callback argCallback) {

        mUsername = argUsername;

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new ApiRequestInterceptor(argUsername, argPassword));

        api = new Retrofit.Builder()
                .baseUrl(IGPodderAPI.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client) // The default client didn't handle well responses like 401
                .build()
                .create(IGPodderAPI.class);

        if (mUsername != null) {
            authenticate(null, argCallback);
        }
    }

    public
    @SynchronizationResult
    int synchronize(@NonNull Context argContext, @NonNull LongSparseArray<Subscription> argLocalSubscriptions) throws IOException {

        Response response = api.login(mUsername).execute();
        if (!response.isSuccess())
            return AUTHENTICATION_ERROR;

        String key = argContext.getResources().getString(R.string.gpodder_last_sync_key);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
        long lastSync = prefs.getLong(key, 0);

        long timestamp;
        @SynchronizationResult int returnValue = SYNCHRONIZATION_OK;


        /**
         * Fetch changes from the server
         */
        Set<String> subscribedUrls = getUrls(argLocalSubscriptions);

        Call<SubscriptionChanges> subscriptionsChanges = api.getDeviceSubscriptionsChanges(mUsername, GPodderUtils.getDeviceID(), lastSync); // lastCall
        Response<SubscriptionChanges> subscriptionsChangesResponse = subscriptionsChanges.execute();

        if (!subscriptionsChangesResponse.isSuccess())
            return SERVER_ERROR;

        SubscriptionChanges subscriptionsChangesResult = subscriptionsChangesResponse.body();
        List<String> added = subscriptionsChangesResult.add;
        List<String> removed = subscriptionsChangesResult.remove;

        added.removeAll(subscribedUrls);

        String newUrl;
        Subscription changedSubscription;
        for (int i = 0; i < added.size(); i++) {
            newUrl = added.get(i);
            changedSubscription = new Subscription(newUrl);
            changedSubscription.subscribe(argContext);
        }

        /**
         * Upload changes from the client to gpodder
         */
        String[] gPodderDeviceSubscriptions;
        Set<String> gPodderSubscriptionsSet = getSet();
        try {
            gPodderDeviceSubscriptions = getDeviceSubscriptionsAsStrings();
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
                    subscription.unsubscribe(argContext);
                } else {
                    Log.w(TAG, "gPodder removed a subscription we are not subscribed to: " + url); // NoI18N
                    VendorCrashReporter.report("Removed unknown subscription", url); // NoI18N
                }
            }


            if (subscription.IsSubscribed() && !gPodderSubscriptionsSet.contains(subscription)) {
                localSubscriptionsChanges.add.add(url);
            }

            if (subscription.IsSubscribed()) {
                //localSubscriptionsChanges.remove.add(url);
                gPodderSubscriptionsSet.remove(url);
            }
        }

        // We are not subscribed to the URL's there are left
        localSubscriptionsChanges.remove.addAll(gPodderSubscriptionsSet);

        Call<UpdatedUrls> updatedUrlsCall = api.uploadDeviceSubscriptionsChanges(localSubscriptionsChanges, mUsername, GPodderUtils.getDeviceID());
        Response<UpdatedUrls> updatedUrlsResponse = updatedUrlsCall.execute();

        if (!updatedUrlsResponse.isSuccess())
            return SERVER_ERROR;

        UpdatedUrls updatedUrls = updatedUrlsResponse.body();

        if (RESPECT_GPODDER_URL_SANITIZER) {
            updateLocalUrls(argContext, argLocalSubscriptions, updatedUrls.getUpdatedUrls());
        }

        timestamp = updatedUrls.getTimestamp();

        if (timestamp > 0) {
            prefs.edit().putLong(key, timestamp).commit();
        }

        return returnValue;
    }

    public
    @SynchronizationResult
    int initialSynchronization(@NonNull Context argContext, @NonNull LongSparseArray<Subscription> argLocalSubscriptions) throws IOException {

        String[] deviceSubscriptions;
        try {
            deviceSubscriptions = getDeviceSubscriptionsAsStrings();
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
            newLocalSubscription = new Subscription(gSubscription.getUrl());
            newLocalSubscription.subscribe(argContext);
        }


        /**
         * Upload local subscriptions to gpodder
         */
        List<String> localSubscriptionUrls = new LinkedList<>();
        for (int i = 0; i < argLocalSubscriptions.size(); i++) {
            newLocalSubscription = argLocalSubscriptions.valueAt(i);
            localSubscriptionUrls.add(newLocalSubscription.getURLString());
        }
        Response response = api.uploadDeviceSubscriptions(localSubscriptionUrls, mUsername, GPodderUtils.getDeviceID()).execute();

        if (!response.isSuccess()) {
            return SERVER_ERROR;
        }

        return SYNCHRONIZATION_OK;
    }

    public Call<List<GSubscription>> search(@NonNull final String argSearchTerm, @Nullable final IWebservice.ICallback<List<GSubscription>> argICallback) {
        Call<List<GSubscription>> result = api.search(argSearchTerm);
        result.enqueue(new Callback<List<GSubscription>>() {
            @Override
            public void onResponse(Response<List<GSubscription>> response) {

                if (argICallback == null)
                    return;

                argICallback.onResponse(response);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d(TAG, t.toString());
            }
        });

        return result;
    }

    public void uploadSubscriptions(final LongSparseArray<ISubscription> argSubscriptions) {
        uploadSubscriptions(argSubscriptions, null);
    }

    public void uploadSubscriptions(final LongSparseArray<ISubscription> argSubscriptions, @Nullable final ICallback argCallback) {

        if (mAuthenticated) {
            uploadSubscriptionsInternal(argSubscriptions, argCallback);
            return;
        }

        authenticate(argCallback, new Callback() {
            @Override
            public void onResponse(Response response) {
                uploadSubscriptionsInternal(argSubscriptions, argCallback);
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    private void uploadSubscriptionsInternal(final LongSparseArray<ISubscription> argSubscriptions, @Nullable ICallback argCalback) {

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

        api.uploadDeviceSubscriptions(subscriptionList, mUsername, GPodderUtils.getDeviceID()).enqueue(callback);
    }

    public void getDeviceSubscriptions() {

        if (mAuthenticated) {
            getDeviceSubscriptions();
            return;
        }

        api.getDeviceSubscriptions(mUsername, GPodderUtils.getDeviceID()).enqueue(new Callback<String[]>() {
            @Override
            public void onResponse(Response<String[]> response) {
                Log.d(TAG, response.toString());
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    public void getAllSubscriptions() {

        if (mAuthenticated) {
            getAllSubscriptions();
            return;
        }

        api.getSubscriptions(mUsername).enqueue(new Callback<List<GSubscription>>() {
            @Override
            public void onResponse(Response<List<GSubscription>> response) {
                Log.d(TAG, response.toString());
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    public void getDeviceSubscriptionsChanges(long argSince) {

        if (mAuthenticated) {
            getDeviceSubscriptionsChanges(argSince);
            return;
        }

        api.getDeviceSubscriptionsChanges(mUsername, GPodderUtils.getDeviceID(), argSince).enqueue(new Callback<SubscriptionChanges>() {
            @Override
            public void onResponse(Response<SubscriptionChanges> response) {
                Log.d(TAG, response.toString());
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d(TAG, error.toString());
            }
        });
    }

    private void uploadSubscriptionChangesInternal(LongSparseArray<ISubscription> argAdded,
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

        api.uploadDeviceSubscriptionsChanges(changes, mUsername, GPodderUtils.getDeviceID()).enqueue(new Callback<UpdatedUrls>() {
            @Override
            public void onResponse(Response<UpdatedUrls> response) {
                Log.d(TAG, response.toString());
                //UpdatedUrls updatedUrls = new Gson
            }

            @Override
            public void onFailure(Throwable error) {
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

        if (response.isSuccess()) {
            storeAuthenticationCookie(response);
            return true;
        }

        return response.isSuccess();
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
        public void onResponse(Response response) {
            mAuthenticated = true;

            com.squareup.okhttp.Headers headers = response.headers();
            int numHeaders = headers.size();

            String name, value;
            for (int i = 0; i < numHeaders; i++) {
                name = headers.name(i);
                value = headers.value(i);
                if (name.equals("Set-Cookie") && name.startsWith("sessionid")) {
                    ApiRequestInterceptor.cookie = value.split(";")[0];
                }
            }
            super.onResponse(response);
        }

        @Override
        public void onFailure(Throwable error) {
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
            super.onFailure(error);
        }

    }

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

    @NonNull
    private static Set<String> getUrls(@NonNull LongSparseArray<Subscription> argLocalSubscriptions) {
        Set<String> urls = getSet();

        for (int i = 0; i < argLocalSubscriptions.size(); i++) {
            long arrayKey = argLocalSubscriptions.keyAt(i);
            // get the object by the key.
            ISubscription subscription = argLocalSubscriptions.get(arrayKey);
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

    private String[] getDeviceSubscriptionsAsStrings() throws IOException {
        Call<String[]> deviceSubscriptions = api.getDeviceSubscriptions(mUsername, GPodderUtils.getDeviceID());
        Response<String[]> deviceSubscriptionsResponse = deviceSubscriptions.execute();

        if (!deviceSubscriptionsResponse.isSuccess())
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
}
