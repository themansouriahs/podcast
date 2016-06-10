package org.bottiger.podcast.provider.base;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.model.events.EpisodeChanged;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;

/**
 * Created by aplb on 02-11-2015.
 */
public abstract class BaseEpisode implements IEpisode {

    private double mProgress = -1;

    public double getProgress() {
        return mProgress;
    }

    @Override
    public boolean isPlaying() {
        return PlayerService.isPlaying() && this.equals(PlayerService.getCurrentItem());
    }

    public void setProgress(double argProgress) {
        if (mProgress == argProgress)
            return;

        mProgress = argProgress;
        notifyPropertyChanged(EpisodeChanged.PROGRESS);
    }

    public void setIsParsing(boolean argIsParsing) {
        setIsParsing(argIsParsing, true);
    }

    protected abstract void notifyPropertyChanged(@EpisodeChanged.Action int argAction);

}
