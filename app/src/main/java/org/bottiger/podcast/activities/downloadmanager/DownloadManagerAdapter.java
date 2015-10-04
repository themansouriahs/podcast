package org.bottiger.podcast.activities.downloadmanager;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.bottiger.podcast.BR;
import org.bottiger.podcast.R;
import org.bottiger.podcast.activities.openopml.OpenOpmlViewHolder;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 04-10-2015.
 */
public class DownloadManagerAdapter extends RecyclerView.Adapter<DownloadItemViewHolder> {

    private List<IEpisode> mDownloadingEpisodes = new LinkedList<>();

    public DownloadManagerAdapter() {
        super();

        URL url = null;
        try {
            url = new URL("http://www.podtrac.com/pts/redirect.mp3/twit.cachefly.net/audio/twit/twit0529/twit0529.mp3");
        } catch (MalformedURLException e) {
            return;
        }
        IEpisode episode1 = new SlimEpisode("Episode 1", url, "Twit episode description");
        IEpisode episode2 = new SlimEpisode("Episode 2", url, "Twit episode description");
        IEpisode episode3 = new SlimEpisode("Episode 3", url, "Twit episode description");
        IEpisode episode4 = new SlimEpisode("Episode 4", url, "Twit episode description");

        mDownloadingEpisodes.add(episode1);
        mDownloadingEpisodes.add(episode2);
        mDownloadingEpisodes.add(episode3);
        mDownloadingEpisodes.add(episode4);
    }

    @Override
    public DownloadItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_download_manager_item, parent, false);
        DownloadItemViewHolder holder = new DownloadItemViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(DownloadItemViewHolder holder, int position) {
        final IEpisode episode = mDownloadingEpisodes.get(position);
        holder.setEpisode(episode);
        holder.getBinding().setVariable(BR.episode, episode);
        /*
        holder.setSubscription(subscription);
        holder.getBinding().setVariable(BR.handlers, holder);
        holder.getBinding().setVariable(BR.subscription, subscription);
        holder.getBinding().executePendingBindings();
        */
    }

    @Override
    public int getItemCount() {
        return mDownloadingEpisodes.size();
    }
}
