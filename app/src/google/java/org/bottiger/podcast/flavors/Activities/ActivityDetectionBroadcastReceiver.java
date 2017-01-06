package org.bottiger.podcast.flavors.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

import io.reactivex.Observer;

/**
 * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
 * Receives a list of one or more DetectedActivity objects associated with the current state of
 * the device.
 */
public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
    protected static final String TAG = ActivityDetectionBroadcastReceiver.class.getSimpleName();

    private Observer<DetectedActivity> mObserver;

    public ActivityDetectionBroadcastReceiver(@NonNull Observer<DetectedActivity> argObserver) {
        mObserver = argObserver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "activity event");
        DetectedActivity updatedActivities =
                intent.getParcelableExtra(Constants.ACTIVITY_EXTRA);
        updateDetectedActivitiesList(updatedActivities);
    }

    /**
     * Processes the list of freshly detected activities. Asks the adapter to update its list of
     * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
     * activities.
     */
    protected void updateDetectedActivitiesList(DetectedActivity detectedActivities) {
        //mAdapter.updateActivities(detectedActivities);
        mObserver.onNext(detectedActivities);
    }
}