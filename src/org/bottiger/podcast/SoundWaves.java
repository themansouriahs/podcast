package org.bottiger.podcast;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

// Acra debugging
@ReportsCrashes(
	      formKey = "", // This is required for backward compatibility but not used
	      formUri = "http://www.backendofyourchoice.com/reportpath"
	  )

public class SoundWaves extends Application {

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
