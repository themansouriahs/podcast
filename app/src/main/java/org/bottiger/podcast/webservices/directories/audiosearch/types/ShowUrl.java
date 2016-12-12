package org.bottiger.podcast.webservices.directories.audiosearch.types;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by aplb on 12-12-2016.
 */

public class ShowUrl {

    @SerializedName("url")
    ShowImageFile mUrls;

    public String getThumbnail() {
        return mUrls.getThumbnail();
    }

}
