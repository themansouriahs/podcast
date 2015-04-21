package org.bottiger.podcast.provider.converter;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;

import java.net.URL;

/**
 * Created by apl on 22-04-2015.
 */
public class EpisodeConverter {

    public static SlimEpisode toSlim(@NonNull FeedItem argEpisode) {

        String title = argEpisode.getTitle();
        String description = argEpisode.getDescription();
        URL url = argEpisode.getUrl();

        SlimEpisode slimEpisode = new SlimEpisode(title, url, description);
        return slimEpisode;
    }
}
