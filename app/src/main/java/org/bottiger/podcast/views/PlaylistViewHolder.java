package org.bottiger.podcast.views;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;

import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.ItemCursorAdapter;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.utils.PaletteCache;

import java.util.HashMap;

/**
 * Created by apl on 30-07-2014.
 */
// Provide a reference to the type of views that you are using
// (custom viewholder)
public class PlaylistViewHolder extends RecyclerView.ViewHolder { //  implements View.OnClickListener

    public FeedItem episode = null;
    public ItemCursorAdapter mAdapter = null;

    public RelativeLayoutWithBackground mLayout;
    public RelativeLayout mMainContainer;

    public PlayPauseImageView mPlayPauseButton;
    public PlayerButtonView mForward;
    public PlayerButtonView mBackward;

    public ImageView mItemBackground;
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
    public PlayerButtonView previousButton;
    public DownloadButtonView downloadButton;
    public PlayerButtonView bookmarkButton;
    public PlayerButtonView queueButton;

    public Callback mPicassoCallback;

    private boolean mIsExpanded;
    private int mCollapsedHeight;
    private int mExpandedHeight;

    // ImageView iv, TextView tv1, TextView tv2, TextView tv3, TextView tv4, TextView tv5, TextView tv6, ViewStub vs, View pv
    public PlaylistViewHolder(View view) {
        super(view);
        //view.setOnClickListener(this);

        mLayout = (RelativeLayoutWithBackground) view.findViewById(R.id.item);
        mMainContainer = (RelativeLayout) view.findViewById(R.id.main_player_container);
        mPlayPauseButton = (PlayPauseImageView) view.findViewById(R.id.list_image);
        mForward = (PlayerButtonView) view.findViewById(R.id.fast_forward);
        mBackward = (PlayerButtonView) view.findViewById(R.id.rewind);
        mItemBackground = (ImageView) view.findViewById(R.id.item_background);
        mMainTitle = (TextView) view.findViewById(R.id.title);
        mSubTitle = (TextView) view.findViewById(R.id.podcast);
        mTimeDuration = (TextView) view.findViewById(R.id.duration);
        mCurrentPosition = (TextView) view.getTag(R.id.current_position);
        mSlash = (TextView) view.findViewById(R.id.time_slash);
        mFileSize = (TextView) view.findViewById(R.id.filesize);

        // Expanded layout
        playerLinearLayout = (PlayerLinearLayout) view.findViewById(R.id.expanded_controls);
        timeSlash = (TextView) view.findViewById(R.id.time_slash);
        currentTime = (TextView) view
                .findViewById(R.id.current_position);
        seekbar = (PlayerSeekbar) view.findViewById(R.id.player_progress);

        queueButton = (PlayerButtonView) view.findViewById(R.id.queue);
        previousButton = (PlayerButtonView) view.findViewById(R.id.previous);
        downloadButton = (DownloadButtonView) view
                .findViewById(R.id.download);
        bookmarkButton = (PlayerButtonView) view.findViewById(R.id.bookmark);
        queueButton = (PlayerButtonView) view.findViewById(R.id.queue);
        duration = (TextView) view.findViewById(R.id.duration);
        filesize = (TextView) view.findViewById(R.id.filesize);
    }


    public static void onClick(View view, PlaylistViewHolder viewHolder) {
        if (viewHolder.episode == null) {
            return;
        }

        if (viewHolder.mAdapter == null) {
            return;
        }

        int listPost = viewHolder.getPosition();
        int type = viewHolder.mAdapter.getItemViewType(listPost);

        if (type == ItemCursorAdapter.TYPE_FIRST) {
            return;
        }

        boolean doAnimate = true;

        int newType = viewHolder.mAdapter.toggleItem(viewHolder.episode.getId());

        if (newType == ItemCursorAdapter.TYPE_EXPAND) {
            if (ItemCursorAdapter.expandedView != null) {
                viewHolder.mAdapter.collapse(ItemCursorAdapter.expandedView, doAnimate);
            }
            viewHolder.mAdapter.expand(viewHolder, doAnimate, true);
            viewHolder.setExpanded(true);
        } else {
            viewHolder.mAdapter.collapse(viewHolder, doAnimate);
            viewHolder.setExpanded(false);
        }
        }

    public boolean isExpanded() {
        return mIsExpanded;
    }

    public void setExpanded(boolean isExpanded) {mIsExpanded = isExpanded;}
}
