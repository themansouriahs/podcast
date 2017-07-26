package org.bottiger.podcast.activities.feedview;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.UIUtils;
import org.bottiger.podcast.views.DownloadButtonView;
import org.bottiger.podcast.views.FeedViewQueueButton;
import org.bottiger.podcast.views.PlayPauseButton;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aplb on 01-10-2015.
 */
public class EpisodeViewHolder extends FeedViewHolder {

    private static final String TAG = EpisodeViewHolder.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({EXPANDED, COLLAPSED, COLLAPSED_WITH_DESCRIPTION, COLLAPSED_LISTENED})
    public @interface DisplayState {}
    public static final int EXPANDED = 0;
    public static final int COLLAPSED = 1;
    public static final int COLLAPSED_WITH_DESCRIPTION = 2;
    public static final int COLLAPSED_LISTENED = 3;

    @EpisodeViewHolder.DisplayState
    int mState = COLLAPSED;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({GONE, SHORT, LONG})
    public @interface DescriptionLength {}
    public static final int GONE = 0;
    public static final int SHORT = 1;
    public static final int LONG = 2;


    public ViewGroup mContainer;
    public TextView mTitle;
    public TextView mTextSecondary;
    public TextView mDescription;
    public PlayPauseButton mPlayPauseButton;
    public FeedViewQueueButton mQueueButton;
    public DownloadButtonView mDownloadButton;

    public boolean DisplayDescription = false;
    public boolean IsMarkedAsListened = false;

    public @ColorInt int mPrimaryColor = Color.BLACK;
    public @ColorInt int mFadedColor = Color.GRAY;

    @SuppressLint("WrongViewCast")
    public EpisodeViewHolder(View view) {
        super(view);

        mContainer          = view.findViewById(R.id.group);
        mTitle              = view.findViewById(R.id.title);
        mTextSecondary      = view.findViewById(R.id.subtitle);
        mDescription        = view.findViewById(R.id.episode_description);
        mPlayPauseButton    = view.findViewById(R.id.play_pause_button);
        mQueueButton        = view.findViewById(R.id.queue_button);
        mDownloadButton     = view.findViewById(R.id.feedview_download_button);

        if (mPlayPauseButton != null)
            mPlayPauseButton.setIconColor(Color.WHITE);
    }


    public @DisplayState int toggleState(boolean argCanDownload) {

        AutoTransition autoTransition = new AutoTransition();
        autoTransition.setDuration(100);
        TransitionManager.beginDelayedTransition((ViewGroup)mContainer.getParent(), autoTransition);

        @DisplayState int newState;
        if (mState != EXPANDED) {
            newState = EXPANDED;
        } else if (IsMarkedAsListened) {
            newState = COLLAPSED_LISTENED;
        } else if (DisplayDescription) {
            newState = COLLAPSED_WITH_DESCRIPTION;
        } else {
            newState = COLLAPSED;
        }

        setState(newState, argCanDownload);
        mState = newState;

        return mState;
    }

    public void setState(@DisplayState int argState, boolean argCanDownload) {
        Log.d(TAG, "Settings viewholder state to: " + argState);
        switch(argState) {
            case EXPANDED: {
                setStateExpanded(argCanDownload);
                break;
            }
            case COLLAPSED: {
                setStateCollapsed(argCanDownload);
                break;
            }
            case COLLAPSED_WITH_DESCRIPTION: {
                setStateCollapsedWithDescription(argCanDownload);
                break;
            }
            case COLLAPSED_LISTENED: {
                setStateListenedCollapsed(argCanDownload);
                break;
            }
        }
        mState = argState;
    }

    private void setStateExpanded(boolean argCanDownload) {
        setTextColor(false);
        setButtonsVisibility(argCanDownload, true);
        setDescriptionLength(LONG);
        setIsRecyclable(false);
    }

    private void setStateCollapsed(boolean argCanDownload) {
        setTextColor(false);
        setButtonsVisibility(argCanDownload, false);
        setDescriptionLength(GONE);
        setIsRecyclable(true);
    }

    private void setStateCollapsedWithDescription(boolean argCanDownload) {
        setTextColor(false);
        setButtonsVisibility(argCanDownload, true);
        setDescriptionLength(SHORT);
        setIsRecyclable(true);
    }

    private void setStateListenedCollapsed(boolean argCanDownload) {
        setTextColor(true);
        setButtonsVisibility(argCanDownload, false);
        setDescriptionLength(GONE);
        setIsRecyclable(true);
    }

    private void setTextColor(boolean argFaded) {
        int textColor = argFaded ? mFadedColor : mPrimaryColor;

        if (!UIUtils.isInNightMode(mContainer.getResources())) {
            mTitle.setTextColor(textColor);
            mDescription.setTextColor(textColor);
        }
    }

    private void setButtonsVisibility(boolean argDownloadVisible, boolean argQueueVisible) {
        int downloadVisibility = argDownloadVisible ? View.VISIBLE : View.INVISIBLE;
        int queueVisibility = argQueueVisible ? View.VISIBLE : View.GONE;
        mDownloadButton.setVisibility(downloadVisibility);
        mQueueButton.setVisibility(queueVisibility);
    }

    private void setDescriptionLength(@DescriptionLength int argLength) {
        switch (argLength) {
            case GONE: {
                mDescription.setVisibility(View.GONE);
                break;
            }
            case SHORT: {
                int lines = mDescription.getResources().getInteger(R.integer.feed_activity_description_lines_default);
                mDescription.setMaxLines(lines);
                mDescription.setEllipsize(TextUtils.TruncateAt.END);
                mDescription.setVisibility(View.VISIBLE);
                break;
            }
            case LONG: {
                mDescription.setMaxLines(Integer.MAX_VALUE);
                mDescription.setEllipsize(null);
                mDescription.setVisibility(View.VISIBLE);
                break;
            }
        }
    }
}
