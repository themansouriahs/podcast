package org.bottiger.podcast.activities.feedview;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.provider.ISubscription;

/**
 * Base class for FeedViewAdapter, consolidating all type checking and differencing in one place.
 */
public abstract class FeedViewAdapterBase extends RecyclerView.Adapter<FeedViewHolder> {

    @Override
    public abstract FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType);

    protected void onBindViewHolderEpisode(EpisodeViewHolder episodeViewHolder, int position) {
    }

    protected void onBindViewHolderFooter(FooterViewHolder footerViewHolder, int position) {
    }

    @Override
    public void onBindViewHolder(FeedViewHolder feedViewHolder, int position) {

        if (feedViewHolder instanceof FooterViewHolder) {
            onBindViewHolderFooter((FooterViewHolder) feedViewHolder, position);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onBindViewHolderEpisode((EpisodeViewHolder) feedViewHolder, position);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    protected void onClickEpisode(EpisodeViewHolder episodeViewHolder, int dataPosition, boolean argCanDownload) {
    }

    protected void onClickFooter(FooterViewHolder footerViewHolder, int dataPosition, boolean argCanDownload) {
    }

    public void onClick(FeedViewHolder feedViewHolder, int dataPosition, boolean argCanDownload) {

        if (feedViewHolder instanceof FooterViewHolder) {
            onClickFooter((FooterViewHolder) feedViewHolder, dataPosition, argCanDownload);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onClickEpisode((EpisodeViewHolder) feedViewHolder, dataPosition, argCanDownload);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    protected void onViewAttachedToWindowEpisode(EpisodeViewHolder episodeViewHolder) {
    }

    protected void onViewAttachedToWindowFooter(FooterViewHolder footerViewHolder) {
    }

    @Override
    public void onViewAttachedToWindow(FeedViewHolder feedViewHolder) {
        super.onViewAttachedToWindow(feedViewHolder);

        if (feedViewHolder instanceof FooterViewHolder) {
            onViewAttachedToWindowFooter((FooterViewHolder) feedViewHolder);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onViewAttachedToWindowEpisode((EpisodeViewHolder) feedViewHolder);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    protected void onViewDetachedFromWindowEpisode(EpisodeViewHolder episodeViewHolder) {
    }

    protected void onViewDetachedFromWindowFooter(FooterViewHolder footerViewHolder) {
    }

    @Override
    public void onViewDetachedFromWindow(FeedViewHolder feedViewHolder) {

        if (feedViewHolder instanceof FooterViewHolder) {
            onViewDetachedFromWindowFooter((FooterViewHolder) feedViewHolder);
            super.onViewDetachedFromWindow(feedViewHolder);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onViewDetachedFromWindowEpisode((EpisodeViewHolder) feedViewHolder);
            super.onViewDetachedFromWindow(feedViewHolder);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    protected void onViewRecycledEpisode(EpisodeViewHolder episodeViewHolder) {
    }

    protected void onViewRecycledFooter(FooterViewHolder footerViewHolder) {
    }

    @Override
    public void onViewRecycled(FeedViewHolder feedViewHolder) {

        if (feedViewHolder instanceof FooterViewHolder) {
            onViewRecycledFooter((FooterViewHolder) feedViewHolder);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            onViewRecycledEpisode((EpisodeViewHolder) feedViewHolder);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }

    protected void getPaletteEpisode(@NonNull final EpisodeViewHolder episodeViewHolder) {
    }

    protected void getPaletteFooter(@NonNull final FooterViewHolder footerViewHolder) {
    }

    protected void getPalette(@NonNull final FeedViewHolder feedViewHolder) {

        if (feedViewHolder instanceof FooterViewHolder) {
            getPaletteFooter((FooterViewHolder) feedViewHolder);
            return;
        }

        if (feedViewHolder instanceof EpisodeViewHolder) {
            getPaletteEpisode((EpisodeViewHolder) feedViewHolder);
            return;
        }

        throw new RuntimeException("Missing feedViewHolder instanceof check");
    }
}
