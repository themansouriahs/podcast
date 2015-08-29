package org.bottiger.podcast.webservices.datastore.gpodder;

/**
 * Created by apl on 23-05-2015.
 */

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Interceptor used to authorize requests.
 */
public class ApiRequestInterceptor implements Interceptor {

    private String mUsername;
    private String mPassword;

    public static String cookie;

    public ApiRequestInterceptor(@NonNull String argUsername, @NonNull String argPassword) {
        mUsername = argUsername;
        mPassword = argPassword;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();

        if (!TextUtils.isEmpty(cookie)) {
            request.headers().newBuilder().add("Cookie", cookie).build();
        } else {
            final String authorizationValue = encodeCredentialsForBasicAuthorization();
            //requestFacade.addHeader("Authorization", authorizationValue);
            request.headers().newBuilder().add("Authorization", authorizationValue).build();
        }

        Response response = chain.proceed(request);

        return response;
    }

    private String encodeCredentialsForBasicAuthorization() {
        final String userAndPassword = mUsername + ":" + mPassword;
        return "Basic " + Base64.encodeToString(userAndPassword.getBytes(), Base64.NO_WRAP);
    }
}
