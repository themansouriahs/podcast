package org.bottiger.podcast.activities.intro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.heinrichreimersoftware.materialintro.app.SlideFragment;

import org.bottiger.podcast.R;
import org.bottiger.podcast.databinding.IntroSettingsFragmentBinding;

/**
 * Created by aplb on 04-12-2016.
 */

public class SettingsIntro extends SlideFragment {

    private static final String TAG = SettingsIntro.class.getSimpleName();

    public static SettingsIntro newInstance() {
        SettingsIntro fragment = new SettingsIntro();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        IntroSettingsFragmentBinding binding = IntroSettingsFragmentBinding.inflate(getLayoutInflater(savedInstanceState));

        CheckboxPresenter autoDownloadView = new CheckboxPresenter(getContext(), R.string.pref_download_on_update_key, R.bool.pref_download_on_update_default);
        CheckboxPresenter wifiOnlyView = new CheckboxPresenter(getContext(), R.string.pref_download_only_wifi_key, R.bool.pref_download_only_wifi_default);

        binding.setAutodownload(autoDownloadView);
        binding.setWifionly(wifiOnlyView);

        return binding.getRoot();
    }
}
