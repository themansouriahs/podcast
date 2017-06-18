package org.bottiger.podcast.activities.downloadmanager;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.service.DownloadService;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;

import java.util.List;

/**
 * Created by aplb on 18-06-2017.
 */

public class DownloadManagerViewModel extends AndroidViewModel {

    private final MutableLiveData<List<QueueEpisode>> downloadingEpisodesList;

    public DownloadManagerViewModel(Application application) {
        super(application);
        MutableLiveData<List<QueueEpisode>> queue = DownloadService.getLiveQueue();
        downloadingEpisodesList = queue;
    }

    public MutableLiveData<List<QueueEpisode>> getData() {
        return downloadingEpisodesList;
    }

}
