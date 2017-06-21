package org.bottiger.podcast.utils.okhttp;

import android.content.Context;
import android.net.TrafficStats;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.net.TrafficStatsCompat;

import org.bottiger.podcast.utils.HttpUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by aplb on 06-04-2016.
 */
/* This interceptor adds a custom User-Agent. */
public class UserAgentInterceptor implements Interceptor {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NONE, SUBSCRIPTION_REFRESH, GPODDER, GENERIC_SEARCH, AUDIOSEARCH, DOWNLOAD_EPISODE})
    public @interface TrafficTag {}
    public static final int NONE                    = 1001;
    public static final int SUBSCRIPTION_REFRESH    = 1002;
    public static final int GPODDER                 = 1003;
    public static final int GENERIC_SEARCH          = 1004;
    public static final int AUDIOSEARCH             = 1005;
    public static final int DOWNLOAD_EPISODE        = 1006;

    private static final String USER_AGENT_HEADER = "User-Agent"; // NoI18N

    private final String userAgent;

    @TrafficTag
    private final int trafficTag;

    public UserAgentInterceptor(@NonNull Context argContext, @TrafficTag int argTag) {
        this.userAgent = HttpUtils.getUserAgent(argContext);
        this.trafficTag = argTag;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
                .header(USER_AGENT_HEADER, userAgent)
                .build();

        TrafficStats.setThreadStatsTag(trafficTag);
        return chain.proceed(requestWithUserAgent);
    }

}