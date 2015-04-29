package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Application;
import android.support.annotation.NonNull;

/**
 * Created by apl on 26-02-2015.
 */
public class CrashReporterFactory {

    public static void startReporter(@NonNull Application argApplication) {
        VendorCrashReporter.init(argApplication);
    }

}
