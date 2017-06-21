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

    public static OkHttpClient.Builder getNewDefaultOkHttpClientBuilder(@NonNull Context argContext, @UserAgentInterceptor.TrafficTag int argTag) {
        return getNewDefaultOkHttpClientBuilder(argContext, 30, argTag);
    }

    public static OkHttpClient.Builder getBackgroundOkHttpClientBuilder(@NonNull Context argContext, @UserAgentInterceptor.TrafficTag int argTag) {
        return getNewDefaultOkHttpClientBuilder(argContext, 120, argTag);
    }

    private static OkHttpClient.Builder getNewDefaultOkHttpClientBuilder(@NonNull Context argContext, int argTimeOut, @UserAgentInterceptor.TrafficTag int argTag) {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.interceptors().add(new UserAgentInterceptor(argContext, argTag));
        okHttpBuilder.connectTimeout(argTimeOut, TimeUnit.SECONDS)
                .writeTimeout(argTimeOut, TimeUnit.SECONDS)
                .readTimeout(argTimeOut, TimeUnit.SECONDS);

        return okHttpBuilder;
    }
}
