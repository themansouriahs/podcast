package org.bottiger.podcast.adapters.viewholders;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.FixedRecyclerView;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlayerRelativeLayout;
import org.bottiger.podcast.views.PlaylistViewHolder;

import java.util.List;

/**
 * Created by apl on 20-01-2015.
 */
public class ExpandableViewHoldersUtil {

    private static TransitionManager sTransitionManager = null;

    private static RelativeLayout.LayoutParams sPlayPauseParams;
    private static RelativeLayout.LayoutParams sTitleParams;
    private static RelativeLayout.LayoutParams sPodcastImageParams;
    private static RelativeLayout.LayoutParams sDurationParams;
    private static float sPodcastImageRadius;

    public static void openH(final PlaylistViewHolder holder, final View expandView, final boolean animate) {
        if (animate) {
            initTransition(holder);
        }

            if (sPodcastImageParams == null) {
                sPodcastImageParams = (RelativeLayout.LayoutParams) holder.mPodcastImage.getLayoutParams();
                sPlayPauseParams = (RelativeLayout.LayoutParams) holder.mPlayPauseButton.getLayoutParams();
                sPodcastImageRadius = holder.mPodcastImage.getRadius();
                sTitleParams = (RelativeLayout.LayoutParams) holder.mMainTitle.getLayoutParams();
                sDurationParams = (RelativeLayout.LayoutParams) holder.mTimeDuration.getLayoutParams();
            }

            RelativeLayout.LayoutParams newParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600);

            holder.mPodcastImage.setRadius(0);
            holder.mPodcastImage.setLayoutParams(newParams);

            sTitleParams.addRule(RelativeLayout.RIGHT_OF, 0);
            sTitleParams.addRule(RelativeLayout.LEFT_OF, 0);
            holder.mMainTitle.setLayoutParams(sTitleParams);

            //sDurationParams.addRule(RelativeLayout.ALIGN_TOP, R.id.expanded_buttons_layout);
            //sDurationParams.addRule(RelativeLayout.ALIGN_TOP, 0);
            //sDurationParams.addRule(RelativeLayout.ALIGN_BASELINE, R.id.expanded_buttons_layout);
            sDurationParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            //sDurationParams.addRule(RelativeLayout.RIGHT_OF, R.id.expanded_buttons_layout);

            sDurationParams.setMargins(0, 405, 10, 0);
            holder.mTimeDuration.setLayoutParams(sDurationParams);
            holder.mTimeDuration.setGravity(Gravity.RIGHT);

            sPlayPauseParams.setMargins(sPlayPauseParams.leftMargin, 100, sPlayPauseParams.rightMargin, sPlayPauseParams.bottomMargin);
            sPlayPauseParams.addRule(RelativeLayout.CENTER_VERTICAL, 0);
            holder.mPlayPauseButton.setLayoutParams(sPlayPauseParams);


            holder.mActionBarGradientView.setAlpha(1);

            holder.mTimeDurationIcon.setVisibility(View.GONE);
            holder.mExpandedLayoutBottom.setVisibility(View.VISIBLE);
            expandView.setVisibility(View.VISIBLE);

    }

    @SuppressLint("NewApi")
    public static void closeH(final PlaylistViewHolder holder, final View expandView, final boolean animate) {

        if (animate) {
            initTransition(holder);
        }


        if (sPodcastImageParams != null) {
            holder.mPodcastImage.setLayoutParams(sPodcastImageParams);
            holder.mPodcastImage.setRadius(sPodcastImageRadius);
        }

        if (sTitleParams != null) {
            sTitleParams.addRule(RelativeLayout.RIGHT_OF, R.id.left_image);
            sTitleParams.addRule(RelativeLayout.LEFT_OF, R.id.list_image);
            holder.mMainTitle.setLayoutParams(sTitleParams);
        }

        if (sDurationParams != null) {
            sDurationParams.addRule(RelativeLayout.ALIGN_TOP, R.id.podcast_duration_ic);
            sDurationParams.addRule(RelativeLayout.RIGHT_OF, R.id.podcast_duration_ic);
            sDurationParams.setMargins(0, 0, 0, 0);
            holder.mTimeDuration.setLayoutParams(sDurationParams);
        }

        holder.mActionBarGradientView.setAlpha(0);

        if (sPlayPauseParams != null) {
            sPlayPauseParams.setMargins(sPlayPauseParams.leftMargin, 0, sPlayPauseParams.rightMargin, sPlayPauseParams.bottomMargin);
            sPlayPauseParams.addRule(RelativeLayout.CENTER_VERTICAL);
            holder.mPlayPauseButton.setLayoutParams(sPlayPauseParams);
        }

        holder.mTimeDuration.setGravity(Gravity.LEFT);
        holder.mTimeDurationIcon.setVisibility(View.VISIBLE);
        holder.mExpandedLayoutBottom.setVisibility(View.GONE);
        expandView.setVisibility(View.GONE);

    }


    public static interface Expandable {
        public View getExpandView();
    }

    public static class KeepOneH<VH extends PlaylistViewHolder & Expandable> {
        public int _opened = -1; //private

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

    @TargetApi(19)
    private static void initTransition(PlaylistViewHolder argPlaylistViewHolder) {
        if (android.os.Build.VERSION.SDK_INT < 19) {
            return;
        }

        if (sTransitionManager == null) {
            sTransitionManager = new TransitionManager();
        }

        ViewGroup viewGroup = (ViewGroup) argPlaylistViewHolder.mLayout.getParent();
        if (viewGroup == null)
            return;

        sTransitionManager.beginDelayedTransition(viewGroup);
    }

}
