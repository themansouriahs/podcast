package org.bottiger.podcast.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
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

    private Drawable mStaticBackground = null;
    private int download_icon = R.drawable.ic_get_app_white;
    private int delete_icon = R.drawable.ic_delete_white;

    public DownloadButtonView(Context context) {
        super(context);
        init(context, null);
    }

    public DownloadButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DownloadButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context argContext, @Nullable AttributeSet attrs) {
        mContext = argContext;

        setOnClickListener(this);

        // Detect of we have defined a background color for the view
        if (attrs != null) {
            int[] attrsArray = new int[]{
                    android.R.attr.background, // 0
            };
            TypedArray ta = mContext.obtainStyledAttributes(attrs, attrsArray);
            mStaticBackground = ta.getDrawable(0);
            ta.recycle();

            if (mStaticBackground != null) {
                download_icon = R.drawable.ic_get_app_grey;
                delete_icon = R.drawable.ic_delete_grey;
            }
        }


        if (!isInEditMode()) {
            //ThemeHelper themeHelper = new ThemeHelper(mContext);
            //int downloadIcon = themeHelper.getAttr(download_icon); //R.drawable.av_download; // FIXME
            // http://stackoverflow.com/questions/7896615/android-how-to-get-value-of-an-attribute-in-code

            setImage(download_icon);
            addState(PlayerButtonView.STATE_DEFAULT, download_icon);
            addState(PlayerButtonView.STATE_DELETE, delete_icon);
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
    public int ButtonColor(@NonNull Palette argPalette) {
        if (mStaticBackground != null)
            return Color.argb(0, 0, 0, 0); // transparent

        return super.ButtonColor(argPalette);
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
