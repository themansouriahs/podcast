package org.bottiger.podcast.webservices.datastore.gpodder;

import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.GSubscription;
import org.bottiger.podcast.webservices.datastore.gpodder.datatypes.SubscriptionChanges;

import java.util.List;
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

}
