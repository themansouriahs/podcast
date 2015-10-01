package org.bottiger.podcast.activities.feedview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.ColorUtils;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.FeedViewQueueButton;
import org.bottiger.podcast.views.PlayPauseImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aplb on 01-10-2015.
 */
public class EpisodeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({EXPANDED, COLLAPSED, COLLAPSED_WITH_DESCRIPTION})
    public @interface DisplayState {}
    public static final int EXPANDED = 0;
    public static final int COLLAPSED = 1;
    public static final int COLLAPSED_WITH_DESCRIPTION = 2;

    @EpisodeViewHolder.DisplayState
    int mState = COLLAPSED;


    public ViewGroup mContainer;
    public TextView mTitle;
    public TextView mTextSecondary;
    public TextView mDescription;
    public PlayPauseImageView mPlayPauseButton;
    public FeedViewQueueButton mQueueButton;
    public DownloadButtonView mDownloadButton;

    public boolean DisplayDescription = false;
    public boolean IsMarkedAsListened = false;

    @SuppressLint("WrongViewCast")
    public EpisodeViewHolder(View view) {
        super(view);
        view.setOnClickListener(this);

        mContainer = (ViewGroup) view.findViewById(R.id.group);
        mTitle = (TextView) view.findViewById(R.id.title);
        mTextSecondary = (TextView) view.findViewById(R.id.subtitle);
        mDescription = (TextView) view.findViewById(R.id.episode_description);
        mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.play_pause_button);
        mQueueButton = (FeedViewQueueButton) view.findViewById(R.id.queue_button);
        mDownloadButton = (DownloadButtonView) view.findViewById(R.id.feedview_download_button);
    }


    @Override
    public void onClick(View view) {

        if (Build.VERSION.SDK_INT >= 19) {
            AutoTransition autoTransition = new AutoTransition();
            autoTransition.setDuration(100);
            TransitionManager.beginDelayedTransition((ViewGroup)mContainer.getParent(), autoTransition);
        }

        Context context = view.getContext();

        if (mState == EXPANDED) {
            doCollapse(context);
            setIsRecyclable(true);
            mState = DisplayDescription ? COLLAPSED_WITH_DESCRIPTION : COLLAPSED;
        } else {
            doExpand(context);
            setIsRecyclable(false);
            mState = EXPANDED;
        }
        view.postInvalidate();
        return;
    }

    private void doExpand(@NonNull Context argContext) {

        int color = ColorUtils.getTextColor(argContext);

        mTitle.setTextColor(color);
        mDescription.setTextColor(color);

        mDescription.setVisibility(View.VISIBLE);
        mDescription.setMaxLines(Integer.MAX_VALUE);
        mDescription.setEllipsize(null);
    }

    private void doCollapse(@NonNull Context argContext) {
        int color = !IsMarkedAsListened ? ColorUtils.getTextColor(argContext) : ColorUtils.getFadedTextColor(argContext);

        mTitle.setTextColor(color);
        mDescription.setTextColor(color);

        int lines = mDescription.getResources().getInteger(R.integer.feed_activity_description_lines_default);
        mDescription.setMaxLines(lines);
        mDescription.setEllipsize(TextUtils.TruncateAt.END);

        if (IsMarkedAsListened || !DisplayDescription) {
            mDescription.setVisibility(View.GONE);
        } else {

        }
    }

    public void modifyLayout(@NonNull ViewGroup argParent) {


        //if (Build.VERSION.SDK_INT >= 19) {
        //    TransitionManager.beginDelayedTransition(argParent);
        //}

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mTitle.getLayoutParams();

        if (DisplayDescription) {
            if (Build.VERSION.SDK_INT >= 17) {
                params.removeRule(RelativeLayout.CENTER_VERTICAL);
            } else {
                params.addRule(RelativeLayout.CENTER_VERTICAL, 0);
            }
        } else {
            params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        }

        //mTitle.setLayoutParams(params);

        int visibility = DisplayDescription ? View.VISIBLE : View.GONE;
        float alphaStart = DisplayDescription ? 0.0f : 1.0f;
        float alphaEnd = DisplayDescription ? 1.0f : 0.0f;

        mDescription.setVisibility(visibility);
        mQueueButton.setVisibility(visibility);

        //mDescription.setAlpha(alphaStart);
        //mDescription.animate().alpha(alphaEnd).start();

        //ViewCompat.animate(mDescription).alpha(alphaEnd).start();

        argParent.updateViewLayout(mTitle, params);

        //if (Build.VERSION.SDK_INT >= 17) {
        //    LayoutTransition transition = argParent.getLayoutTransition();
        //    transition.enableTransitionType(LayoutTransition.CHANGING);
        //}
    }
}
