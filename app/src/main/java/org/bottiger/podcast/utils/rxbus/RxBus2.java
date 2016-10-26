package org.bottiger.podcast.utils.rxbus;

import io.reactivex.Observable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.processors.ReplayProcessor;

/**
 * Created by aplb on 18-10-2016.
 */
public final class RxBus2 {
    private final PublishProcessor<Object> mBus = PublishProcessor.create();

    public void send(final Object event) {
        this.mBus.onNext(event);
    }

    public Observable<Object> toObservable() {
        return this.mBus.toObservable();
    }

    public boolean hasSubscribers() {
        return this.mBus.hasSubscribers();
    }

}
