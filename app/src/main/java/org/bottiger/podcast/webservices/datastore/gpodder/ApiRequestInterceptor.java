package org.bottiger.podcast.webservices.datastore.gpodder;

/**
 * Created by apl on 23-05-2015.
 */

import android.text.TextUtils;
import android.util.Base64;

import retrofit.RequestInterceptor;

/**
 * Interceptor used to authorize requests.
 */
public class ApiRequestInterceptor implements RequestInterceptor {

    private String cookie;

    @Override
    public void intercept(RequestInterceptor.RequestFacade requestFacade) {

        if (!TextUtils.isEmpty(cookie)) {
            requestFacade.addHeader("Set-Cookie", cookie);
        }
    }

    public void setUser(String user) {
        this.cookie = user;
    }
}
