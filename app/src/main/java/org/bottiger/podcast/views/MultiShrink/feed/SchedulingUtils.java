package org.bottiger.podcast.views.MultiShrink.feed;

/**
 * Created by apl on 14-02-2015.
 */
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;

/** Static methods that are useful for scheduling actions to occur at a later time. */
public class SchedulingUtils {


    /** Runs a piece of code just before the next draw, after layout and measurement */
    public static void doOnPreDraw(final View view, final boolean drawNextFrame,
                                   final Runnable runnable) {
        final OnPreDrawListener listener = new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                runnable.run();
                return drawNextFrame;
            }
        };
        view.getViewTreeObserver().addOnPreDrawListener(listener);
    }
}
