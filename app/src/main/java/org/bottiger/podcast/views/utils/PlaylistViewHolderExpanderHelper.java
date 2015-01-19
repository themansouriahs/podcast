package org.bottiger.podcast.views.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.views.PlayPauseImageView;
import org.bottiger.podcast.views.PlayerLinearLayout;
import org.bottiger.podcast.views.PlaylistViewHolder;
import org.bottiger.podcast.views.RelativeLayoutWithBackground;

/**
 * Created by apl on 14-01-2015.
 */
public class PlaylistViewHolderExpanderHelper {

    private Context mContext;
    private ItemCursorAdapter mAdapter;
    private int mExpandedHeight;

    private AnimatorSet mAnimatorSet = new AnimatorSet();

    private int mSmallButtonSize;
    private int mLargeButtonSize;

    private RecyclerView.LayoutParams paramsRecyclerView;
    private RelativeLayout.LayoutParams paramsBackground;
    private ViewGroup.LayoutParams paramsButton;

    public static PlaylistViewHolder expandedView = null;
    public static RelativeLayoutWithBackground expandedLayout = null;

    public PlaylistViewHolderExpanderHelper(@NonNull Context argContext, @NonNull ItemCursorAdapter argAdapter, int argExpandedHeight) {
        mContext = argContext;
        mAdapter = argAdapter;
        mExpandedHeight = argExpandedHeight;

        mSmallButtonSize = PlayPauseImageView.getSmallSize(mContext);
        mLargeButtonSize = PlayPauseImageView.getLargeSize(mContext);
    }

    public void expand(final PlaylistViewHolder viewHolder, final boolean isAnimated, final boolean forceRefresh) {

        View player = viewHolder.playerLinearLayout;
        if (isAnimated) {
            expand(viewHolder);
        } else {

            if (mExpandedHeight < 0) {
                throw new IllegalStateException("mExpandedHeight should not set");
            }

            viewHolder.mLayout.getLayoutParams().height = mExpandedHeight;//imageStartHeight-initialHeight;
            viewHolder.mItemBackground.getLayoutParams().height = mExpandedHeight;

            /*
            viewHolder.mForward.setVisibility(View.VISIBLE);
            viewHolder.mBackward.setVisibility(View.VISIBLE);
            */

            viewHolder.mPlayPauseButton.getLayoutParams().height = mLargeButtonSize;
            viewHolder.mPlayPauseButton.getLayoutParams().width = mLargeButtonSize;
            viewHolder.mPlayPauseButton.setTranslationY(200);
            Log.d("mExpandedHeight", "expand " + viewHolder.episode.getTitle() + " no animation");

            player.setVisibility(View.VISIBLE);
            player.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            player.getLayoutParams().height = player.getMeasuredHeight();
            player.requestLayout();

            viewHolder.mLayout.requestLayout();

        }
        //extended_player.requestLayout();
    }

    public void collapse(final PlaylistViewHolder viewHolder, final boolean isAnimated) {
        View player = viewHolder.playerLinearLayout;
        if (isAnimated) {
            collapse(viewHolder);
        } else {

            viewHolder.mBackward.setVisibility(View.INVISIBLE);
            viewHolder.mForward.setVisibility(View.INVISIBLE);

            viewHolder.mPlayPauseButton.getLayoutParams().height = mSmallButtonSize;
            viewHolder.mPlayPauseButton.getLayoutParams().width = mSmallButtonSize;

            player.setVisibility(View.GONE);
            viewHolder.mLayout.getLayoutParams().height = ItemCursorAdapter.mCollapsedHeight;//imageStartHeight-initialHeight;

            viewHolder.mPlayPauseButton.requestLayout();

        }
    }

    private void expand(final PlaylistViewHolder viewHolder) {

        final PlayerLinearLayout player = viewHolder.playerLinearLayout;
        final RelativeLayoutWithBackground layoutWithBackground = viewHolder.mLayout;
        final ImageView layoutBackground = viewHolder.mItemBackground;

        final int imageStartHeight = layoutBackground.getHeight();

        layoutWithBackground.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int initialMeasuredHeight = layoutWithBackground.getMeasuredHeight();

        player.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int initialPlayerMeasuredHeight = player.getMeasuredHeight();
        player.requestLayout();
        player.setVisibility(View.VISIBLE);


        /*
        viewHolder.mForward.setAlpha(0f);
        viewHolder.mBackward.setAlpha(0f);
        viewHolder.mForward.setVisibility(View.VISIBLE);
        viewHolder.mBackward.setVisibility(View.VISIBLE);
        */

        final int targtetHeight = initialPlayerMeasuredHeight + imageStartHeight; // + initialMeasuredHeight; //player.getMeasuredHeight();

        layoutBackground.getLayoutParams().height = viewHolder.mMainContainer.getMeasuredHeight();

        // 1dp/ms
        int duration = getDuration(targtetHeight);

        if (minHeight < 0) {
            minHeight = imageStartHeight;
            maxHeight = targtetHeight; // evt imageStartHeight+initialPlayerMeasureHeig
        }

        final Incrementer inc = new Incrementer();
        final Incrementer buttonInc = new Incrementer();
        final Incrementer buttonTrans = new Incrementer();

        ObjectAnimator animator;
        ObjectAnimator buttonAnimator;
        ObjectAnimator buttonTransAnimator = ObjectAnimator.ofInt(buttonTrans, "interp", 0, 200);

        if (minHeight > 0) {
            Log.d("mExpandedHeight", "expand goal => " + maxHeight);
            animator = ObjectAnimator.ofInt(inc, "interp", minHeight, maxHeight); // targtetHeight
        } else {
            animator = ObjectAnimator.ofInt(inc, "interp", imageStartHeight, initialMeasuredHeight); // targtetHeight
            minHeight = imageStartHeight;
            maxHeight = initialMeasuredHeight;
        }
        animator.setDuration(duration);
        buttonTransAnimator.setDuration(duration);


        final RecyclerView.LayoutParams paramsT1 = (RecyclerView.LayoutParams)viewHolder.mLayout.getLayoutParams();
        final RelativeLayout.LayoutParams paramsT2 = (RelativeLayout.LayoutParams)viewHolder.mItemBackground.getLayoutParams();
        final ViewGroup.LayoutParams paramsButton = viewHolder.mPlayPauseButton.getLayoutParams();

        buttonAnimator = ObjectAnimator.ofInt(buttonInc, "interp", mSmallButtonSize, mLargeButtonSize);
        buttonAnimator.setDuration(duration);

        buttonAnimator.addUpdateListener(getButtonUpdateListener(viewHolder, paramsButton, buttonInc,buttonTrans));

        animator.addUpdateListener(getUpdateListener(viewHolder, inc, paramsT1, paramsT2));

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                viewHolder.mPlayPauseButton.bringToFront();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mExpandedHeight = maxHeight;
                Log.d("mExpandedHeight", "mExpandedHeight => " + mExpandedHeight);
                mAdapter.notifyDataSetChanged(); // BUGFIX
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        mAnimatorSet.playTogether(animator, buttonAnimator, buttonTransAnimator);
        mAnimatorSet.start();

        expandedView = viewHolder;
        expandedLayout = layoutWithBackground;

    }

