package org.bottiger.podcast.provider;

/**
 * Created by aplb on 01-12-2016.
 */

public interface IPersistedSub extends ISubscription {

    int getScore();

    int getClicks();
    void incrementClicks();

}
