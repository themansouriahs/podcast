package org.bottiger.podcast.parser.syndication.namespace;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.parser.syndication.handler.HandlerState;
import org.bottiger.podcast.parser.syndication.util.SyndDateUtils;
import org.bottiger.podcast.parser.syndication.util.SyndTypeUtils;
import org.bottiger.podcast.provider.FeedItem;
import org.xml.sax.Attributes;

import android.util.Log;

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

	public final static String ENC_URL = "url";
	public final static String ENC_LEN = "length";
	public final static String ENC_TYPE = "type";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(ITEM)) {
			state.setCurrentItem(new FeedItem());
			state.getItems().add(state.getCurrentItem());
			state.getCurrentItem().setFeed(state.getFeed());

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
					if (MainActivity.debugging)
						Log.d(TAG, "Length attribute could not be parsed.");
				}
				
				//state.getCurrentItem().setMedia(
				//		new FeedMedia(state.getCurrentItem(), url, size, type));
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
					//state.getFeed().setImage(new FeedImage());
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
					state.getFeed().setTitle(content);
				} else if (second.equals(IMAGE) && third != null && third.equals(CHANNEL)) {
					// FIXME
					//state.getFeed().getImage().setTitle(content);
				}
			} else if (top.equals(LINK)) {
				if (second.equals(CHANNEL)) {
					// FIXME
					state.getFeed().setLink(content);
				} else if (second.equals(ITEM)) {
					//state.getCurrentItem().setLink(content);
					state.getCurrentItem().url = content;
				}
			} else if (top.equals(PUBDATE) && second.equals(ITEM)) {
				state.getCurrentItem().setPubDate(
						SyndDateUtils.parseRFC822Date(content));
			} else if (top.equals(URL) && second.equals(IMAGE) && third != null && third.equals(CHANNEL)) {
				//state.getFeed().getImage().setDownload_url(content);
				state.getFeed().imageURL = content;
			} else if (localName.equals(DESCR)) {
				if (second.equals(CHANNEL)) {
					state.getFeed().setDescription(content);
				} else if (second.equals(ITEM)) {
					state.getCurrentItem().setDescription(content);
				}

			} else if (localName.equals(LANGUAGE)) {
				// FIXME
				//state.getFeed().setLanguage(content.toLowerCase());
			}
		}
	}

}
