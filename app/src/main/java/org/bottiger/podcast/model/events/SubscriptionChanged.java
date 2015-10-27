package org.bottiger.podcast.model.events;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aplb on 12-10-2015.
 */
public class SubscriptionChanged implements ItemChanged {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ADDED, REMOVED, CHANGED})
    public @interface Action {}
    public static final int ADDED = 1;
    public static final int REMOVED = 2;
    public static final int CHANGED = 3;

    private long id;
    private @Action int action;

    public SubscriptionChanged(long argId, @Action int argAction) {
        id = argId;
        action = argAction;
    }

    public long getId() {
        return id;
    }

    public @Action int getAction() {
        return action;
    }
}
