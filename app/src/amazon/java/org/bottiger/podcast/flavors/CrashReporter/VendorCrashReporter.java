package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Application;
import android.support.annotation.NonNull;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;

/**
 * Created by apl on 26-02-2015.
 *
 * Only used on the Amazon app store
 */
public class VendorCrashReporter {

    public static void init(@NonNull Application argApplication) {
        ACRAConfiguration config = ACRA.getConfig();
        try {
            config.setMode(ReportingInteractionMode.SILENT);
        } catch (ACRAConfigurationException e) {
            e.printStackTrace();
        } finally {
            ACRA.init(argApplication, config);
        }
    }

    public static void report(@NonNull String argKey, @NonNull String argValue) {
        ACRA.getErrorReporter().putCustomData(argKey, argValue);
    }

    public static void handleException(@NonNull Throwable argException) {
        ACRA.getErrorReporter().handleException(argException);
    }
}
