package org.bottiger.podcast.webservices.datastore.gpodder;

import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GActionsList;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GDevice;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GDeviceUpdates;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GEpisode;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GEpisodeAction;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GSetting;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GSubscription;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GTag;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.SubscriptionChanges;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.UpdatedUrls;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by apl on 23-05-2015.
 */
public interface IGPodderAPI {

    /**
     * Auth string:
     * String basicAuth = "Basic " + Base64.encodeToString(String.format("%s:%s", "your_user_name", "your_password").getBytes(), Base64.NO_WRAP);
     *
     * From: http://stackoverflow.com/questions/21370620/retrofit-post-request-w-basic-http-authentication-cannot-retry-streamed-http
    */
    @POST("/api/2/auth/{username}/login.json")
    //void login(@Path("user") String user, @Header("Authorization") String authorization, Callback<String> callback);
    Call<ResponseBody> login(@Path("username") String user);

    @POST("/api/2/auth/{username}/logout.json")
    void logout(@Path("username") String user);


    /*
    Subscriptions API: http://gpoddernet.readthedocs.org/en/latest/api/reference/subscriptions.html
     */

    // Get Subscriptions of Device
    @GET("/subscriptions/{username}/{deviceid}.json")
    Call<String[]> getDeviceSubscriptions(@Path("username") String user, @Path("deviceid") String device);

    // Get Subscriptions
    @GET("/subscriptions/{username}.json")
    Call<List<GSubscription>> getSubscriptions(@Path("username") String user);

    // Upload Subscriptions of Device
    @PUT("/subscriptions/{username}/{deviceid}.json")
    Call<ResponseBody> uploadDeviceSubscriptions(@Body List<String> subscriptions, @Path("username") String user, @Path("deviceid") String device);

    // Upload Subscription Changes
    @POST("/api/2/subscriptions/{username}/{deviceid}.json")
    Call<UpdatedUrls> uploadDeviceSubscriptionsChanges(@Body SubscriptionChanges subscriptionChanges, @Path("username") String user, @Path("deviceid") String device);

    // Get Subscription Changes
    @GET("/api/2/subscriptions/{username}/{deviceid}.json")
    Call<SubscriptionChanges> getDeviceSubscriptionsChanges(@Path("username") String user, @Path("deviceid") String device, @Query("since") long since);

    /*
        Episode actions: http://gpoddernet.readthedocs.org/en/latest/api/reference/events.html
     */

    // Upload Episode Actions
    @POST("/api/2/episodes/{username}.json")
    Call<UpdatedUrls> uploadEpisodeActions(@Body GEpisodeAction[] actions,
                                            @Path("username") String username);

    // Get Episode Actions
    @GET("/api/2/episodes/{username}.json")
    Call<GActionsList> getEpisodeActions(@Path("username") String user,
                           @Query("podcast") String podcast,
                           @Query("device") String device,
                           @Query("since") long since,
                           @Query("aggregated ") boolean aggregated);

    /*
        Directory API: http://gpoddernet.readthedocs.org/en/latest/api/reference/directory.html
     */

    // Retrieve Top Tags
    @GET("/api/2/tags/{count}.json")
    Call<GTag> getTopTags(@Path("count") int amount);

    // Retrieve Podcasts for Tag
    @GET("/api/2/tag/{tag}/{count}.json")
    Call<List<GSubscription>> getPodcastForTag(@Path("tag") String tag, @Path("count") int amount);

    // Retrieve Podcast Data
    @GET("/api/2/data/podcast.json")
    Call<GSubscription> getPodcastData(@Query("url") String podcastURL);

    // Retrieve Episode Data
    @GET("/api/2/data/episode.json")
    Callback<GEpisode> getEpisodeData(@Query("url") String episodeURL);

    // Podcast Toplist
    @GET("/toplist/{number}.json")
    Call<List<GSubscription>> getPodcastToplist(@Path("number") int amount);

    // Podcast Search
    @GET("/search.json")
    Call<List<GSubscription>> search(@Query("q") String query, @Query("scale_logo") String scale_logo);
    @GET("/search.json")
    Call<List<GSubscription>> search(@Query("q") String query);

    /*
        Suggestions API: http://gpoddernet.readthedocs.org/en/latest/api/reference/suggestions.html
     */

    // Retrieve Suggested Podcasts
    @GET("/suggestions/{number}.json")
    Call<List<GSubscription>> suggestedPodcasts(@Path("number") int amount);

    /*
        Device API: http://gpoddernet.readthedocs.org/en/latest/api/reference/devices.html
     */

    // Update Device Data
    @POST("/api/2/devices/{username}/{deviceid}.json")
    Call<ResponseBody> updateDeviceData(@Path("username") String username, @Path("deviceid") String deviceid, @Body GDevice device);

    // List devices
    @POST("/api/2/devices/{username}.json")
    Call<List<GDevice>> getDeviceList(@Path("username") String username);

    // Get device updates
    @GET("/api/2/updates/{username}/{deviceid}.json")
    Call<GDeviceUpdates> getDeviceUpdate(@Path("username") String username,
                         @Path("deviceid") String deviceid,
                         @Query("since") long since,
                         @Query("include_actions") boolean includeActions);

    /*
        Save Settings: http://gpoddernet.readthedocs.org/en/latest/api/reference/settings.html
     */

    // Save settings
    @POST("/api/2/settings/{username}/{scope}.json")
    Call saveSettings(@Path("username") String username,
                      @Path("scope") String scope,
                      @Body GSetting settings,
                      @Query("podcast") String podcast,
                      @Query("device") String device,
                      @Query("episode") String episode);

    // Get settings
    @GET("/api/2/settings/{username}/{scope}.json")
    Call<Map<String,String>> getSettings(@Path("username") String username,
                     @Path("scope") String scope,
                     @Query("podcast") String podcast,
                     @Query("device") String device,
                     @Query("episode") String episode);

    /*
        Favorites API: http://gpoddernet.readthedocs.org/en/latest/api/reference/favorites.html
     */

    // Get Favorite Episodes
    @GET("/api/2/favorites/{username}.json")
    Call<List<GEpisode>> getFavorites(@Path("username") String username);

    /*
        Device Synchronization API: http://gpoddernet.readthedocs.org/en/latest/api/reference/sync.html
     */

    // Get Sync Status
    @GET("/api/2/sync-devices/{username}.json")
    Call getSyncStatus(@Path("username") String username);
}
