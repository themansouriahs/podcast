package org.bottiger.podcast.webservices.directories;

import java.util.List;

/**
 * Created by apl on 13-04-2015.
 */
public interface ISearchParameters {

    public void addKeyword(String argKeyword);
    public List<String> getKeywords();
}
