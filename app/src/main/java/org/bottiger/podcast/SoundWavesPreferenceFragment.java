package org.bottiger.podcast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import org.bottiger.podcast.service.PodcastService;

/**
 * Created by apl on 13-02-2015.
 */
public class SoundWavesPreferenceFragment extends PreferenceFragment {

    private Activity mActivity;

    public static final String CURRENT_VERSION = "pref_current_version";

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onAttach (Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Preference prefVersion = findPreference(CURRENT_VERSION);
        String packageName = mActivity.getApplicationContext().getPackageName();
        String version = "Unknown";
        try {
            version = mActivity.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        version += "-" + BuildConfig.FLAVOR + "-" + BuildConfig.BUILD_TYPE;

        prefVersion.setSummary(version);
    }
}
