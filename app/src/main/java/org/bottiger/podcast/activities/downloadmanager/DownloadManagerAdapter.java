package org.bottiger.podcast.activities.downloadmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;

import org.bottiger.podcast.BR;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.service.DownloadService;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.utils.ImageLoaderUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 04-10-2015.
 */
class DownloadManagerAdapter extends RecyclerView.Adapter<DownloadItemViewHolder> {

    private static final String TAG = DownloadManagerAdapter.class.getSimpleName();

    @NonNull
    private Context mContext;
    private SoundWavesDownloadManager mDownloadManager;
    private List<QueueEpisode> queueEpisodes = new LinkedList<>();

    private TextView mEmptyTextView;

    DownloadManagerAdapter(@NonNull Context argContext, @NonNull TextView argEmptyTextView) {
        super();
        mContext = argContext;
        mEmptyTextView = argEmptyTextView;

        mDownloadManager = SoundWaves.getAppContext(mContext).getDownloadManager();

        setEmptyTextViewVisibility();
    }

    public void setData(@Nullable List<QueueEpisode> argQueueEpisodes) {

        if (argQueueEpisodes == null) {
            queueEpisodes.clear();
            return;
        }

        queueEpisodes = argQueueEpisodes;
        notifyDataSetChanged();
    }

    @Override
    public DownloadItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_download_manager_item, parent, false);
        return new DownloadItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(DownloadItemViewHolder holder, int position) {

        QueueEpisode queueEpisode = queueEpisodes.get(position);

        if (queueEpisode == null)
            return;

        IEpisode episode = queueEpisode.getEpisode();

        DownloadViewModel viewModel = new DownloadViewModel(mContext, this, (FeedItem)episode, position); // FIXME no type casting
        viewModel.subscribe();
        //queueEpisodes.add(viewModel);
        holder.getBinding().setVariable(BR.viewModel, viewModel);

        String artWork = episode.getArtwork(mContext);
        if (!TextUtils.isEmpty(artWork)) {
            RequestOptions options = ImageLoaderUtils.getRequestOptions(mContext);
            options.centerCrop();
            options.placeholder(R.drawable.generic_podcast);
            ImageLoaderUtils.loadImageInto(holder.mImageView, artWork, ImageLoaderUtils.DEFAULT, options);
        }
    }

    @Override
    public int getItemCount() {
        return mDownloadManager.getQueueSize();
    }

    public void removed(int argPosition) {
        DownloadService.removeFromQueue(argPosition);
        setEmptyTextViewVisibility();
        super.notifyItemRemoved(argPosition);
    }

    /*
    public void downloadComplete(@NonNull IEpisode argEpisode) {
        Log.d("here", "er go");
    }

    public void updateProgress(@NonNull IEpisode argEpisode, int argProgress) {
        for (int i = 0; i < queueEpisodes.size(); i++) {
            DownloadViewModel viewModel = queueEpisodes.get(i);
            IEpisode episode = viewModel.getEpisode();
            if (argEpisode.equals(episode)) {
                viewModel.updateProgress(argProgress);
            }
        }
    }
    */

    boolean onItemMove(int fromPosition, int toPosition) {
        DownloadService.move(fromPosition, toPosition);

        // BUG: can couse "Inconsistency detected. Invalid item position 4(offset:4).state:5"
        notifyItemMoved(fromPosition, toPosition);
        //notifyDataSetChanged();
        return true;
    }

    private void setEmptyTextViewVisibility() {
        int visibility = getItemCount() != 0 ? View.GONE : View.VISIBLE;
        mEmptyTextView.setVisibility(visibility);
    }
}
