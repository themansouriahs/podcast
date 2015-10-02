package org.bottiger.podcast.parser.syndication.namespace;

import org.bottiger.podcast.parser.syndication.handler.HandlerState;
import org.bottiger.podcast.provider.FeedItem;
import org.xml.sax.Attributes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class NSITunes extends Namespace{
	public static final String NSTAG = "itunes";
	public static final String NSURI = "http://www.itunes.com/dtds/podcast-1.0.dtd";
	
	private static final String IMAGE = "image";
	private static final String IMAGE_TITLE = "image";
	private static final String IMAGE_HREF = "href";
	
	private static final String AUTHOR = "author";
	public final static String DURATION = "duration";
	
	
	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {

		if (localName.equals(IMAGE) ) {
            state.getSubscription().setImageURL(attributes.getValue(IMAGE_HREF));
		}
		
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(DURATION)) {
			try {
				String durationString = state.getContentBuf().toString();
				SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
				Date date = sdf.parse(durationString);

				long duration = date.getTime();

				FeedItem item = state.getCurrentItem();
				item.setDuration(duration, true);

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		
	}

}
