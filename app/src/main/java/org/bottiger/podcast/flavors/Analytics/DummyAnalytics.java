package org.bottiger.podcast.flavors.Analytics;

/**
 * Created by apl on 21-02-2015.
 */
public class DummyAnalytics implements IAnalytics {
    @Override
    public void startTracking() {
        return;
    }

    @Override
    public void stopTracking() {
        return;
    }

    @Override
    public void activityPause() {
        return;
    }

    @Override
    public void activityResume() {
        return;
    }

    @Override
    public void trackEvent(EVENT_TYPE argEvent) {
        return;
    }
}
