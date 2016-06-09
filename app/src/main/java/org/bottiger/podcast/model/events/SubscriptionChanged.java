package org.bottiger.podcast.model.events;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aplb on 12-10-2015.
 */
public class SubscriptionChanged implements ItemChanged {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ADDED, REMOVED, CHANGED, SUBSCRIBED})
    public @interface Action {}
    public static final int ADDED = 1;
    public static final int REMOVED = 2;
    public static final int CHANGED = 3;
    public static final int SUBSCRIBED = 4;

    private long id;
    private @Action int action;
    private String tag;

    public SubscriptionChanged(long argId, @Action int argAction, @NonNull String argTag) {
        id = argId;
        action = argAction;
        tag = argTag;
    }

    public long getId() {
        return id;
    }

    public @Action int getAction() {
        return action;
    }

    @NonNull
    public String getTag() {
        return tag;
    }
}
