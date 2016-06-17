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
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.PlayerService;

import java.util.Date;
import java.util.List;

/**
 * Created by apl on 24-03-2015.
 */
public class DialogBulkDownload {

    private static final int PLAYLIST_EPISODE_DOWNLOAD_COUNT = 10;

    private enum ACTION { DAY, WEEK, MONTH, PLAYLIST, NONE};
    private ACTION mAction = ACTION.NONE;

    private Context mContext;
    private Playlist mPlaylist;
    @Nullable
    private Subscription mSubscription = null;


    public Dialog onCreateDialog(@NonNull final Activity argActivity, @NonNull Playlist argPlaylist) {
        return onCreateDialog(argActivity, argPlaylist, null);
    }

    public Dialog onCreateDialog(@NonNull final Activity argActivity,
                                 @NonNull Playlist argPlaylist,
                                 @Nullable Subscription argSubscription) {
        return createDialog(argActivity, argPlaylist, argSubscription);
    }

    private Dialog createDialog(@NonNull final Activity argActivity,
                                             @NonNull Playlist argPlaylist,
                                             @Nullable Subscription argSubscription) {
        mContext = argActivity.getApplicationContext();
        mPlaylist = argPlaylist;
        mSubscription = argSubscription;

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

        SoundWavesDownloadManager downloadManager = SoundWaves.getAppContext(mContext).getDownloadManager();

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        long bytesToKeep = SoundWavesDownloadManager.bytesToKeep(sharedPreferences, mContext.getResources());

        IEpisode episode;
        Date date;

        List<FeedItem> episodes = mSubscription != null ? mSubscription.getEpisodes().getFilteredList() : mPlaylist.getPlaylist();

        int downloadCandidatesCount = Math.min(episodes.size(), PLAYLIST_EPISODE_DOWNLOAD_COUNT);

        for (int i = 0; i < downloadCandidatesCount; i++) {
            episode = episodes.get(i);
            date = episode.getDateTime();
            if (date != null) {
                boolean validDate = validDate(mAction, date);

                if (validDate) {
                    downloadManager.addItemToQueue(episode, SoundWavesDownloadManager.LAST);
                }
            }
        }

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

        return argDate.after(thresholdDate);

    }
}
