package org.bottiger.podcast.utils;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.bottiger.podcast.SettingsActivity;
import org.bottiger.podcast.activities.downloadmanager.DownloadManagerActivity;

/**
 * Created by apl on 26-02-2015.
 */
public class TransitionUtils {

    public static void openSettings(@NonNull Context argContext) {
        startActivityWrapper(argContext, SettingsActivity.class);
    }

    public static void openDownloadManager(@NonNull Context argContext) {
        startActivityWrapper(argContext, DownloadManagerActivity.class);
    }

    private static void startActivityWrapper(Context argContext, Class<?> cls) {
        Intent intent = new Intent(argContext, cls);
        argContext.startActivity(intent);
    }

}
