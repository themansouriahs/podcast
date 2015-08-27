package org.bottiger.podcast.webservices.datastore;

import android.support.annotation.Nullable;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

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
    public void success(Object o, Response response) {
        if (mCallback != null)
            mCallback.success((T)o, response);

        if (mICallback != null)
            mICallback.success(o, response);
    }

    @Override
    public void failure(String error) {
        if (mICallback != null)
            mICallback.failure(error);
    }

    @Override
    public void failure(RetrofitError error) {
        if (mCallback == null)
            return;

        mCallback.failure(error);
    }
}
