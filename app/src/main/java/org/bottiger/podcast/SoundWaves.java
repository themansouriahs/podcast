package org.bottiger.podcast;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.bottiger.soundwaves.Soundwaves;

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
        formUriBasicAuthPassword = "", // optional
        disableSSLCertValidation = true,
        httpMethod = org.acra.sender.HttpSender.Method.POST,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        socketTimeout = 10000)

public class SoundWaves extends Application {

    private static Context context;

	// package name
	public static final String packageName = "org.bottiger.soundwaves";

	// Bugsense API Key.
	// https://www.bugsense.com/dashboard/project/
	public final String bugSenseAPIKey = "";

	// zubhium API Key.
	public final String zubhiumAPIKey = "";

	// Google API Key: https://code.google.com/apis/console/
	// Get fingerprint like this:
	// keytool -exportcert -alias androiddebugkey -keystore
	// .android/debug.keystore -list -v
	public final String googleReaderConsumerKey = "";

    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
	
	public String getZubhiumAPIKey() {
		return this.zubhiumAPIKey;
	}

	public String getBugSenseAPIKey() {
		return this.bugSenseAPIKey;
	}

	public String getGoogleReaderConsumerKey() {
		return this.googleReaderConsumerKey;
	}
}
