package org.bottiger.podcast.flavors.Analytics;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.BuildConfig;

/**
 * Created by apl on 21-02-2015.
 */
public class AnalyticsFactory {

    public static IAnalytics getAnalytics(@NonNull Context argContext) {

        String flavor = BuildConfig.FLAVOR;

        if (flavor == "google") {
            return new PlayAnalytics(argContext);
        } else if (flavor == "amazon") {

        }

        return new DummyAnalytics();
    }
}
