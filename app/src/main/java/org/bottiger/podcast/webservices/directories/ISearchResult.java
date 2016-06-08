package org.bottiger.podcast.webservices.directories;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;

/**
 * Created by apl on 13-04-2015.
 */
public interface ISearchResult {

    @NonNull
    String getSearchQuery();

    @NonNull
    Iterable<ISubscription> getResults();
}
