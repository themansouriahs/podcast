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
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.bottiger.podcast.R;
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

    public static PlaylistViewHolder expandedView = null;
    public static RelativeLayoutWithBackground expandedLayout = null;

    public PlaylistViewHolderExpanderHelper(@NonNull Context argContext, @NonNull ItemCursorAdapter argAdapter, int argExpandedHeight) {
        mContext = argContext;
        mAdapter = argAdapter;
        mExpandedHeight = argExpandedHeight;
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

            viewHolder.mPlayPauseButton.getLayoutParams().height = PlayPauseImageView.getLargeSize(mContext);
            viewHolder.mPlayPauseButton.getLayoutParams().width = PlayPauseImageView.getLargeSize(mContext);
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

            viewHolder.mPlayPauseButton.getLayoutParams().height = PlayPauseImageView.getSmallSize(mContext);
            viewHolder.mPlayPauseButton.getLayoutParams().width = PlayPauseImageView.getSmallSize(mContext);

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

        //final ItemCursorAdapter adapter = this;

        // 1dp/ms
        int duration = (int) (targtetHeight / player.getContext().getResources().getDisplayMetrics().density);

        if (minHeight < 0) {
            minHeight = imageStartHeight;
            maxHeight = targtetHeight; // evt imageStartHeight+initialPlayerMeasureHeig
        }

        class Incrementer {
            private int mHeight = -1;
            public void setHeight(int newHeight) {
                mHeight = newHeight;
            }
            public int getHeight() {return mHeight; }
        }
        final Incrementer inc = new Incrementer();
        final Incrementer buttonInc = new Incrementer();
        final Incrementer buttonTrans = new Incrementer();

        ObjectAnimator animator;
        ObjectAnimator buttonAnimator;
        ObjectAnimator buttonTransAnimator = ObjectAnimator.ofInt(buttonTrans, "height", 0, 200);

        if (minHeight > 0) {
            Log.d("mExpandedHeight", "expand goal => " + maxHeight);
            animator = ObjectAnimator.ofInt(inc, "height", minHeight, maxHeight); // targtetHeight
        } else {
            animator = ObjectAnimator.ofInt(inc, "height", imageStartHeight, initialMeasuredHeight); // targtetHeight
            minHeight = imageStartHeight;
            maxHeight = initialMeasuredHeight;
        }
        animator.setDuration(duration);
        buttonTransAnimator.setDuration(duration);


        final RecyclerView.LayoutParams paramsT1 = (RecyclerView.LayoutParams)viewHolder.mLayout.getLayoutParams();
        final RelativeLayout.LayoutParams paramsT2 = (RelativeLayout.LayoutParams)viewHolder.mItemBackground.getLayoutParams();
        final ViewGroup.LayoutParams paramsButton = viewHolder.mPlayPauseButton.getLayoutParams();

        int smallButton = mContext.getResources().getDimensionPixelSize(R.dimen.playpause_button_size_normal);
        int largeButton = mContext.getResources().getDimensionPixelSize(R.dimen.playpause_button_size);
        //ValueAnimator wAnimator = ObjectAnimator.ofInt(viewHolder.mPlayPauseButton, "layout_width", smallButton, largeButton);
        //ValueAnimator hAnimator = ObjectAnimator.ofInt(viewHolder.mPlayPauseButton, "layout_height", smallButton, largeButton);
        buttonAnimator = ObjectAnimator.ofInt(buttonInc, "height", smallButton, largeButton);
        buttonAnimator.setDuration(duration);

        buttonAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int height = buttonInc.getHeight();
                paramsButton.height = height;
                paramsButton.width = height;
                //paramsButton.height = height/2;
                //paramsButton.width = height/2;

                Log.d("mExpandedHeight", "expand " + viewHolder.episode.getTitle() + " current height => " + height);
                viewHolder.mPlayPauseButton.setLayoutParams(paramsButton);
                viewHolder.mPlayPauseButton.setTranslationY(buttonTrans.getHeight());
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int height = inc.getHeight();
                paramsT1.height = height;
                paramsT2.height = height;
                //paramsButton.height = height/2;
                //paramsButton.width = height/2;

                Log.d("mExpandedHeight", "current height => " + height);
                //viewHolder.mPlayPauseButton.setLayoutParams(paramsButton);

                viewHolder.mLayout.setLayoutParams(paramsT1);
                viewHolder.mItemBackground.setLayoutParams(paramsT2);
            }
        });

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

        //animator.start();

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animator, buttonAnimator, buttonTransAnimator);
        animatorSet.start();

        expandedView = viewHolder;
        expandedLayout = layoutWithBackground;

    }

    private static int minHeight = -1;
    private static int maxHeight = -1;

    private void collapse(final PlaylistViewHolder viewHolder) {
        final PlayerLinearLayout player = viewHolder.playerLinearLayout;
        final RelativeLayoutWithBackground layoutWithBackground = viewHolder.mLayout;
        final RelativeLayout mainContainer = viewHolder.mMainContainer;

        final int initialHeight = layoutWithBackground.getHeight();
        final int targerHeight = mainContainer.getHeight();

        layoutWithBackground.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // 1dp/ms
        int duration = (int)(initialHeight / player.getContext().getResources().getDisplayMetrics().density);

        class Incrementer {
            private int mHeight = -1;
            public void setHeight(int newHeight) {
                mHeight = newHeight;
            }
            public int getHeight() {return mHeight; }
        }
        final Incrementer inc = new Incrementer();
        final Incrementer buttonInc = new Incrementer();
        final Incrementer buttonTransInc = new Incrementer();

        //ObjectAnimator animator = ObjectAnimator.ofInt(inc, "height", initialHeight, targerHeight);

        ObjectAnimator buttonAnimator;
        ObjectAnimator animator = ObjectAnimator.ofInt(inc, "height", maxHeight, minHeight);
        ObjectAnimator buttonTrans = ObjectAnimator.ofInt(buttonTransInc, "height", 200, 0);
        animator.setDuration(duration);
        buttonTrans.setDuration(duration);

        final RecyclerView.LayoutParams paramsT1 = (RecyclerView.LayoutParams)viewHolder.mLayout.getLayoutParams();
        final RelativeLayout.LayoutParams paramsT2 = (RelativeLayout.LayoutParams)viewHolder.mItemBackground.getLayoutParams();

        final ViewGroup.LayoutParams paramsButton = viewHolder.mPlayPauseButton.getLayoutParams();

        int smallButton = mContext.getResources().getDimensionPixelSize(R.dimen.playpause_button_size_normal);
        int largeButton = mContext.getResources().getDimensionPixelSize(R.dimen.playpause_button_size);
        //ValueAnimator wAnimator = ObjectAnimator.ofInt(viewHolder.mPlayPauseButton, "layout_width", smallButton, largeButton);
        //ValueAnimator hAnimator = ObjectAnimator.ofInt(viewHolder.mPlayPauseButton, "layout_height", smallButton, largeButton);
        buttonAnimator = ObjectAnimator.ofInt(buttonInc, "height", largeButton,smallButton);
        buttonAnimator.setDuration(duration);

        buttonAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int height = buttonInc.getHeight();
                paramsButton.height = height;
                paramsButton.width = height;
                //paramsButton.height = height/2;
                //paramsButton.width = height/2;

                Log.d("mExpandedHeight", "collapse" + viewHolder.episode.getTitle() + " current height => " + height);
                viewHolder.mPlayPauseButton.setLayoutParams(paramsButton);
                viewHolder.mPlayPauseButton.setTranslationY(buttonTransInc.getHeight());
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Log.d("extended_player", "new Height:" + inc.getHeight());
                paramsT1.height = inc.getHeight();
                paramsT2.height = inc.getHeight();

                viewHolder.mLayout.setLayoutParams(paramsT1);
                viewHolder.mItemBackground.setLayoutParams(paramsT2);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
        //animator.start();
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animator, buttonAnimator, buttonTrans);
        animatorSet.start();

        expandedView = null;
        expandedLayout = null;

        viewHolder.mBackward.setVisibility(View.INVISIBLE);
        viewHolder.mForward.setVisibility(View.INVISIBLE);
    }
}
