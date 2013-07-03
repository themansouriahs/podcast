package org.bottiger.podcast.parser.syndication.handler;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.input.XmlStreamReader;
import org.bottiger.podcast.provider.Subscription;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class FeedHandler {

	public Subscription parseFeed(Subscription feed) throws SAXException, IOException,
			ParserConfigurationException, UnsupportedFeedtypeException {
		TypeGetter tg = new TypeGetter();
		TypeGetter.Type type = tg.getType(feed);
		SyndHandler handler = new SyndHandler(feed, type);

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser saxParser = factory.newSAXParser();
		//File file = new File(feed.getFile_url());
		File file = new File(feed.url);
		Reader inputStreamReader = new XmlStreamReader(file);
		InputSource inputSource = new InputSource(inputStreamReader);

		saxParser.parse(inputSource, handler);
		inputStreamReader.close();
		return handler.state.feed;
	}
}
