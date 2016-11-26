package org.bottiger.podcast.utils.chapter;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.ErrorUtils;
import org.bottiger.podcast.utils.id3reader.ID3Chapter;
import org.bottiger.podcast.utils.id3reader.ID3ReaderException;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Created by aplb on 23-11-2016.
 */

public class ChapterUtil {

    private static final String TAG = ChapterUtil.class.getSimpleName();

    public static Flowable<List<Chapter>> getChapters(@NonNull final Context argContext,
                                                      @NonNull IEpisode argEpisode) throws SecurityException {

        return Flowable.just(argEpisode).map(new Function<IEpisode, List<Chapter>>() {
            @Override
            public List<Chapter> apply(IEpisode argEpisode) throws Exception {

                List<Chapter> chapters = null;
                String arch = System.getProperty("os.arch");

                try {
                    if (arch.contains("a")) { // https://developer.android.com/ndk/guides/abis.html
                        chapters = getM4aChapters(argContext, argEpisode);
                    } else {
                        chapters = getMp3Chapters(argEpisode);
                    }
                } catch (UnsatisfiedLinkError unsatisfiedLinkError) {
                    ErrorUtils.handleException(unsatisfiedLinkError, TAG);
                } finally {
                    if (chapters == null) {
                        chapters = getMp3Chapters(argEpisode);
                    }
                }

                return chapters;
            }
        });
    }

    private static List<Chapter> getMp3Chapters(@NonNull final IEpisode argEpisode) {

        List<Chapter> chapters = null;
        ChapterReader reader = new ChapterReader();

        try {
            InputStream is = argEpisode.getUrl().openStream();
            reader.readInputStream(is);
            chapters = reader.getChapters();
        } catch (ID3ReaderException | IOException e) {
            VendorCrashReporter.handleException(e);
            e.printStackTrace();
        }

        // reader.getChapters() can return null
        if (chapters == null) {
            chapters = new LinkedList<>();
        }

        return chapters;
    }

    private static List<Chapter> getM4aChapters(@NonNull final Context argContext,
                                                @NonNull final IEpisode argEpisode) throws SecurityException {

        List<Chapter> chapters = new LinkedList<>();

        if (!argEpisode.isDownloaded()) {
            return chapters;
        }

        FeedItem item = (FeedItem)argEpisode;
        Uri path;
        try {
            path = Uri.parse((item.getAbsolutePath()));
        } catch (IOException e) {
            VendorCrashReporter.handleException(e);
            e.printStackTrace();
            return chapters;
        }
        FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
        mmr.setDataSource(argContext, path);
        int chapterCount = Integer.parseInt(mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_CHAPTER_COUNT));

        for (int i = 0; i < chapterCount; i++) {

            String title = mmr.extractMetadataFromChapter(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE, i);
            String startStr = mmr.extractMetadataFromChapter(FFmpegMediaMetadataRetriever.METADATA_KEY_CHAPTER_START_TIME, i);
            long start = Long.parseLong(startStr);

            String link = "";

            Chapter chapter = new ID3Chapter(start, title, item, link);
            chapters.add(chapter);
        }

        return chapters;
    }

    @Nullable
    public static Integer getCurrentChapterIndex(@Nullable IEpisode argEpisode, long argCurrentPosition) {

        if (argEpisode == null || !argEpisode.hasChapters()) {
            return null;
        }

        List<Chapter> chapters = argEpisode.getChapters();
        Chapter chapter;
        int chapterIndex = -1;

        for (int i = 0; i < chapters.size(); i++) {
            boolean isLast = i == chapters.size()-1;

            // If last
            if (isLast) {
                chapterIndex = i;
                break;
            }

            chapter = chapters.get(i);
            if (argCurrentPosition >= chapter.getStart()) {
                continue;
            }

            chapterIndex = i > 0 ? i-1 : i;
            break;
        }

        return chapterIndex;
    }
}
