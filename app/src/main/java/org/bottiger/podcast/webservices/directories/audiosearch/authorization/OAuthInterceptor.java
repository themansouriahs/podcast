package org.bottiger.podcast.webservices.directories.audiosearch.authorization;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by aplb on 12-12-2016.
 */

public class OAuthInterceptor implements Interceptor {

    private String AccessToken;

    public OAuthInterceptor(String accessToken){
        this.AccessToken = accessToken;
    }
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request newRequest;
        newRequest = request.newBuilder()
                .addHeader("Authorization", "Bearer " + AccessToken)
                .build();
        return chain.proceed(newRequest);
    }
}
