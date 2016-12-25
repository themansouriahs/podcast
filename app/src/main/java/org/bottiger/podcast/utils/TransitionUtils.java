package org.bottiger.podcast.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;

import org.bottiger.podcast.SettingsActivity;
import org.bottiger.podcast.TopActivity;
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

        if ( ContextCompat.checkSelfPermission( argActivity, Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions( argActivity, new String[] {  Manifest.permission.CAMERA  }, TopActivity.PERMISSION_TO_USE_CAMERA);
        }

        Intent intent = new Intent(argActivity, WebScannerActivity.class);
        argActivity.startActivityForResult(intent, WebScannerActivity.SCAN_QR_REQUEST);
    }

}
