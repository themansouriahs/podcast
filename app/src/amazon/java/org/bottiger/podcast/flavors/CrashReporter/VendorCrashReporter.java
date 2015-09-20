package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Application;
import android.support.annotation.NonNull;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.MetaData;


/**
 * Created by apl on 26-02-2015.
 *
 * Only used on the Amazon app store
 */
public class VendorCrashReporter {

    public static void init(@NonNull Application argApplication) {
        Bugsnag.init(argApplication);
        /*
        ACRAConfiguration config = ACRA.getNewDefaultConfig(argApplication);
        try {
            config.setMode(ReportingInteractionMode.SILENT);
        } catch (ACRAConfigurationException e) {
            e.printStackTrace();
        } finally {
            ACRA.init(argApplication, config);
        }*/
    }

    public static void report(@NonNull String argKey, @NonNull String argValue) {
        MetaData metaData = new MetaData();
        metaData.addToTab("User", argKey, argValue);
        Bugsnag.notify(new Exception("Non-fatal"), metaData);
    }

    public static void handleException(@NonNull Throwable argException) {
        Bugsnag.notify(argException);
        /*
        if (!BuildConfig.DEBUG)
            ACRA.getErrorReporter().handleException(argException);
            */
    }

    public static void handleException(@NonNull Throwable argException, @NonNull String[] argKey, @NonNull String[] argValue) {
        MetaData metaData = new MetaData();

        for(int i = 0; i < argKey.length; i++) {
            metaData.addToTab("User", argKey[i], argValue[i]);
        }

        Bugsnag.notify(argException, metaData);
    }
}
