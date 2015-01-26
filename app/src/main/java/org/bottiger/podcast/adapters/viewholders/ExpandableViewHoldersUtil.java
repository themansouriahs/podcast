package org.bottiger.podcast.adapters.viewholders;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlayerLinearLayout;
import org.bottiger.podcast.views.PlaylistViewHolder;

import java.util.List;

/**
 * Created by apl on 20-01-2015.
 */
public class ExpandableViewHoldersUtil {

    private static int offset = -1;

    public static void openH(final PlaylistViewHolder holder, final View expandView, final boolean animate) {
        if (animate) {
            expandView.setVisibility(View.VISIBLE);
            final Animator animator = ViewHolderAnimator.ofItemViewHeight(holder, true);
            final Animator animator2 = LayoutAnimator.ofHeight(holder.mItemBackground, ViewHolderAnimator.sStart, ViewHolderAnimator.sEnd);

            int mSmallButtonSize = PlayPauseImageView.getSmallSize(expandView.getContext());
            int mLargeButtonSize = PlayPauseImageView.getLargeSize(expandView.getContext());

            calcOffset(holder, ViewHolderAnimator.sStart);

            final List<Animator> aList =LayoutAnimator.ofButton(holder.mPlayPauseButton, offset, mSmallButtonSize, mLargeButtonSize, true);

            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    final ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(expandView, View.ALPHA, 1);
                    alphaAnimator.addListener(new ViewHolderAnimator.ViewHolderAnimatorListener(holder));
                    alphaAnimator.start();
                }
            });

            AnimatorSet aSet = new AnimatorSet();
            aList.add(animator);
            aList.add(animator2);
            aSet.playTogether(aList);
            //aSet.playTogether(animator, animator2);
            aSet.start();
            //animator.start();
        }
        else {
            expandView.setVisibility(View.VISIBLE);
            expandView.setAlpha(1);
        }
    }

    public static void closeH(final PlaylistViewHolder holder, final View expandView, final boolean animate) {
        if (animate) {
            expandView.setVisibility(View.GONE);
            final Animator animator = ViewHolderAnimator.ofItemViewHeight(holder, false);
            final Animator animator2 = LayoutAnimator.ofHeight(holder.mItemBackground, ViewHolderAnimator.sEnd, ViewHolderAnimator.sStart);

            int mSmallButtonSize = PlayPauseImageView.getSmallSize(expandView.getContext());
            int mLargeButtonSize = PlayPauseImageView.getLargeSize(expandView.getContext());

            final List<Animator> aList =LayoutAnimator.ofButton(holder.mPlayPauseButton, offset, mSmallButtonSize, mLargeButtonSize, false);

            expandView.setVisibility(View.VISIBLE);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    expandView.setVisibility(View.GONE);
                    expandView.setAlpha(0);
                }
                @Override public void onAnimationCancel(Animator animation) {
                    expandView.setVisibility(View.GONE);
                    expandView.setAlpha(0);
                }
            });

            AnimatorSet aSet = new AnimatorSet();
            aList.add(animator);
            aList.add(animator2);
            aSet.playTogether(aList);
            //aSet.playTogether(animator, animator2);
            aSet.start();
            //animator.start();
        }
        else {
            expandView.setVisibility(View.GONE);
            expandView.setAlpha(0);
        }
    }

    private static void calcOffset(PlaylistViewHolder viewHolder, int baseHeight) {
        if (offset > 0)
            return;
        final PlayerLinearLayout player = viewHolder.playerLinearLayout;
        int c = viewHolder.mPlayPauseButton.getBottom()-viewHolder.mPlayPauseButton.getTop();
        offset = baseHeight-c;
    }

    public static interface Expandable {
        public View getExpandView();
    }

    public static class KeepOneH<VH extends PlaylistViewHolder & Expandable> {
        private int _opened = -1;

        public void bind(VH holder, int pos) {
            if (pos == _opened)
                ExpandableViewHoldersUtil.openH(holder, holder.getExpandView(), false);
            else
                ExpandableViewHoldersUtil.closeH(holder, holder.getExpandView(), false);
        }

        @SuppressWarnings("unchecked")
        public void toggle(VH holder) {
            if (_opened == holder.getPosition()) {
                _opened = -1;
                ExpandableViewHoldersUtil.closeH(holder, holder.getExpandView(), true);
            }
            else {
                int previous = _opened;
                _opened = holder.getPosition();
                ExpandableViewHoldersUtil.openH(holder, holder.getExpandView(), true);

                final VH oldHolder = (VH) ((RecyclerView) holder.itemView.getParent()).findViewHolderForPosition(previous);
                if (oldHolder != null)
                    ExpandableViewHoldersUtil.closeH(oldHolder, oldHolder.getExpandView(), true);
            }
        }
    }

}
