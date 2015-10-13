package org.bottiger.podcast.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.bottiger.podcast.activities.feedview.EpisodeViewHolder;
import org.bottiger.podcast.activities.feedview.FeedViewAdapter;
import org.bottiger.podcast.listeners.PlayerStatusObservable;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.views.PlayPauseImageView;

import java.util.ArrayList;

/**
 * Created by apl on 21-04-2015.
 */
public class FeedViewDiscoveryAdapter extends FeedViewAdapter {

    private ArrayList<SlimEpisode> mEpisodes = new ArrayList<>();

    public FeedViewDiscoveryAdapter(@NonNull Activity activity, @NonNull ISubscription argSubscription) {
        super(activity, argSubscription);
    }

    @Override
    public EpisodeViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return super.onCreateViewHolder(viewGroup, i);
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size();
    }

    public void setDataset(@NonNull SortedList<SlimEpisode> argEpisodes) {
        for (int i = 0; i < argEpisodes.size(); i++) {
            SlimEpisode episode = argEpisodes.get(i);
            mEpisodes.add(episode);
        }
    }

    @Override
    protected IEpisode getItemForPosition(int argPosition) {
        return mEpisodes.get(argPosition);
    }

    @Override
    protected void bindButtons(@NonNull EpisodeViewHolder episodeViewHolder, @NonNull IEpisode argEpisode) {

        //episodeViewHolder.mDescription.setVisibility(View.VISIBLE);
        episodeViewHolder.DisplayDescription = true;
        //episodeViewHolder.modifyLayout((RelativeLayout)episodeViewHolder.itemView);

        episodeViewHolder.mPlayPauseButton.setEpisode(argEpisode, PlayPauseImageView.DISCOVERY_FEEDVIEW);
        episodeViewHolder.mQueueButton.setEpisode(argEpisode, PlayPauseImageView.DISCOVERY_FEEDVIEW);
        episodeViewHolder.mDownloadButton.setVisibility(View.INVISIBLE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) episodeViewHolder.mQueueButton.getLayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            params.removeRule(RelativeLayout.BELOW);
        } else {
            params.addRule(RelativeLayout.BELOW, 0);
        }

        episodeViewHolder.mPlayPauseButton.setStatus(PlayerStatusObservable.STOPPED);

        getPalette(episodeViewHolder);
        return;
    }
}
