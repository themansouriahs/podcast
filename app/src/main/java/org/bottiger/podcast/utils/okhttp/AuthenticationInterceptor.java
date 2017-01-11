package org.bottiger.podcast.utils.okhttp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bottiger.podcast.utils.HttpUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by aplb on 11-01-2017.
 */

public class AuthenticationInterceptor implements Interceptor {

    public static final String AUTHENTICATION_HEADER = "Authorization";
    private String mCredential ;

    public void setCredenticals(@Nullable String argCredential) {
        mCredential = argCredential;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestWithUserAgent = originalRequest.newBuilder();

        String credentials = mCredential;
        if (credentials != null) {
            requestWithUserAgent.header(AUTHENTICATION_HEADER, credentials);
        }
        return chain.proceed(requestWithUserAgent.build());
    }
}
