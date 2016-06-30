package org.bottiger.podcast.service;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.ISubscription;

/**
 * Created by apl on 20-11-2014.
 */
public interface IDownloadCompleteCallback {

    void complete(boolean argSucces, @NonNull ISubscription argSubscription);

}
