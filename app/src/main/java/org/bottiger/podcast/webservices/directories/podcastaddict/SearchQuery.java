package org.bottiger.podcast.webservices.directories.podcastaddict;

import com.google.gson.annotations.SerializedName;

/**
 * Created by aplb on 06-12-2016.
 */

public class SearchQuery {


    //Call<Response> search(@Part String query, @Part String language, @Part String dateFilter, @Part String filterExplicit);

    @SerializedName("query")
    String mQuery;

    @SerializedName("language")
    String mlanguage = "en";

    @SerializedName("dateFilter")
    String mDateFilter = "0";

    @SerializedName("filterExplicit")
    boolean mFilterExplicit = false;

    SearchQuery(String argQuery) {
        mQuery = argQuery;
    }


}
