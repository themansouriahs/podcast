package org.bottiger.podcast.flavors.MediaCast;

import android.support.v7.media.MediaRouter;

/**
 * Created by apl on 11-04-2015.
 */
public interface IMediaRouteStateListener {

    public void onStateChanged(IMediaCast castProvider);
}
