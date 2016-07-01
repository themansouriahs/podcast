package org.bottiger.podcast;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.activities.pastelog.LogSubmitActivity;

/**
 * Created by apl on 13-02-2015.
 */
public class SoundWavesPreferenceFragment extends PreferenceFragment {

    private static final String TAG = "SWPreferenceFrag";

    public static final String CURRENT_VERSION = "pref_current_version";

    private Context mContext;

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        String key = getResources().getString(R.string.pref_submit_debug_logs_key);
        Preference submitDebugLog = this.findPreference(key);
        submitDebugLog.setOnPreferenceClickListener(new SubmitDebugLogListener());
        submitDebugLog.setSummary(getVersion(getActivity()));
    }

    @Override
    public void onAttach (Context argContext) {
        mContext = argContext;
        super.onAttach(argContext);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Preference prefVersion = findPreference(CURRENT_VERSION);
        String packageName = getActivity().getApplicationContext().getPackageName();
        String version = "Unknown";
        try {
            version = getActivity().getPackageManager().getPackageInfo(packageName, 0).versionName;
            version += "." + getActivity().getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        version += "-" + BuildConfig.FLAVOR + "-" + BuildConfig.BUILD_TYPE;

        prefVersion.setSummary(version);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // On Android 4.3 onAttach() is not called
        if (mContext == null) {
            mContext = getActivity();
        }

        return view;
    }

    private class SubmitDebugLogListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Intent intent = new Intent(getActivity(), LogSubmitActivity.class);
            startActivity(intent);
            return true;
        }
    }

    private @NonNull
    String getVersion(@Nullable Context context) {
        try {
            if (context == null) return "";

            String app     = context.getString(R.string.app_name);
            String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;

            return String.format("%s %s", app, version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
            return context.getString(R.string.app_name);
        }
    }
}
