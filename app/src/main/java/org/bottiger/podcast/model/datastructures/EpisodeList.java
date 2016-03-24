package org.bottiger.podcast.model.datastructures;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;

import org.bottiger.podcast.provider.IEpisode;

import java.util.LinkedList;

import javax.annotation.Nonnegative;

/**
 * Created by aplb on 20-01-2016.
 */
public class EpisodeList<T> extends SortedList<T> {

    private EpisodeFilter mFilter = new EpisodeFilter();

    public EpisodeList(Class<T> klass, Callback<T> callback) {
        super(klass, callback);
    }

    public EpisodeList(Class<T> klass, Callback<T> callback, int initialCapacity) {
        super(klass, callback, initialCapacity);
    }

    @NonNull
    public LinkedList<IEpisode> getUnfilteredList() {
        LinkedList<IEpisode> list =  new LinkedList<>();
        IEpisode episode;
        for (int i = 0; i < size(); i++) {
            episode = (IEpisode) get(i);
            list.add(episode);
        }

        return list;
    }

    @NonNull
    public LinkedList<IEpisode> getFilteredList() {
        LinkedList<IEpisode> list =  new LinkedList<>();
        IEpisode episode;
        for (int i = 0; i < size(); i++) {
            episode = (IEpisode) get(i);
            if (mFilter.match(episode)) {
                list.add(episode);
            }
        }

        return list;
    }

    @Nullable
    public IEpisode getNewest() {
        long currentDate = -1;
        IEpisode episode = null;
        for (int i = 0; i < size(); i++) {
            episode = (IEpisode) get(i);
            long episodeDate = episode.getCreatedAt().getTime();
            if (episodeDate > currentDate) {
                currentDate = episodeDate;
            }
        }

        return episode;
    }

    public void setFilter(@Nullable EpisodeFilter argFilter) {
        mFilter = argFilter;
    }

    @NonNull
    public EpisodeFilter getFilter() {
        return mFilter;
    }
}
