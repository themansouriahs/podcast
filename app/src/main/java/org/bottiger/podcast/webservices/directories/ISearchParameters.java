package org.bottiger.podcast.webservices.directories;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Created by apl on 13-04-2015.
 */
public interface ISearchParameters {

    public void addSearchTerm(@NonNull String argKeyword);

    @NonNull
    public List<String> getKeywords();
}
