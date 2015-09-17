package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bugsnag.android.Bugsnag;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;

/**
 * Created by apl on 26-02-2015.
 */
public class VendorCrashReporter {

    public static void init(@NonNull Application argApplication) {
        Bugsnag.init(argApplication);
        /*
        ACRAConfiguration config = ACRA.getNewDefaultConfig(argApplication);
        try {
            config.setFormUri("");
            config.setMailTo(ApplicationConfiguration.ACRA_MAIL);
            config.setResToastText(R.string.crash_toast);
            config.setMode(ReportingInteractionMode.TOAST);
        } catch (ACRAConfigurationException e) {
            e.printStackTrace();
        } finally {
            ACRA.init(argApplication, config);
        }
        */
    }

    public static void report(@NonNull String argKey, @NonNull String argValue) {
        //ACRA.getErrorReporter().putCustomData(argKey, argValue);
    }

    public static void handleException(@NonNull Throwable argException) {
        //ACRA.getErrorReporter().handleException(argException);
    }

}
