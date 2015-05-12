package org.bottiger.podcast.provider.converter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;

import java.net.URL;

/**
 * Created by apl on 22-04-2015.
 */
public class EpisodeConverter {

    @Nullable
    public static SlimEpisode toSlim(@NonNull FeedItem argEpisode) {

        String title = argEpisode.getTitle();
        String description = argEpisode.getDescription();
        URL url = argEpisode.getUrl();

        if (title == null || description == null || url == null)
            return null;

        SlimEpisode slimEpisode = new SlimEpisode(title, url, description);

        return slimEpisode;
    }
}
