package org.bottiger.podcast.webservices.directories.itunes.types;

import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.utils.ErrorUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by aplb on 04-01-2017.
 */

public class LookupResult {

    @SerializedName("collectionId")
    @Expose
    private long collectionId;

    @SerializedName("collectionName")
    @Expose
    private String collectionName;

    @SerializedName("feedUrl")
    @Expose
    private String feedUrl;

    @SerializedName("artworkUrl100")
    @Expose
    private String artworkUrl;

    @Nullable
    public SlimSubscription toSubscription() {
        try {
            return new SlimSubscription(collectionName, new URL(feedUrl), artworkUrl);
        } catch (MalformedURLException e) {
            ErrorUtils.handleException(e);
            return null;
        }
    }

    public String Url() {
        return feedUrl;
    }
}
