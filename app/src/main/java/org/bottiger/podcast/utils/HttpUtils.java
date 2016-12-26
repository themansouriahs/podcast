package org.bottiger.podcast.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.okhttp.UserAgentInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by aplb on 07-06-2016.
 */

public class HttpUtils {

    @NonNull
    public static String getUserAgent(@NonNull Context argContext) {
        return argContext.getResources().getString(R.string.app_name) + "-" + BuildConfig.VERSION_NAME;
    }

    public static OkHttpClient.Builder getNewDefaultOkHttpClientBuilder(@NonNull Context argContext) {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.interceptors().add(new UserAgentInterceptor(argContext));
        okHttpBuilder.connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS);

        return okHttpBuilder;
    }
}
