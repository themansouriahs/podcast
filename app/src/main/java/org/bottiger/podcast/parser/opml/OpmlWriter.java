package org.bottiger.podcast.parser.opml;

import android.support.v4.util.LongSparseArray;
import android.support.v7.util.SortedList;
import android.util.Log;
import android.util.Xml;

import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** Writes OPML documents. */
public class OpmlWriter {

	private static final String TAG = "OpmlWriter";
	private static final String ENCODING = "UTF-8";
	private static final String OPML_VERSION = "2.0";
	private static final String OPML_TITLE = "SoundWaves Subscriptions";

	/**
	 * Takes a list of feeds and a writer and writes those into an OPML
	 * document.
	 * 
	 * @throws IOException
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	public void writeDocument(LongSparseArray<ISubscription> feeds, Writer writer)
			throws IllegalArgumentException, IllegalStateException, IOException {

        Log.d(TAG, "Starting to write document");
		XmlSerializer xs = Xml.newSerializer();
		xs.setOutput(writer);

		xs.startDocument(ENCODING, false);
		xs.startTag(null, OpmlSymbols.OPML);
		xs.attribute(null, OpmlSymbols.VERSION, OPML_VERSION);

		xs.startTag(null, OpmlSymbols.HEAD);
		xs.startTag(null, OpmlSymbols.TITLE);
		xs.text(OPML_TITLE);
		xs.endTag(null, OpmlSymbols.TITLE);
		xs.endTag(null, OpmlSymbols.HEAD);

		xs.startTag(null, OpmlSymbols.BODY);

		long key;
		ISubscription feed;
		for (int i = 0; i < feeds.size(); i++) {
			key = feeds.keyAt(i);
			feed = feeds.get(key);

			xs.startTag(null, OpmlSymbols.OUTLINE);
			xs.attribute(null, OpmlSymbols.TEXT, feed.getTitle());
			xs.attribute(null, OpmlSymbols.TITLE, feed.getTitle());
			/*
			if (feed.getType() != null) {
				xs.attribute(null, OpmlSymbols.TYPE, feed.getType());
			}*/
			xs.attribute(null, OpmlSymbols.XMLURL, feed.getURLString());
			if (feed.getURLString() != null) {
				xs.attribute(null, OpmlSymbols.HTMLURL, feed.getURLString());
			}
			xs.endTag(null, OpmlSymbols.OUTLINE);
		}
		xs.endTag(null, OpmlSymbols.BODY);
		xs.endTag(null, OpmlSymbols.OPML);
		xs.endDocument();
		Log.d(TAG, "Finished writing document");
	}

	/**
	 * Takes a list of feeds and a writer and writes those into an OPML
	 * document.
	 *
	 * @throws IOException
	 * @throws IllegalStateException
	 * @throws IllegalArgumentException
	 */
	@Deprecated
	public void writeDocument(List<Subscription> feeds, Writer writer)
			throws IllegalArgumentException, IllegalStateException, IOException {

		Log.d(TAG, "Starting to write document");
		XmlSerializer xs = Xml.newSerializer();
		xs.setOutput(writer);

		xs.startDocument(ENCODING, false);
		xs.startTag(null, OpmlSymbols.OPML);
		xs.attribute(null, OpmlSymbols.VERSION, OPML_VERSION);

		xs.startTag(null, OpmlSymbols.HEAD);
		xs.startTag(null, OpmlSymbols.TITLE);
		xs.text(OPML_TITLE);
		xs.endTag(null, OpmlSymbols.TITLE);
		xs.endTag(null, OpmlSymbols.HEAD);

		xs.startTag(null, OpmlSymbols.BODY);
		for (int i = 0; i < feeds.size(); i++) {
			Subscription feed = feeds.get(i);
			xs.startTag(null, OpmlSymbols.OUTLINE);
			xs.attribute(null, OpmlSymbols.TEXT, feed.getTitle());
			xs.attribute(null, OpmlSymbols.TITLE, feed.getTitle());
			/*
			if (feed.getType() != null) {
				xs.attribute(null, OpmlSymbols.TYPE, feed.getType());
			}*/
			xs.attribute(null, OpmlSymbols.XMLURL, feed.getURLString());
			xs.attribute(null, OpmlSymbols.HTMLURL, feed.getURLString());
			xs.endTag(null, OpmlSymbols.OUTLINE);
		}
		xs.endTag(null, OpmlSymbols.BODY);
		xs.endTag(null, OpmlSymbols.OPML);
		xs.endDocument();
		Log.d(TAG, "Finished writing document");
	}
}
