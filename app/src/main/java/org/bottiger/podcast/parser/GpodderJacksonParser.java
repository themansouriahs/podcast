package org.bottiger.podcast.parser;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.provider.gpodder.GPodderEpisodeWrapper;
import org.bottiger.podcast.provider.gpodder.GPodderSubscriptionWrapper;

import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class GpodderJacksonParser {

	private boolean debug = false;

	/**
	 * See example here: http://wiki.fasterxml.com/JacksonInFiveMinutes For
	 * array parsing:
	 * http://www.mkyong.com/java/jackson-streaming-api-to-read-and-write-json/
	 * 
	 * @param jsonArray
	 * @param subscription
	 * @return
	 */
	public GPodderSubscriptionWrapper streamParser(String sr) {

		GPodderSubscriptionWrapper subscriptionWrapper = new GPodderSubscriptionWrapper();

		JsonFactory f = new JsonFactory();
		try {
			JsonParser jp = f.createJsonParser(sr);

			boolean topLevelArray = false;
			boolean topLevelObject = false;
			String testValue;

			jp.nextToken();
			testValue = jp.getText();
			jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
			testValue = jp.getText();
			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String fieldname = jp.getCurrentName();
				if (MainActivity.debugging && debug)
					Log.d("Jackson Parser Profiler", "fieldname: " + fieldname);
				String prefieldValue = jp.getText();


				
				if (false && (topLevelObject || topLevelArray)) {

					if (fieldname.equals(JsonToken.END_ARRAY)) {
						topLevelArray = false;
					}

					if (fieldname.equals(JsonToken.END_OBJECT)) {
						topLevelObject = false;
					}

				} else {

					jp.nextToken(); // move to value, or
									// START_OBJECT/START_ARRAY
					String fieldValue = jp.getText();
					
					if (fieldValue.equals("[")) { // jp.equals(JsonToken.START_ARRAY)) {
						topLevelArray = true;
					}

					if (fieldValue.equals("{")) { // jp.equals(JsonToken.START_OBJECT)) {
						topLevelObject = true;
					}

					if ("content_types".equals(fieldname)) {
						// public Collection<String> content_types;
						LinkedList<String> contentTypes = new LinkedList<String>();
						while (jp.nextToken() != JsonToken.END_ARRAY) {
							contentTypes.add(jp.getText());
						}

						subscriptionWrapper.content_types = contentTypes;
					}

					if ("urls".equals(fieldname)) {
						// if (current == JsonToken.START_ARRAY) {
						LinkedList<String> urlCollection = new LinkedList<String>();
						while (jp.nextToken() != JsonToken.END_ARRAY) {
							urlCollection.add(jp.getText());
						}
						subscriptionWrapper.urls = urlCollection;
						// }
					}

					/**
					 * Parse individual episodes
					 */
					if ("episodes".equals(fieldname)) {
						// public Collection<GPodderEpisodeWrapper> episodes;
						subscriptionWrapper.episodes = parseEpisode(jp);
					}

					if ("author".equals(fieldname)) {
						subscriptionWrapper.author = jp.getText();
					}

					if ("title".equals(fieldname)) {
						subscriptionWrapper.title = jp.getText();
					}

					if ("link".equals(fieldname)) {
						subscriptionWrapper.link = jp.getText();
					}

					if ("description".equals(fieldname)) {
						subscriptionWrapper.description = jp.getText();
					}

					if ("subtitle".equals(fieldname)) {
						subscriptionWrapper.subtitle = jp.getText();
					}

					if ("language".equals(fieldname)) {
						subscriptionWrapper.language = jp.getText();
					}

					if ("new_location".equals(fieldname)) {
						subscriptionWrapper.new_location = jp.getText();
					}

					if ("logo".equals(fieldname)) {
						String logo = prefieldValue;
						subscriptionWrapper.logo = logo;
					}

					if ("logo_data".equals(fieldname)) {
						subscriptionWrapper.logo_data = jp.getText();
					}

					if ("hub".equals(fieldname)) {
						subscriptionWrapper.hub = jp.getText();
					}

					if ("http_last_modified".equals(fieldname)) {
						subscriptionWrapper.http_last_modified = jp.getText();
					}

					if ("http_etag".equals(fieldname)) {
						subscriptionWrapper.http_etag = jp.getText();
					}

					if ("license".equals(fieldname)) {
						subscriptionWrapper.license = jp.getText();
					}

				}

			}

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * GPodderSubscriptionWrapper[] subscriptionWrappers = gson.fromJson(
		 * jsonArray.toString(), GPodderSubscriptionWrapper[].class);
		 * 
		 * 
		 * if (subscriptionWrappers.length > 1) { int i =
		 * subscriptionWrappers.length; i = 6 + i; }
		 * 
		 * if (subscriptionWrappers.length > 0) { subscriptionWrapper =
		 * subscriptionWrappers[0];
		 * 
		 * Subscription subscription = subscriptionWrapper.getSubscription(cr);
		 * subscription.update(cr); }
		 */

		return subscriptionWrapper;
	}

	private Collection<GPodderEpisodeWrapper> parseEpisode(JsonParser jp)
			throws JsonParseException, IOException {
		LinkedList<GPodderEpisodeWrapper> episodes = new LinkedList<GPodderEpisodeWrapper>();
		while (jp.nextToken() != JsonToken.END_ARRAY) {

			GPodderEpisodeWrapper episode = new GPodderEpisodeWrapper();

			while (jp.nextToken() != JsonToken.END_OBJECT) {

				String namefield = jp.getCurrentName();
				if (MainActivity.debugging && debug)
					Log.d("Jackson Parser Profiler", "namefield: " + namefield);
				JsonToken currentEpisode = jp.nextToken();
				String tokenValue = jp.getText(); 

				if (currentEpisode == JsonToken.END_ARRAY) {
					break;
				}

				if ("author".equals(namefield)) {
					episode.author = jp.getText();
				}

				if ("content".equals(namefield)) {
					episode.content = jp.getText();
				}

				if ("content_types".equals(namefield)) {
					episode.content_types = parseStringArray(jp);
				}

				if ("description".equals(namefield)) {
					episode.description = jp.getText();
				}

				if ("duration".equals(namefield)) {
					String duration = jp.getText();
					if (duration != null && !duration.equals("null"))
						episode.duration = Integer.parseInt(duration);
				}

				/** Files */
				if ("files".equals(namefield)) {
					LinkedList<GPodderEpisodeWrapper.JSONFile> fileCollection = new LinkedList<GPodderEpisodeWrapper.JSONFile>();
					while (jp.nextToken() != JsonToken.END_ARRAY) {
						GPodderEpisodeWrapper.JSONFile file = new GPodderEpisodeWrapper.JSONFile();
						fileCollection.add(file);
						// stringCollection.add(jp.getText());
						String filenamefield = jp.getCurrentName();
						JsonToken current = jp.nextToken();

						if (current == JsonToken.END_ARRAY) {
							break;
						}

						if (MainActivity.debugging && debug)
							Log.d("Jackson Parser Profiler", "filenamefield: "
									+ filenamefield);

						if ("filesize".equals(filenamefield)) {
							String filesize = jp.getText();
							if (filesize != null && !filesize.equals("null"))
								file.filesize = Integer.parseInt(filesize);
						}

						if ("mimetype".equals(filenamefield)) {
							file.minetype = jp.getText();
						}

						if ("urls".equals(filenamefield)) {
							// if (current == JsonToken.START_ARRAY) {
							LinkedList<String> urlCollection = new LinkedList<String>();
							String url = jp.getText();
							urlCollection.add(url);
							while (jp.nextToken() != JsonToken.END_ARRAY) {
								url = jp.getText();
								urlCollection.add(url);
							}
							file.urls = urlCollection;
							// }
						}
					}

					episode.files = fileCollection;
				}

				if ("guid".equals(namefield)) {
					episode.guid = jp.getText();
				}

				if ("language".equals(namefield)) {
					episode.language = jp.getText();
				}

				if ("license".equals(namefield)) {
					episode.license = jp.getText();
				}

				if ("link".equals(namefield)) {
					episode.link = jp.getText();
				}

				if ("number".equals(namefield)) {
					String number = jp.getText();
					if (number != null && !number.equals("null"))
						episode.number = Integer.parseInt(number);
				}

				if ("released".equals(namefield)) {
					String released = jp.getText();
					if (released != null && !released.equals("null"))
						episode.released = Integer.parseInt(released);
				}

				if ("short_title".equals(namefield)) {
					episode.short_title = jp.getText();
				}

				if ("subtitle".equals(namefield)) {
					episode.subtitle = jp.getText();
				}

				if ("title".equals(namefield)) {
					episode.title = jp.getText();
				}
			}

			episodes.add(episode);

		}
		return episodes;
	}

	private Collection<String> parseStringArray(JsonParser jp)
			throws JsonParseException, IOException {
		LinkedList<String> stringCollection = new LinkedList<String>();
		while (jp.nextToken() != JsonToken.END_ARRAY) {
			stringCollection.add(jp.getText());
		}
		return stringCollection;
	}

}
