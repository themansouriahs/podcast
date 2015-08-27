package org.bottiger.podcast.webservices.datastore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;

import org.bottiger.podcast.provider.ISubscription;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Arvid on 8/27/2015.
 */
public interface IWebservice {

    void authenticate(@Nullable ICallback argCallback);

    void uploadSubscriptions(LongSparseArray<ISubscription> argSubscriptions);
    void uploadSubscriptions(LongSparseArray<ISubscription> argSubscriptions, @Nullable ICallback argCallback);

    interface ICallback<T> {
        void success(T s, Response response);
        void failure(String error);
    }

}
