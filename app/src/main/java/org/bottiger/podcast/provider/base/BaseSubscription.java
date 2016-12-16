package org.bottiger.podcast.provider.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.graphics.Palette;
import android.support.v7.util.SortedList;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.PreloadTarget;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.listeners.PaletteListener;
import org.bottiger.podcast.model.datastructures.EpisodeList;
import org.bottiger.podcast.model.events.SubscriptionChanged;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.rxbus.RxBasicSubscriber;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.internal.operators.single.SingleObserveOn;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

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

    /**
     * See SubscriptionColumns for documentation
     */
    protected String mTitle;
    protected String mImageURL;
    protected String mDescription;
    protected String mUrlString;
    protected String mLink;

    private Single<ColorExtractor> mColorObservable;

    @NonNull
    protected EpisodeList mEpisodes;
    protected SortedList.Callback<IEpisode> mEpisodesListCallback = new SortedList.Callback<IEpisode>() {

        @Override
        public int compare(IEpisode o1, IEpisode o2) {

            if (o1 == null)
                return 1;

            if (o2 == null)
                return -1;

            Date dt1 = o1.getDateTime();

            if (dt1 == null)
                return 1;

            Date dt2 = o2.getDateTime();

            if (dt2 == null)
                return -1;

            return o2.getDateTime().compareTo(o1.getDateTime());
        }

        @Override
        public void onInserted(int position, int count) {

        }

        @Override
        public void onRemoved(int position, int count) {

        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {

        }

        @Override
        public void onChanged(int position, int count) {

        }

        @Override
        public boolean areContentsTheSame(IEpisode oldItem, IEpisode newItem) {
            return false;
        }

        @Override
        public boolean areItemsTheSame(IEpisode item1, IEpisode item2) {
            return item1.equals(item2);
        }
    };

    protected void init() {
    }

    @NonNull
    @Override
    public EpisodeList<IEpisode> getEpisodes() {
        return mEpisodes;
    }

    @Override
    public void setTitle(String argTitle) {
        if (mTitle != null && mTitle.equals(argTitle))
            return;

        mTitle = argTitle.trim();
        notifyPropertyChanged(null);
    }

    public void setImageURL(String argUrl) {

        if (argUrl == null)
            return;

        argUrl = argUrl.trim();

        if (mImageURL != null && mImageURL.equals(argUrl))
            return;

        mImageURL = argUrl;
        notifyPropertyChanged(null);
    }

    public void setURL(String argUrl) {
        if (mUrlString != null && mUrlString.equals(argUrl))
            return;

        mUrlString = argUrl.trim();
        notifyPropertyChanged(null);
    }

    public void setLink(@NonNull String argLink) {
        mLink = argLink;
    }

    public void setDescription(String content) {
        if (mDescription != null && mDescription.equals(content))
            return;

        mDescription = content.trim();
        notifyPropertyChanged(null);
    }

    @android.support.annotation.Nullable
    public String getDescription() {
        return mDescription;
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
            }).replay(1).autoConnect().firstOrError();
        }

        return mColorObservable;
    }

    @WorkerThread
    private ColorExtractor getColorExtractor(@NonNull Context argContext) {

        /*
        if (mPrimaryColor != -1 && mPrimaryTintColor != -1 && mSecondaryColor != -1) {
            return new ColorExtractor(mPrimaryColor, mPrimaryTintColor, mSecondaryColor);
        }
        */

        Bitmap bitmap;
        Palette palette = null;

        try {
            bitmap = (Bitmap) ImageLoaderUtils.getGlide(argContext, getImageURL())
                    .into(200, 200) // Width and height
                    .get();
            palette = Palette.from(bitmap).generate();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return new ColorExtractor(palette);
    }

    @MainThread
    public void cacheImage(@NonNull final Context argContext) {
        Single.just(argContext)
                .subscribeOn(Schedulers.io())
                .map(new Function<Context, Boolean>() {
                    @Override
                    public Boolean apply(Context context) throws Exception {
                        try {
                            Glide.with(context).load(getImageURL()).diskCacheStrategy(DiskCacheStrategy.ALL).into(200, 200).get();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return false;
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                            return false;
                        }

                        return true;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(Boolean value) {
                        if (value) {
                            notifyPropertyChanged(SubscriptionChanged.CHANGED, "ImageCached");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        ErrorUtils.handleException(e);
                    }
                });
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

        // For some reason this doesn't work
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

    @DbItemType
    public int getDbItemType() {
        return SUBSCRIPTION;
    }

    public boolean isPinned() {
        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (mUrlString == null) {
            return 31;
        }

        return mUrlString.hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        boolean isInstanceof = obj instanceof ISubscription;
        if (!isInstanceof) {
            return false;
        }

        ISubscription other = (ISubscription) obj;

        return mUrlString != null && mUrlString.equals(other.getURLString());

    }
}
