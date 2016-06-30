package org.bottiger.podcast.player.exoplayer;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by aplb on 19-06-2016.
 */
public class FileCacheInterceptor implements Interceptor {

    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        // Make Request to server
        Response response = chain.proceed(request);

        // Handle response
        //response.body().bytes();

        return response;
    }
}
