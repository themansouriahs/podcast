package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.views.PlayPauseImageView;

/**
 * Created by aplb on 15-09-2015.
 */
public class DialogOpenVideoExternally extends DialogFragment {

    // FIXME: Hack
    private static IEpisode sEpisode;

    /**
     * Create a new instance of DialogOpenVideoExternally
     */
    public static DialogOpenVideoExternally newInstance(@NonNull IEpisode argEpisode) {
        sEpisode = argEpisode;
        return new DialogOpenVideoExternally();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Activity activity = getActivity();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final String openExternallyKey = getResources().getString(R.string.pref_open_video_externally_key);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.open_video_externally_title)
                .setMessage(R.string.open_video_externally_body)
                .setPositiveButton(R.string.open_video_externally_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Open another player with the video
                        persistChoice(prefs, openExternallyKey, true);
                        PlayPauseImageView.openVideoExternally(sEpisode, activity);
                        sEpisode = null;
                    }
                })
                .setNegativeButton(R.string.open_video_externally_reject, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Play the episode
                        persistChoice(prefs, openExternallyKey, false);
                        SoundWaves.sBoundPlayerService.toggle(sEpisode);
                        sEpisode = null;
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    private static void persistChoice(SharedPreferences argPrefs, String argKey, boolean argDoOpen) {
        argPrefs.edit().putBoolean(argKey, argDoOpen).commit();
    }

}
