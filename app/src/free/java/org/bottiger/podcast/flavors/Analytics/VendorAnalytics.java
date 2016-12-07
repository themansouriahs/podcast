package org.bottiger.podcast.flavors.Analytics;

import android.content.Context;
import android.support.annotation.Nullable;

/**
 * Created by apl on 21-02-2015.
 */
public class VendorAnalytics implements IAnalytics {
    public VendorAnalytics(Context argContext) {

    }

    @Override
    public boolean doShare() {
        return false;
    }

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

    public void trackEvent(EVENT_TYPE argEvent) {
        trackEvent(argEvent, null);
    }

    public void trackEvent(EVENT_TYPE argEvent, @Nullable Integer argValue) {
        return;
    }

    @Override
    public void logFeed(@NonNull String url, boolean argDidSubscribe) {}
}
