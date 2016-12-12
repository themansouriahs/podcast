package org.bottiger.podcast.webservices.directories.audiosearch.authorization;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by aplb on 12-12-2016.
 */
public interface AuthorizationService {

    @Headers("Content-Type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    @POST("oauth/token")
    Call<AuthResult> getAccessToken(@Field("grant_type")  String grantType, @Header("Authorization") String authorizationSignature);

}
