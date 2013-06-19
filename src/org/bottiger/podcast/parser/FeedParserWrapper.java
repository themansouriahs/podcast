package org.bottiger.podcast.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bottiger.podcast.provider.BulkUpdater;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.gpodder.GPodderSubscriptionWrapper;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.utils.Log;
import org.bottiger.podcast.utils.StrUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.xml.sax.SAXException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndImage;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.FeedException;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import com.google.gson.Gson;

/**
 * 
 * @author Arvid Böttiger
 * 
 */
public class FeedParserWrapper {

	private final Log log = Log.getLog(getClass());
	private Context mContext;
	private ContentResolver cr;
	private SimpleDateFormat dt = new SimpleDateFormat(FeedItem.default_format);

	private SharedPreferences sharedPrefs;

	private static FeedItem mostRecentItem = null;

	public FeedParserWrapper(Context context) {
		this.mContext = context;
		this.cr = context.getContentResolver();
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public void parse(Subscription subscription) {

		// try {
		jsonFetcherAndParser(subscription, mostRecentItem);
		/*
		 * If we can't parse the feed with RSSFeed we try with ROME
		 */
		/*
		 * } catch (Exception e) { e.printStackTrace(); romeParser(subscription,
		 * mostRecentItem); }
		 */

	}

	/**
	 * Fetches a formatted json file from mygpo-feedservice.appspot.com with the
	 * most recent episodes
	 * 
	 * @param subscription
	 * @param recentItem
	 */
	private void jsonFetcherAndParser(Subscription subscription,
			FeedItem recentItem) {

		String baseURL = "http://feeds.gpodder.net/parse?url=";
		String extraHeader = "Accept";
		String extraHeaderValue = "application/json";

		ArrayList<FeedItem> rssEpisodes = new ArrayList<FeedItem>();

		URL url;
		try {
			url = new URL(baseURL + subscription.getURL().toString());
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty(extraHeader, extraHeaderValue);

			BufferedReader in = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));

			Object rootObject = JSONValue.parse(in);
			JSONArray mainArray = (JSONArray) rootObject;
			if (mainArray != null) {
				JSONObject mainDataObject = (JSONObject) mainArray.get(0);

				String image = "";

				if (mainDataObject.get("logo") != null)
					image = mainDataObject.get("logo").toString();

				updateSubscription(subscription, mainDataObject, cr);

				JSONArray episodeDataObject = (JSONArray) mainDataObject
						.get("episodes");

				if (episodeDataObject != null) {
					int numOfEpisodes = episodeDataObject.size();
					for (int i = 0; i < numOfEpisodes; i++) {
						FeedItem item = new FeedItem();

						JSONObject episode = (JSONObject) episodeDataObject
								.get(i);
						Number duration = (Number) episode.get("duration");

						JSONArray fileData = (JSONArray) episode.get("files");
						if (fileData.size() > 0) {

							JSONObject files = (JSONObject) fileData.get(0);

							String episodeURL = "";
							JSONArray urlsData = (JSONArray) files.get("urls");
							if (urlsData.size() > 0) {
								episodeURL = (String) urlsData.get(0);
							} else {
								episodeURL = (String) episode.get("link");
							}

							item.type = (String) files.get("mimetype");
							Number filesize = (Number) files.get("filesize");
							Number episodeNumber = (Number) files.get("number");

							Number released = (Number) episode.get("released");
							Date time = null;
							if (released != null)
								time = new Date(released.longValue() * 1000);

							if (time != null)
								item.date = dt.format(time);
							if (duration != null) {
								item.duration_ms = duration.intValue() * 1000;
								item.duration_string = StrUtils
										.formatTime(item.duration_ms);
							}

							if (filesize != null)
								item.filesize = filesize.intValue();
							if (episodeNumber != null)
								item.setEpisodeNumber(episodeNumber.intValue());
							item.image = image;
							item.url = episodeURL;
							item.resource = item.url;

							item.title = (String) episode.get("title");
							item.author = (String) episode.get("author");
							item.content = (String) episode.get("description");

							rssEpisodes.add(item);
						}
					}
				}
			}

			updateFeed(subscription, rssEpisodes);

			PodcastDownloadManager.startDownload(mContext);

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Parses a json Object an updates the feed
	 * 
	 * @param jsonArray
	 * @param subscription
	 */
	public void feedParser(org.json.JSONArray jsonArray) {
		GPodderSubscriptionWrapper subscriptionWrapper = jsonParser(jsonArray);
		if (subscriptionWrapper != null) { // FIXME this should not be null
			Subscription subscription = subscriptionWrapper.getSubscription(cr);
			ArrayList<FeedItem> episodes = subscriptionWrapper.getEpisodes(cr); // Optimize?
																				// FIXME
			updateFeed(subscription, episodes);
			PodcastDownloadManager.startDownload(mContext);
		}
	}

	/**
	 * 
	 * @param jsonArray
	 * @param subscription
	 * @return
	 */
	private GPodderSubscriptionWrapper jsonParser(org.json.JSONArray jsonArray) {

		Gson gson = new Gson();
		GPodderSubscriptionWrapper subscriptionWrapper = null;

		GPodderSubscriptionWrapper[] subscriptionWrappers = gson.fromJson(
				jsonArray.toString(), GPodderSubscriptionWrapper[].class);

		if (subscriptionWrappers.length > 1) {
			int i = subscriptionWrappers.length;
			i = 6 + i;
		}

		if (subscriptionWrappers.length > 0) {
			subscriptionWrapper = subscriptionWrappers[0];

			Subscription subscription = subscriptionWrapper.getSubscription(cr);
			subscription.update(cr);
		}

		return subscriptionWrapper;
	}

	private void updateSubscription(Subscription subscription,
			JSONObject jsonObject, ContentResolver contentResolver) {
		String subscriptionTitle = "";
		String subscriptionDescription = "";
		String image = "";

		if (jsonObject.get("logo") != null)
			image = jsonObject.get("logo").toString();

		if (jsonObject.get("title") != null)
			subscriptionTitle = jsonObject.get("title").toString();

		if (jsonObject.get("description") != null)
			subscriptionDescription = jsonObject.get("description").toString();

		if (!subscription.title.equals(subscriptionTitle)
				|| !subscription.description.equals(subscriptionDescription)) {
			subscription.title = subscriptionTitle;
			subscription.description = subscriptionDescription;
			subscription.imageURL = image;
			subscription.update(cr);
		}
	}

	/**
	 * Parses a Feed with ROME. This is very robust, but also very slow.
	 * 
	 * @param subscription
	 * @param recentItem
	 */
	private void romeParser(Subscription subscription, FeedItem recentItem) {

		XmlReader reader = null;

		try {

			reader = new XmlReader(subscription.getURL());
			SyndFeed feed = new SyndFeedInput().build(reader);
			// System.out.println("Feed Title: " + feed.getAuthor());

			// subscriptionTitle = feed.getTitle();
			// subscriptionDescription = feed.getDescription();

			SyndImage image = feed.getImage();
			// subscriptionImage = (image != null) ? image.getUrl() : "";

			for (Iterator i = feed.getEntries().iterator(); i.hasNext();) {
				SyndEntry entry = (SyndEntry) i.next();
				FeedItem currentItem = fromRSSEntry(entry);

				if (currentItem != null) {
					// updateFeed(subscription, currentItem);
				}

				// System.out.println(entry.getTitle());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (FeedException e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}

	/*
	 * private FeedItem fromRSSEntry(Item entry) { FeedItem item = new
	 * FeedItem();
	 * 
	 * SimpleDateFormat dt = new SimpleDateFormat(item.default_format);
	 * 
	 * item.author = entry.getAuthor(); item.title = entry.getTitle();
	 * item.content = (entry.getDescription() != null) ? entry
	 * .getDescription().toString() : ""; item.date =
	 * dt.format(entry.getPubDate()); item.resource = entry.getUri(); item.url =
	 * entry.getLink();
	 * 
	 * return item; }
	 */

	private FeedItem fromRSSEntry(SyndEntry entry) {
		FeedItem item = new FeedItem();

		SimpleDateFormat dt = new SimpleDateFormat(FeedItem.default_format);

		item.author = entry.getAuthor();
		item.title = entry.getTitle();
		item.content = (entry.getDescription() != null) ? entry
				.getDescription().toString() : "";
		item.date = dt.format(entry.getPublishedDate());
		item.resource = entry.getUri();
		item.url = entry.getLink();

		return item;
	}

	private FeedItem checkItem(FeedItem item) throws SAXException {
		if (item.title == null)
			item.title = "(Untitled)";
		item.title = strip(item.title);

		if (item.resource == null) {
			// log.warn("item have not a resource link: " + item.title);
			return null;
		}
		if (item.author == null)
			item.author = "(Unknown)";

		if (item.content == null)
			item.content = "(No content)";

		SimpleDateFormat correctRSSFormatter = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss Z");
		DateFormat wrongRSSFormatter = new SimpleDateFormat(
				"dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		String ff = ItemColumns.DATE_FORMAT;
		SimpleDateFormat correctSQLFormatter = new SimpleDateFormat(ff);

		// In case the date is in a wrong format
		Date date = null;
		try {
			date = correctRSSFormatter.parse(item.date);
		} catch (ParseException e) {
			try {
				date = wrongRSSFormatter.parse(item.date);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		if (item.getDate().equals("")) {

			date = new Date();
			// item.date = correctFormatter.format(date);
			// log.warn("item.date: " + item.date);
		}

		if (date != null)
			item.date = correctSQLFormatter.format(date);

		return item;
	}

	private String strip(String str) {

		Pattern pattern = Pattern.compile("\\n");
		Matcher matcher = pattern.matcher(str);
		str = matcher.replaceAll("");

		pattern = Pattern.compile("\\s+");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll(" ");

		pattern = Pattern.compile("^\\s+");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll("");

		pattern = Pattern.compile("\\s+$");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll("");

		return str;
	}

	public int updateFeed(Subscription subscription, ArrayList<FeedItem> items) {
		long update_date = subscription.lastItemUpdated;
		int add_num = 0;
		boolean insertSucces = false;
		boolean autoDownload = sharedPrefs.getBoolean(
				"pref_download_on_update", true);

		if (!items.isEmpty()) {

			// Sort the items to find the oldest
			Collections.sort(items);
			FeedItem oldestItem = items.get(items.size() - 1);

			/*
			HashMap<String, FeedItem> databaseItems = FeedItem.allAsList(cr,
					subscription, oldestItem.getDate());
					*/
			HashMap<String, FeedItem> databaseItems = FeedItem.allAsList(cr, subscription, null);
			
			BulkUpdater bulkUpdater = new BulkUpdater();

			// we iterate over all the input items
			for (FeedItem item : items) {

				// and if the item is not included in the database already we
				// add it
				String url = item.getURL();
				if (!databaseItems.containsKey(url)) {

					Long itemDate = item.getLongDate();

					if (itemDate > update_date) {
						update_date = itemDate;
					}
					add_num++;

					/*
					 * Download podcasts
					 */
					if (autoDownload && item != null) {
						PodcastDownloadManager.addItemToQueue(item);
					}

					subscription.fail_count = 0;
					subscription.lastItemUpdated = update_date;
					ContentProviderOperation cpo = subscription.update(cr,
							true, true);
					bulkUpdater.addOperation(cpo);

					log.debug("add url: " + subscription.url + "\n add num = "
							+ add_num);
				}
				
				item = addItem(subscription, item, bulkUpdater);
			}

			bulkUpdater.commit(cr);
			return add_num;
		}
		return 0;
	}

	private synchronized FeedItem addItem(Subscription subscription,
			FeedItem item, BulkUpdater updater) {
		Long sub_id = subscription.id;
		boolean insertSucces = false;

		item.sub_id = sub_id;

		String where = ItemColumns.SUBS_ID + "=" + sub_id + " and "
				+ ItemColumns.RESOURCE + "= '" + item.resource + "'";

		Cursor cursor = cr.query(ItemColumns.URI,
				new String[] { BaseColumns._ID }, where, null, null);

		if (cursor.moveToFirst()) {
			item.update(cr);
			log.debug("Updating episode: " + item.title + "(" + item.image + ")");
		} else {
			Uri insertedUri = item.insert(cr);
			item = FeedItem.getMostRecent(cr);
			insertSucces = true;
			log.debug("Inserting new episode: " + item.title);
		}

		if (cursor != null)
			cursor.close();

		if (insertSucces)
			return item;

		return null;

	}

}
