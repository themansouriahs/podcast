package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import org.acra.ACRA;
import org.bottiger.podcast.SoundWaves;

/**
 * Created by apl on 26-02-2015.
 *
 * Only used on the Play store
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
