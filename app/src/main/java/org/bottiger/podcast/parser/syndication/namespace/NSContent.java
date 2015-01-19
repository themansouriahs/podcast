package org.bottiger.podcast.parser.syndication.namespace;

import org.bottiger.podcast.parser.syndication.handler.HandlerState;
import org.xml.sax.Attributes;



public class NSContent extends Namespace {
	public static final String NSTAG = "content";
	public static final String NSURI = "http://purl.org/rss/1.0/modules/content/";
	
	private static final String ENCODED = "encoded";
	
	@Override
	public SyndElement handleElementStart(String localName, HandlerState state,
			Attributes attributes) {
		return new SyndElement(localName, this);
	}

	@Override
	public void handleElementEnd(String localName, HandlerState state) {
		if (localName.equals(ENCODED)) {
			// FIXME
			//state.getCurrentItem().setContentEncoded(state.getContentBuf().toString());
		}
	}

}
