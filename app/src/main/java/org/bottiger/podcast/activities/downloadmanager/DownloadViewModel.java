package org.bottiger.podcast.activities.downloadmanager;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.annotation.NonNull;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.model.events.DownloadProgress;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.DownloadStatus;

import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by aplb on 05-10-2015.
 */
public class DownloadViewModel {

    private static final String TAG = "DownloadViewModel";

    private static final int MAX_PROGRESS = 100;

    public final ObservableField<String> subtitle = new ObservableField<>();
    public final ObservableInt progress = new ObservableInt();

    private Context mContext;
    private DownloadManagerAdapter mAdapter;
    private IEpisode mEpisode;
    private int mPosition;

    private Subscription mRxSubscription = null;

    public DownloadViewModel(@NonNull Context argContext,
                             @NonNull DownloadManagerAdapter argAdapter,
                             @NonNull final FeedItem argEpisode,
                             int argPosition) {
        mContext = argContext;
        mAdapter = argAdapter;
        mEpisode = argEpisode;
        mPosition = argPosition;
        updateProgress(0); //updateProgress(isFirst() ? 60 : 0);

        mRxSubscription = argEpisode._downloadProgressChangeObservable
                .onBackpressureDrop()
                .ofType(DownloadProgress.class)
                .filter(new Func1<DownloadProgress, Boolean>() {
                    @Override
                    public Boolean call(DownloadProgress downloadProgress) {
                        return argEpisode.equals(downloadProgress.getEpisode());
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DownloadProgress>() {
                    @Override
                    public void call(DownloadProgress downloadProgress) {
                        Log.v(TAG, "Recieved downloadProgress event. Progress: " + downloadProgress.getProgress());

                        if (downloadProgress.getStatus() == DownloadStatus.DOWNLOADING) {
                            updateProgress(downloadProgress.getProgress());
                        }

                        if (downloadProgress.getStatus() == DownloadStatus.DONE || downloadProgress.getStatus() == DownloadStatus.ERROR) {
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        VendorCrashReporter.report("subscribeError" , throwable.toString());
                        Log.d(TAG, "error: " + throwable.toString());
                    }
                });
    }

    public void unsubscribe() {
        if (mRxSubscription != null && !mRxSubscription.isUnsubscribed()) {
            mRxSubscription.unsubscribe();
        }
    }

    public IEpisode getEpisode() {
        return mEpisode;
    }

    public String getTitle() {
        return mEpisode.getTitle();
    }

    private String makeSubtitle() {

        double currentFilesize = 0;

        if (progress.get() != 0) {
            currentFilesize = (double) mEpisode.getFilesize() * progress.get() / 100.0;
        }

        Resources res = mContext.getResources();
        String currentFilesizeFormatted = "";

        long filesize = mEpisode.getFilesize();


        if (filesize < 0) {
            return mContext.getResources().getString(R.string.unknown_filesize);
        }

        String totalFilesizeFormatted = Formatter.formatFileSize(mContext, filesize);

        if (currentFilesize > 0) {
            currentFilesizeFormatted = Formatter.formatFileSize(mContext, (long)currentFilesize);
        } else {
            // http://stackoverflow.com/questions/13493011/getquantitystring-returns-wrong-string-with-0-value
            return totalFilesizeFormatted;
        }

        return res.getString(R.string.download_progress,
                currentFilesizeFormatted,
                totalFilesizeFormatted);
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    private boolean isFirst() {
        return mPosition == 0;
    }

    public void updateProgress(int argProgress) {
        progress.set(argProgress);
        subtitle.set(makeSubtitle());
    }

    public void onClickRemove(View view) {
        mAdapter.removed(mPosition);
    }

}
