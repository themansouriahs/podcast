package org.bottiger.podcast.utils;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.view.View;

import org.bottiger.podcast.ToolbarActivity;

/**
 * Created by aplb on 15-09-2015.
 */
public class SharedAdapterUtils {

    public static void AddPaddingToLastElement(@NonNull CardView argCardView, int argDefaultBottomPadding, boolean argIsLast) {

        int[] paddings = calculatePaddings(argCardView, argDefaultBottomPadding, argIsLast);
        argCardView.setContentPadding(paddings[0], paddings[1], paddings[2], paddings[3]);

    }

    public static void AddPaddingToLastElement(@NonNull View argView, int argDefaultBottomPadding, boolean argIsLast) {
        //int[] paddings = calculatePaddings(argView, argDefaultBottomPadding, argIsLast);
        //argView.setPadding(paddings[0], paddings[1], paddings[2], paddings[3]);

    }

    private static int[] calculatePaddings(@NonNull View argView, int argDefaultBottomPadding, boolean argIsLast) {
        int left = argView.getPaddingLeft();
        int right = argView.getPaddingRight();
        int top = argView.getPaddingTop();
        int bottom = argView.getPaddingTop();

        Resources resources = argView.getResources();
        int newBottomPadding = argIsLast ? ToolbarActivity.getNavigationBarHeight(resources) : 0;
        newBottomPadding += argDefaultBottomPadding;

        return new int[] { left, top, right, newBottomPadding};
    }

}
