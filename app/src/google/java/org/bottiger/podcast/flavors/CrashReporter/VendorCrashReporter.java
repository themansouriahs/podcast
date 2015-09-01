package org.bottiger.podcast.flavors.CrashReporter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;
import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.SoundWaves;

import io.fabric.sdk.android.Fabric;

/**
 * Created by apl on 26-02-2015.
 *
 * Only used on the Play store
 */
public class VendorCrashReporter {

    public static void init(@NonNull Application argApplication) {
        Fabric.with(argApplication, new Crashlytics());

        /*
        ACRAConfiguration config = ACRA.getNewDefaultConfig(argApplication);
        try {
            config.setMode(ReportingInteractionMode.SILENT);
        } catch (ACRAConfigurationException e) {
            e.printStackTrace();
        } finally {
            ACRA.init(argApplication, config);
        }
        */
    }
	
	public static void report(@NonNull String argKey, @NonNull String argValue) {
        /*
        if (!BuildConfig.DEBUG)
            ACRA.getErrorReporter().putCustomData(argKey, argValue);
            */
    }

    public static void handleException(@NonNull Throwable argException) {
        /*
        if (!BuildConfig.DEBUG)
            ACRA.getErrorReporter().handleException(argException);
            */
    }

}
