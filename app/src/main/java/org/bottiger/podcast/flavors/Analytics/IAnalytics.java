package org.bottiger.podcast.flavors.Analytics;

/**
 * Created by apl on 21-02-2015.
 */
public interface IAnalytics {

    public enum EVENT_TYPE { PLAY,
                             PAUSE,
                             PLAY_FROM_PLAYLIST,
                             PLAY_FROM_FEEDVIEW,
                             SUBSCRIBE_TO_FEED
    };

    public void startTracking();
    public void stopTracking();

    public void activityPause();
    public void activityResume();

    public void trackEvent(EVENT_TYPE argEvent);
}
