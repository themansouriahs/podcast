package org.bottiger.podcast.parser;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.PodcastLog;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndImage;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.FeedException;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;

public class XMLFeedParserWrapper {
	
	private final PodcastLog log = PodcastLog.getLog(getClass());
	private Context mContext;
	private ContentResolver cr;
	private SimpleDateFormat dt = new SimpleDateFormat(FeedItem.default_format);

	private SharedPreferences sharedPrefs;

	public XMLFeedParserWrapper(Context context) {
		this.mContext = context;
		this.cr = context.getContentResolver();
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
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

}
