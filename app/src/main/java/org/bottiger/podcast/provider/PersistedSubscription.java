package org.bottiger.podcast.provider;

import org.bottiger.podcast.provider.base.BaseSubscription;

/**
 * Created by aplb on 01-12-2016.
 */

public abstract class PersistedSubscription extends BaseSubscription implements IPersistedSub {

    public long id;
    private int mClicks = 0;

    public long getId() {
        return id;
    }

    public void setId(long argId) {
        this.id = argId;
    }

    public int getScore() {
        return mClicks;
    }

    public int getClicks() {
        return mClicks;
    }

    protected void setClicks(int argNumClicks) {
        mClicks = argNumClicks;
    }

    public void incrementClicks() {
        mClicks++;
    }
}
