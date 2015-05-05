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
public class SubscriptionFilter implements IPlaylistFilter, SharedPreferences.OnSharedPreferenceChangeListener {

    public enum MODE {SHOW_ALL, SHOW_NONE, SHOW_SELECTED};

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

    public boolean isShown(Long argID) {
        try {
            mLock.lock();

            if (mFilterType == DisplayFilter.ALL)
                return true;

            if (mFilterType == DisplayFilter.MANUAL)
                return false;

            return mSubscriptions.contains(argID);
        } finally {
            mLock.unlock();
        }
    }

    public MODE getMode() {
        if (mFilterType == DisplayFilter.ALL) {
            return MODE.SHOW_ALL;
        }

        if (mFilterType == DisplayFilter.MANUAL) {
            return MODE.SHOW_NONE;
        }

        if (mFilterType == DisplayFilter.SELECTED) {
            return MODE.SHOW_SELECTED;
        }
        return MODE.SHOW_NONE;
    }

    public void setMode(MODE argMode, Context argContext) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(argContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (argMode == MODE.SHOW_ALL) {
            mFilterType = DisplayFilter.ALL;
            editor.putString(SELECTED_SUBSCRIPTIONS_KEY, Long.toString(mFilterType));
        } else if (argMode == MODE.SHOW_NONE) {
            mFilterType = DisplayFilter.MANUAL;
            editor.putString(SELECTED_SUBSCRIPTIONS_KEY, Long.toString(mFilterType));
        } else if (argMode == MODE.SHOW_SELECTED) {
            mFilterType = DisplayFilter.SELECTED;
            editor.putString(SELECTED_SUBSCRIPTIONS_KEY, toPreferenceValue(mSubscriptions));
        }

        editor.commit();
    }

    public String toSQL() {
        String sql = "";

        try {
            mLock.lock();

            if (mFilterType == DisplayFilter.MANUAL) {
                return "(" + ItemColumns.TABLE_NAME + "." + ItemColumns.PRIORITY + " > 0)";
                //return " 0 "; // false for all subscriptions
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
