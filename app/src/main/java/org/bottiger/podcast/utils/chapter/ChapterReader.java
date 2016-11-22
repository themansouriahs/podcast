package org.bottiger.podcast.utils.chapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.IEpisode;
import org.bottiger.podcast.utils.id3reader.ID3Chapter;
import org.bottiger.podcast.utils.id3reader.ID3Reader;
import org.bottiger.podcast.utils.id3reader.ID3ReaderException;
import org.bottiger.podcast.utils.id3reader.model.FrameHeader;
import org.bottiger.podcast.utils.id3reader.model.TagHeader;
import org.reactivestreams.Subscriber;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;

public class ChapterReader extends ID3Reader {
    private static final String TAG = "ID3ChapterReader";

	private static final String FRAME_ID_CHAPTER = "CHAP";
	private static final String FRAME_ID_TITLE = "TIT2";
    private static final String FRAME_ID_LINK = "WXXX";

	private List<Chapter> chapters;
	private ID3Chapter currentChapter;

	@WorkerThread
	public static Flowable<List<Chapter>> getChapters(@NonNull final IEpisode argEpisode) {

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

	@Override
	public int onStartTagHeader(TagHeader header) {
		chapters = new ArrayList<Chapter>();
		System.out.println(header.toString());
		return ID3Reader.ACTION_DONT_SKIP;
	}

	@Override
	public int onStartFrameHeader(FrameHeader header, InputStream input)
			throws IOException, ID3ReaderException {
		System.out.println(header.toString());
		if (header.getId().equals(FRAME_ID_CHAPTER)) {
			if (currentChapter != null) {
				if (!hasId3Chapter(currentChapter)) {
					chapters.add(currentChapter);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Found chapter: " + currentChapter);
					currentChapter = null;
				}
			}
            StringBuffer elementId = new StringBuffer();
			readISOString(elementId, input, Integer.MAX_VALUE);
			char[] startTimeSource = readBytes(input, 4);
			long startTime = ((int) startTimeSource[0] << 24)
					| ((int) startTimeSource[1] << 16)
					| ((int) startTimeSource[2] << 8) | startTimeSource[3];
			currentChapter = new ID3Chapter(elementId.toString(), startTime);
			skipBytes(input, 12);
			return ID3Reader.ACTION_DONT_SKIP;
		} else if (header.getId().equals(FRAME_ID_TITLE)) {
			if (currentChapter != null && currentChapter.getTitle() == null) {
                StringBuffer title = new StringBuffer();
                readString(title, input, header.getSize());
				currentChapter
						.setTitle(title.toString());
				if (BuildConfig.DEBUG) Log.d(TAG, "Found title: " + currentChapter.getTitle());

				return ID3Reader.ACTION_DONT_SKIP;
			}
		} else if (header.getId().equals(FRAME_ID_LINK)) {
            if (currentChapter != null) {
                // skip description
                int descriptionLength = readString(null, input, header.getSize());
                StringBuffer link = new StringBuffer();
                readISOString(link, input, header.getSize() - descriptionLength);
                String decodedLink = URLDecoder.decode(link.toString(), "UTF-8");

                currentChapter.setLink(decodedLink);

                if (BuildConfig.DEBUG) Log.d(TAG, "Found link: " + currentChapter.getLink());
                return ID3Reader.ACTION_DONT_SKIP;
            }
        } else if (header.getId().equals("APIC")) {
            Log.d(TAG, header.toString());
        }

		return super.onStartFrameHeader(header, input);
	}

	private boolean hasId3Chapter(ID3Chapter chapter) {
		for (Chapter c : chapters) {
			if (((ID3Chapter) c).getId3ID().equals(chapter.getId3ID())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onEndTag() {
		if (currentChapter != null) {
			if (!hasId3Chapter(currentChapter)) {
				chapters.add(currentChapter);
			}
		}
		System.out.println("Reached end of tag");
		if (chapters != null) {
			for (Chapter c : chapters) {
				System.out.println(c.toString());
			}
		}
	}

	@Override
	public void onNoTagHeaderFound() {
		System.out.println("No tag header found");
		super.onNoTagHeaderFound();
	}

	public List<Chapter> getChapters() {
		return chapters;
	}

}
