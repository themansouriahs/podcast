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
import org.bottiger.podcast.utils.ThemeHelper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String DARK_THEME_KEY = "pref_dark_theme";
	public static final String CLOUD_SUPPORT = "pref_cloud support";

	public static final String HAPI_PREFS_FILE_NAME = "org.bottiger.podcast_preferences";
	private PodcastService serviceBinder = null;
	ComponentName service = null;

	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceBinder = ((PodcastService.PodcastBinder) service)
					.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			serviceBinder = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// This is bizar, but works:
		// http://stackoverflow.com/questions/11751498/how-to-change-preferenceactivity-theme
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		//setTheme(ThemeHelper.getTheme(prefs));
        setTheme(R.style.SoundWaves_PreferenceActivity_Light);

		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		service = startService(new Intent(this, PodcastService.class));

		Intent bindIntent = new Intent(this, PodcastService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(DARK_THEME_KEY)) {
			recreate();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	    // Set up a listener whenever a key changes
	    getPreferenceScreen().getSharedPreferences()
	            .registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	protected void onPause() {

		super.onPause();
		if (serviceBinder != null)
			serviceBinder.updateSetting();
		
	    // Unregister the listener whenever a key changes
	    getPreferenceScreen().getSharedPreferences()
	            .unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
		// stopService(new Intent(this, service.getClass()));
	}

}
