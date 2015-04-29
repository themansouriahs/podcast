package org.bottiger.podcast.views.MultiShrink.feed;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.animation.PathInterpolator;

/**
 * Created by apl on 17-02-2015.
 */
public class PathInterpolatorCompat {

    private PathInterpolator mPathInterpolator;

    private float mcontrolX1, mcontrolY1, mcontrolX2, mcontrolY2;

    public PathInterpolatorCompat(float controlX1, float controlY1, float controlX2, float controlY2) {
        if (isLolipop())  {
            setPathInterpolator(controlX1, controlY1, controlX2, controlY2);
        } else {
            mcontrolX1 = controlX1;
            mcontrolX2 = controlX2;
            mcontrolY1 = controlY1;
            mcontrolY2 = controlY2;
        }
    }

    @TargetApi(21)
    private void setPathInterpolator(float controlX1, float controlY1, float controlX2, float controlY2) {
        mPathInterpolator = new PathInterpolator(controlX1, controlY1, controlX2, controlY2);
    }

    public float getInterpolation(float t) {
        return isLolipop() ? getLolipopInterpolation(t) : getLegacyInterpolation(t);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public float getLolipopInterpolation(float t) {
        return mPathInterpolator.getInterpolation(t);
    }

    public float getLegacyInterpolation(float t) {
        if (t <= 0) {
            return 0;
        } else if (t >= 1) {
            return 1;
        }
        float dx = mcontrolX2-mcontrolX1;

        return dx*t+mcontrolX1;
    }

    private boolean isLolipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
