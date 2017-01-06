package org.bottiger.podcast.cloud;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by aplb on 21-03-2016.
 */
public class EventLogger {

    private static final String TAG = "EventLogger";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SUBSCRIBE_PODCAST, UNSUBSCRIBE_PODCAST, LISTEN_EPISODE, LISTEN_PODCAST, START_APP})
    public @interface EventType {}
    public static final int SUBSCRIBE_PODCAST = 0;
    public static final int UNSUBSCRIBE_PODCAST = 1;
    public static final int LISTEN_EPISODE = 2;
    public static final int LISTEN_PODCAST = 3;
    public static final int START_APP = 4;

    private static final OkHttpClient client = new OkHttpClient();

    public static void postEvent(@NonNull Context argContent, @EventType int argType,
                                 @Nullable Integer value1,
                                 @Nullable String value2,
                                 @Nullable String value3) {

        IAnalytics analytics = SoundWaves.getAppContext(argContent).getAnalystics();

        if (analytics == null || !analytics.doShare()) {
            return;
        }

       String android_id = Settings.Secure.getString(argContent.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // https://api.soundwavesapp.com/eventlog
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("api.soundwavesapp.com")
                .addPathSegment("eventlog")
                .addQueryParameter("phone_id", android_id)
                .addQueryParameter("event", String.valueOf(argType))
                .addQueryParameter("value1", value1 == null ? "0" : value1.toString())
                .addQueryParameter("value2", value2)
                .addQueryParameter("value3", value3)
                .build();

        if (argType == SUBSCRIBE_PODCAST) {
            analytics.logFeed(value2, true);
        }

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "succes");
                response.body().close();
            }
        });
    }

}
