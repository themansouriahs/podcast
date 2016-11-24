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

    public static Flowable<List<Chapter>> getChapters(@NonNull Context argContext,
                                                      @NonNull IEpisode argEpisode) throws SecurityException {

        Flowable<List<Chapter>> chapters;

        String arch = System.getProperty("os.arch");

        if (arch.contains("arm")) {
            chapters = getM4aChapters(argContext, argEpisode);
        } else {
            chapters = getMp3Chapters(argEpisode);
        }

        return chapters;
    }

    private static Flowable<List<Chapter>> getMp3Chapters(@NonNull final IEpisode argEpisode) {

        return Flowable.just(argEpisode).map(new Function<IEpisode, List<Chapter>>() {
            @Override
            public List<Chapter> apply(IEpisode argEpisode) throws Exception {

                List<Chapter> chapters = new LinkedList<>();
                ChapterReader reader = new ChapterReader();

                try {
                    InputStream is = argEpisode.getUrl().openStream();
                    reader.readInputStream(is);
                    chapters = reader.getChapters();
                } catch (ID3ReaderException | IOException e) {
                    VendorCrashReporter.handleException(e);
                    e.printStackTrace();
                }

                return chapters;
            }
        });
    }

    private static Flowable<List<Chapter>> getM4aChapters(@NonNull final Context argContext,
                                                          @NonNull final IEpisode argEpisode) throws SecurityException {

        return Flowable.just(argEpisode).map(new Function<IEpisode, List<Chapter>>() {
            @Override
            public List<Chapter> apply(IEpisode argEpisode) throws Exception {

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
        });
    }
}
