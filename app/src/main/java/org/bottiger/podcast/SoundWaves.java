package org.bottiger.podcast;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.bottiger.podcast.flavors.Analytics;

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
        formUriBasicAuthLogin = "soundwaves", // optional
        formUriBasicAuthPassword = "qAizvWWLZuUtKclMnQTNoExZevGayn", // optional
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
    public static final String ANALYTICS_ID = "UA-59611883-1";

    // Global constants
    private Boolean mFirstRun = null;


    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        if (!BuildConfig.DEBUG)
            ACRA.init(this);

        if (BuildConfig.FLAVOR.toString() == "google") {
            Analytics analytics = new Analytics(this);
            analytics.startTracking();
        }

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
