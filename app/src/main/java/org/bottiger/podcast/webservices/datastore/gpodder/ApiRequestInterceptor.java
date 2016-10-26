package org.bottiger.podcast.webservices.datastore.gpodder;

/**
 * Created by apl on 23-05-2015.
 */

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Interceptor used to authorize requests.
 */
class ApiRequestInterceptor implements Interceptor {

    @Nullable private String mUsername;
    @Nullable private String mPassword;

    static String cookie;

    ApiRequestInterceptor(@Nullable  String argUsername, @Nullable String argPassword) {
        mUsername = argUsername;
        mPassword = argPassword;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        Request.Builder requestBuilder = request.newBuilder();

        boolean authenticating = false;
        if (shouldAuthenticate()) {
            if (!TextUtils.isEmpty(cookie)) {
                requestBuilder.addHeader("Cookie", cookie);
            } else {
                final String authorizationValue = encodeCredentialsForBasicAuthorization();
                requestBuilder.addHeader("Authorization", authorizationValue);
                authenticating = true;
            }
        }

        request = requestBuilder.build();
        Response response = chain.proceed(request);

        if (authenticating) {
            Headers headers = response.headers();
            int numHeaders = headers.size();

            String name, value;
            for (int i = 0; i < numHeaders; i++) {
                name = headers.name(i);
                value = headers.value(i);
                if (name.equals("Set-Cookie") && value.startsWith("sessionid")) {
                    ApiRequestInterceptor.cookie = value.split(";")[0];
                }
            }
        }

        return response;
    }

    private String encodeCredentialsForBasicAuthorization() {
        final String userAndPassword = mUsername + ":" + mPassword;
        return "Basic " + Base64.encodeToString(userAndPassword.getBytes(), Base64.NO_WRAP);
    }

    private boolean shouldAuthenticate() {
        return mUsername != null;
    }
}
