package org.bottiger.podcast;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.utils.ThemeHelper;

/**
 * Created by apl on 13-02-2015.
 */
public class SoundWavesPreferenceFragment extends PreferenceFragment {

    public static final String CURRENT_VERSION = "pref_current_version";

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

    }

    @Override
    public void onAttach (Context argContext) {
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
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        version += "-" + BuildConfig.FLAVOR + "-" + BuildConfig.BUILD_TYPE;

        prefVersion.setSummary(version);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        // The attributes you want retrieved
        int[] attrs = {R.attr.themeBackground};
        //ThemeHelper helper = new ThemeHelper(mContext);
        //int color = helper.getAttr(R.attr.themeBackground);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int theme = ThemeHelper.getTheme(prefs);

        // Parse MyCustomStyle, using Context.obtainStyledAttributes()
        TypedArray ta = getActivity().obtainStyledAttributes(theme, attrs);

        // Fetch the text from your style like this.
        //String text = ta.getString(2);

        // Fetching the colors defined in your style
        //int textColor = ta.getColor(0, Color.BLACK);
        int backgroundColor = ta.getColor(0, Color.BLACK);

        // Do some logging to see if we have retrieved correct values
        //Log.i("Retrieved text:", text);
        //Log.i("Retrieved textColor as hex:", Integer.toHexString(textColor));
        //Log.i("Retrieved background as hex:", Integer.toHexString(backgroundColor));

        // OH, and don't forget to recycle the TypedArray
        ta.recycle();


        /*
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[] { R.attr.themeBackground };
        int indexOfAttrTextSize = 0;
        TypedArray a = mContext.obtainStyledAttributes(typedValue.data, textSizeAttr);
        int color = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();*/

        view.setBackgroundColor(Color.WHITE);

        return view;
    }
}
