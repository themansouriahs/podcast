package org.bottiger.podcast.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PlayerStatusProgressData;
import org.bottiger.podcast.model.events.DownloadProgress;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.UIUtils;

import io.reactivex.BackpressureStrategy;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by apl on 02-09-2014.
 */
public class DownloadButtonView extends PlayerButtonView implements View.OnClickListener {

    private static final String TAG = "DownloadButtonView";

    private Context mContext;

    private Drawable mStaticBackground = null;
    private static final @DrawableRes int download_icon = R.drawable.ic_get_app_24dp; //ic_get_app_24dp;
    private static final @DrawableRes int queued_icon = R.drawable.ic_query_builder_24dp;
    private static final @DrawableRes int delete_icon = R.drawable.ic_delete_24dp;

    private static final int BITMAP_OFFSET = 5;

    private RectF buttonRectangle;

    private Subscription mRxSubscription = null;
    private Disposable mRxDisposable = null;

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
        setImageResource(download_icon);

        buttonRectangle = new RectF();

        setOnClickListener(this);

        if (!isInEditMode()) {
            setImage(download_icon);
            addState(PlayerButtonView.STATE_DEFAULT, download_icon);
            addState(PlayerButtonView.STATE_DELETE, delete_icon);
            addState(PlayerButtonView.STATE_QUEUE, queued_icon);
        }

        addDownloadCompletedCallback(new PlayerButtonView.DownloadStatus() {
            @Override
            public void FileComplete() {
                setState(PlayerButtonView.STATE_DELETE);
                setProgressPercent(new DownloadProgress());
            }

            @Override
            public void FileDeleted() {
                setState(PlayerButtonView.STATE_DEFAULT);
                setProgressPercent(new DownloadProgress());
            }
        });

        mRxDisposable = SoundWaves.getRxBus2()
                .toObservable()
                .toFlowable(BackpressureStrategy.LATEST)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .ofType(DownloadProgress.class)
                .subscribe(new Consumer<DownloadProgress>() {
                    public void accept(DownloadProgress downloadProgress) {
                        setProgressPercent(downloadProgress);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d(TAG, throwable.toString());
                    }
                });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int rectSize = Math.min(width, height);

        int left = width>height ? (width-rectSize)/2 : 0;
        int top = width<height ? (height-rectSize)/2 : 0;

        buttonRectangle.set(left + BITMAP_OFFSET, top + BITMAP_OFFSET, left + rectSize - BITMAP_OFFSET, top + rectSize - BITMAP_OFFSET);

