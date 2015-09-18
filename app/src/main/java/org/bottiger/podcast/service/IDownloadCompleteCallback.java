package org.bottiger.podcast.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;

import java.util.List;

/**
 * Created by apl on 20-11-2014.
 */
public interface IDownloadCompleteCallback {

    void complete(boolean argSucces, @NonNull ISubscription argSubscription);

}
