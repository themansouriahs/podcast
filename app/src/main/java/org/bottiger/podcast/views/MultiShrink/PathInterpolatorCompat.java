package org.bottiger.podcast.views.MultiShrink;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.animation.PathInterpolator;

/**
 * Created by apl on 17-02-2015.
 */
public class PathInterpolatorCompat {

    private PathInterpolator mPathInterpolator;

    public PathInterpolatorCompat(float controlX1, float controlY1, float controlX2, float controlY2) {
        if (isLolipop())  {
            mPathInterpolator = new PathInterpolator(0.16f, 0.4f, 0.2f, 1);
        } else {

        }
    }

    public float getInterpolation(float t) {
        return isLolipop() ? getLolipopInterpolation(t) : getLegacyInterpolation(t);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public float getLolipopInterpolation(float t) {
        return mPathInterpolator.getInterpolation(t);
    }

    public float getLegacyInterpolation(float t) {
        return 1;
    }

    private boolean isLolipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
