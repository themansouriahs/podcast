package org.bottiger.podcast.activities.downloadmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bottiger.podcast.BR;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.Downloader.engines.IDownloadEngine;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ImageLoaderUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 04-10-2015.
 */
public class DownloadManagerAdapter extends RecyclerView.Adapter<DownloadItemViewHolder> {

    @NonNull
    private Context mContext;
    private SoundWavesDownloadManager mDownloadManager;
    private List<IEpisode> mDownloadingEpisodes = new LinkedList<>();
    private List<DownloadViewModel> mViewModels = new LinkedList<>();

    private TextView mEmptyTextView;

    public DownloadManagerAdapter(@NonNull Context argContext, @NonNull TextView argEmptyTextView) {
        super();
        mContext = argContext;
        mEmptyTextView = argEmptyTextView;

        mDownloadManager = SoundWaves.getDownloadManager();
        IEpisode downloadingEpisode = mDownloadManager.getDownloadingItem();
        if (downloadingEpisode != null)
            mDownloadingEpisodes.add(downloadingEpisode);

        for (int i = 0; i < mDownloadManager.getQueueSize(); i++) {
            mDownloadingEpisodes.add(mDownloadManager.getQueueItem(i).getEpisode());
        }

        setEmptyTextViewVisibility();
    }

    @Override
    public DownloadItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_download_manager_item, parent, false);
        return new DownloadItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(DownloadItemViewHolder holder, int position) {
        final IEpisode episode = mDownloadingEpisodes.get(position);
        DownloadViewModel viewModel = new DownloadViewModel(mContext, this, (FeedItem)episode, position); // FIXME no type casting
        mViewModels.add(viewModel);
        holder.getBinding().setVariable(BR.viewModel, viewModel);

        ImageLoaderUtils.loadImageInto(holder.mImageView, episode.getArtwork().toString(), false, true);
    }

    @Override
    public int getItemCount() {
        return mDownloadingEpisodes.size();
    }

    public void removed(int argPosition) {
        super.notifyItemRemoved(argPosition);
        IEpisode episode = mDownloadingEpisodes.get(argPosition);
        if (argPosition == 0) {
            mDownloadManager.cancelCurrentDownload();
        } else {
            mDownloadManager.removeFromQueue(episode);
        }
        mDownloadingEpisodes.remove(episode);
        setEmptyTextViewVisibility();
        super.notifyDataSetChanged();
    }

    public void downloadComplete(@NonNull IEpisode argEpisode) {

    }

    public void updateProgress(@NonNull IEpisode argEpisode, int argProgress) {
        for (int i = 0; i < mViewModels.size(); i++) {
            DownloadViewModel viewModel = mViewModels.get(i);
            IEpisode episode = viewModel.getEpisode();
            if (argEpisode.equals(episode)) {
                viewModel.updateProgress(argProgress);
            }
        }
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

    private void setEmptyTextViewVisibility() {
        int visibility = mDownloadingEpisodes.size() > 0 ? View.GONE : View.VISIBLE;
        mEmptyTextView.setVisibility(visibility);
    }
}
