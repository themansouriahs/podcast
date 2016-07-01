package org.bottiger.podcast.webservices.datastore.gpodder;

/**
 * Created by apl on 23-05-2015.
 */

import java.io.IOException;
import java.util.HashSet;

import okhttp3.Interceptor;
import okhttp3.Response;


/**
 * This Interceptor add all received Cookies to the app DefaultPreferences.
 * Your implementation on how to save the Cookies on the Preferences MAY VARY.
 * <p>
 * Created by tsuharesu on 4/1/15.
 */
public class ReceivedCookiesInterceptor implements Interceptor {
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());

        if (!originalResponse.headers("Set-Cookie").isEmpty()) {
            HashSet<String> cookies = new HashSet<>();

            for (String header : originalResponse.headers("Set-Cookie")) {
                cookies.add(header);
                ApiRequestInterceptor.cookie = header;
            }

            /*
            Preferences.getDefaultPreferences().edit()
                    .putStringSet(Preferences.PREF_COOKIES, cookies)
                    .apply();
                    */

        }

        return originalResponse;
    }
}

