package org.bottiger.podcast.provider.SlimImplementations;

import android.support.annotation.NonNull;

import org.bottiger.podcast.provider.ISubscription;

/**
 * Created by apl on 15-04-2015.
 */
public class SlimSubscription implements ISubscription {

    private String mTitle;

    public SlimSubscription(@NonNull String argTitle) {
        mTitle = argTitle;
    }

    @NonNull
    @Override
    public String getTitle() {
        return mTitle;
    }
}
