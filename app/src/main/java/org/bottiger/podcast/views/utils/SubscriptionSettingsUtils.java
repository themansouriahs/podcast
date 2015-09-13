package org.bottiger.podcast.views.utils;

import android.content.ContentResolver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.CompoundButton;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.Subscription;

/**
 * Created by aplb on 13-09-2015.
 */
public class SubscriptionSettingsUtils {

    @NonNull View mLayout;
    @NonNull Subscription mSubscription;

    @NonNull
    SwitchCompat mShowDescription;
    @NonNull
    SwitchCompat mAddNewToPlaylist;
    @NonNull
    SwitchCompat mAutoDownload;
    @NonNull
    SwitchCompat mDeleteAfterPlayback;
    @NonNull
    SwitchCompat mListOldestFirst;

    @Nullable OnSettingsChangedListener mShowDescriptionListener;
    @Nullable OnSettingsChangedListener mAddNewToPlaylistListener;
    @Nullable OnSettingsChangedListener mAutoDownloadListener;
    @Nullable OnSettingsChangedListener mDeleteAfterPlaybackListener;
    @Nullable OnSettingsChangedListener mListOldestFirstListener;


    public interface OnSettingsChangedListener {
        void OnSettingsChanged(boolean isChecked);
    }

    public SubscriptionSettingsUtils(@NonNull View argLayout, @NonNull Subscription argSubscription) {
        mLayout = argLayout;
        mSubscription = argSubscription;

        mShowDescription = (SwitchCompat) mLayout.findViewById(R.id.feed_description_switch);
        mAddNewToPlaylist = (SwitchCompat) mLayout.findViewById(R.id.feed_add_to_playlist_switch);
        mAutoDownload = (SwitchCompat)mLayout.findViewById(R.id.feed_auto_download_switch);
        mDeleteAfterPlayback = (SwitchCompat) mLayout.findViewById(R.id.feed_auto_delete_switch);
        mListOldestFirst = (SwitchCompat) mLayout.findViewById(R.id.feed_oldest_first_switch);

        final ContentResolver contentResolver = mLayout.getContext().getContentResolver();

        mShowDescription.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSubscription.setShowDescription(isChecked);
                OnSwitchChangedHandler(isChecked, contentResolver, mShowDescriptionListener);
            }
        });

        mAddNewToPlaylist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSubscription.setAddNewToPlaylist(isChecked);
                OnSwitchChangedHandler(isChecked, contentResolver, mAddNewToPlaylistListener);
            }
        });

        mAutoDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSubscription.setDownloadNew(isChecked);
                OnSwitchChangedHandler(isChecked, contentResolver, mAutoDownloadListener);
            }
        });

        mDeleteAfterPlayback.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSubscription.setDeleteWhenListened(isChecked);
                OnSwitchChangedHandler(isChecked, contentResolver, mDeleteAfterPlaybackListener);
            }
        });

        mListOldestFirst.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSubscription.setShowDescription(isChecked);
                OnSwitchChangedHandler(isChecked, contentResolver, mListOldestFirstListener);
            }
        });

        mShowDescription.setChecked(mSubscription.isShowDescription());
        mAddNewToPlaylist.setChecked(mSubscription.isAddNewToPlaylist());
        mAutoDownload.setChecked(mSubscription.isDownloadNew());
        mDeleteAfterPlayback.setChecked(mSubscription.isDeleteWhenListened());
        mListOldestFirst.setChecked(mSubscription.isListOldestFirst());
    }

    private void OnSwitchChangedHandler(boolean isChecked,
                                        @NonNull ContentResolver argContentResolver,
                                        @Nullable OnSettingsChangedListener argOnSettingsChangedListener) {
        mSubscription.update(argContentResolver);
        if (argOnSettingsChangedListener != null) {
            argOnSettingsChangedListener.OnSettingsChanged(isChecked);
        }
    }

    public void setListOldestFirstListener(@Nullable OnSettingsChangedListener argListOldestFirstListener) {
        this.mListOldestFirstListener = argListOldestFirstListener;
    }

    public void setDeleteAfterPlaybackListener(@Nullable OnSettingsChangedListener argDeleteAfterPlaybackListener) {
        this.mDeleteAfterPlaybackListener = argDeleteAfterPlaybackListener;
    }

    public void setAutoDownloadListener(@Nullable OnSettingsChangedListener argAutoDownloadListener) {
        this.mAutoDownloadListener = argAutoDownloadListener;
    }

    public void setAddNewToPlaylistListener(@Nullable OnSettingsChangedListener argAddNewToPlaylistListener) {
        this.mAddNewToPlaylistListener = argAddNewToPlaylistListener;
    }

    public void setShowDescriptionListener(@Nullable OnSettingsChangedListener argShowDescriptionListener) {
        this.mShowDescriptionListener = argShowDescriptionListener;
    }
}
