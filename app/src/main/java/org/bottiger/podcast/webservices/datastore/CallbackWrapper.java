package org.bottiger.podcast.webservices.datastore;

import android.support.annotation.Nullable;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by Arvid on 8/27/2015.
 */
public class CallbackWrapper<T> implements Callback<T>, IWebservice.ICallback {

    private Callback<T> mCallback;
    private IWebservice.ICallback mICallback;

    public CallbackWrapper(@Nullable IWebservice.ICallback argICallback, @Nullable Callback<T> argCallback) {
        mCallback = argCallback;
        mICallback = argICallback;
    }

    @Override
    public void onResponse(Response response, Retrofit argRetrofit) {
        if (mCallback != null)
            mCallback.onResponse(response, argRetrofit);

        if (mICallback != null)
            mICallback.onResponse(response, argRetrofit);
    }

    @Override
    public void onFailure(Throwable error) {
        if (mCallback != null)
            mCallback.onFailure(error);

        if (mICallback != null)
            mICallback.onFailure(error);
    }
}
