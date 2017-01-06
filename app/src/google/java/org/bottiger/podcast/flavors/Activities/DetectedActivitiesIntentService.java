package org.bottiger.podcast.flavors.Activities;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

/**
 *  IntentService for handling incoming intents that are generated as a result of requesting
 *  activity updates using
 *  {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates}.
 */
public class DetectedActivitiesIntentService extends IntentService {

    protected static final String TAG = "DetectedActivitiesIS";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        Intent localIntent = new Intent(Constants.BROADCAST_ACTION);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.

        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
        // Log each activity.
        Log.i(TAG, "activities detected");
        for (DetectedActivity da: detectedActivities) {
            Log.i(TAG, getActivityString(
                    da.getType()) + " " + da.getConfidence() + "%"
            );
        }

        DetectedActivity detectedActivity = result.getMostProbableActivity();
        Log.i(TAG, "activity detected");

        // Broadcast the list of detected activities.
        localIntent.putExtra(Constants.ACTIVITY_EXTRA, detectedActivity);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    /**
     * Returns a human readable String corresponding to a detected activity type.
     */
    private static String getActivityString(int detectedActivityType) {
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return "Vehicle"; // NoI18N
            case DetectedActivity.ON_BICYCLE:
                return "Bicycle";  // NoI18N
            case DetectedActivity.ON_FOOT:
                return "Foot";  // NoI18N
            case DetectedActivity.RUNNING:
                return "Running";  // NoI18N
            case DetectedActivity.STILL:
                return "Still";  // NoI18N
            case DetectedActivity.TILTING:
                return "Tilting";  // NoI18N
            case DetectedActivity.UNKNOWN:
                return "Unknown";  // NoI18N
            case DetectedActivity.WALKING:
                return "Walking";  // NoI18N
            default:
                return "N/A";  // NoI18N
        }
    }
}