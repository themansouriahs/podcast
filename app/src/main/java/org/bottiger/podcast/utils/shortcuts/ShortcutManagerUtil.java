package org.bottiger.podcast.utils.shortcuts;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v7.util.SortedList;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.activities.feedview.FeedActivity;
import org.bottiger.podcast.playlist.Playlist;
import org.bottiger.podcast.provider.IDbItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.IPersistedSub;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.base.BaseSubscription;
import org.bottiger.podcast.receiver.DownloadManagerReceiver;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.ImageLoaderUtils;
import org.bottiger.podcast.utils.UIUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by aplb on 30-11-2016.
 */

public class ShortcutManagerUtil {

    private static final String TAG = ShortcutManagerUtil.class.getSimpleName();

    private static final boolean SHOW_DOWNLOAD_OPTION = false;
    private static final int NUM_EPISODES = 5;

    public static void updateAppShortcuts(@NonNull Context argContext, @NonNull Playlist argPlaylist) {
        if (Build.VERSION.SDK_INT < 25) {
            return;
        }

        updateAppShortcuts25(argContext, argPlaylist);
    }

    @TargetApi(25)
    private static void updateAppShortcuts25(@NonNull final Context argContext, @NonNull Playlist argPlaylist) {
        final ShortcutManager shortcutManager = argContext.getSystemService(ShortcutManager.class);
        final List<ShortcutInfo> shortcuts = new LinkedList<>();

        final int otherShortcuts;
        shortcutManager.removeAllDynamicShortcuts();


        if (SHOW_DOWNLOAD_OPTION) {
            shortcuts.add(getDownloadRecent(argContext));
            otherShortcuts = 1;
        } else {
            otherShortcuts = 0;
        }

        SoundWaves.getAppContext(argContext).getLibraryInstance().getLoadedSubscriptions()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map(itemsList -> {
                    List<ShortcutInfo> list = new LinkedList<>();
                    List<IDbItem> items = new LinkedList<>();
                    int numDynamicIcons = numDynamicShortcuts(shortcutManager) - otherShortcuts; //minus the download icon
                    int numItems = itemsList.size() >= numDynamicShortcuts(shortcutManager) ? numDynamicIcons : itemsList.size();

                    items.addAll(getSubscriptions(itemsList, numItems));

                    ShortcutInfo shortCut;
                    for (int i = 0; i < numItems; i++) {
                        shortCut = getShortCut(argContext, items.get(i));
                        list.add(shortCut);
                    }

                    return list;
                }).subscribe(new BaseSubscription.BasicColorExtractorObserver<List<ShortcutInfo>>() {
                    @Override
                    public void onSuccess(List<ShortcutInfo> argShortcuts) {
                        shortcuts.addAll(argShortcuts);
                        shortcutManager.setDynamicShortcuts(shortcuts);
                    }
                });
    }

    @TargetApi(25)
    private static ShortcutInfo getDownloadRecent(@NonNull Context argContext) {

        String shortLabel = argContext.getResources().getString(R.string.shortcut_short_download);
        String longLabel = argContext.getResources().getString(R.string.shortcut_long_download);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(argContext, "idDownload") // NoI18N
                .setShortLabel(shortLabel) // 10 characters
                .setLongLabel(longLabel) // 25 characters
                .setIcon(Icon.createWithResource(argContext, R.drawable.ic_get_app_black))
                .setIntent(DownloadManagerReceiver.getFetchNewIntent(argContext))
                .build();

        return shortcut;
    }

    @TargetApi(25)
    @WorkerThread
    private static ShortcutInfo getShortCut(@NonNull Context argContext, @NonNull IDbItem argItem) {
        switch (argItem.getType()) {
            case IDbItem.EPISODE: {
                return getEpisodeShortCut(argContext, (IEpisode) argItem);
            }
            case IDbItem.SUBSCRIPTION: {
                return getSubscriptionShortCut(argContext, (ISubscription) argItem);
            }
        }

        throw new IllegalStateException("Unknown type");
    }

