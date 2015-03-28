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
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.Downloader.EpisodeDownloadManager;
import org.bottiger.podcast.utils.OPMLImportExport;

import java.util.Date;

/**
 * Created by apl on 24-03-2015.
 */
public class DialogBulkDownload {

    private enum ACTION { DAY, WEEK, MONTH, NONE};
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

        Resources res = argActivity.getResources();
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

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        long bytesToKeep = EpisodeDownloadManager.bytesToKeep(sharedPreferences);

        FeedItem episode;
        Date date;
        int playlistSize = mPlaylist.size();
        for (int i = 0; i < playlistSize; i++) {
            episode = mPlaylist.getItem(i);
            date = episode.getDateTime();
            boolean doDownload = validDate(mAction, date);

            if (doDownload) {
                EpisodeDownloadManager.addItemToQueue(episode, EpisodeDownloadManager.QUEUE_POSITION.LAST);
            }
        }

        EpisodeDownloadManager.startDownload(mContext);

    }

    private boolean validDate(ACTION argAction, Date argDate) {
        long ageMs = 0;

        long nowMs = System.currentTimeMillis();

        long dayMs = 24 * 60 * 60 * 1000; // ms in a day
        long weekMs = dayMs * 7;
        long monthMs = dayMs * 30;

        if (argAction == ACTION.NONE) {
            return false;
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
