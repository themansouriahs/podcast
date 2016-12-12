package org.bottiger.podcast.webservices.directories.audiosearch;

import org.bottiger.podcast.webservices.directories.audiosearch.types.Chart;
import org.bottiger.podcast.webservices.directories.audiosearch.types.Show;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by aplb on 12-12-2016.
 */

interface AudioSearchEndpoint {

    // https://www.audiosear.ch/developer#!/shows/get_search_shows_query
    @GET("api/search/shows/{query}")
    Call<SearchResult> search(@Path("query") String argQuery,
                              @Query("sort_by") String argSortBy,
                              @Query("sort_order") String argSortOrder,
                              @Query("size") String argSize,
                              @Query("from") String argFrom,
                              @Query("page") String argPage);

    @GET("api/shows/{id}")
    Call<Show> show(@Path("id") long argId);

    @GET("api/chart_daily")
    Call<Chart> chart_daily(@Query("limit") int argLimit,
                            @Query("country") String argCountry);
}
