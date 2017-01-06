package org.bottiger.podcast.flavors.Activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by aplb on 06-01-2017.
 */

public class ActivityConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks,
                                                    GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = ActivityConnectionCallbacks.class.getSimpleName();

    private VendorActivityTracker mVendorActivityTracker;

    public ActivityConnectionCallbacks(@NonNull VendorActivityTracker argVendorActivityTracker) {
        mVendorActivityTracker = argVendorActivityTracker;
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        mVendorActivityTracker.requestActivityUpdatesHandler();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mVendorActivityTracker.getGoogleApiClient().connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
}
