package org.bottiger.podcast.playlist.filters;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.ItemColumns;

import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by apl on 29-04-2015.
 */
public class SubscriptionFilter implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final String SELECTED_SUBSCRIPTIONS_KEY;
    private static final String SEPARATOR = ",";

    private Long mFilterType = DisplayFilter.MANUAL;
    private final HashSet<Long> mSubscriptions = new HashSet<>();
    private ReentrantLock mLock = new ReentrantLock();

    public SubscriptionFilter(@NonNull Context argContext) {
        Context context = argContext.getApplicationContext();
        Resources resources = context.getResources();
        SELECTED_SUBSCRIPTIONS_KEY = resources.getString(R.string.pref_playlist_subscriptions_key);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(sharedPreferences, SELECTED_SUBSCRIPTIONS_KEY);
    }

    public void add(Long argID) {
        try {
            mLock.lock();

            if (mSubscriptions.contains(argID)) {
                return;
            }

            mSubscriptions.add(argID);
        } finally {
            mLock.unlock();
        }
    }

    public boolean remove(Long argID) {
        try {
            mLock.lock();

            if (mSubscriptions.contains(argID)) {
                mSubscriptions.remove(argID);
                return true;
            }
            return false;
        } finally {
            mLock.unlock();
        }
    }

    public String toSQL() {
        String sql = "";

        try {
            mLock.lock();

            if (mFilterType == DisplayFilter.MANUAL) {
                return " 0 "; // false for all subscriptions
            }

            if (mFilterType == DisplayFilter.ALL) {
                return " 1 "; // true for all subscriptions
            }

            if (!mSubscriptions.isEmpty()) {

                sql = " " + ItemColumns.SUBS_ID + " IN (";
                sql += TextUtils.join(",", mSubscriptions);
                sql += ")";
            }
        } finally {
            mLock.unlock();
        }
        return sql;
    }

    public void clear() {
        mFilterType = DisplayFilter.ALL;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == SELECTED_SUBSCRIPTIONS_KEY) {
            try {
                mLock.lock();
                String prefrenceValue = sharedPreferences.getString(key, "");
                mSubscriptions.clear();

                if (TextUtils.isEmpty(prefrenceValue)) {
                    clear();
                    return;
                }

                if (prefrenceValue.contains(SEPARATOR)) {
                    for (Long subscriptionId : parsePreference(prefrenceValue)) {
                        mSubscriptions.add(subscriptionId);
                        mFilterType = DisplayFilter.MANUAL;
                    }
                    return;
                }

                Long longValue = Long.valueOf(prefrenceValue);

                // in case there is only one subscription in the list
                if (longValue > 0) {
                    mSubscriptions.add(longValue);
                    mFilterType = DisplayFilter.MANUAL;
                    return;
                }

                mFilterType = longValue;

            } finally {
                mLock.unlock();
            }
        }
    }

    private HashSet<Long> parsePreference(@Nullable String argPrefValue) {
        HashSet<Long> longs = new HashSet<>();
        String[] strings = TextUtils.split(argPrefValue, SEPARATOR);
        for (int i = 0; i < strings.length; i++) {
            Long value = Long.valueOf(strings[i]);
            longs.add(value);
        }
        return longs;
    }

    private String toPreferenceValue(@NonNull HashSet<Long> argLongs) {
        return TextUtils.join(SEPARATOR, mSubscriptions);
    }

    private static class DisplayFilter {
        public static final long MANUAL = 0;
        public static final long ALL = -1;
        public static final long SELECTED = -2;
    }
}