    private static int minHeight = -1;
    private static int maxHeight = -1;

    private void collapse(final PlaylistViewHolder viewHolder) {

        final RelativeLayoutWithBackground layoutWithBackground = viewHolder.mLayout;

        final int initialHeight = layoutWithBackground.getHeight();

        layoutWithBackground.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // 1dp/ms
        int duration = getDuration(initialHeight);

        final Incrementer inc = new Incrementer();
        final Incrementer buttonInc = new Incrementer();
        final Incrementer buttonTransInc = new Incrementer();

        ObjectAnimator buttonAnimator;
        ObjectAnimator animator = ObjectAnimator.ofInt(inc, "interp", maxHeight, minHeight);
        ObjectAnimator buttonTrans = ObjectAnimator.ofInt(buttonTransInc, "interp", 200, 0);
        animator.setDuration(duration);
        buttonTrans.setDuration(duration);

        final RecyclerView.LayoutParams paramsT1 = (RecyclerView.LayoutParams)viewHolder.mLayout.getLayoutParams();
        final RelativeLayout.LayoutParams paramsT2 = (RelativeLayout.LayoutParams)viewHolder.mItemBackground.getLayoutParams();

        final ViewGroup.LayoutParams paramsButton = viewHolder.mPlayPauseButton.getLayoutParams();

        buttonAnimator = ObjectAnimator.ofInt(buttonInc, "interp", mLargeButtonSize, mSmallButtonSize);
        buttonAnimator.setDuration(duration);
        buttonAnimator.addUpdateListener(getButtonUpdateListener(viewHolder, paramsButton, buttonInc,buttonTransInc));

        animator.addUpdateListener(getUpdateListener(viewHolder, inc, paramsT1, paramsT2));

        mAnimatorSet.playTogether(animator, buttonAnimator, buttonTrans);
        mAnimatorSet.start();

        expandedView = null;
        expandedLayout = null;

        viewHolder.mBackward.setVisibility(View.INVISIBLE);
        viewHolder.mForward.setVisibility(View.INVISIBLE);
    }

    private ValueAnimator.AnimatorUpdateListener getButtonUpdateListener(@NonNull final PlaylistViewHolder viewHolder,
                                                                         @NonNull final ViewGroup.LayoutParams paramsButton,
                                                                         @NonNull final Incrementer buttonInc,
                                                                         @NonNull final Incrementer buttonTransInc) {

        return new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int height = buttonInc.getInterp();
                paramsButton.height = height;
                paramsButton.width = height;

                Log.d("mExpandedHeight", "collapse" + viewHolder.episode.getTitle() + " current height => " + height);
                viewHolder.mPlayPauseButton.setLayoutParams(paramsButton);
                viewHolder.mPlayPauseButton.setTranslationY(buttonTransInc.getInterp());
            }
        };
    }

    private ValueAnimator.AnimatorUpdateListener getUpdateListener(@NonNull final PlaylistViewHolder viewHolder,
                                                                   @NonNull final Incrementer inc,
                                                                   @NonNull final RecyclerView.LayoutParams paramsRecyclerView,
                                                                   @NonNull final RelativeLayout.LayoutParams paramsBackground) {

        return new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Log.d("extended_player", "new Height:" + inc.getInterp());
                paramsRecyclerView.height = inc.getInterp();
                paramsBackground.height = inc.getInterp();

                viewHolder.mLayout.setLayoutParams(paramsRecyclerView);
                viewHolder.mItemBackground.setLayoutParams(paramsBackground);
            }
        };
    }

    private int getDuration(int argHeight) {
        // 1 dp / ms
        int duration = (int)(argHeight / mContext.getResources().getDisplayMetrics().density);
        return duration;
    }

    class Incrementer {
        private int mInterp = -1;
        public void setInterp(int newInterp) {
            mInterp = newInterp;
        }
        public int getInterp() {return mInterp; }
    }
}
