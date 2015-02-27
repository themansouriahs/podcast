package org.bottiger.podcast.flavors.Analytics;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsConfig;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.regions.Regions;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.SoundWaves;

/**
 * Created by apl on 19-02-2015.
 *
 * Only used on the Amazon app store
 */
public class VendorAnalytics implements IAnalytics {

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
                ApplicationConfiguration.AMAZON_AMAZON_AWS_ACCOUNT,
                ApplicationConfiguration.AMAZON_COGNITO_IDENTITY_POOL, /* Identity Pool ID */
                ApplicationConfiguration.AMAZON_UNAUTHENTICATED_ARN,
                ApplicationConfiguration.AMAZON_AUTHENTICATED_ARN,
                Regions.US_EAST_1
        );

        try {
            AnalyticsConfig options = new AnalyticsConfig();
            options.withAllowsWANDelivery(true);
            analytics = MobileAnalyticsManager.getOrCreateInstance(
                    mContext.getApplicationContext(),
                    ApplicationConfiguration.AMAZON_APP_ID, //Mobile Analytics App ID
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
    public void activityPause() {
        if(analytics != null) {
            analytics.getSessionClient().pauseSession();
            analytics.getEventClient().submitEvents();
        }
    }

    @Override
    public void activityResume() {
        if(analytics != null) {
            analytics.getSessionClient().resumeSession();
        }
    }

    @Override
    public void trackEvent(EVENT_TYPE argEvent) {
        return;
    }

}
