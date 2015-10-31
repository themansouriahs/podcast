package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.PlayerService;

import java.util.Date;

/**
 * Created by apl on 24-03-2015.
 */
public class DialogBulkDownload {

    private static final int PLAYLIST_EPISODE_DOWNLOAD_COUNT = 10;

    private enum ACTION { DAY, WEEK, MONTH, PLAYLIST, NONE};
    private ACTION mAction = ACTION.NONE;

    private Context mContext;
    private Playlist mPlaylist;


    public Dialog onCreateDialog(@NonNull final Activity argActivity, @NonNull Playlist argPlaylist) {

        mContext = argActivity.getApplicationContext();
        mPlaylist = argPlaylist;

        AlertDialog.Builder builder = new AlertDialog.Builder(argActivity);
        // Get the layout inflater
        LayoutInflater inflater = argActivity.getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_bulk_download, null);

        TextView textView = (TextView) view.findViewById(R.id.dialog_bulk_download_text);
        RadioButton dayButton = (RadioButton) view.findViewById(R.id.radio_day);
        RadioButton weekButton = (RadioButton) view.findViewById(R.id.radio_week);
        RadioButton monthButton = (RadioButton) view.findViewById(R.id.radio_month);
        RadioButton playlistButton = (RadioButton) view.findViewById(R.id.radio_playlist);

        Resources res = mContext.getResources();
        String formattedString = res.getQuantityString(R.plurals.bulk_download_playlist, PLAYLIST_EPISODE_DOWNLOAD_COUNT, PLAYLIST_EPISODE_DOWNLOAD_COUNT);
        playlistButton.setText(formattedString);

        dayButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(ACTION.DAY, isChecked);
            }
        });

        weekButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(ACTION.WEEK, isChecked);
            }
        });

        monthButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(ACTION.MONTH, isChecked);
            }
        });

        playlistButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(ACTION.PLAYLIST, isChecked);
            }
        });

        String bulkDownloadInstructions = res.getString(R.string.bulk_download_instructions);
        textView.setText(bulkDownloadInstructions);

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                queueEpisodesForDownload(mAction);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //DialogOPML.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

    private void onRadioButtonClicked(ACTION argACTION, boolean isChecked) {
        if (isChecked) {
            mAction = argACTION;
        }
    }

    private void queueEpisodesForDownload(ACTION argACTION) {
        if (argACTION == ACTION.NONE) {
            return;
        }

        SoundWavesDownloadManager downloadManager = SoundWaves.getDownloadManager();

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        long bytesToKeep = SoundWavesDownloadManager.bytesToKeep(sharedPreferences);

        IEpisode episode;
        Date date;

        int downloadCandidatesCount = Math.min(mPlaylist.size(), PLAYLIST_EPISODE_DOWNLOAD_COUNT);

        for (int i = 0; i < downloadCandidatesCount; i++) {
            episode = mPlaylist.getItem(i);
            date = episode.getDateTime();
            if (date != null) {
                boolean validDate = validDate(mAction, date);

                if (validDate && downloadManager != null) {
                    downloadManager.addItemToQueue(episode, SoundWavesDownloadManager.LAST);
                }
            }
        }

        /*
        if (downloadManager != null)
        downloadManager.startDownload();
        */

    }

    private boolean validDate(ACTION argAction, Date argDate) {
        long ageMs = 0;

        long nowMs = System.currentTimeMillis();

        long dayMs = 24 * 60 * 60 * 1000; // ms in a day
        long weekMs = dayMs * 7;
        long monthMs = dayMs * 30;

        if (argAction == ACTION.NONE) {
            return false;
        } else if (argAction == ACTION.PLAYLIST) {
            return true;
        } else if (argAction == ACTION.DAY) {
            ageMs = dayMs;
        } else if (argAction == ACTION.WEEK) {
            ageMs = weekMs;
        } else if (argAction == ACTION.MONTH) {
            ageMs = monthMs;
        }

        long thresholdMs = nowMs-ageMs;
        Date thresholdDate =new Date(thresholdMs);

        boolean newerThan = argDate.after(thresholdDate);
        return newerThan;

    }
}
