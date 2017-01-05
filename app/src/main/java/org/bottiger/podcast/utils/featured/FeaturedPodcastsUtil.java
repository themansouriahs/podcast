package org.bottiger.podcast.utils.featured;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.SortedList;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.Downloader.SoundWavesDownloadManager;
import org.bottiger.podcast.service.IDownloadCompleteCallback;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.StrUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by aplb on 04-01-2017.
 */
public class FeaturedPodcastsUtil {

    private static List<FeaturedPodcast> FEATURED_PODCASTS = null;

    @Nullable
    public static SlimSubscription getFeaturedPodcats() {
        try {
            init();
        } catch (MalformedURLException e) {
            ErrorUtils.handleException(e);
            return null;
        }

        if (FEATURED_PODCASTS == null) {
            return null;
        }

        SlimSubscription slimSubscription = null;
        FeaturedPodcast featuredPodcast;
        for (int i = 0; i < FEATURED_PODCASTS.size(); i++) {
            featuredPodcast = FEATURED_PODCASTS.get(i);
            if (isShows(featuredPodcast)) {
                slimSubscription = featuredPodcast.slimSubscription;
                break;
            }
        }

        return slimSubscription;
    }

    public static boolean hasFeaturedPodcast() {
        return getFeaturedPodcats() != null;
    }

    public static boolean showFeaturedPodcast(@Nullable String argSearchQuery) {
        return hasFeaturedPodcast() && TextUtils.isEmpty(argSearchQuery);
    }

    public static boolean isFeatured(@Nullable ISubscription argSubscription) {
        if (argSubscription == null) {
            return false;
        }

        return argSubscription.equals(getFeaturedPodcats());
    }

    public static Spanned getFeaturedHeadline(@NonNull ISubscription argSubscription, @NonNull Resources argResources) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        String boldPrefix = argResources.getString(R.string.featured_prefix) + " ";
        return StrUtils.getTextWithBoldPrefix(stringBuilder, boldPrefix, argSubscription.getTitle());
    }

    public static IDownloadCompleteCallback getRefreshCallback(@NonNull final Context argContext) {
        return new IDownloadCompleteCallback() {
            @Override
            public void complete(boolean argSucces, ISubscription argSubscription) {
                if (!argSucces)
                    return;

                final SlimSubscription slimSubscription = (SlimSubscription)argSubscription;
                IEpisode episode = (IEpisode) slimSubscription.getEpisodes().get(0);

                String prefKey = "hasDownloadedTest5";

                if (episode != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(argContext);
                    String lastDownload = prefs.getString(prefKey, "");
                    String url= episode.getURL();
                    if (!lastDownload.equals(url)) {
                        prefs.edit().putString(prefKey, url).commit();
                        SoundWavesDownloadManager manager = SoundWaves.getAppContext(argContext).getDownloadManager();
                        manager.addItemToQueue(episode, SoundWavesDownloadManager.ANYWHERE);
                    }
                }
            }
        };
    }

    private static boolean isShows(@NonNull FeaturedPodcast argFeaturedPodcast) {
        long now = System.currentTimeMillis();
        return argFeaturedPodcast.startDate.getTimeInMillis() < now && argFeaturedPodcast.endDate.getTimeInMillis() > now;
    }

    private static void init() throws MalformedURLException {
        if (FEATURED_PODCASTS != null) {
            return;
        }

        FEATURED_PODCASTS = new LinkedList<>();

        // Month: January = 0;

        FeaturedPodcast featuredPodcast1 = new FeaturedPodcast();
        featuredPodcast1.slimSubscription = new SlimSubscription("TWiL",
                new URL("http://feeds.twit.tv/twil.xml"),
                "http://elroy.twit.tv/sites/default/files/styles/twit_album_art_144x144/public/images/shows/this_week_in_law/album_art/audio/twil1400audio.jpg");
        featuredPodcast1.slimSubscription.setDescription("Join legal blogger (and trained attorney) Denise Howell along with J. Michael Keyes and Emory Roane as they discuss breaking issues in technology law, including patents, copyrights, and more. Records live every Friday at 2:00pm Eastern / 11:00am Pacific / 19:00 UTC.");
        featuredPodcast1.startDate.set(2017, 0, 4);
        featuredPodcast1.endDate.set(2017, 0, 10);

        FEATURED_PODCASTS.add(featuredPodcast1);
    }

    private static class FeaturedPodcast {
        SlimSubscription slimSubscription;
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
    }
}