        if(mProgress!=0 && mProgress < 100) {
            if (getState() != PlayerButtonView.STATE_DEFAULT) {
                setState(PlayerButtonView.STATE_DEFAULT);
            }
            canvas.drawArc(buttonRectangle, -90, Math.round(360 * mProgress / 100F), false, mForegroundColorPaint);
        }
    }

    @Override
    public @ColorInt
    int getForegroundColor(@NonNull ColorExtractor extractor) {
        return extractor.getPrimaryTint();
    }

    @Override
    public int ButtonColor(@NonNull Palette argPalette) {
        if (mStaticBackground != null)
            return Color.argb(0, 0, 0, 0); // transparent

        return super.ButtonColor(argPalette);
    }

    @Override
    public synchronized void unsetEpisodeId() {
        if (mRxSubscription != null && !mRxSubscription.isUnsubscribed()) {
            mRxSubscription.unsubscribe();
        }

        if (mRxDisposable != null && !mRxDisposable.isDisposed()) {
            mRxDisposable.dispose();
        }
        super.unsetEpisodeId();
    }

    @Override
    public void setEpisode(@NonNull final IEpisode argItem) {
        super.setEpisode(argItem);

        setState(calcState());

        setProgressPercent(new DownloadProgress());
    }

    public void enabledProgressListener(boolean argEnabled) {
        if (argEnabled && getEpisode() instanceof FeedItem) {

            final FeedItem item = (FeedItem)getEpisode();
            mRxSubscription = item._downloadProgressChangeObservable
                    .onBackpressureDrop()
                    .ofType(DownloadProgress.class)
                    .filter(new Func1<DownloadProgress, Boolean>() {
                        @Override
                        public Boolean call(DownloadProgress downloadProgress) {
                            return getEpisode().equals(downloadProgress.getEpisode());
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<DownloadProgress>() {
                        @Override
                        public void call(DownloadProgress downloadProgress) {
                            Log.v(TAG, "Recieved downloadProgress event. Progress: " + downloadProgress.getProgress());
                            setProgressPercent(downloadProgress);

                            if (downloadProgress.getStatus() == org.bottiger.podcast.service.DownloadStatus.ERROR) {
                                String errorMsg = String.format(getResources().getString(R.string.download_aborted_snackbar), item.getTitle());
                                UIUtils.disPlayBottomSnackBar(DownloadButtonView.this, errorMsg, null, true);
                                setState(STATE_DEFAULT);
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

        if (!argEnabled && mRxSubscription != null && !mRxSubscription.isUnsubscribed()) {
            mRxSubscription.unsubscribe();
        }
    }

    public void setProgressPercent(@NonNull DownloadProgress argProgress) {

        /*
        FIXME: Becasue we register each button ASAP we can have a registered button without a EpisodeID
                I should take a look at that.
         */
        try {
            if (!getEpisode().equals(argProgress.getEpisode())) {
                mProgress = 0;
                setState(calcState());
                invalidate();
                return;
            }
        } catch (Exception e) {
            VendorCrashReporter.handleException(e);
            return;
        }

        int newProgress = argProgress.getProgress();

        if (newProgress == 100) {
            if (mDownloadCompletedCallback != null) {
                mDownloadCompletedCallback.FileComplete();
            }
            setState(PlayerButtonView.STATE_DELETE);
        } else {
            if (mProgress != newProgress) {
                mProgress = newProgress;
                invalidate();
                return;
            }
        }

        if (argProgress.getStatus() == org.bottiger.podcast.service.DownloadStatus.DELETED) {
            setState(PlayerButtonView.STATE_DEFAULT);
        }
    }

    @Override
    public void onClick(View view) {

        if (view == null)
            return;

        Context context = view.getContext();
        if (context instanceof TopActivity) {
            TopActivity topActivity = (TopActivity) context;
            @SoundWavesDownloadManager.Result int result = SoundWavesDownloadManager.checkPermission(topActivity);
        }


        String viewStr = view.toString();
        Log.d(TAG, "onCLick: view => " + viewStr);

        if (getEpisode() == null) {
            Log.e(TAG, "Episode is null");

            if (BuildConfig.DEBUG)
                throw new IllegalStateException("Episode can not be null");
            else
                return;
        }

        if (getState() == PlayerButtonView.STATE_DEFAULT) {
            Log.v(TAG, "Queue download");
            SoundWaves.getAppContext(getContext()).getDownloadManager().addItemAndStartDownload(getEpisode(), SoundWavesDownloadManager.STARTED_MANUALLY);
            setState(PlayerButtonView.STATE_QUEUE);
        } else if (getState() == PlayerButtonView.STATE_DELETE) {
            Log.v(TAG, "Delete file");
            IEpisode episode = getEpisode();
            if (episode instanceof FeedItem) {
                ((FeedItem)episode).delFile(mContext);
                setState(PlayerButtonView.STATE_DEFAULT);
            }
        }
    }

    private int calcState() {

        IEpisode episode = getEpisode();

        if (episode == null)
            return PlayerButtonView.STATE_DOWNLOAD;

        if (episode.isDownloaded()) {
            return PlayerButtonView.STATE_DELETE;
        }

        org.bottiger.podcast.service.DownloadStatus status = SoundWaves.getAppContext(getContext()).getDownloadManager().getStatus(getEpisode());

        if (status == org.bottiger.podcast.service.DownloadStatus.DOWNLOADING) {
            return PlayerButtonView.STATE_DEFAULT;
        }

        if (status == org.bottiger.podcast.service.DownloadStatus.PENDING) {
            return PlayerButtonView.STATE_QUEUE;
        }

        return PlayerButtonView.STATE_DEFAULT;
    }
}
