package org.bottiger.podcast.views;

import android.app.Activity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.PlaylistAdapter;
import org.bottiger.podcast.adapters.viewholders.ExpandableViewHoldersUtil;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by apl on 30-07-2014.
 */
// Provide a reference to the type of views that you are using
// (custom viewholder)
public class PlaylistViewHolder extends RecyclerView.ViewHolder implements ExpandableViewHoldersUtil.Expandable,
                                                                            View.OnClickListener,
                                                                            View.OnCreateContextMenuListener { //

    public IEpisode episode = null;
    public PlaylistAdapter mAdapter = null;

    public CardView mLayout;
    public RelativeLayout mMainContainer;

    public PlayPauseImageView mPlayPauseButton;
    public PlayerButtonView mForward;
    public PlayerButtonView mBackward;

    public View mActionBarGradientView;

    public CardView mPodcastImage;
    public ScrollView mExpandedLayoutBottom;

    public ImageViewTinted mItemBackground;
    public TextView mMainTitle;
    public TextView mSubTitle;
    public TextView mTimeDuration;
    public ImageView mTimeDurationIcon;
    public TextView mCurrentPosition;
    public TextView mSlash;
    public TextView mFileSize;
    public TextView mPlaylistPosition;

    // expnded extended_player
    public RelativeLayout playerRelativeLayout;
    public LinearLayout buttonLayout;
    public TextView timeSlash;
    public PlayerSeekbar seekbar;
    public TextView currentTime;
    public TextView filesize;
    public TextView description;

    public PlayerButtonView favoriteButton;
    public PlayerButtonView removeButton;
    public DownloadButtonView downloadButton;

    private Activity mActivity;

    // ImageView iv, TextView tv1, TextView tv2, TextView tv3, TextView tv4, TextView tv5, TextView tv6, ViewStub vs, View pv
    public PlaylistViewHolder(View view, Activity argActivity) {
        super(view);
        //view.setOnClickListener(this);

        mActivity = argActivity;

        mLayout = (CardView) view.findViewById(R.id.item);
        mMainContainer = (RelativeLayout) view.findViewById(R.id.main_player_container);
        mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.list_image);
        //mForward = (PlayerButtonView) view.findViewById(R.id.fast_forward);
        //mBackward = (PlayerButtonView) view.findViewById(R.id.rewind);
        mItemBackground = (ImageViewTinted) view.findViewById(R.id.item_background);
        mMainTitle = (TextView) view.findViewById(R.id.podcast_title);
        mTimeDuration = (TextView) view.findViewById(R.id.podcast_duration);
        mTimeDurationIcon = (ImageView) view.findViewById(R.id.podcast_duration_ic);
        mCurrentPosition = (TextView) view.getTag(R.id.current_position);
        //mFileSize = (TextView) view.findViewById(R.id.filesize);
        mPlaylistPosition = (TextView) view.findViewById(R.id.playlist_position);

        mPodcastImage = (CardView) view.findViewById(R.id.left_image);

        mActionBarGradientView = view.findViewById(R.id.episode_top_gradient);

        // Expanded layout
        playerRelativeLayout = (RelativeLayout) view.findViewById(R.id.expanded_layout);
        currentTime = (TextView) view
                .findViewById(R.id.current_position);
        seekbar = (PlayerSeekbar) view.findViewById(R.id.player_progress);

        favoriteButton = (PlayerButtonView) view.findViewById(R.id.favorite);
        removeButton = (PlayerButtonView) view.findViewById(R.id.remove_episode);
        downloadButton = (DownloadButtonView) view
                .findViewById(R.id.download);
        buttonLayout = (LinearLayout) view.findViewById(R.id.expanded_buttons_layout);
        mExpandedLayoutBottom = (ScrollView) view.findViewById(R.id.expanded_layout_bottom);
        description = (TextView) view.findViewById(R.id.podcast_description);

        //view.setOnCreateContextMenuListener(this);
    }

    @Override
    public View getExpandView() {
        return playerRelativeLayout;
    }

    @Override
    public void onClick(View v) {
        return;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = mActivity.getMenuInflater();
        //inflater.inflate(R.menu.playlist_context_menu, menu);
    }
}
