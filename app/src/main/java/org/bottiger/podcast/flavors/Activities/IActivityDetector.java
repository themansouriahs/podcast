package org.bottiger.podcast.flavors.Activities;

import io.reactivex.Flowable;

/**
 * Created by aplb on 06-01-2017.
 */

public interface IActivityDetector {

    Flowable<Integer> getActivityes();

    void start();
    void stop();

    void pause();
    void resume();

}
