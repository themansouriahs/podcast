package org.bottiger.podcast.webservices.datastore.gpodder.datatypes;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Arvid on 8/25/2015.
 */
public class GEpisodeAction {

    public static final String DOWNLOAD = "download";
    public static final String DELETE = "delete";
    public static final String PLAY = "play";
    public static final String NEW = "new";
    public static final String FLATTR = "flattr";
    public static final String NA = "na";

    @StringDef({DOWNLOAD, DELETE, PLAY, NEW, FLATTR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {}

    public String podcast; // The feed URL to the podcast feed the episode belongs to (required)
    public String episode; // The media URL of the episode (required)
    public String device; // The device ID on which the action has taken place (see Devices)
    public String action; // One of: download, play, delete, new (required)
    public String timestamp; // A UTC timestamp when the action took place, in ISO 8601 format
    public long started; // Only valid for “play”. the position (in seconds) at which the client started playback. Requires position and total to be set.
    public long position; // Only valid for “play”. the position (in seconds) at which the client stopped playback
    public long total; // Only valid for “play”. the total length of the file in seconds. Requires position and started to be set.

    @Action
    public String getAction() {
        return action;
    }
}
