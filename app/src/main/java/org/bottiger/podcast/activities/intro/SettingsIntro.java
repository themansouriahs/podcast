package org.bottiger.podcast.activities.intro;

import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.BoolRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.VideoView;

import com.heinrichreimersoftware.materialintro.app.SlideFragment;

import org.bottiger.podcast.R;
import org.bottiger.podcast.databinding.IntroSettingsFragmentBinding;
import org.bottiger.podcast.utils.PreferenceHelper;

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
