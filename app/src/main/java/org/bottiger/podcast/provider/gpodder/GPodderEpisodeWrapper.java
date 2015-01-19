package org.bottiger.podcast.provider.gpodder;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.StrUtils;

import android.content.ContentResolver;

public class GPodderEpisodeWrapper {

	public String guid;
	public String title;
	public String subtitle;
	public String short_title;
	public String content;
	public int number;
	public int released;
	public String description;
	public String link;
	public String author;
	public int duration;
	public String language;
	public String license;
	public Collection<String> content_types;
	public Collection<JSONFile> files;

	public static class JSONFile {
		public int filesize;
		public String minetype;
		public Collection<String> urls;
	}

	/*
	 * public String fetch-feed; public String fetch-logo; public String
	 * hub-subscription;
	 */

	public FeedItem toFeedItem(ContentResolver contentResolver,
			Subscription subscription, FeedItem cachedFeedItem) {

		SimpleDateFormat dt = new SimpleDateFormat(FeedItem.default_format);

		String url = null;
		JSONFile file = null;

		Iterator<JSONFile> fileIterator = files.iterator();
		while (fileIterator.hasNext()) {
			file = fileIterator.next();
			Collection<String> urlCollection = file.urls;
			if (urlCollection != null) {
				Iterator<String> urls = urlCollection.iterator();
				while (urls.hasNext()) {
					String urlCandidate = urls.next();
					if (urlCandidate != null && !urlCandidate.equals("")) {
						url = urlCandidate;
					}
				}

			}
		}

		if (url != null) {

			FeedItem episode = FeedItem.getByURL(contentResolver, url);
			if (episode == null) {
				cachedFeedItem.reset();
				episode = cachedFeedItem;
			}

			Date time = new Date((long) released * 1000);

			episode.setTitle(title);
			episode.sub_title = subtitle;
			episode.content = content;
			episode.date = dt.format(time);
			episode.setEpisodeNumber(number);
			episode.author = author;

			episode.duration_ms = duration * 1000;
			episode.duration_string = StrUtils.formatTime(episode.duration_ms);

			episode.sub_id = subscription.getId();
			episode.image = subscription.getImageURL();

			episode.url = url;

			if (files.size() > 0) {
				episode.filesize = file.filesize;

			}

			episode.resource = episode.url;

			return episode;
		} else {
			int j = 6;
			j = j + 6;
			return null;
		}
	}
}
