package org.bottiger.podcast.flavors.Activities;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.PreferenceHelper;

import java.util.ArrayList;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.BehaviorSubject;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;
import static org.bottiger.podcast.flavors.Activities.Constants.BROADCAST_ACTION;
import static org.bottiger.podcast.flavors.Activities.Constants.DEBUGGING;
import static org.bottiger.podcast.flavors.Activities.Constants.IN_VEHICLE;
import static org.bottiger.podcast.flavors.Activities.Constants.OTHER;
import static org.bottiger.podcast.flavors.Activities.Constants.STILL;

/**
 * Created by aplb on 05-01-2017.
 */

public class VendorActivityTracker implements IActivityDetector, ResultCallback<Status>, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = VendorActivityTracker.class.getSimpleName();

    private final GoogleApiClient mGoogleApiClient;
    private final Context mContext;

    /**
     * A receiver for DetectedActivity objects broadcast by the
     * {@code ActivityDetectionIntentService}.
     */
    private ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    private BehaviorSubject<DetectedActivity> mSubject = BehaviorSubject.create();

    public VendorActivityTracker(@NonNull Context argContext) {

        mContext = argContext;
        ActivityConnectionCallbacks activityConnectionCallbacks = new ActivityConnectionCallbacks(this);
        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver(mSubject);

        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(argContext)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(activityConnectionCallbacks)
                .addOnConnectionFailedListener(activityConnectionCallbacks)
                .build();
    }

    public Flowable<Integer> getActivityes() {
        return mSubject
                .toFlowable(BackpressureStrategy.LATEST)
                .filter(new Predicate<DetectedActivity>() {
                    @Override
                    public boolean test(DetectedActivity detectedActivity) throws Exception {
                        // Still is reported every time a car holds still.
                        // We only want to report walking/driving/etc
                        return detectedActivity.getType() != STILL;
                    }
                })
                .map(new Function<DetectedActivity, Integer>() {
            @Override
            public Integer apply(DetectedActivity detectedActivities) throws Exception {

                if (detectedActivities == null) {
                    return STILL;
                }

                Log.d(TAG, "activity: " + detectedActivities.getType());

                switch (detectedActivities.getType()) {
                    case DetectedActivity.IN_VEHICLE:
                    case DetectedActivity.ON_BICYCLE: {
                        return IN_VEHICLE;
                    }
                    case DetectedActivity.TILTING: {
                        return DEBUGGING;
                    }
                }

                return OTHER;
            }
        });
    }

    public void start() {
        if (isEnabled()) {
            mGoogleApiClient.connect();
            Log.i(TAG, "Activity Tracker connected");
        }
    }

    public void stop() {
        Log.i(TAG, "Activity Tracker disconnected");
        mGoogleApiClient.disconnect();
    }

    public void pause() {
        // Unregister the broadcast receiver that was registered during onResume().
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mBroadcastReceiver);
    }

    public void resume() {
        if (isEnabled()) {
            // Register the broadcast receiver that informs this activity of the DetectedActivity
            // object broadcast sent by the intent service.
            LocalBroadcastManager.getInstance(mContext).registerReceiver(mBroadcastReceiver,
                    new IntentFilter(BROADCAST_ACTION));
        }
    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            /*
            // Toggle the status of activity updates requested, and save in shared preferences.
            boolean requestingUpdates = !getUpdatesRequestedState();
            setUpdatesRequestedState(requestingUpdates);

            // Update the UI. Requesting activity updates enables the Remove Activity Updates
            // button, and removing activity updates enables the Add Activity Updates button.
            setButtonsEnabledState();

            Toast.makeText(
                    this,
                    getString(requestingUpdates ? R.string.activity_updates_added :
                            R.string.activity_updates_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
            */
        }
    }

    /**
     * Registers for activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code requestActivityUpdates()} completes
     * successfully, the {@code DetectedActivitiesIntentService} starts receiving callbacks when
     * activities are detected.
     */
    public void requestActivityUpdatesHandler() {
        if (!mGoogleApiClient.isConnected()) {
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
    }

    /**
     * Removes activity recognition updates using
     * {@link com.google.android.gms.location.ActivityRecognitionApi#removeActivityUpdates} which
     * returns a {@link com.google.android.gms.common.api.PendingResult}. Since this activity
     * implements the PendingResult interface, the activity itself receives the callback, and the
     * code within {@code onResult} executes. Note: once {@code removeActivityUpdates()} completes
     * successfully, the {@code DetectedActivitiesIntentService} stops receiving callbacks about
     * detected activities.
     */
    public void removeActivityUpdatesButtonHandler() {
        if (!mGoogleApiClient.isConnected()) {
            return;
        }
        // Remove all activity updates for the PendingIntent that was used to request activity
        // updates.
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                mGoogleApiClient,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(mContext, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @NonNull
    GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    private boolean isEnabled() {
        return PreferenceHelper.getBooleanPreferenceValue(mContext, R.string.pref_driving_mode_key, R.bool.pref_driving_mode_default);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String argChangedKey) {
        String drivingModeKey = mContext.getString(R.string.pref_driving_mode_key);

        if (drivingModeKey.equals(argChangedKey)) {
            if (isEnabled()) {
                start();
                resume();
            } else {
                removeActivityUpdatesButtonHandler();
                pause();
                stop();
            }
        }
    }
}
