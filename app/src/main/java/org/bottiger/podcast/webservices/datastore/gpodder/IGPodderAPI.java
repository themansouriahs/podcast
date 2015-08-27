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
import java.util.Set;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created by apl on 23-05-2015.
 */
public interface IGPodderAPI {

    public static final String baseUrl = "https://gpodder.net";

    /**
     * Auth string:
     * String basicAuth = "Basic " + Base64.encodeToString(String.format("%s:%s", "your_user_name", "your_password").getBytes(), Base64.NO_WRAP);
     *
     * From: http://stackoverflow.com/questions/21370620/retrofit-post-request-w-basic-http-authentication-cannot-retry-streamed-http
    */
    @POST("/api/2/auth/{username}/login.json")
    //void login(@Path("user") String user, @Header("Authorization") String authorization, Callback<String> callback);
    void login(@Path("username") String user, Callback<String> callback);

    @POST("/api/2/auth/{username}/logout.json")
    void logout(@Path("username") String user);


    /*
    Subscriptions API: http://gpoddernet.readthedocs.org/en/latest/api/reference/subscriptions.html
     */

    // Get Subscriptions of Device
    @GET("/subscriptions/{username}/{deviceid}.json")
    void getDeviceSubscriptions(@Path("username") String user, @Path("deviceid") String device, Callback<List<GSubscription>> callback);

    // Get Subscriptions
    @GET("/subscriptions/{username}.json")
    void getSubscriptions(@Path("username") String user, Callback<List<GSubscription>> callback);

    // Upload Subscriptions of Device
    @PUT("/subscriptions/{username}/{deviceid}.json")
    void uploadDeviceSubscriptions(@Body List<String> subscriptions, @Path("username") String user, @Path("deviceid") String device, Callback<String> callback);

    // Upload Subscription Changes
    @POST("/api/2/subscriptions/{username}/{deviceid}.json")
    void uploadDeviceSubscriptionsChanges(@Body SubscriptionChanges subscriptionChanges, @Path("username") String user, @Path("deviceid") String device, Callback<String> callback);

    // Get Subscription Changes
    @GET("/api/2/subscriptions/{username}/{deviceid}.json")
    void getDeviceSubscriptionsChanges(@Path("username") String user, @Path("deviceid") String device, @Query("since") long since, Callback<SubscriptionChanges> callback);

    /*
        Episode actions: http://gpoddernet.readthedocs.org/en/latest/api/reference/events.html
     */

    // Upload Episode Actions
    @POST("/api/2/episodes/{username}.json")
    void uploadEpisodeActions(@Body List<GEpisodeAction> actions,
                              @Path("username") String username,
                              Callback<UpdatedUrls> callback);

    // Get Episode Actions
    @GET("/api/2/episodes/{username}.json")
    void getEpisodeActions(@Path("username") String user,
                           @Query("podcast") String podcast,
                           @Query("device") String device,
                           @Query("since") long since,
                           @Query("aggregated ") boolean aggregated,
                           Callback<GActionsList> callback);

    /*
        Directory API: http://gpoddernet.readthedocs.org/en/latest/api/reference/directory.html
     */

    // Retrieve Top Tags
    @GET("/api/2/tags/{count}.json")
    void getTopTags(@Path("count") int amount, Callback<GTag> callback);

    // Retrieve Podcasts for Tag
    @GET("/api/2/tag/{tag}/{count}.json")
    void getPodcastForTag(@Path("tag") String tag, @Path("count") int amount, Callback<List<GSubscription>> callback);

    // Retrieve Podcast Data
    @GET("/api/2/data/podcast.json")
    void getPodcastData(@Query("url") String podcastURL, Callback<GSubscription> callback);

    // Retrieve Episode Data
    @GET("/api/2/data/episode.json")
    void getEpisodeData(@Query("url") String episodeURL, Callback<GEpisode> callback);

    // Podcast Toplist¶
    @GET("/toplist/{number}.json")
    void getPodcastToplist(@Path("count") int amount, Callback<List<GSubscription>> callback);

    // Podcast Search
    @GET("/search.json")
    void search(@Query("q") String query, @Query("scale_logo") String scale_logo, Callback<List<GSubscription>> callback);

    /*
        Suggestions API: http://gpoddernet.readthedocs.org/en/latest/api/reference/suggestions.html
     */

    // Retrieve Suggested Podcasts
    @GET("/suggestions/{number}.json")
    void suggestedPodcasts(@Path("number") int amount, Callback<List<GSubscription>> callback);

    /*
        Device API: http://gpoddernet.readthedocs.org/en/latest/api/reference/devices.html
     */

    // Update Device Data
    @POST("/api/2/devices/{username}/{deviceid}.json")
    void updateDeviceData(@Path("username") String username, @Path("deviceid") String deviceid, @Body GDevice device);

    // List devices
    @POST("/api/2/devices/{username}.json")
    void getDeviceList(@Path("username") String username, Callback<List<GDevice>> callback);

    // Get device updates
    @GET("/api/2/updates/{username}/{deviceid}.json")
    void getDeviceUpdate(@Path("username") String username,
                         @Path("deviceid") String deviceid,
                         @Query("since") long since,
                         @Query("include_actions") boolean includeActions,
                         Callback<GDeviceUpdates> callback);

    /*
        Save Settings: http://gpoddernet.readthedocs.org/en/latest/api/reference/settings.html
     */

    // Save settings
    @POST("/api/2/settings/{username}/{scope}.json")
    void saveSettings(@Path("username") String username,
                      @Path("scope") String scope,
                      @Body GSetting settings,
                      @Query("podcast") String podcast,
                      @Query("device") String device,
                      @Query("episode") String episode);

    // Get settings
    @GET("/api/2/settings/{username}/{scope}.json")
    void getSettings(@Path("username") String username,
                     @Path("scope") String scope,
                     @Query("podcast") String podcast,
                     @Query("device") String device,
                     @Query("episode") String episode,
                     Callback<Map<String,String>> callback);

    /*
        Favorites API: http://gpoddernet.readthedocs.org/en/latest/api/reference/favorites.html
     */

    // Get Favorite Episodes
    @GET("/api/2/favorites/{username}.json")
    void getFavorites(@Path("username") String username, Callback<List<GEpisode>> callback);

    /*
        Device Synchronization API: http://gpoddernet.readthedocs.org/en/latest/api/reference/sync.html
     */

    // Get Sync Status
    @GET("/api/2/sync-devices/{username}.json")
    void getSyncStatus(@Path("username") String username);
}
