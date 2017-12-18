package org.bottiger.podcast.utils;

import android.support.annotation.Nullable;
import android.util.Log;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;

/**
 * Created by aplb on 23-11-2016.
 */

public class ErrorUtils {

    private static final String TAG = ErrorUtils.class.getSimpleName();

    public static void handleException(@Nullable Throwable argThrowable, @Nullable String argTag) {
        VendorCrashReporter.handleException(argThrowable);
        //argThrowable.printStackTrace();

        String tag = argTag != null ? argTag : TAG;
        Log.w(TAG, argThrowable.toString());
    }

    public static void handleException(@Nullable Throwable argThrowable) {
        handleException(argThrowable, null);
    }
}
