package org.bottiger.podcast.provider;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import org.bottiger.podcast.provider.base.BaseSubscription;

/**
 * Created by aplb on 01-12-2016.
 */
@Entity(tableName = SubscriptionColumns.TABLE_NAME,
        indices = {@Index(SubscriptionColumns.LAST_ITEM_UPDATED), @Index(ItemColumns.URL)})
public abstract class PersistedSubscription extends BaseSubscription implements IPersistedSub {

    /**
     * Unique ID
     */
    @PrimaryKey()
    @ColumnInfo(name = SubscriptionColumns._ID)
    public long id;

    @ColumnInfo(name = SubscriptionColumns.SUBSCRIBED_AT)
    public long mSubscribedAt;

    @ColumnInfo(name = SubscriptionColumns.LAST_ITEM_UPDATED)
    public long lastItemUpdated;

    @ColumnInfo(name = SubscriptionColumns.LAST_UPDATED)
    public long lastUpdated;

    @ColumnInfo(name = SubscriptionColumns.STATUS)
    public long status;

    /**
     * Settings is a bitmasked int with various settings
     */
    @ColumnInfo(name = SubscriptionColumns.SETTINGS)
    public int mSettings;

    @ColumnInfo(name = SubscriptionColumns.NEW_EPISODES)
    public int new_episodes_cache;

    @ColumnInfo(name = SubscriptionColumns.EPISODE_COUNT)
    public int episode_count_cache;

    @ColumnInfo(name = SubscriptionColumns.RATING)
    public int mClicks = 0;
    /**
     * See SubscriptionColumns for documentation
     */
    public String comment;
    public String sync_id;

    public long fail_count;
    @Deprecated public long auto_download;

    public long getSubscribedAt() {
        return mSubscribedAt;
    }

    protected void setSubscribedAt(long argSubscribedAt) {
        mSubscribedAt = argSubscribedAt;
    }

    public void subscribe() {
        mSubscribedAt = System.currentTimeMillis();
    }

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
