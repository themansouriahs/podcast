package org.bottiger.podcast.model;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aplb on 13-10-2015.
 */
public class EpisodeChanged {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ADDED, REMOVED, CHANGED, PARSED})
    public @interface Action {}
    public static final int ADDED = 1;
    public static final int REMOVED = 2;
    public static final int CHANGED = 3;
    public static final int PARSED = 4;

    private long id;
    private String url;
    private @Action int action;

    public EpisodeChanged(long argId, @NonNull String argUrl, @Action int argAction) {
        id = argId;
        url = argUrl;
        action = argAction;
    }

    public long getId() {
        return id;
    }

    public @Action int getAction() {
        return action;
    }

    public String getUrl() {
        return url;
    }
}
