package org.bottiger.podcast.webservices.datastore.gpodder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.client.json.gson.GsonFactory;
import com.squareup.okhttp.OkHttpClient;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.webservices.datastore.CallbackWrapper;
import org.bottiger.podcast.webservices.datastore.IWebservice;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GSubscription;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.SubscriptionChanges;

import java.util.LinkedList;
import java.util.List;

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

    private IGPodderAPI api;
    private String mUsername;

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

        CallbackWrapper<String> callback = new CallbackWrapper(argCalback, mDummyCallback);

        api.uploadDeviceSubscriptions(subscriptionList, mUsername, GPodderUtils.getDeviceID()).enqueue(callback);
    }

    public void getDeviceSubscriptions() {

        if (mAuthenticated) {
            getDeviceSubscriptions();
            return;
        }

        api.getDeviceSubscriptions(mUsername, GPodderUtils.getDeviceID()).enqueue(new Callback<List<GSubscription>>() {
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

        for (int i = 0; i < argAdded.size(); i++)
        {
            subscription = argAdded.valueAt(i);
            changes.add.add(subscription.getURLString());
        }

        for (int i = 0; i < argRemoved.size(); i++)
        {
            subscription = argRemoved.valueAt(i);
            changes.remove.add(subscription.getURLString());
        }

        api.uploadDeviceSubscriptionsChanges(changes, mUsername, GPodderUtils.getDeviceID()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Response<String> response) {
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
}
