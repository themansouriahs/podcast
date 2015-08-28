package org.bottiger.podcast.webservices.datastore.gpodder;

/**
 * Created by apl on 23-05-2015.
 */

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

/**
 * Interceptor used to authorize requests.
 */
public class ApiRequestInterceptor implements RequestInterceptor {

    private String mUsername;
    private String mPassword;

    public static String cookie;

    public ApiRequestInterceptor(@NonNull String argUsername, @NonNull String argPassword) {
        mUsername = argUsername;
        mPassword = argPassword;
    }

    @Override
    public void intercept(RequestInterceptor.RequestFacade requestFacade) {

        if (!TextUtils.isEmpty(cookie)) {
            requestFacade.addHeader("Cookie", cookie);
        } else {
            final String authorizationValue = encodeCredentialsForBasicAuthorization();
            requestFacade.addHeader("Authorization", authorizationValue);
        }
    }

    private String encodeCredentialsForBasicAuthorization() {
        final String userAndPassword = mUsername + ":" + mPassword;
        return "Basic " + Base64.encodeToString(userAndPassword.getBytes(), Base64.NO_WRAP);
    }
}
