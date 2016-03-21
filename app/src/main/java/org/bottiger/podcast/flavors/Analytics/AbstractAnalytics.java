package org.bottiger.podcast.flavors.Analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.bottiger.podcast.BuildConfig;

/**
 * Created by apl on 23-03-2015.
 */
public abstract class AbstractAnalytics {

    private Context mContext;
    private SharedPreferences mPrefs;

    public AbstractAnalytics(@NonNull Context argContext) {
        mContext = argContext;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext.getApplicationContext());
    }

    public boolean doShare() {
        return mPrefs.getBoolean("pref_anonymous_feedback", shareAsDefault());
    }


    // Only share if private mode is off
    private boolean shareAsDefault() {
        return !BuildConfig.PRIVATE_MODE;
    }
}
