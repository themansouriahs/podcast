package org.bottiger.podcast.model.events;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.DownloadStatus;

/**
 * Created by apl on 25-05-2015.
 */
public class DownloadProgress {

    private int progress = 0;
    private DownloadStatus status = DownloadStatus.NOTHING;
    private IEpisode episode = null;

    public DownloadProgress() {
    }

    public DownloadProgress(@NonNull IEpisode argEpisode, DownloadStatus argStatus, int argProgress) {
        status = argStatus;
        progress = argProgress;
        episode = argEpisode;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    @Nullable
    public IEpisode getEpisode() {
        return episode;
    }
}
