package org.bottiger.podcast.webservices.directories;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.Subscription;

/**
 * Created by apl on 13-04-2015.
 */
public interface ISearchResult {

    @NonNull
    public Iterable<Subscription> getResults();
}
