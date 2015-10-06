package org.bottiger.podcast.activities.downloadmanager;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.annotation.NonNull;
import android.text.format.Formatter;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.IEpisode;

/**
 * Created by aplb on 05-10-2015.
 */
public class DownloadViewModel {

    private static final int MAX_PROGRESS = 100;

    public final ObservableInt progress = new ObservableInt();

    private Context mContext;
    private IEpisode mEpisode;
    private int mPosition;

    public DownloadViewModel(@NonNull Context argContext, @NonNull IEpisode argEpisode, int argPosition) {
        mContext = argContext;
        mEpisode = argEpisode;
        mPosition = argPosition;
        progress.set(isFirst() ? 60 : 0);
    }

    public IEpisode getEpisode() {
        return mEpisode;
    }

    public String getTitle() {
        return mEpisode.getTitle();
    }

    public String getSubtitle() {

        double currentFilesize = 0;

        if (isFirst() && progress.get() != 0) {
            currentFilesize = (double) mEpisode.getFilesize() * 100.0 / progress.get();
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
                (int)currentFilesize,
                currentFilesizeFormatted,
                totalFilesizeFormatted);
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    //public int getProgress() {
    //    return isFirst() ? 60 : 0;
    //}

    private boolean isFirst() {
        return mPosition == 0;
    }
}
