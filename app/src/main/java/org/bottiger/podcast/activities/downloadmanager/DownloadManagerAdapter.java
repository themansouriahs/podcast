package org.bottiger.podcast.activities.downloadmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import org.bottiger.podcast.BR;
import org.bottiger.podcast.R;
import org.bottiger.podcast.activities.openopml.OpenOpmlViewHolder;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.utils.ImageLoaderUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 04-10-2015.
 */
public class DownloadManagerAdapter extends RecyclerView.Adapter<DownloadItemViewHolder> {

    @NonNull
    private Context mContext;
    private List<IEpisode> mDownloadingEpisodes = new LinkedList<>();

    public DownloadManagerAdapter(@NonNull Context argContext) {
        super();
        mContext = argContext;

        long fileSize = 142356744;

        URL url = null;
        URL urlImage = null;
        try {
            url = new URL("http://www.podtrac.com/pts/redirect.mp3/twit.cachefly.net/audio/twit/twit0529/twit0529.mp3");
            urlImage = new URL("http://twit.cachefly.net/coverart/twit/twit144audio.jpg");
        } catch (MalformedURLException e) {
            return;
        }
        SlimEpisode episode1 = new SlimEpisode("Episode 1", url, "Twit episode description");
        SlimEpisode episode2 = new SlimEpisode("Episode 2", url, "Twit episode description");
        SlimEpisode episode3 = new SlimEpisode("Episode 3", url, "Twit episode description");
        SlimEpisode episode4 = new SlimEpisode("Episode 4", url, "Twit episode description");

        episode1.setArtwork(urlImage);
        episode2.setArtwork(urlImage);
        episode3.setArtwork(urlImage);
        episode4.setArtwork(urlImage);

        episode1.setFilesize(fileSize);
        episode2.setFilesize(fileSize);
        episode3.setFilesize(fileSize);
        episode4.setFilesize(fileSize);

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
        DownloadViewModel viewModel = new DownloadViewModel(mContext, episode, position);
        holder.getBinding().setVariable(BR.viewModel, viewModel);

        ImageLoaderUtils.loadImageInto(holder.mImageView, episode.getArtwork(mContext).toString(), false, true);
    }

    @Override
    public int getItemCount() {
        return mDownloadingEpisodes.size();
    }

    boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mDownloadingEpisodes, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mDownloadingEpisodes, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }
}
