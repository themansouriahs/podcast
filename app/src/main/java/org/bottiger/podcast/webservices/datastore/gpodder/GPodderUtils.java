package org.bottiger.podcast.webservices.datastore.gpodder;

/**
 * Created by Arvid on 8/23/2015.
 */
public class GPodderUtils {

    public static String getDeviceID() {
        return android.os.Build.MODEL.replaceAll("\\s", "");
    }

    public static String getDeviceCaption() {
        return "SoundWaves gPodder sync"; // NoI18N
    }

    public static String getDeviceType() {
        return "Android device"; // NoI18N
    }
}
