package org.bottiger.podcast.flavors.MessagingService;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.common.WebPlayerShared;
import org.bottiger.podcast.provider.IEpisode;

import java.util.Map;

/**
 * Created by aplb on 03-11-2016.
 */

public class MessagingServiceShared {

    public static void updateEpisode(@NonNull Context argContext, Map<String, String> map) {
        int timeMs = Integer.parseInt(map.get(WebPlayerShared.TIME_KEY));
        String url = map.get(WebPlayerShared.URL_KEY);

        IEpisode episode = SoundWaves.getAppContext(argContext).getLibraryInstance().getEpisode(url);
        if (episode != null) {
            episode.setOffset(timeMs);
        }
    }

}
