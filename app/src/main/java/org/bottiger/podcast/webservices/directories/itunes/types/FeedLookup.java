package org.bottiger.podcast.webservices.directories.itunes.types;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aplb on 04-01-2017.
 */

public class FeedLookup {

    @SerializedName("resultCount")
    @Expose
    private int resultCount;

    @SerializedName("results")
    @Expose
    private List<LookupResult> results = new ArrayList<>();

    public List<LookupResult> getResults() {
        return results;
    }

}
