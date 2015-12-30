package org.bottiger.podcast.webservices.datastore.gpodder;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Arvid on 8/23/2015.
 */
public class GPodderUtils {

    private static final String baseUrl = "https://gpodder.net"; // NoI18N
    private static final String baseDeviceName = "SoundWaves gPodder sync"; // NoI18N

    public static final String serverNameKey = "gpodder_server"; // NoI18N
    public static final String deviceNameKey = "gpodder_device"; // NoI18N

    public static String getServer(@Nullable SharedPreferences argSharedPreferences) {
        if (argSharedPreferences == null)
            return baseUrl;

        return argSharedPreferences.getString(serverNameKey, baseUrl);
    }

    public static String getDeviceCaption(@NonNull Context argContext) {
        SharedPreferences argSharedPreferences = PreferenceManager.getDefaultSharedPreferences(argContext);
        return argSharedPreferences.getString(deviceNameKey, getDeviceID());
    }

    private static String getDeviceID() {
        return android.os.Build.MODEL.replaceAll("\\s", "");
    }

    public static String getDeviceType() {
        return "Android device"; // NoI18N
    }
}
