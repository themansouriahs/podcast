package org.bottiger.podcast.provider;

import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

/**
 * Created by apl on 22-05-2015.
 */
public class SubscriptionLoader {

    public static Subscription fetchFromCursor(@NonNull  Subscription sub, @NonNull Cursor cursor) {
        sub.id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

        int lastUpdatedIndex = cursor
                .getColumnIndex(SubscriptionColumns.LAST_UPDATED);
        int urlIndex = cursor.getColumnIndex(SubscriptionColumns.URL);

        String lastUpdatedString = cursor.getString(lastUpdatedIndex);
        sub.lastUpdated = Long.parseLong(lastUpdatedString);

        sub.title = cursor.getString(cursor
                .getColumnIndex(SubscriptionColumns.TITLE));
        sub.url = cursor.getString(urlIndex);
        sub.imageURL = cursor.getString(cursor
                .getColumnIndex(SubscriptionColumns.IMAGE_URL));
        sub.comment = cursor.getString(cursor
                .getColumnIndex(SubscriptionColumns.COMMENT));
        sub.description = cursor.getString(cursor
                .getColumnIndex(SubscriptionColumns.DESCRIPTION));
        sub.sync_id = cursor.getString(cursor
                .getColumnIndex(SubscriptionColumns.REMOTE_ID));

        sub.auto_download = cursor.getLong(cursor
                .getColumnIndex(SubscriptionColumns.AUTO_DOWNLOAD));
        sub.lastItemUpdated = cursor.getLong(cursor
                .getColumnIndex(SubscriptionColumns.LAST_ITEM_UPDATED));
        sub.fail_count = cursor.getLong(cursor
                .getColumnIndex(SubscriptionColumns.FAIL_COUNT));

        sub.status = cursor.getLong(cursor.getColumnIndex(SubscriptionColumns.STATUS));

        sub.setSettings(cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.SETTINGS)));

        sub.setPrimaryColor(cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.PRIMARY_COLOR)));
        sub.setPrimaryTintColor(cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.PRIMARY_TINT_COLOR)));
        sub.setSecondaryColor(cursor.getInt(cursor
                .getColumnIndex(SubscriptionColumns.SECONDARY_COLOR)));

        int newEpisodesIndex = cursor
                .getColumnIndex(SubscriptionColumns.NEW_EPISODES);
        if (newEpisodesIndex > 0) {
            sub.setNewEpisodes(cursor.getInt(newEpisodesIndex));
        }

        int episodeCountIndex = cursor.getColumnIndex(SubscriptionColumns.EPISODE_COUNT);
        if (episodeCountIndex > 0) {
            sub.setEpisodeCount(cursor.getInt(episodeCountIndex));
        }

        sub.setIsRefreshing(false);

        return sub;
    }

}
