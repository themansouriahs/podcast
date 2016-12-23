package org.bottiger.podcast.provider;

/**
 * Created by aplb on 01-12-2016.
 */

public interface IPersistedSub extends ISubscription {

    long getId();
    void setId(long argId);

    int getScore();

    int getClicks();
    void incrementClicks();

}
