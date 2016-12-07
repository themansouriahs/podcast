package org.bottiger.podcast.webservices.directories.podcastaddict;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.utils.ErrorUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by aplb on 07-12-2016.
 */

class SubscriptionResult {

    @SerializedName("url")
    private String mUrl;

    @SerializedName("name")
    private String mTitle;

    @SerializedName("description")
    String mDescription;

    @SerializedName("thumbnail")
    private String mThumbnail;

    @SerializedName("type")
    String mType;

    @SerializedName("language")
    String mLanguage;

    @SerializedName("author")
    String mAuthor;

    @SerializedName("explicit")
    int mExplicit;

    @SerializedName("lastPublicationDate")
    long lastPubDate;

    @SerializedName("timestamp")
    String mTimestamp;

    @Nullable
    SlimSubscription toSubscription() {
        URL url = null;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException e) {
            ErrorUtils.handleException(e);
        }

        if (url == null) {
            return null;
        }

        return new SlimSubscription(mTitle, url, mThumbnail);
    }

}
