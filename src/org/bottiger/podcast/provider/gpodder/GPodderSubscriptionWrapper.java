package org.bottiger.podcast.provider.gpodder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;

import android.content.ContentResolver;

public class GPodderSubscriptionWrapper {

	private String title;
	private String link;
	private String description;
	private String subtitle;
	private String author;
	private String language;
	private Collection<String> urls;
	private String new_location;
	private String logo;
	private String logo_data; // works
	private Collection<String> content_types;
	private String hub;
	// private String errors;
	// private String warnings;
	private String http_last_modified;
	private String http_etag;
	private String license;
	private Collection<GPodderEpisodeWrapper> episodes;

	public Subscription getSubscription(ContentResolver contentResolver) {

		String url = urls.iterator().next();
		Subscription subscription = Subscription.getByUrl(contentResolver, url);
		if (subscription == null) {
			subscription = new Subscription();
		}

		subscription.title = title;
		subscription.description = description;
		subscription.imageURL = logo;
		subscription.url = url;

		return subscription;
	}

	public ArrayList<FeedItem> getEpisodes(ContentResolver contentResolver) {
		ArrayList<FeedItem> items = new ArrayList<FeedItem>();
		if (episodes != null) {
			for (GPodderEpisodeWrapper episode : episodes) {
				items.add(episode.toFeedItem(contentResolver,
						getSubscription(contentResolver)));
			}
		}
		return items;
	}

}
