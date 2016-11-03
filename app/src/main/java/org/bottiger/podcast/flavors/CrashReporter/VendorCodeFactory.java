package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Application;
import android.support.annotation.NonNull;

import org.bottiger.podcast.flavors.MessagingService.InstanceIDService;

/**
 * Created by apl on 26-02-2015.
 */
public class VendorCodeFactory {

    public static void startReporter(@NonNull Application argApplication) {
        VendorCrashReporter.init(argApplication);
    }

    public static void startFirebase(@NonNull Application argApplication) {
        InstanceIDService.init(argApplication.getApplicationContext());
    }

}
