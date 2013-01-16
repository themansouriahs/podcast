package info.bottiger.podcast;

import android.app.Application;

public class SoundWaves extends Application {
	
	// Bugsense API Key.
	// https://www.bugsense.com/dashboard/project/
	public final String bugSenseAPIKey = "";
	
	// Google API Key: https://code.google.com/apis/console/
	// Get fingerprint like this:
	// keytool -exportcert -alias androiddebugkey -keystore .android/debug.keystore -list -v
	public final String googleReaderConsumerKey = "";

	public String getBugSenseAPIKey() {
		return this.bugSenseAPIKey;
	}
	
	public String getGoogleReaderConsumerKey() {
		return this.googleReaderConsumerKey;
	}
}
