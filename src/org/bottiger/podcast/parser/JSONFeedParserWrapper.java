package org.bottiger.podcast.parser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.bottiger.podcast.provider.DatabaseHelper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.gpodder.GPodderEpisodeWrapper;
import org.bottiger.podcast.provider.gpodder.GPodderEpisodeWrapper.JSONFile;
import org.bottiger.podcast.provider.gpodder.GPodderSubscriptionWrapper;
import org.bottiger.podcast.service.PodcastDownloadManager;
import org.bottiger.podcast.utils.PodcastLog;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

/**
 * 
 * @author Arvid Böttiger
 * 
 */
public class JSONFeedParserWrapper {

	private final boolean USE_GSON = true;
	private final boolean USE_COMPILED_STATMENTS = true;

	private final PodcastLog log = PodcastLog.getLog(getClass());
	private Context mContext;
	private ContentResolver cr;
	private SimpleDateFormat dt = new SimpleDateFormat(FeedItem.default_format);

	private SharedPreferences sharedPrefs;

	/** Cached objecs used for Object creation */
	static final Object cacheLock = new Object();
	Subscription cachedSubscriptionObject = new Subscription();
	ArrayList<FeedItem> cachedEpisodeObjects = new ArrayList<FeedItem>();

	public static final String[] UPDATE_COLUMNS = { ItemColumns.TITLE,
			ItemColumns.AUTHOR, ItemColumns.DATE, ItemColumns.CONTENT,
			ItemColumns.FILESIZE, ItemColumns.DURATION, ItemColumns.LENGTH,
			ItemColumns.SUB_TITLE, ItemColumns.EPISODE_NUMBER,
			ItemColumns.DURATION_MS };

	public JSONFeedParserWrapper(Context context) {
		this.mContext = context;
		this.cr = context.getContentResolver();
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * Parses a json Object an updates the feed
	 * 
	 * @param jsonArray
	 * @param subscription
	 */
	public void feedParser(org.json.JSONArray jsonArray) {
		// public void feedParser(URL url) {

		GPodderSubscriptionWrapper subscriptionWrapper = null;
		long start = 0;
		long end = 0;

		if (USE_GSON) {

			start = System.currentTimeMillis();
			subscriptionWrapper = jsonParser(jsonArray);
			end = System.currentTimeMillis();
			Log.d("Parser Profiler", "gson time: " + (end - start));

		} else {
			GpodderJacksonParser jacksonParser = new GpodderJacksonParser();
			start = System.currentTimeMillis();

			// StringReader sr = new StringReader(jsonArray.toString());
			subscriptionWrapper = jacksonParser.streamParser(jsonArray
					.toString());
			end = System.currentTimeMillis();
			Log.d("Parser Profiler", "jackson time: " + (end - start));
		}

		if (subscriptionWrapper != null) { // FIXME this should not be null
			start = System.currentTimeMillis();
			Subscription subscription;
			ArrayList<FeedItem> episodes;
			/*
			synchronized (cacheLock) {
				subscription = subscriptionWrapper.getSubscription(cr,
						cachedSubscriptionObject);
				episodes = subscriptionWrapper.getEpisodes(cr, subscription,
						cachedEpisodeObjects);
			}
			end = System.currentTimeMillis();
			long timeDiff = (end - start);
			long arraySize = episodes.size();
			Log.d("Parser Profiler", "object creation time: " + timeDiff
					+ " for " + arraySize + " objects. "
					+ ((double) timeDiff / (double) arraySize) + " pr object");
			*/
			start = System.currentTimeMillis();
			updateFeed(subscriptionWrapper, false);
			end = System.currentTimeMillis();
			Log.d("Parser Profiler", "updateFeed time: " + (end - start));
			PodcastDownloadManager.startDownload(mContext);
		}
	}

	/*
	 * private GPodderSubscriptionWrapper[] jacson(String json) { ObjectMapper
	 * mapper = new ObjectMapper(); GPodderSubscriptionWrapper[] item = null;
	 * try { item = mapper.readValue(json, GPodderSubscriptionWrapper[].class);
	 * } catch (JsonParseException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } catch (JsonMappingException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } catch (IOException e) {
	 * // TODO Auto-generated catch block e.printStackTrace(); } if (item !=
	 * null) { int party = 1; party = 2 + party; }
	 * 
	 * return item; }
	 */

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

			Subscription subscription = subscriptionWrapper.getSubscription(cr,
					cachedSubscriptionObject);
			subscription.update(cr);
		}

