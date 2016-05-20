package org.bottiger.podcast.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;
import android.support.annotation.BoolRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.bottiger.podcast.R;

/**
 * Created by apl on 09-04-2015.
 */
public class PreferenceHelper implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Activity mActivity;
    private String mPrefKey;

    public void setOrientation(@NonNull final Activity argActivity, @NonNull SharedPreferences argSharedPreferences) {
        mActivity = argActivity;
        mPrefKey = argActivity.getResources().getString(R.string.pref_screen_rotation_key);
        Boolean allowRotation = argSharedPreferences.getBoolean(mPrefKey, false);

        setScreenOrientation(argActivity, allowRotation);

        argSharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    private void setScreenOrientation(@NonNull Activity argActivity, @NonNull Boolean argAllowRotation) {
        if (argAllowRotation) {
            argActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            argActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mPrefKey == key) {
            Boolean allowRotation = sharedPreferences.getBoolean(mPrefKey, false);
            setScreenOrientation(mActivity, allowRotation);
        }
    }

    public static boolean getBooleanPreferenceValue(@NonNull Context argContext,
                                             @StringRes int argKeyId,
                                             @BoolRes int argDefaultId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
        String key = argContext.getResources().getString(argKeyId);
        boolean defaultValue = argContext.getResources().getBoolean(argDefaultId);

        return prefs.getBoolean(key, defaultValue);
    }

    public static int getIntegerPreferenceValue(@NonNull Context argContext,
                                                    @StringRes int argKeyId,
                                                    @IntegerRes int argDefaultId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
        String key = argContext.getResources().getString(argKeyId);
        Integer defaultValue = argContext.getResources().getInteger(argDefaultId);

        return prefs.getInt(key, defaultValue);
    }

    public static String getStringPreferenceValue(@NonNull Context argContext,
                                                  @StringRes int argKeyId,
                                                  @StringRes int argDefaultId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
        String key = argContext.getResources().getString(argKeyId);
        String defaultValue = argContext.getResources().getString(argDefaultId);

        return prefs.getString(key, defaultValue);
    }
}
