package org.bottiger.podcast;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.bottiger.podcast.flavors.Analytics.AnalyticsFactory;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.CrashReporterFactory;

//Acra debugging
@ReportsCrashes(
        // not used
        formUri = "https://acra.bottiger.org/acra-soundwaves/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = ApplicationConfiguration.formUriBasicAuthLogin, // optional
        formUriBasicAuthPassword = ApplicationConfiguration.formUriBasicAuthPassword, // optional
        disableSSLCertValidation = true,
        mode = ReportingInteractionMode.DIALOG,
        forceCloseDialogAfterToast=true,
        httpMethod = org.acra.sender.HttpSender.Method.POST,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        socketTimeout = 10000)

public class SoundWaves extends Application {

    private static Context context;
    // Global constants
    private Boolean mFirstRun = null;

    public static IAnalytics sAnalytics;


    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        if (!BuildConfig.DEBUG) {
            CrashReporterFactory.startReporter(this);
        }

        sAnalytics = AnalyticsFactory.getAnalytics(this);
        sAnalytics.startTracking();

        context = getApplicationContext();

        firstRun(context);
    }

    public static Context getAppContext() {
        return context;
    }

    private void firstRun(@NonNull Context argContext) {
        SharedPreferences sharedPref = argContext.getSharedPreferences(ApplicationConfiguration.packageName, Context.MODE_PRIVATE);
        String key = getString(R.string.preference_first_run_key);
        boolean firstRun = sharedPref.getBoolean(key, true);
        if (firstRun) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(key, false);
            editor.commit();
        }
        mFirstRun = firstRun;
    }

    public boolean IsFirstRun() {
        if (mFirstRun == null) {
            throw new IllegalStateException("First run can not be null!");
        }

        return mFirstRun.booleanValue();
    }
}
