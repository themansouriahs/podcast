package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Application;
import android.support.annotation.NonNull;

import org.acra.ACRA;

/**
 * Created by apl on 26-02-2015.
 *
 * Only used on the Amazon app store
 */
public class VendorCrashReporter {

    public static void init(@NonNull Application argApplication) {
        ACRA.init(argApplication);
    }

    public static void report(@NonNull String argKey, @NonNull String argValue) {
        ACRA.getErrorReporter().putCustomData(argKey, argValue);
    }

    public static void handleException(@NonNull Throwable argException) {
        ACRA.getErrorReporter().handleException(argException);
    }
}
