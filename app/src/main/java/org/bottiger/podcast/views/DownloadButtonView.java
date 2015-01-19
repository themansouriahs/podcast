package org.bottiger.podcast.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.utils.ThemeHelper;

/**
 * Created by apl on 02-09-2014.
 */
public class DownloadButtonView extends PlayerButtonView implements View.OnClickListener {

    private Context mContext;
    private FeedItem mEpisode;

    public DownloadButtonView(Context context) {
        super(context);
        init(context);
    }

    public DownloadButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DownloadButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context argContext) {
        mContext = argContext;

        setOnClickListener(this);


        if (!isInEditMode()) {
            ThemeHelper themeHelper = new ThemeHelper(mContext);
            int downloadIcon = themeHelper.getAttr(R.attr.download_icon); //R.drawable.av_download; // FIXME
            // http://stackoverflow.com/questions/7896615/android-how-to-get-value-of-an-attribute-in-code

            setImage(downloadIcon);
            addState(PlayerButtonView.STATE_DEFAULT, downloadIcon);
            addState(PlayerButtonView.STATE_DELETE, R.drawable.ic_action_delete);
        }

        addDownloadCompletedCallback(new PlayerButtonView.DownloadStatus() {
            @Override
            public void FileComplete() {
                setState(PlayerButtonView.STATE_DELETE);
                setProgressPercent(0);
            }

            @Override
            public void FileDeleted() {
                setState(PlayerButtonView.STATE_DEFAULT);
                setProgressPercent(0);
            }
        });
    }

    @Override
    public FeedItem getEpisode() {
        return mEpisode;
    }

    public void setEpisode(@NonNull FeedItem argItem) {
        mEpisode = argItem;
        setEpisodeId(mEpisode.getId());
        setState(mEpisode.isDownloaded() ? PlayerButtonView.STATE_DELETE : PlayerButtonView.STATE_DEFAULT);
    }

    @Override
    public void onClick(View view) {

        if (mEpisode == null) {
            throw new IllegalStateException("Episode can not be null");
        }

        if (getState() == PlayerButtonView.STATE_DEFAULT) {
            PodcastDownloadManager.addItemAndStartDownload(mEpisode, mContext);
        } else if (getState() == PlayerButtonView.STATE_DELETE) {
            mEpisode.delFile(mContext.getContentResolver());
            setState(PlayerButtonView.STATE_DEFAULT);
        } else {
            throw new IllegalStateException("State is not defined");
        }
    }
}
