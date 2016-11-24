package org.bottiger.podcast.utils.rxbus;

import android.util.Log;

import org.bottiger.podcast.utils.ErrorUtils;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Created by aplb on 23-11-2016.
 */

public abstract class RxBasicSubscriber<T> implements Subscriber<T> {

    private static final String TAG = RxBasicSubscriber.class.getSimpleName();

    public void onSubscribe(Subscription s) {
        Log.d(TAG, "onSubscribe");
        s.request(Long.MAX_VALUE);
    }

    public void onError(Throwable t) {
        ErrorUtils.handleException(t, TAG);
    }

    public void onComplete() {
        Log.d(TAG, "complete");
    }
}
