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

// Acra debugging
/*
@ReportsCrashes(
	      formKey = "", // This is required for backward compatibility but not used
	      formUri = "http://www.backendofyourchoice.com/reportpath"
	  )
*/
//Acra debugging
@ReportsCrashes(formKey = "", // This is required for backward compatibility but
        // not used
        formUri = "https://acra.bottiger.org/acra-soundwaves/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "soundwaves2", // optional
        formUriBasicAuthPassword = "", // optional
        disableSSLCertValidation = true,
        mode = ReportingInteractionMode.SILENT,
        forceCloseDialogAfterToast=true,
        httpMethod = org.acra.sender.HttpSender.Method.POST,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        socketTimeout = 10000)

public class SoundWaves extends Application {

    private static Context context;

	// package name
	public static final String packageName = "org.bottiger.soundwaves";

    // Google Analytics
    public static final String ANALYTICS_ID = "";

    // Amazon Analytics
    public static final String AMAZON_APP_ID                = "";
    public static final String AMAZON_AMAZON_AWS_ACCOUNT    = "";
    public static final String AMAZON_COGNITO_IDENTITY_POOL = "";
    public static final String AMAZON_UNAUTHENTICATED_ARN   = "";
    public static final String AMAZON_AUTHENTICATED_ARN     = "";

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
        SharedPreferences sharedPref = argContext.getSharedPreferences(packageName, Context.MODE_PRIVATE);
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
