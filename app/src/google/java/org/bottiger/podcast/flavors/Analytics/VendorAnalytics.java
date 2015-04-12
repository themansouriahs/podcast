package org.bottiger.podcast.flavors.Analytics;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.bottiger.podcast.ApplicationConfiguration;

import java.util.HashMap;

/**
 * Created by apl on 11-02-2015.
 *
 * Only used on the Google Play Store
 */
public class VendorAnalytics extends AbstractAnalytics implements IAnalytics {

    private static final String CATEGORY_PLAYBACK       = "Playback";
    private static final String CATEGORY_BEHAVIOR       = "Behavior";
    private static final String CATEGORY_USAGE          = "Usage";
    private static final String CATEGORY_INFRASTRUCTURE = "Infrastructure";

    private Context mContext;

    public VendorAnalytics(@NonNull Context argContext) {
        super(argContext);
        mContext = argContext;
    }

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    private HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public void startTracking() {
        Tracker tracker = getTracker(TrackerName.APP_TRACKER);
        tracker.enableExceptionReporting(true);
        tracker.enableAutoActivityTracking(true);
    }

    public void stopTracking() {
        return;
    }

    @Override
    public void activityPause() {
        return;
    }

    @Override
    public void activityResume() {
        return;
    }

    public void trackEvent(EVENT_TYPE argEvent) {
        if (!doShare())
            return;

        EventData eventData = getEventData(argEvent);

        // Build and send an Event.
        getTracker(TrackerName.APP_TRACKER).send(new HitBuilders.EventBuilder()
                .setCategory(eventData.Category)
                .setAction(eventData.Action)
                .setLabel(eventData.LabelID)
                .build());
    }

    private synchronized Tracker getTracker(TrackerName trackerId) {
        if (trackerId != TrackerName.APP_TRACKER) {
            throw new IllegalStateException("TrackerName not supported");
        }

        if (!mTrackers.containsKey(trackerId)) {

            com.google.android.gms.analytics.GoogleAnalytics analytics = com.google.android.gms.analytics.GoogleAnalytics.getInstance(mContext);
            Tracker t = analytics.newTracker(ApplicationConfiguration.ANALYTICS_ID);
            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }

    @NonNull
    private EventData getEventData(EVENT_TYPE argType) {
        EventData eventData = null;

        if (argType == EVENT_TYPE.PLAY) {
            eventData = new EventData();
            eventData.Category = CATEGORY_PLAYBACK;
            eventData.Action = "Play";
            eventData.LabelID = "Play";
        }

        if (argType == EVENT_TYPE.PAUSE) {
            eventData = new EventData();
            eventData.Category = CATEGORY_PLAYBACK;
            eventData.Action = "Pause";
            eventData.LabelID = "Pause";
        }

        if (argType == EVENT_TYPE.PLAY_FROM_PLAYLIST) {
            eventData = new EventData();
            eventData.Category = CATEGORY_BEHAVIOR;
            eventData.Action = "Play from playlist";
            eventData.LabelID = "Playlist";
        }

        if (argType == EVENT_TYPE.PLAY_FROM_FEEDVIEW) {
            eventData = new EventData();
            eventData.Category = CATEGORY_BEHAVIOR;
            eventData.Action = "Play from feedview";
            eventData.LabelID = "FeedView";
        }

        if (argType == EVENT_TYPE.SUBSCRIBE_TO_FEED) {
            eventData = new EventData();
            eventData.Category = CATEGORY_USAGE;
            eventData.Action = "Subscribe to feed";
            eventData.LabelID = "Subscribe";
        }

        if (argType == EVENT_TYPE.OPML_IMPORT) {
            eventData = new EventData();
            eventData.Category = CATEGORY_USAGE;
            eventData.Action = "OPML import";
            eventData.LabelID = "OPML";
        }

        if (argType == EVENT_TYPE.OPML_EXPORT) {
            eventData = new EventData();
            eventData.Category = CATEGORY_USAGE;
            eventData.Action = "OPML export";
            eventData.LabelID = "OPML";
        }

        if (argType == EVENT_TYPE.DATABASE_UPGRADE) {
            eventData = new EventData();
            eventData.Category = CATEGORY_INFRASTRUCTURE;
            eventData.Action = "Upgrade";
            eventData.LabelID = "Database upgrade";
        }

        if (argType == EVENT_TYPE.MEDIA_ROUTING) {
            eventData = new EventData();
            eventData.Category = CATEGORY_USAGE;
            eventData.Action = "MediaRouter";
            eventData.LabelID = "Playing using MediaRouter";
        }

        if (eventData != null)
            return eventData;

        eventData = new EventData();
        eventData.Category = "Unknown";
        eventData.Action = "Unknown";
        eventData.LabelID = "Unknown";

        return eventData;
    }

    private class EventData {
        public String Category;
        public String Action;
        public String LabelID;
    }
}
