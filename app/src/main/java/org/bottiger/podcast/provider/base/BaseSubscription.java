package org.bottiger.podcast.provider.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.PreloadTarget;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.utils.ColorExtractor;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.single.SingleObserveOn;

/**
 * Created by aplb on 08-06-2016.
 */

public abstract class BaseSubscription implements ISubscription {

    private static final String TAG = BaseSubscription.class.getSimpleName();

    // If the Subscription contains unpersisted changes.
    protected boolean mIsDirty = false;
    protected boolean mIsLoaded = false;
    protected boolean mIsRefreshing = false;

    protected int mPrimaryColor;
    protected int mPrimaryTintColor;
    protected int mSecondaryColor;

    private Single<ColorExtractor> mColorObservable;

    protected void init() {
    }

    public Single<ColorExtractor> getColors(@NonNull final Context argContext) {
        if (mColorObservable == null) {
            mColorObservable = Observable.create(new ObservableOnSubscribe<ColorExtractor>() {
                @Override
                public void subscribe(final ObservableEmitter<ColorExtractor> e) throws Exception {
                    ColorExtractor colorExtractor = getColorExtractor(argContext);
                    e.onNext(colorExtractor);
                    e.onComplete();
                }
            }).replay(1)
                    .autoConnect()
                    .firstOrError();
        }

        return mColorObservable;
    }

    @WorkerThread
    private ColorExtractor getColorExtractor(@NonNull Context argContext) {

        if (mPrimaryColor != 0 && mPrimaryTintColor != 0 && mSecondaryColor != 0) {
            return new ColorExtractor(mPrimaryColor, mPrimaryTintColor, mSecondaryColor);
        }

        Bitmap bitmap = null;
        Palette palette = null;

        try {
            bitmap = Glide.
                    with(argContext).
                    load(getImageURL()).
                    asBitmap().
                    into(200, 200). // Width and height
                    get();
            Palette.from(bitmap).generate();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return new ColorExtractor(palette);
    }

    public void fetchImage(@NonNull Context argContext) {
        PreloadTarget<Bitmap> preloadTarget = PreloadTarget.obtain(500, 500);
        Glide.with(argContext.getApplicationContext())
        .load(getImageURL())
                .asBitmap()
                .into(preloadTarget);
    }

    public boolean doSkipIntro() {
        return false;
    }

    @Nullable
    protected IEpisode getMatchingEpisode(@NonNull IEpisode argEpisode) {
        for (int i = 0; i < getEpisodes().size(); i++) {
            if (getEpisodes().get(i).equals(argEpisode))
                return getEpisodes().get(i);
        }

        return null;
    }

    public boolean contains(@NonNull IEpisode argEpisode) {

        // For some reason thi doen't work
        //return mEpisodes.indexOf(argEpisode) >= 0;

        IEpisode matchingEpisode = getMatchingEpisode(argEpisode);

        return matchingEpisode != null;
    }

    @Override
    public void onPaletteFound(@Nullable Palette argChangedPalette) {
        ColorExtractor extractor = new ColorExtractor(argChangedPalette);
        int newPrimaryColor = extractor.getPrimary();
        int newPrimaryTintColor = extractor.getPrimaryTint();
        int newSecondaryColor = extractor.getSecondary();

        if (newPrimaryColor != mPrimaryColor || newPrimaryTintColor != mPrimaryTintColor ||newSecondaryColor != mSecondaryColor) {
            mIsDirty = true;
        }

        mPrimaryColor     = newPrimaryColor;
        mPrimaryTintColor = newPrimaryTintColor;
        mSecondaryColor   = newSecondaryColor;
        notifyPropertyChanged(null);
    }

    protected void notifyPropertyChanged(@android.support.annotation.Nullable String argTag) {
        notifyPropertyChanged(SubscriptionChanged.CHANGED, argTag);
    }

    protected void notifyPropertyChanged(@SubscriptionChanged.Action int event, @android.support.annotation.Nullable String argTag) {
    }

    public static abstract class BasicColorExtractorObserver<ColorExtractor> implements SingleObserver<ColorExtractor> {
        @Override
        public void onSubscribe(Disposable d) {
            Log.d(TAG, "onSubscribe");
        }

        @Override
        public void onError(Throwable e) {
            Log.d(TAG, "onError");
            VendorCrashReporter.handleException(e);
        }
    }
}
