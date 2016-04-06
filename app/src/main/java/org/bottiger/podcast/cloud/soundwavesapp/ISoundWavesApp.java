package org.bottiger.podcast.cloud.soundwavesapp;

import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;

import retrofit2.http.GET;
import retrofit2.http.Path;


/**
 * Created by apl on 22-04-2015.
 */
public interface ISoundWavesApp {

    @GET("/podcasts/{url}")
    SlimSubscription podcast(
            @Path("url") String podcastUrl
    );


    //@PUT("/podcasts/{url}")
    //public String simplePut();

}
