package org.bottiger.podcast.activities.downloadmanager;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.annotation.NonNull;
import android.text.format.Formatter;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by aplb on 05-10-2015.
 */
public class DownloadViewModel {

    private static final int MAX_PROGRESS = 100;

    public final ObservableField<String> subtitle = new ObservableField<>();
    public final ObservableInt progress = new ObservableInt();

    private Context mContext;
    private DownloadManagerAdapter mAdapter;
    private IEpisode mEpisode;
    private int mPosition;

    public DownloadViewModel(@NonNull Context argContext,
                             @NonNull DownloadManagerAdapter argAdapter,
                             @NonNull IEpisode argEpisode,
                             int argPosition) {
        mContext = argContext;
        mAdapter = argAdapter;
        mEpisode = argEpisode;
        mPosition = argPosition;
        updateProgress(isFirst() ? 60 : 0);
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

        String totalFilesizeFormatted = Formatter.formatFileSize(mContext, mEpisode.getFilesize());

        if (currentFilesize > 0) {
            currentFilesizeFormatted = Formatter.formatFileSize(mContext, (long)currentFilesize);
        } else {
            // http://stackoverflow.com/questions/13493011/getquantitystring-returns-wrong-string-with-0-value
            return totalFilesizeFormatted;
        }

        return res.getQuantityString(R.plurals.download_progress,
                (int) currentFilesize,
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
