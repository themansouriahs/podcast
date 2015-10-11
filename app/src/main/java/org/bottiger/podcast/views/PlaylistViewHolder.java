package org.bottiger.podcast.views;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.adapters.viewholders.ExpandableViewHoldersUtil;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 30-07-2014.
 */
// Provide a reference to the type of views that you are using
// (custom viewholder)
public class PlaylistViewHolder extends RecyclerView.ViewHolder implements ExpandableViewHoldersUtil.Expandable { //

    public IEpisode episode = null;
    public PlaylistAdapter mAdapter = null;

    public CardView mLayout;
    public RelativeLayout mMainContainer;

    public PlayPauseImageView mPlayPauseButton;
    public PlayerButtonView mForward;
    public PlayerButtonView mBackward;

    public ScrollView mExpandedLayoutBottom;

    public ImageViewTinted mPodcastImage;
    public TextView mMainTitle;
    public TextView mSubTitle;
    public TextView mTimeDuration;
    //public ImageView mTimeDurationIcon;
    public TextView mCurrentPosition;
    public TextView mSlash;
    public TextView mFileSize;
    public TextView mPlaylistPosition;

    // expnded extended_player
    public RelativeLayout mExpandedLayoutControls;
    public LinearLayout buttonLayout;
    public TextView timeSlash;
    public PlayerSeekbar seekbar;
    public TextView currentTime;
    public TextView filesize;
    public TextView description;

    public MaterialFavoriteButton favoriteButton;
    public PlayerButtonView removeButton;
    public DownloadButtonView downloadButton;

    private Activity mActivity;
    public String mArtwork;

    private boolean mHasColor;
    private int mEpisodePrimaryColor;

    // ImageView iv, TextView tv1, TextView tv2, TextView tv3, TextView tv4, TextView tv5, TextView tv6, ViewStub vs, View pv
    public PlaylistViewHolder(View view, Activity argActivity) {
        super(view);

        mActivity = argActivity;

        mLayout = (CardView) view.findViewById(R.id.item);
        mMainContainer = (RelativeLayout) view.findViewById(R.id.main_player_container);
        mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.list_image);
        mPodcastImage = (ImageViewTinted) view.findViewById(R.id.podcast_image);
        mMainTitle = (TextView) view.findViewById(R.id.podcast_title);
        mTimeDuration = (TextView) view.findViewById(R.id.podcast_duration);
        //mTimeDurationIcon = (ImageView) view.findViewById(R.id.podcast_duration_ic);
        mCurrentPosition = (TextView) view.getTag(R.id.current_position);
        mPlaylistPosition = (TextView) view.findViewById(R.id.playlist_position);

        // Expanded layout
        mExpandedLayoutControls = (RelativeLayout) view.findViewById(R.id.expanded_layout_controls);
        currentTime = (TextView) view
                .findViewById(R.id.current_position);
        seekbar = (PlayerSeekbar) view.findViewById(R.id.top_player_seekbar);

        favoriteButton = (MaterialFavoriteButton) view.findViewById(R.id.favorite);
        removeButton = (PlayerButtonView) view.findViewById(R.id.remove_episode);
        downloadButton = (DownloadButtonView) view
                .findViewById(R.id.expanded_download);
        buttonLayout = (LinearLayout) view.findViewById(R.id.expanded_buttons_layout);
        mExpandedLayoutBottom = (ScrollView) view.findViewById(R.id.expanded_layout_bottom);
        description = (TextView) view.findViewById(R.id.podcast_description);
    }

    @Override
    public View getExpandView() {
        return mExpandedLayoutControls;
    }

    public void setArtwork(@Nullable String argArtwork) {
        mArtwork = argArtwork;
    }

    public String getArtwork() {
        return mArtwork;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public int getEpisodePrimaryColor() {
        return mEpisodePrimaryColor;
    }

    public void setEpisodePrimaryColor(int mEpisodePrimaryColor) {
        this.mEpisodePrimaryColor = mEpisodePrimaryColor;
        this.mHasColor = true;
    }

    public boolean hasColor() {
        return mHasColor;
    }
}
