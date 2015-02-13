package org.bottiger.podcast;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import org.bottiger.podcast.service.PodcastService;

/**
 * Created by apl on 13-02-2015.
 */
public class SoundWavesPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //addPreferencesFromResource(R.xml.settings);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        /*
        service = startService(new Intent(this, PodcastService.class));

        Intent bindIntent = new Intent(this, PodcastService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        */
    }
}
