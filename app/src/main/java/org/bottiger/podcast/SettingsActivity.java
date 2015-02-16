/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bottiger.podcast;

import org.bottiger.podcast.service.PodcastService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;

public class SettingsActivity extends ToolbarActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String DARK_THEME_KEY = "pref_dark_theme";
	public static final String KEY_FAST_FORWARD = "pref_player_forward_amount";
	public static final String KEY_REWIND = "pref_player_backward_amount";
	public static final String CLOUD_SUPPORT = "pref_cloud support";

	public static final String HAPI_PREFS_FILE_NAME = "org.bottiger.podcast_preferences";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// This is bizar, but works:
		// http://stackoverflow.com/questions/11751498/how-to-change-preferenceactivity-theme
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		//setTheme(ThemeHelper.getTheme(prefs));
        setTheme(R.style.SoundWaves_PreferenceActivity_Light);

        //setContentView();

        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SoundWavesPreferenceFragment()).commit();

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected int getLayout() {
        return R.layout.preference_activity;
    }

    @Override
    protected int getStatusBarHeight() {
        return 0;
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(DARK_THEME_KEY)) {
			recreate();
			return;
		}
	}

}
