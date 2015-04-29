package org.bottiger.podcast.service;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;

import java.util.List;

/**
 * Created by apl on 20-11-2014.
 */
public interface IDownloadCompleteCallback {

    public void complete(boolean argSucces, ISubscription argSubscription);

}
