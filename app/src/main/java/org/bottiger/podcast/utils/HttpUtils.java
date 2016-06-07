package org.bottiger.podcast.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;

/**
 * Created by aplb on 07-06-2016.
 */

public class HttpUtils {

    @NonNull
    public static String getUserAgent(@NonNull Context argContext) {
        return argContext.getResources().getString(R.string.app_name) + "-" + BuildConfig.VERSION_NAME;
    }
}
