package org.bottiger.podcast.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.bottiger.podcast.R;

/**
 * Created by aplb on 27-12-2015.
 */
public class PlaybackSpeed {

    public static final float DEFAULT = 1.0f;
    public static final float UNDEFINED = -1.0f;

    public static final float sSpeedMaximum = 2.0f;
    public static final float sSpeedMinimum = 0.5f;
    public static final float sSpeedIncrements = 0.1f;

    public static int toMap(float argSpeed) {
        int playbackSpeedHash = 0;
        for (float i = sSpeedMinimum*10; i < argSpeed+sSpeedIncrements/2; i = (i + sSpeedIncrements*10)) {
            playbackSpeedHash++;
        }

        return playbackSpeedHash;
    }

    public static float toSpeed(int argMap) {
        float speed = sSpeedMinimum + argMap*sSpeedIncrements;

        return speed;
    }

    public static String toString(float argSpeed) {
        return argSpeed + "X";
    }

    public static float globalPlaybackSpeed(@NonNull Context argContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
        String key = argContext.getResources().getString(R.string.soundwaves_player_playback_speed_key);

        if (!prefs.contains(key)) {
            return UNDEFINED;
        }

        int timesTen = prefs.getInt(key, (int)(DEFAULT*10));
        return ((float)timesTen)/10;
    }

    public static void setGlobalPlaybackSpeed(@NonNull Context argContext, float argSpeed) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
        String key = argContext.getResources().getString(R.string.soundwaves_player_playback_speed_key);
        int storedSpeed = Math.round(argSpeed * 10);
        prefs.edit().putInt(key, storedSpeed).apply();
    }
}
