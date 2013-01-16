package info.bottiger.podcast;

import android.app.Application;

public class SoundWaves extends Application {
	
	public final String bugSenseAPIKey = "";
	public final String googleReaderConsumerKey = "";

	public String getBugSenseAPIKey() {
		return this.bugSenseAPIKey;
	}
	
	public String getGoogleReaderConsumerKey() {
		return this.googleReaderConsumerKey;
	}
}
