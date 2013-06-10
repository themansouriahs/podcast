package org.bottiger.podcast;

import android.app.Application;
import android.graphics.Bitmap.CompressFormat;

public class SoundWaves extends Application {

	// package name
	public static final String packageName = "org.bottiger.soundwaves";

	// Bugsense API Key.
	// https://www.bugsense.com/dashboard/project/
	public final String bugSenseAPIKey = "11b0ee02";

	// zubhium API Key.
	public final String zubhiumAPIKey = "2168f55feb0b29864c927eae05b78c";

	// Google API Key: https://code.google.com/apis/console/
	// Get fingerprint like this:
	// keytool -exportcert -alias androiddebugkey -keystore
	// .android/debug.keystore -list -v
	public final String googleReaderConsumerKey = "13654253758-cc5plpi2m80d9g9utkp6jm1t9pebi7r4.apps.googleusercontent.com";

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
