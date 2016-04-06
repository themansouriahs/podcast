package org.bottiger.podcast.utils.okhttp;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by aplb on 06-04-2016.
 */
/* This interceptor adds a custom User-Agent. */
public class UserAgentInterceptor implements Interceptor {

    private final String userAgent ;

    public UserAgentInterceptor(@NonNull Context argContext) {
        this.userAgent = argContext.getResources().getString(R.string.app_name) + "-" + BuildConfig.VERSION_NAME;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build();
        return chain.proceed(requestWithUserAgent);
    }
}