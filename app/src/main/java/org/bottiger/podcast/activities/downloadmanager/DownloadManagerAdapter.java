package org.bottiger.podcast.activities.downloadmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bottiger.podcast.BR;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.provider.SlimImplementations.SlimEpisode;
import org.bottiger.podcast.service.DownloadService;
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

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by aplb on 04-10-2015.
 */
public class DownloadManagerAdapter extends RecyclerView.Adapter<DownloadItemViewHolder> {

    private static final String TAG = "DownloadManagerAdapter";

    @NonNull
    private Context mContext;
    private SoundWavesDownloadManager mDownloadManager;
    private List<DownloadViewModel> mViewModels = new LinkedList<>();

    private TextView mEmptyTextView;

    public DownloadManagerAdapter(@NonNull Context argContext, @NonNull TextView argEmptyTextView) {
        super();
        mContext = argContext;
        mEmptyTextView = argEmptyTextView;

        mDownloadManager = SoundWaves.getDownloadManager();

        SoundWaves.getRxBus().toObserverable()
                .onBackpressureDrop()
                .ofType(SoundWavesDownloadManager.DownloadManagerChanged.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<SoundWavesDownloadManager.DownloadManagerChanged>() {
                    @Override
                    public void call(SoundWavesDownloadManager.DownloadManagerChanged downloadManagerChanged) {
                        Log.d(TAG, "DownloadManagerChanged, size: " + downloadManagerChanged.queueSize);
                        DownloadManagerAdapter.this.notifyDataSetChanged();

                        setEmptyTextViewVisibility();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.w(TAG, "Erorr");
                    }
                });

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

        QueueEpisode queueEpisode = mDownloadManager.getQueueItem(position);

        if (queueEpisode == null)
            return;

        IEpisode episode = queueEpisode.getEpisode();

        if (episode == null)
            return;

        DownloadViewModel viewModel = new DownloadViewModel(mContext, this, (FeedItem)episode, position); // FIXME no type casting
        mViewModels.add(viewModel);
        holder.getBinding().setVariable(BR.viewModel, viewModel);

        String artWork = episode.getArtwork();
        if (!TextUtils.isEmpty(artWork)) {
            ImageLoaderUtils.loadImageInto(holder.mImageView, artWork, null, true, false, true);
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

    public void downloadComplete(@NonNull IEpisode argEpisode) {
        Log.d("here", "er go");
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
        DownloadService.move(fromPosition, toPosition);

        // BUG: can couse "Inconsistency detected. Invalid item position 4(offset:4).state:5"
        notifyItemMoved(fromPosition, toPosition);
        //notifyDataSetChanged();
        return true;
    }

    private void setEmptyTextViewVisibility() {
        int visibility = mDownloadManager.getQueueSize() != 0 ? View.GONE : View.VISIBLE;
        mEmptyTextView.setVisibility(visibility);
    }
}
