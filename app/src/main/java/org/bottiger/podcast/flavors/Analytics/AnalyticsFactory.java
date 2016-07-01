package org.bottiger.podcast.flavors.Analytics;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by apl on 21-02-2015.
 */
public class AnalyticsFactory {

    public static IAnalytics getAnalytics(@NonNull Context argContext) {
        return new VendorAnalytics(argContext);
    }
}