    @TargetApi(25)
    @WorkerThread
    private static ShortcutInfo getSubscriptionShortCut(@NonNull Context argContext, @NonNull ISubscription argSubscription) {
        String pkg = argContext.getPackageName();
        String action = pkg + ".OPEN";
        Intent intent = FeedActivity.getIntent(argContext, argSubscription);
        intent.setAction(action);
        intent.setData(Uri.parse(argSubscription.getURLString()));

        return getShortCut(argContext,
                intent,
                argSubscription.getImageURL(),
                argSubscription.getTitle(),
                argSubscription.getTitle(),
                argSubscription.getURLString());
    }

    @TargetApi(25)
    @WorkerThread
    private static ShortcutInfo getEpisodeShortCut(@NonNull final Context argContext,
                                                   @NonNull final IEpisode argEpisode) {
        return getShortCut(argContext,
                DownloadManagerReceiver.getFetchNewIntent(argContext),
                argEpisode.getArtwork(argContext),
                argEpisode.getTitle(),
                argEpisode.getTitle(),
                argEpisode.getURL());
    }

    @TargetApi(25)
    @WorkerThread
    private static ShortcutInfo getShortCut(@NonNull Context argContext,
                                            @NonNull Intent argIntent,
                                            @Nullable String argArtwork,
                                            @NonNull String argShortTitle,
                                            @NonNull String argLongTitle,
                                            @NonNull String argId) {
        Bitmap bitmap = null;
        Icon icon = null;

        if (argArtwork != null) {
            try {
                int size = (int) UIUtils.convertDpToPixel(102, argContext); // I recall the icon size should be 102 dp
                bitmap = ImageLoaderUtils.getGlide(argContext, argArtwork)
                        .submit(size, size) // Width and height
                        .get();

                bitmap = getCroppedBitmap(bitmap);
            } catch (InterruptedException | ExecutionException e) {
                //e.printStackTrace();
                ErrorUtils.handleException(e, TAG);
            }
        }

        if (bitmap != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                icon = Icon.createWithBitmap(bitmap); //icon = Icon.createWithAdaptiveBitmap(bitmap);
            } else {
                icon = Icon.createWithBitmap(bitmap);
            }
        } else {
            icon = Icon.createWithResource(argContext, R.drawable.generic_podcast);
        }

        ShortcutInfo.Builder shortcutBuilder = new ShortcutInfo.Builder(argContext, "id"+argId)
                .setShortLabel(argShortTitle) // 10 characters
                .setLongLabel(argLongTitle) // 25 characters
                .setIcon(icon);

        shortcutBuilder
                .setIntent(argIntent)
                .build();

        return shortcutBuilder.build();
    }

    @TargetApi(25)
    private static int numDynamicShortcuts(@NonNull ShortcutManager argShortcutManager) {
        return Math.min(NUM_EPISODES, argShortcutManager.getMaxShortcutCountPerActivity());
    }

    private static IEpisode getEpisode(@NonNull Playlist argPlaylist, int argNumItem) {
        return argPlaylist.getItem(argNumItem);
    }

    private static List<IPersistedSub> getSubscriptions(@NonNull List<? extends IPersistedSub> argSubscriptions, int argNumItem) {
        int numSubscription = argSubscriptions.size();
        List<IPersistedSub> subscriptionsList = new LinkedList<>();
        IPersistedSub subscription;

        for (int i = 0; i < numSubscription; i++) {
            subscription = argSubscriptions.get(i);
            subscriptionsList.add(subscription);
        }

        Collections.sort(subscriptionsList, new Comparator<IPersistedSub>() {
            @Override
            public int compare(IPersistedSub iPersistedSub, IPersistedSub t1) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                int score1 = iPersistedSub.getScore();
                int score2 = t1.getScore();
                return score1 > score2 ? -1 : (score1 < score2) ? 1 : 0;
            }
        });

        return subscriptionsList.subList(0, argNumItem);
    }

    // From: http://stackoverflow.com/questions/11932805/cropping-circular-area-from-bitmap-in-android
    private static Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }

}