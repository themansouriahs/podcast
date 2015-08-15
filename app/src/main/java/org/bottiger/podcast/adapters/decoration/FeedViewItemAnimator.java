package org.bottiger.podcast.adapters.decoration;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.bottiger.podcast.adapters.FeedViewAdapter;

/**
 * Created by Arvid on 8/15/2015.
 */
public class FeedViewItemAnimator extends DefaultItemAnimator {

    @Override
    public boolean animateChange (RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {

        FeedViewAdapter.EpisodeViewHolder episodeViewHolder = ((FeedViewAdapter.EpisodeViewHolder)oldHolder);

        if(episodeViewHolder != null)
        {
            float alpha = 0.0f;

            if (FeedViewAdapter.mIsExpanded) {
                episodeViewHolder.mDescription.setVisibility(View.VISIBLE);

                alpha = 1.0f;
            }

            episodeViewHolder.mDescription.animate().alpha(alpha);

            if (!FeedViewAdapter.mIsExpanded) {
                episodeViewHolder.mDescription.setVisibility(View.GONE);
            }

            dispatchChangeFinished(oldHolder, true);
        }

        if(newHolder != null)
        {
            dispatchChangeFinished(newHolder, false);
        }

        return false;
    }

}
