package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.playlist.Playlist;

/**
 * Created by Arvid on 8/18/2015.
 */
public class DialogPlaylistFilters {

    private Context mContext;
    private Playlist mPlaylist;

    public Dialog onCreateDialog(@NonNull final Activity argActivity, @NonNull Playlist argPlaylist) {

        mContext = argActivity.getApplicationContext();
        mPlaylist = argPlaylist;

        AlertDialog.Builder builder = new AlertDialog.Builder(argActivity);
// Get the layout inflater
        LayoutInflater inflater = argActivity.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_playlist_filters, null);

        builder.setView(view);
        builder.setPositiveButton(R.string.apply_filters, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //
            }
        });
        builder.setNegativeButton(R.string.reset_filters, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //
            }
        });

        return builder.create();
    }
}
