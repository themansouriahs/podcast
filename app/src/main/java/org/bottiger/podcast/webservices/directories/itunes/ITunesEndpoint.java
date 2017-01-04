package org.bottiger.podcast.webservices.directories.itunes;

import org.bottiger.podcast.webservices.directories.itunes.types.Feed;
import org.bottiger.podcast.webservices.directories.itunes.types.FeedLookup;
import org.bottiger.podcast.webservices.directories.itunes.types.TopList;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by aplb on 04-01-2017.
 */

interface ITunesEndpoint {

    @GET("{country}/rss/toppodcasts/limit={limit}/explicit=true/json")
    Call<TopList> chart(@Path("limit") int argLimit, @Path("country") String argCountry);

    @GET("/lookup")
    Call<FeedLookup> lookup(@Query("id") String argId);

}
