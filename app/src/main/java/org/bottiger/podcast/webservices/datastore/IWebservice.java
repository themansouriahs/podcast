package org.bottiger.podcast.webservices.datastore;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;

import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GSubscription;

import java.util.List;

import retrofit.Call;
import retrofit.Response;

/**
 * Created by Arvid on 8/27/2015.
 */
public interface IWebservice {

    void authenticate(@Nullable ICallback argCallback);

    Call<List<GSubscription>> getTopList(int amount, @Nullable String argTag, @Nullable ICallback<List<GSubscription>> argCallback);

    void uploadSubscriptions(LongSparseArray<ISubscription> argSubscriptions);
    void uploadSubscriptions(LongSparseArray<ISubscription> argSubscriptions, @Nullable ICallback argCallback);

    interface ICallback<T> {
        void onResponse(Response<T> response);
        void onFailure(Throwable throwable);
    }

}
