package org.bottiger.podcast.webservices.datastore.gpodder;

import retrofit.Callback;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by apl on 23-05-2015.
 */
public interface GPodderAPI {

    /**
     * Auth string:
     * String basicAuth = "Basic " + Base64.encodeToString(String.format("%s:%s", "your_user_name", "your_password").getBytes(), Base64.NO_WRAP);
     *
     * From: http://stackoverflow.com/questions/21370620/retrofit-post-request-w-basic-http-authentication-cannot-retry-streamed-http
    */
    @POST("/api/2/auth/{username]/login.json")
    void login(@Path("user") String user, @Header("Authorization") String authorization, Callback<String> callback);

    @POST("/api/2/auth/{username]/logout.json")
    void logout(@Path("user") String user);

}
