package org.bottiger.podcast.webservices.datastore.gpodder;

import retrofit.RestAdapter;
import retrofit.client.OkClient;

/**
 * Created by apl on 23-05-2015.
 */
public class Authentication {

    public void authenticate() {
        ApiRequestInterceptor requestInterceptor = new ApiRequestInterceptor();
        requestInterceptor.setUser("boo"); // I pass the user from my model

        // Fetch data from: http://gpodder.net/clientconfig.json

        GPodderAPI apiService = new RestAdapter.Builder()
                .setRequestInterceptor(requestInterceptor)
                .setEndpoint("http://gpodder.net/")
                .setClient(new OkClient()) // The default client didn't handle well responses like 401
                .build()
                .create(GPodderAPI.class);
    }
}

