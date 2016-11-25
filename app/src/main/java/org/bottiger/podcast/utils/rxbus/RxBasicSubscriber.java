package org.bottiger.podcast.utils.rxbus;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.bottiger.podcast.utils.ErrorUtils;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Created by aplb on 23-11-2016.
 */

public abstract class RxBasicSubscriber<T> implements Subscriber<T> {

    private static final String TAG = RxBasicSubscriber.class.getSimpleName();
    private String mTag = TAG;

    public RxBasicSubscriber() {
    }

    public RxBasicSubscriber(@Nullable String argTag) {
        if (!TextUtils.isEmpty(argTag)) {
            mTag = argTag;
        }
    }

    public void onSubscribe(Subscription s) {
        Log.d(mTag, "onSubscribe");
        s.request(Long.MAX_VALUE);
    }

    public void onError(Throwable t) {
        ErrorUtils.handleException(t, mTag);
    }

    public void onComplete() {
        Log.d(mTag, "complete");
    }
}
