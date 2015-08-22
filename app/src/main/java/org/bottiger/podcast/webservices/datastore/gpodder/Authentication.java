package org.bottiger.podcast.webservices.datastore.gpodder;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashSet;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.OkClient;
import retrofit.client.Response;

/**
 * Created by apl on 23-05-2015.
 */
public class Authentication {

    private static final String TAG = "Authentication";

    public void authenticate(@NonNull String argUsername, @NonNull String argPassword) {
        ApiRequestInterceptor requestInterceptor = new ApiRequestInterceptor(argUsername, argPassword);

        // Fetch data from: http://gpodder.net/clientconfig.json

        GPodderAPI apiService = new RestAdapter.Builder()
                .setRequestInterceptor(requestInterceptor)
                .setEndpoint(GPodderAPI.baseUrl)
                .setClient(new OkClient()) // The default client didn't handle well responses like 401
                .build()
                .create(GPodderAPI.class);

        apiService.login(argUsername, new Callback<String>() {
            @Override
            public void success(String s, Response response) {
                int numHeaders = response.getHeaders().size();

                for (int i = 0; i < numHeaders; i++) {
                    Header header = response.getHeaders().get(i);
                    if (header.getName().equals("Set-Cookie") && header.getValue().startsWith("sessionid")) {
                        ApiRequestInterceptor.cookie = header.getValue();
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {
                Response response = error.getResponse();

                switch (response.getStatus()) {
                    case 401: {
                        // 401 Unauthorized
                        return;
                    }
                    case 400: {
                        // If the client provides a cookie, but for a different username than the one given
                        return;
                    }
                }

                Log.d(TAG, error.toString());
            }
        });
    }
}

