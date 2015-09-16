package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.utils.PaletteHelper;
import org.bottiger.podcast.views.PlayPauseImageView;

import java.util.ArrayList;

/**
 * Created by apl on 21-04-2015.
 */
public class FeedViewDiscoveryAdapter extends FeedViewAdapter {

    private ArrayList<SlimEpisode> mEpisodes = new ArrayList<>();

    public FeedViewDiscoveryAdapter(@NonNull Activity activity, @NonNull ISubscription argSubscription, @Nullable Cursor dataset) {
        super(activity, argSubscription, dataset);
    }

    @Override
    public void setDataset(Cursor c) {
        return;
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }

    public void setDataset(@NonNull ArrayList<SlimEpisode> argEpisodes) {
        for (IEpisode episode : argEpisodes) {
            if (episode instanceof SlimEpisode)
                mEpisodes.add((SlimEpisode)episode);
        }
    }

    @Override
    protected IEpisode getItemForPosition(int argPosition) {
        return mEpisodes.get(argPosition);
    }

    @Override
    protected void bindButtons(@NonNull EpisodeViewHolder episodeViewHolder, @NonNull IEpisode argEpisode) {

        episodeViewHolder.mPlayPauseButton.setEpisode(argEpisode, PlayPauseImageView.DISCOVERY_FEEDVIEW);
        episodeViewHolder.mQueueButton.setEpisode(argEpisode, PlayPauseImageView.DISCOVERY_FEEDVIEW);
        episodeViewHolder.mDownloadButton.setVisibility(View.GONE);

        getPalette(episodeViewHolder);
        return;
    }
}
