package org.bottiger.podcast.flavors.Activities;

import android.content.Context;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;

/**
 * Created by aplb on 05-01-2017.
 */
public class VendorActivityTracker implements IActivityDetector{

    public VendorActivityTracker(Context context) {
    }

    @Override
    public Flowable<Integer> getActivityes() {
        return BehaviorSubject.create()
                .toFlowable(BackpressureStrategy.LATEST)
                .map(new Function<Object, Integer>() {
                    @Override
                    public Integer apply(Object o) throws Exception {
                        return 1;
                    }
                });
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
