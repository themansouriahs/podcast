package org.bottiger.podcast.parser.syndication.namespace;

import java.util.concurrent.TimeUnit;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.parser.syndication.handler.HandlerState;
import org.bottiger.podcast.parser.syndication.util.SyndTypeUtils;
import org.xml.sax.Attributes;

import android.util.Log;

/** Processes tags from the http://search.yahoo.com/mrss/ namespace. */
public class NSMedia extends Namespace {
	private static final String TAG = "NSMedia";

	public static final String NSTAG = "media";
	public static final String NSURI = "http://search.yahoo.com/mrss/";

	private static final String CONTENT = "content";
	private static final String DOWNLOAD_URL = "url";
	private static final String SIZE = "fileSize";
	private static final String MIME_TYPE = "type";
	private static final String DURATION = "duration";

	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		if (localName.equals(CONTENT)) {
			String url = attributes.getValue(DOWNLOAD_URL);
			String type = attributes.getValue(MIME_TYPE);
			/* FIXME
			if (state.getCurrentItem().getMedia() == null
					&& url != null
					&& (SyndTypeUtils.enclosureTypeValid(type) || ((type = SyndTypeUtils
							.getValidMimeTypeFromUrl(url)) != null))) {

				long size = 0;
				try {
					size = Long.parseLong(attributes.getValue(SIZE));
				} catch (NumberFormatException e) {
					if (MainActivity.debugging)
						Log.d(TAG, "Length attribute could not be parsed.");
				}
				
				int duration = 0;
				try {
					String durationStr = attributes.getValue(DURATION);
					if (durationStr != null) {
						duration = (int) TimeUnit.MILLISECONDS.convert(
								Long.parseLong(durationStr), TimeUnit.SECONDS);
					}
				} catch (NumberFormatException e) {
					if (MainActivity.debugging)
						Log.d(TAG, "Duration attribute could not be parsed");
				}
				
				state.getCurrentItem().setMedia(
						new FeedMedia(state.getCurrentItem(), url, size, type));
						
			}
		*/
		}
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {

	}

}