		return subscriptionWrapper;
	}

	public int updateFeed(GPodderSubscriptionWrapper subscriptionWrapper,
			boolean updateExisting) {
		Subscription subscription = subscriptionWrapper.getSubscription(cr,
				cachedSubscriptionObject);
		Collection<GPodderEpisodeWrapper> episodes = subscriptionWrapper.episodes;
		
		long update_date = subscription.lastItemUpdated;
		int add_num = 0;
		boolean insertSucces = false;
		boolean autoDownload = sharedPrefs.getBoolean(
				"pref_download_on_update", true);
		SimpleDateFormat dt = new SimpleDateFormat(FeedItem.default_format);
		
		
		SQLiteStatement sqlStatment = DatabaseHelper.prepareFeedUpdateQuery(mContext, UPDATE_COLUMNS);

		if (episodes != null && !episodes.isEmpty()) {

			FeedItem oldestItem = null;
			HashMap<String, FeedItem> databaseItems = null;

			String oldestDate = null;
			// Sort the items to find the oldest
			try {
				// FIXME why are there null's in here?
				episodes.removeAll(Collections.singleton(null));

				Iterator<GPodderEpisodeWrapper> episodeItr = episodes.iterator();
				int oldestRelease = 0;
				GPodderEpisodeWrapper oldestEpisode = null;
				
				while (episodeItr.hasNext()) {
					GPodderEpisodeWrapper episode = episodeItr.next();
					if (oldestRelease < episode.released || oldestEpisode == null) {
						oldestEpisode = episode;
					}
				}

				Date time = new Date((long) oldestEpisode.released * 1000);
				oldestDate = dt.format(time);
			} catch (Exception e) {
				e.printStackTrace();
			}
			databaseItems = FeedItem.allAsList(cr, subscription, oldestDate);
			// HashMap<String, FeedItem> databaseItems = FeedItem.allAsList(cr,
			// subscription, null);

			DatabaseHelper bulkUpdater = new DatabaseHelper();

			// we iterate over all the input items
			for (GPodderEpisodeWrapper episode : episodes) {

				// and if the item is not included in the database already we
				// add it
				String url = null;
				
				Iterator<JSONFile> itr = episode.files.iterator();
				while (itr.hasNext()) {
					JSONFile file = itr.next();
					if (file.urls != null) {
						Iterator<String> itrURL = file.urls.iterator();
						while (itrURL.hasNext()) {
							String urlCandidate = itrURL.next();
							if (urlCandidate != null) {
								url = urlCandidate;
								break;
							}
						}
					}
				}
				
				if (url == null) {
					int j = 5;
					j = j +5;
				}
				
				if (databaseItems != null && !databaseItems.containsKey(url)) {

					FeedItem item = FeedItem.getByURL(cr, url);
					
					Long itemDate = item.getLongDate();

					if (itemDate > update_date) {
						update_date = itemDate;
					}
					Uri insertedItem = item.insert(cr);

					if (insertedItem != null) {
						Log.d("Feed Updater", "Inserting new episode: "
								+ item.title);

						add_num++;
					} else if (updateExisting) {
						
						if (USE_COMPILED_STATMENTS) {
							DatabaseHelper.executeStatment(sqlStatment, columnValues(item), item.url);
						} else {
							bulkUpdater.addOperation(item.update(cr, true, true));
						}
						//bulkUpdater.addOperation(item.update(cr, true, true));
						
					}

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

					Log.d("Feed Updater", "add url: " + subscription.url
							+ "\n add num = " + add_num);
				} else {
					if (updateExisting) {
						FeedItem item = FeedItem.getByURL(cr, url);
						if (USE_COMPILED_STATMENTS) {
							DatabaseHelper.executeStatment(sqlStatment, columnValues(item), item.url);
						} else {
							bulkUpdater.addOperation(item.update(cr, true, true));
						}
						Log.d("Feed Updater", "Updating episode: " + item.title
								+ "(" + item.image + ")");
					}
				}

				// item = addItem(subscription, item, bulkUpdater);
			}

			long start = System.currentTimeMillis();
			bulkUpdater.commit(cr);
			long end = System.currentTimeMillis();
			Log.d("Parser Profiler", "commit database time: " + (end - start));
			return add_num;
		}
		return 0;
	}
	
	/*
	 * 	public static final String[] UPDATE_COLUMNS = { ItemColumns.TITLE,
			ItemColumns.AUTHOR, ItemColumns.DATE, ItemColumns.CONTENT,
			ItemColumns.FILESIZE, ItemColumns.DURATION, ItemColumns.LENGTH,
			ItemColumns.SUB_TITLE, ItemColumns.EPISODE_NUMBER,
			ItemColumns.DURATION_MS };
	 */
	private Object[] columnValues(FeedItem item) {
		Object[] values = new Object[UPDATE_COLUMNS.length];
		values[0] = item.title;
		values[1] = item.author;
		values[2] = item.date;
		values[3] = item.content;
		values[4] = item.filesize;
		values[5] = item.duration_string;
		values[6] = item.length;
		values[7] = item.sub_title;
		values[8] = item.episodeNumber;
		values[9] = item.duration_ms;
		return values;
	}

}
