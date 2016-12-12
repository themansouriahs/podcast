package org.bottiger.podcast.webservices.directories.audiosearch.types;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.utils.ErrorUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by aplb on 12-12-2016.
 */

public class Show {

    @SerializedName("id")
    long mId;

    @SerializedName("title")
    String mTitle;

    @SerializedName("description")
    String mDescription;

    @SerializedName("rss_url")
    String mUrl;

    @SerializedName("image_files")
    List<ShowUrl> mImageFiles;

    @Nullable
    public SlimSubscription toSubscription() {
        URL url = null;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException e) {
            ErrorUtils.handleException(e);
        }

        if (url == null) {
            return null;
        }

        String imageFile = "";

        if (mImageFiles != null && mImageFiles.size() > 0) {
            imageFile = mImageFiles.get(0).getThumbnail();

            if (TextUtils.isEmpty(imageFile)) {
                imageFile = "";
            }
        }

        return new SlimSubscription(mTitle, url, imageFile);
    }

}
