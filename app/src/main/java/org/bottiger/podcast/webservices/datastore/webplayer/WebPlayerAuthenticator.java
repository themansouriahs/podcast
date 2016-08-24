package org.bottiger.podcast.webservices.datastore.webplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.IEpisode;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by aplb on 20-08-2016.
 */

public class WebPlayerAuthenticator {

    public static final String EPISODE_URL = "episode_url";
    public static final String EPISODE_cover = "episode_cover";
    public static final String EPISODE_TITLE = "episode_title";
    public static final String EPISODE_SUBTITLE = "episode_subtitle";
    public static final String EPISODE_WEBSITE = "episode_website";
    public static final String EPISODE_DESCRIPTION = "episode_description";
    public static final String EPISODE_POSITION = "episode_position";

    public static void authenticate(@Nullable String argScannedQRValue, @NonNull Context argContext, @Nullable IEpisode argEpisode) {
        if (argScannedQRValue == null)
            return;

        String[] values = argScannedQRValue.split("-");
        final String cookie = values[1];
        String nonce = values[0];

        String url = "https://soundwaves-bottiger.appspot.com/auth?4350967=" + nonce + "&jsessionid=" + cookie;
        Log.d("dsfdsfsd", url);
        Log.d("dsfdsfsd---", argScannedQRValue);
        Log.d("dsfdsfsd---", cookie);
        Log.d("dsfdsfsd---", nonce);

        //final OkHttpClient client = new OkHttpClient();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Interceptor.Chain chain) throws IOException {
                        final Request original = chain.request();

                        final Request authorized = original.newBuilder()
                                .addHeader("Cookie", "JSESSIONID=" + cookie)
                                .build();

                        return chain.proceed(authorized);
                    }
                })
                .build();

        FormBody.Builder formBody = new FormBody.Builder();

        if (argEpisode != null) {

            String offset_seconds = String.valueOf(argEpisode.getOffset()/1000);

            formBody.add(EPISODE_URL, argEpisode.getURL())
                    .add(EPISODE_cover, argEpisode.getArtwork(argContext))
                    .add(EPISODE_TITLE, argEpisode.getTitle())
                    .add(EPISODE_SUBTITLE, argEpisode.getTitle())
                    .add(EPISODE_WEBSITE, argEpisode.getSubscription(argContext).getURLString())
                    .add(EPISODE_DESCRIPTION, argEpisode.getDescription())
                    .add(EPISODE_POSITION, offset_seconds);
        }

        formBody.add("4350967", nonce)
                .build();

        RequestBody requestBody = formBody.build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("fail", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
    }

}
