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

import com.bumptech.glide.request.RequestOptions;

import org.bottiger.podcast.BR;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.QueueEpisode;
import org.bottiger.podcast.service.DownloadService;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.utils.ImageLoaderUtils;

import java.util.LinkedList;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by aplb on 04-10-2015.
 */
class DownloadManagerAdapter extends RecyclerView.Adapter<DownloadItemViewHolder> {

    private static final String TAG = "DownloadManagerAdapter";

    @NonNull
    private Context mContext;
    private SoundWavesDownloadManager mDownloadManager;
    private List<DownloadViewModel> mViewModels = new LinkedList<>();

    private TextView mEmptyTextView;

    private Subscription mRxSubscription = null;

    DownloadManagerAdapter(@NonNull Context argContext, @NonNull TextView argEmptyTextView) {
        super();
        mContext = argContext;
        mEmptyTextView = argEmptyTextView;

        mDownloadManager = SoundWaves.getAppContext(mContext).getDownloadManager();

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
        viewModel.subscribe();
        mViewModels.add(viewModel);
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
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRxSubscription = SoundWaves.getRxBus().toObserverable()
                .onBackpressureDrop()
                .ofType(SoundWavesDownloadManager.DownloadManagerChanged.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<SoundWavesDownloadManager.DownloadManagerChanged>() {
                    @Override
                    public void call(SoundWavesDownloadManager.DownloadManagerChanged downloadManagerChanged) {
                        Log.d(TAG, "DownloadManagerChanged, size: " + downloadManagerChanged.queueSize);

                        switch (downloadManagerChanged.action) {
                            case SoundWavesDownloadManager.ADDED: {
                                break;
                            }
                            case SoundWavesDownloadManager.CLEARED: {
                                break;
                            }
                            case SoundWavesDownloadManager.REMOVED: {
                                break;
                            }
                        }

                        clearViewModels();
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
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        if (mRxSubscription != null && !mRxSubscription.isUnsubscribed()) {
            mRxSubscription.unsubscribe();
        }

        clearViewModels();
    }

    private void clearViewModels() {
        for (int i = 0; i < mViewModels.size(); i++) {
            mViewModels.get(i).unsubscribe();
        }
        mViewModels.clear();
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
        int visibility = getItemCount() != 0 ? View.GONE : View.VISIBLE;
        mEmptyTextView.setVisibility(visibility);
    }
}
