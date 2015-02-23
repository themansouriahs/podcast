package org.bottiger.podcast.flavors.Analytics;

/**
 * Created by apl on 21-02-2015.
 */
public interface IAnalytics {

    public enum EVENT_TYPE { PLAY, PAUSE };

    public void startTracking();
    public void stopTracking();

    public void trackEvent(EVENT_TYPE argEvent);
}
