package org.bottiger.podcast.utils;

import android.os.Build;

/**
 * Created by aplb on 05-06-2016.
 */

public final class AndroidUtil {

    /**
     * Like {@link android.os.Build.VERSION#SDK_INT}, but in a place where it can be conveniently
     * overridden for local testing.
     */
    public static final int SDK_INT =
            (Build.VERSION.SDK_INT == 23 && Build.VERSION.CODENAME.charAt(0) == 'N') ? 24
                    : Build.VERSION.SDK_INT;

}
