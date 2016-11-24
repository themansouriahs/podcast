package org.bottiger.podcast.provider.base;

import android.support.annotation.NonNull;

import org.bottiger.podcast.model.events.EpisodeChanged;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.chapter.Chapter;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 02-11-2015.
 */
public abstract class BaseEpisode implements IEpisode {

    private List<Chapter> mChapters;
    private double mProgress = -1;

    public BaseEpisode() {
        mChapters = new LinkedList<>();
    }

    public double getProgress() {
        return mProgress;
    }

    @Override
    public boolean isPlaying() {
        return PlayerService.isPlaying() && this.equals(PlayerService.getCurrentItem());
    }

    @NonNull
    public List<Chapter> getChapters() {
        return mChapters;
    }

    public void setChapters(@NonNull List<Chapter> argChapters) {
        boolean changed = mChapters.size() != argChapters.size();
        mChapters = argChapters;

        if (changed) {
            notifyPropertyChanged(EpisodeChanged.CHANGED);
        }
    }

    public boolean hasChapters() {
        return mChapters.size()>0;
    }

    public void setProgress(double argProgress) {
        if (mProgress == argProgress)
            return;

        mProgress = argProgress;
        notifyPropertyChanged(EpisodeChanged.DOWNLOAD_PROGRESS);
    }

    /**
     * http://docs.oracle.com/javase/6/docs/api/java/lang/String.html#compareTo
     *      %28java.lang.String%29
     * @return True of the current FeedItem is newer than the supplied argument
     */
    public boolean newerThan(IEpisode item) {
        int comparator = this.getDateTime().compareTo(item.getDateTime());
        return comparator > 0;
    }

    public void setIsParsing(boolean argIsParsing) {
        setIsParsing(argIsParsing, true);
    }

    protected abstract void notifyPropertyChanged(@EpisodeChanged.Action int argAction);

    public boolean hasBeenDownloadedOnce() {
        return false;
    }

}
