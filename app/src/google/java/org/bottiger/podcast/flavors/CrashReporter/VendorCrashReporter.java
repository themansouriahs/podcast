package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.MetaData;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.SoundWaves;


/**
 * Created by apl on 26-02-2015.
 *
 * Only used on the Play store
 */
public class VendorCrashReporter {

    public static void init(@NonNull Application argApplication) {
        Bugsnag.init(argApplication);
    }
	
	public static void report(@NonNull String argKey, @NonNull String argValue) {
        MetaData metaData = new MetaData();
        metaData.addToTab("User", argKey, argValue);
        Bugsnag.notify(new Exception("Non-fatal: " + argKey), metaData);
    }

    public static void handleException(@NonNull Throwable argException) {
        Bugsnag.notify(argException);
    }

    public static void handleException(@NonNull Throwable argException, @NonNull String[] argKey, @NonNull String[] argValue) {
        MetaData metaData = new MetaData();

        for(int i = 0; i < argKey.length; i++) {
            metaData.addToTab("Extra", argKey[i], argValue[i]);
        }

        Bugsnag.notify(argException, metaData);
    }

}
