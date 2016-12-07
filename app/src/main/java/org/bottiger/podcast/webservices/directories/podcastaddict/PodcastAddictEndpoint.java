package org.bottiger.podcast.webservices.directories.podcastaddict;

import java.util.List;
import java.util.Map;

import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by aplb on 06-12-2016.
 */

public interface PodcastAddictEndpoint {

    @FormUrlEncoded
    @POST("ws/php/v2.33/searchpodcast.php")
    Call<SearchResult> search(@Field("query") String argQuery,
                                    @Field("language") String argLanguage,
                                    @Field("dateFilter") String argdateFilter,
                                    @Field("filterExplicit") String argfilterExplicit,
                                    @Field("type") String argType);
}