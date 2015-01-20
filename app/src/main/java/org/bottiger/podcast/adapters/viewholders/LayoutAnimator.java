package org.bottiger.podcast.adapters.viewholders;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

import java.util.LinkedList;
import java.util.List;

public class LayoutAnimator {

    public static class LayoutHeightUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        private final View _view;

        public LayoutHeightUpdateListener(View view) {
            _view = view;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            final ViewGroup.LayoutParams lp = _view.getLayoutParams();
            lp.height = (int) animation.getAnimatedValue();
            _view.setLayoutParams(lp);
        }

    }

    public static class LayoutSizeUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        private final View _view;

        public LayoutSizeUpdateListener(View view) {
            _view = view;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            final ViewGroup.LayoutParams lp = _view.getLayoutParams();
            lp.height = (int) animation.getAnimatedValue();
            lp.width = (int) animation.getAnimatedValue();
            _view.setLayoutParams(lp);
        }

    }

    public static class LayoutYTranslationUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        private final View _view;

        public LayoutYTranslationUpdateListener(View view) {
            _view = view;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            _view.setTranslationY((float)animation.getAnimatedValue());
        }

    }

    public static Animator ofHeight(View view, int start, int end) {
        final ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(new LayoutHeightUpdateListener(view));
        return animator;
    }

    public static List<Animator> ofButton(View view, int offset, int small, int large, boolean argExpand) {
        int TY0 = 0;
        int TY1 = offset;
        int size0 = small;
        int size1 = large;

        if (!argExpand) {
            TY0 = offset;
            TY1 = 0;
            size0 = large;
            size1 = small;
        }

        final ValueAnimator animator = ValueAnimator.ofFloat(TY0, TY1);
        animator.addUpdateListener(new LayoutYTranslationUpdateListener(view));

        final ValueAnimator animator2 = ValueAnimator.ofInt(size0, size1);
        animator2.addUpdateListener(new LayoutSizeUpdateListener(view));

        List<Animator> aList = new LinkedList<>();
        aList.add(animator);
        aList.add(animator2);

        return aList;
    }
}
