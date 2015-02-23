package org.bottiger.podcast.flavors.Analytics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsConfig;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.regions.Regions;

/**
 * Created by apl on 19-02-2015.
 *
 * Only used on the Amazon app store
 */
public class VendorAnalytics implements IAnalytics{

    private static MobileAnalyticsManager analytics;
    private Context mContext;

    public VendorAnalytics(@NonNull Context argContext) {
        mContext = argContext;
    }

    public void startTracking() {

        // Create a credentials provider.
        // Replace AWS_ACCOUNT_ID, COGNITO_IDENTITY_POOL, UNAUTHENTICATED_ROLE and AUTHENTICATED_ROLE with the applicable values.
        CognitoCachingCredentialsProvider cognitoProvider = new CognitoCachingCredentialsProvider(
                mContext.getApplicationContext(),
                "",
                "", /* Identity Pool ID */
                "",
                "",
                Regions.US_EAST_1
        );

        try {
            AnalyticsConfig options = new AnalyticsConfig();
            options.withAllowsWANDelivery(true);
            analytics = MobileAnalyticsManager.getOrCreateInstance(
                    mContext.getApplicationContext(),
                    "yourAppId",
                    Regions.US_EAST_1,
                    cognitoProvider,
                    options
            );
        } catch(InitializationException ex) {
            Log.e(this.getClass().getName(), "Failed to initialize Amazon Mobile Analytics", ex);
        }
    }

    @Override
    public void stopTracking() {
        return;
    }

    @Override
    public void trackEvent(EVENT_TYPE argEvent) {
        return;
    }

}
