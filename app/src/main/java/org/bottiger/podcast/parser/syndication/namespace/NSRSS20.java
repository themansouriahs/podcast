package org.bottiger.podcast.parser.syndication.namespace;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.parser.syndication.handler.HandlerState;
import org.bottiger.podcast.parser.syndication.util.SyndDateUtils;
import org.bottiger.podcast.parser.syndication.util.SyndTypeUtils;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.jsoup.Jsoup;
import org.xml.sax.Attributes;

import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * SAX-Parser for reading RSS-Feeds
 * 
 * @author daniel
 * 
 */
public class NSRSS20 extends Namespace {
	private static final String TAG = "NSRSS20";
	public static final String NSTAG = "rss";
	public static final String NSURI = "";

	public final static String CHANNEL = "channel";
	public final static String ITEM = "item";
	public final static String GUID = "guid";
	public final static String TITLE = "title";
	public final static String LINK = "link";
	public final static String DESCR = "description";
	public final static String PUBDATE = "pubDate";
	public final static String ENCLOSURE = "enclosure";
	public final static String IMAGE = "image";
	public final static String URL = "url";
	public final static String LANGUAGE = "language";

    public final static String ITUNES_IMAGE = "itunes:image";

	public final static String ENC_URL = "url";
	public final static String ENC_LEN = "length";
	public final static String ENC_TYPE = "type";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(ITEM)) {
			state.setCurrentItem(new FeedItem());
			state.getItems().add(state.getCurrentItem());

            if (state.getSubscription().getType() == ISubscription.DEFAULT) {
                state.getCurrentItem().setFeed((Subscription)state.getSubscription());
            }
		} else if (localName.equals(ENCLOSURE)) {
			String type = attributes.getValue(ENC_TYPE);
			String url = attributes.getValue(ENC_URL);
			
			
			if (state.getCurrentItem().getMedia() == null
					&& (SyndTypeUtils.enclosureTypeValid(type) || ((type = SyndTypeUtils
							.getValidMimeTypeFromUrl(url)) != null))) {

				long size = 0;
				try {
					size = Long.parseLong(attributes.getValue(ENC_LEN));
				} catch (NumberFormatException e) {
					Log.d(TAG, "Length attribute could not be parsed.");
				}

				FeedItem item = state.getCurrentItem();
				item.setURL(url);
				item.setFilesize(size);
				item.setType(type);
			}
		} else if (localName.equals(IMAGE)) {
			if (state.getTagstack().size() >= 1) {
				String parent = state.getTagstack().peek().getName();
				if (parent.equals(CHANNEL)) {
					// FIXME
					//state.getSubscription().setImage(new FeedImage());
				}
			}
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ITEM)) {
			state.setCurrentItem(null);
		} else if (state.getTagstack().size() >= 2
				&& state.getContentBuf() != null) {
			String content = state.getContentBuf().toString();
			SyndElement topElement = state.getTagstack().peek();
			String top = topElement.getName();
			SyndElement secondElement = state.getSecondTag();
			String second = secondElement.getName();
			String third = null;
			if (state.getTagstack().size() >= 3) {
				third = state.getThirdTag().getName();
			}

			if (top.equals(GUID) && second.equals(ITEM)) {
				// FIXME
				// state.getCurrentItem().setItemIdentifier(content);
			} else if (top.equals(TITLE)) {
				if (second.equals(ITEM)) {
					state.getCurrentItem().setTitle(content);
				} else if (second.equals(CHANNEL)) {
					state.getSubscription().setTitle(content);
				} else if (second.equals(IMAGE) && third != null && third.equals(CHANNEL)) {
					// FIXME
					//state.getSubscription().getImage().setTitle(content);
				}
			} else if (top.equals(LINK)) {
				if (second.equals(CHANNEL)) {
					// FIXME
					// state.getSubscription().setURL(content);
				} else if (second.equals(ITEM)) {
					//state.getCurrentItem().setLink(content);
                    if (TextUtils.isEmpty(state.getCurrentItem().url)) {
                        state.getCurrentItem().url = content;
                    }
				}
			} else if (top.equals(PUBDATE) && second.equals(ITEM)) {
                FeedItem item = state.getCurrentItem();
                if (item != null) { // bug:  LearnOutLoud.com
                    item.setPubDate(
							SyndDateUtils.parseRFC822Date(content));
                }
			} else if (top.equals(URL) && second.equals(IMAGE) && third != null && third.equals(CHANNEL)) {

				if (TextUtils.isEmpty(state.getSubscription().getImageURL())) {
                    state.getSubscription().setImageURL(content);
                }

			} else if (top.equals(ITUNES_IMAGE)) {

                state.getSubscription().setImageURL(content);

            } else if (localName.equals(DESCR)) {
				if (second.equals(CHANNEL)) {
					state.getSubscription().setDescription(content);
				} else if (second.equals(ITEM)) {
					String description = Jsoup.parse(content).text(); // FIXME make sure this is correct
					state.getCurrentItem().setDescription(description);
				}

			} else if (localName.equals(LANGUAGE)) {
				// FIXME
				//state.getSubscription().setLanguage(content.toLowerCase());
			}
		}
	}

}
