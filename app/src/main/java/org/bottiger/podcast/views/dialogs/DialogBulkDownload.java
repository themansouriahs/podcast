package org.bottiger.podcast.views.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.utils.StorageUtils;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by apl on 24-03-2015.
 */
public class DialogBulkDownload {

    private static final int PLAYLIST_EPISODE_DOWNLOAD_COUNT = 20;
    private static final int AVERAGE_EPISODE_SIZE_MB = 20;

    private enum ACTION { DAY, WEEK, MONTH, TOP, ALL, NONE};
    private ACTION mAction = ACTION.TOP;

    private Context mContext;
    private Playlist mPlaylist;
    @Nullable
    private Subscription mSubscription = null;

    private LinkedList<RadioButton> mButtons = new LinkedList<>();


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
        final TextView textViewWarning = (TextView) view.findViewById(R.id.dialog_bulk_download_warning);
        final EditText editText = (EditText) view.findViewById(R.id.radio_top_episodes_edittext);
        final RadioButton dayButton = (RadioButton) view.findViewById(R.id.radio_day);
        final RadioButton weekButton = (RadioButton) view.findViewById(R.id.radio_week);
        final RadioButton monthButton = (RadioButton) view.findViewById(R.id.radio_month);
        final RadioButton allButton = (RadioButton) view.findViewById(R.id.radio_all);
        final RadioButton topButton = (RadioButton) view.findViewById(R.id.radio_top_episodes);

        mButtons.add(dayButton);
        mButtons.add(weekButton);
        mButtons.add(monthButton);
        mButtons.add(allButton);
        mButtons.add(topButton);

        // Set initial state
        setWarningLabel(textViewWarning, getEpisodeCountValue(editText));
        topButton.setChecked(true);

        editText.setText(String.valueOf(PLAYLIST_EPISODE_DOWNLOAD_COUNT));

        dayButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(dayButton, ACTION.DAY, isChecked, textViewWarning, editText);
            }
        });

        weekButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(weekButton, ACTION.WEEK, isChecked, textViewWarning, editText);
            }
        });

        monthButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(monthButton, ACTION.MONTH, isChecked, textViewWarning, editText);
            }
        });

        topButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(topButton, ACTION.TOP, isChecked, textViewWarning, editText);
            }
        });

        allButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DialogBulkDownload.this.onRadioButtonClicked(allButton, ACTION.ALL, isChecked, textViewWarning, editText);
            }
        });

        String bulkDownloadInstructions = mContext.getResources().getString(R.string.bulk_download_instructions);
        textView.setText(bulkDownloadInstructions);

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                int count = getEpisodeCountValue(editText);
                queueEpisodesForDownload(mAction, count);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        return builder.create();
    }

    private void onRadioButtonClicked(CompoundButton argCompoundButton,
                                      ACTION argACTION,
                                      boolean isChecked,
                                      @NonNull TextView argWarningView,
                                      @NonNull EditText argEditText) {
        if (isChecked) {
            mAction = argACTION;

            for (int i = 0; i < mButtons.size(); i++) {
                CompoundButton button = mButtons.get(i);
                button.setChecked(argCompoundButton.equals(button));
            }
        }

        setWarningLabel(argWarningView, getEpisodeCountValue(argEditText));
    }

    private void queueEpisodesForDownload(ACTION argACTION, int argCount) {
        if (argACTION == ACTION.NONE) {
            return;
        }

        SoundWavesDownloadManager downloadManager = SoundWaves.getAppContext(mContext).getDownloadManager();

        IEpisode episode;
        Date date;

        List<IEpisode> episodes = mSubscription != null ? mSubscription.getEpisodes().getFilteredList() : mPlaylist.getPlaylist();

        int downloadCandidatesCount = Math.min(episodes.size(), argCount);

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
        } else if (argAction == ACTION.TOP) {
            return true;
        } else if (argAction == ACTION.DAY) {
            ageMs = dayMs;
        } else if (argAction == ACTION.WEEK) {
            ageMs = weekMs;
        } else if (argAction == ACTION.MONTH) {
            ageMs = monthMs;
        } else if (argAction == ACTION.ALL) {
            return true;
        }

        long thresholdMs = nowMs-ageMs;
        Date thresholdDate =new Date(thresholdMs);

        return argDate.after(thresholdDate);
    }

    private int getEpisodeCountValue(@Nullable EditText argEditText) {
        if (argEditText == null)
            return PLAYLIST_EPISODE_DOWNLOAD_COUNT;

        int count;
        try {
            count = Integer.parseInt(String.valueOf(argEditText.getText()));
        } catch (Exception e) {
            return 0;
        }

        return count;
    }

    private void setWarningLabel(@NonNull TextView argTextViewWarning, int argCount) {
        int storageGB = StorageUtils.getStorageCapacityGB(mContext);
        boolean showWarningGB = storageGB > 0;
        boolean showWarningDueToAction = mAction == ACTION.MONTH || mAction == ACTION.ALL;
        showWarningDueToAction = showWarningDueToAction || (mAction == ACTION.TOP && AVERAGE_EPISODE_SIZE_MB*argCount>storageGB*1000);
        boolean showWarning = showWarningGB && showWarningDueToAction;

        if (showWarning) {
            String textViewWarningText = String.format(mContext.getResources().getString(R.string.bulk_download_capacaity_warning), storageGB);
            argTextViewWarning.setText(textViewWarningText);
        }
        argTextViewWarning.setVisibility(showWarning ? View.VISIBLE : View.GONE);
    }
}
