package org.bottiger.podcast.parser.syndication.handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.input.XmlStreamReader;
import org.bottiger.podcast.BuildConfig;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.support.annotation.NonNull;

public class FeedHandler {

	public ISubscription parseFeed(ContentResolver contentResolver, @NonNull ISubscription subscription, InputStream feedContent) throws SAXException, IOException,
			ParserConfigurationException, UnsupportedFeedtypeException {

        if (subscription == null) {
            VendorCrashReporter.report("feedContent", feedContent.toString());
        }

        SyndHandler handler;

        try {
            TypeGetter tg = new TypeGetter();
            TypeGetter.Type type = tg.getType(subscription, feedContent);
            handler = new SyndHandler(contentResolver, subscription, type);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser saxParser = factory.newSAXParser();

            InputStream stream = feedContent;//new ByteArrayInputStream(feedContent.getBytes());

            Reader inputStreamReader = new XmlStreamReader(stream, false, "UTF-8");
            InputSource inputSource = new InputSource(inputStreamReader);

            saxParser.parse(inputSource, handler);
            inputStreamReader.close();

            return handler.state.feed;
        } catch (UnsupportedFeedtypeException udte) {
            return null;
        } catch (Exception e) {
            String substring = subscription == null ? "null" : subscription.toString();
            //VendorCrashReporter.report("feedContent", feedContent + " aaaaaand: subscription: " + substring);
            if (BuildConfig.DEBUG) {
                throw e;
            }
        }

        return null;
	}
}
