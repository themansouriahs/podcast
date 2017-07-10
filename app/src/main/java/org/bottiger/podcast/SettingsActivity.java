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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;

import org.bottiger.podcast.utils.UIUtils;

public class SettingsActivity extends ToolbarActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static String DARK_THEME_KEY = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// This is bizar, but works:
		// http://stackoverflow.com/questions/11751498/how-to-change-preferenceactivity-theme
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this.getApplicationContext());

        DARK_THEME_KEY = getResources().getString(R.string.pref_dark_theme_key);
        String defaultValue = getResources().getString(R.string.pref_theme_default);
        String lightTheme = prefs.getString(DARK_THEME_KEY, defaultValue);
        int lightThemeInt = Integer.parseInt(lightTheme);
        //int style = lightTheme || System.currentTimeMillis() > 0 ? R.style.PreferenceTheme : R.style.PreferenceThemeDark;

        //setTheme(style);

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
        // Unregister the listener whenever a key changes
        getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected int getLayout() {
        return R.layout.preference_activity;
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (!TextUtils.isEmpty(DARK_THEME_KEY) && key.equals(DARK_THEME_KEY)) {
            UIUtils.setTheme(this);
            recreate();
			return;
		}
	}

    @Override
    protected boolean transparentNavigationBar() {
        return false;
    }

}
