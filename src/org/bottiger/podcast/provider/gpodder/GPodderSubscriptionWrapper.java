package org.bottiger.podcast.provider.gpodder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;

import android.content.ContentResolver;

public class GPodderSubscriptionWrapper {

	public String title;
	public String link;
	public String description;
	public String subtitle;
	public String author;
	public String language;
	public Collection<String> urls;
	public String new_location;
	public String logo;
	public String logo_data; // works
	public Collection<String> content_types;
	public String hub;
	// public String errors;
	// public String warnings;
	public String http_last_modified;
	public String http_etag;
	public String license;
	public Collection<GPodderEpisodeWrapper> episodes;

	public Subscription getSubscription(ContentResolver contentResolver,
			Subscription cachedSubscriptionObject) {

		String url = urls.iterator().next();

		Subscription subscription;

		subscription = Subscription.getByUrl(contentResolver, url);
		if (subscription == null) {
			if (cachedSubscriptionObject != null)
				cachedSubscriptionObject.reset();
			else
				cachedSubscriptionObject = new Subscription();
			
			subscription = cachedSubscriptionObject;
		}

		subscription.title = title;
		subscription.description = description;
		subscription.imageURL = logo;
		subscription.url = url;

		return subscription;
	}

	public ArrayList<FeedItem> getEpisodes(ContentResolver contentResolver,
			Subscription subscription, ArrayList<FeedItem> cachedEpisodeObjects) {

		ArrayList<FeedItem> items = new ArrayList<FeedItem>();
		if (episodes != null) {
			// for (GPodderEpisodeWrapper episode : episodes) {
			
			// Create an iterator in order to iterate over the wrapped episodes
			Iterator<GPodderEpisodeWrapper> wrapperIterator = episodes.iterator();
			Iterator<FeedItem> cacheIterator = cachedEpisodeObjects.iterator();
			
			// Calculate the amount of times the loop should run
			int largestLoop = episodes.size() > cachedEpisodeObjects.size() ? episodes
					.size() : cachedEpisodeObjects.size();
					
			// populate "items" with new objects.
			for (int i = 0; i < episodes.size(); i++) {
				
				// Get a wrapped episode from the iterator if it exists
				GPodderEpisodeWrapper episode = getNextFromIterator(wrapperIterator);
				FeedItem cachedEpisode = getNextFromIterator(cacheIterator);
				
				if (cachedEpisode == null) {
					cachedEpisode = new FeedItem();
					cachedEpisodeObjects.add(cachedEpisode);
				}
				
				if (episode != null)
					items.add(episode.toFeedItem(contentResolver, subscription, cachedEpisode));
			}
		}
		return items;
	}
	
	private <T> T getNextFromIterator(Iterator<T> iterator) {
		if (iterator.hasNext())
			return iterator.next();
		return null;
	}

}
