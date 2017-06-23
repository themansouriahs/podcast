package org.bottiger.podcast.utils.rxbus;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.model.events.SubscriptionChanged;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * courtesy: https://gist.github.com/benjchristensen/04eef9ca0851f3a5d7bf
 */
@Deprecated
public class RxBus {

    //private final PublishSubject<Object> _bus = PublishSubject.create();

    // If multiple threads are going to emit events to this
    // then it must be made thread-safe like this instead
    @Deprecated
    private final Subject<Object, Object> _bus = new SerializedSubject<>(PublishSubject.create());

    @Deprecated
    public void send(Object o) {
        _bus.onNext(o);
    }

    @Deprecated
    public Observable<Object> toObserverable() {
        return _bus;
    }

    @Deprecated
    public boolean hasObservers() {
        return _bus.hasObservers();
    }
}
