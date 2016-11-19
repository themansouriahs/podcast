package org.bottiger.podcast.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.zxing.integration.android.IntentIntegrator;

import org.bottiger.podcast.SettingsActivity;
import org.bottiger.podcast.WebScannerActivity;
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

    public static void openWebPlayerAuthenticator(@NonNull Activity argActivity) {
        new IntentIntegrator(argActivity).initiateScan(); // `this` is the current Activity
    }

    public static void startWebScannerActivity(@NonNull Activity argActivity) {
        Intent intent = new Intent(argActivity, WebScannerActivity.class);
        argActivity.startActivity(intent);
    }

}
