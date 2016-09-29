package org.bottiger.podcast.provider.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.PreloadTarget;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Created by aplb on 08-06-2016.
 */

public abstract class BaseSubscription implements ISubscription {

    public void fetchImage(@NonNull Context argContext) {
        PreloadTarget<Bitmap> preloadTarget = PreloadTarget.obtain(500, 500);
        Glide.with(argContext.getApplicationContext())
        .load(getImageURL())
                .asBitmap()
                .into(preloadTarget);
    }

    public boolean doSkipIntro() {
        return false;
    }

    @Nullable
    protected IEpisode getMatchingEpisode(@NonNull IEpisode argEpisode) {
        for (int i = 0; i < getEpisodes().size(); i++) {
            if (getEpisodes().get(i).equals(argEpisode))
                return getEpisodes().get(i);
        }

        return null;
    }

    public boolean contains(@NonNull IEpisode argEpisode) {

        // For some reason thi doen't work
        //return mEpisodes.indexOf(argEpisode) >= 0;

        IEpisode matchingEpisode = getMatchingEpisode(argEpisode);

        return matchingEpisode != null;
    }
}
