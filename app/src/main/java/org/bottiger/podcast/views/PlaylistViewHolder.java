package org.bottiger.podcast.views;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;

import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.adapters.viewholders.ExpandableViewHoldersUtil;
import org.bottiger.podcast.provider.FeedItem;

/**
 * Created by apl on 30-07-2014.
 */
// Provide a reference to the type of views that you are using
// (custom viewholder)
public class PlaylistViewHolder extends RecyclerView.ViewHolder implements ExpandableViewHoldersUtil.Expandable, View.OnClickListener { //

    public FeedItem episode = null;
    public PlaylistAdapter mAdapter = null;

    public RelativeLayoutWithBackground mLayout;
    public RelativeLayout mMainContainer;

    public PlayPauseImageView mPlayPauseButton;
    public PlayerButtonView mForward;
    public PlayerButtonView mBackward;

    public View mActionBarGradientView;

    public ImageViewTinted mItemBackground;
    public TextView mMainTitle;
    public TextView mSubTitle;
    public TextView mTimeDuration;
    public TextView mCurrentPosition;
    public TextView mSlash;
    public TextView mFileSize;

    // expnded extended_player
    public PlayerLinearLayout playerLinearLayout;
    public TextView timeSlash;
    public PlayerSeekbar seekbar;
    public TextView currentTime;
    public TextView duration;
    public TextView filesize;

    public PlayerButtonView favoriteButton;
    public PlayerButtonView previousButton;
    public DownloadButtonView downloadButton;

    public Callback mPicassoCallback;

    // ImageView iv, TextView tv1, TextView tv2, TextView tv3, TextView tv4, TextView tv5, TextView tv6, ViewStub vs, View pv
    public PlaylistViewHolder(View view) {
        super(view);
        //view.setOnClickListener(this);

        mLayout = (RelativeLayoutWithBackground) view.findViewById(R.id.item);
        mMainContainer = (RelativeLayout) view.findViewById(R.id.main_player_container);
        mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.list_image);
        //mForward = (PlayerButtonView) view.findViewById(R.id.fast_forward);
        //mBackward = (PlayerButtonView) view.findViewById(R.id.rewind);
        mItemBackground = (ImageViewTinted) view.findViewById(R.id.item_background);
        mMainTitle = (TextView) view.findViewById(R.id.podcast_title);
        //mSubTitle = (TextView) view.findViewById(R.id.podcast);
        mTimeDuration = (TextView) view.findViewById(R.id.duration);
        mCurrentPosition = (TextView) view.getTag(R.id.current_position);
        mSlash = (TextView) view.findViewById(R.id.time_slash);
        mFileSize = (TextView) view.findViewById(R.id.filesize);

        mActionBarGradientView = view.findViewById(R.id.episode_top_gradient);

        // Expanded layout
        playerLinearLayout = (PlayerLinearLayout) view.findViewById(R.id.expanded_controls);
        timeSlash = (TextView) view.findViewById(R.id.time_slash);
        currentTime = (TextView) view
                .findViewById(R.id.current_position);
        seekbar = (PlayerSeekbar) view.findViewById(R.id.player_progress);

        favoriteButton = (PlayerButtonView) view.findViewById(R.id.favorite);
        previousButton = (PlayerButtonView) view.findViewById(R.id.previous);
        downloadButton = (DownloadButtonView) view
                .findViewById(R.id.download);
        duration = (TextView) view.findViewById(R.id.duration);
        filesize = (TextView) view.findViewById(R.id.filesize);
    }

    @Override
    public View getExpandView() {
        return playerLinearLayout;
    }

    @Override
    public void onClick(View v) {
        return;
    }
}
