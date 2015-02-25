package org.bottiger.podcast.utils;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.bottiger.podcast.SettingsActivity;

/**
 * Created by apl on 26-02-2015.
 */
public class TransitionUtils {

    public static void openSettings(@NonNull Context argContext) {
        Intent i = new Intent(argContext, SettingsActivity.class);
        argContext.startActivity(i);
    }

}
