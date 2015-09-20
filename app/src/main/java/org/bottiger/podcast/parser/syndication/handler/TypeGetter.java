package org.bottiger.podcast.parser.syndication.handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.Reader;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.input.XmlStreamReader;
import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

/** Gets the type of a specific feed by reading the root element. */
public class TypeGetter {
	private static final String TAG = "TypeGetter";

	enum Type {
		RSS20, RSS091, ATOM, INVALID
	}

	private static final String ATOM_ROOT = "feed";
	private static final String RSS_ROOT = "rss";

	public Type getType(ISubscription feed, InputStream feedContent) throws UnsupportedFeedtypeException {
		XmlPullParserFactory factory;
		if (feed.getURL().toString() != null) {
			try {
				factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser xpp = factory.newPullParser();
				xpp.setInput(createReader(feed, feedContent));
				int eventType = xpp.getEventType();

				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_TAG) {
						String tag = xpp.getName();
						if (tag.equals(ATOM_ROOT)) {
							//feed.setType(Subscription.TYPE_ATOM1); // FIXME
							if (MainActivity.debugging)
								Log.d(TAG, "Recognized type Atom");
							return Type.ATOM;
						} else if (tag.equals(RSS_ROOT)) {
							String strVersion = xpp.getAttributeValue(null,
									"version");
							if (strVersion != null) {

								if (strVersion.equals("2.0")) {
									//feed.setType(Subscription.TYPE_RSS2); // FIXME
									if (MainActivity.debugging)
										Log.d(TAG, "Recognized type RSS 2.0");
									return Type.RSS20;
								} else if (strVersion.equals("0.91")
										|| strVersion.equals("0.92")) {
									if (MainActivity.debugging)
										Log.d(TAG,
												"Recognized type RSS 0.91/0.92");
									return Type.RSS091;
								}
							}
							throw new UnsupportedFeedtypeException(Type.INVALID);
						} else {
							Log.d(TAG, "Type is invalid: " + tag + ".");
							throw new UnsupportedFeedtypeException(Type.INVALID);
						}
					} else {
						eventType = xpp.next();
					}
				}

			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Log.d(TAG, "Type is invalid");
		throw new UnsupportedFeedtypeException(Type.INVALID);
	}

	private Reader createReader(ISubscription feed, InputStream feedContent) {
		Reader reader;
		try {
			//reader = new XmlStreamReader(new File(feed.getFile_url()));
			
			// http://stackoverflow.com/questions/4897876/reading-utf-8-bom-marker
			InputStream stream = checkForUtf8BOMAndDiscardIfAny(feedContent);//new ByteArrayInputStream(feedContent.getBytes("UTF-8"));
			BOMInputStream bomIn = new BOMInputStream(stream);
			reader = new XmlStreamReader(bomIn);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return reader;
	}
	
	/**
	 * http://stackoverflow.com/questions/9736999/how-to-remove-bom-from-an-xml-file-in-java
	 */
	private static InputStream checkForUtf8BOMAndDiscardIfAny(InputStream inputStream) throws IOException {
	    PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
	    byte[] bom = new byte[3];
	    if (pushbackInputStream.read(bom) != -1) {
	        if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
	            pushbackInputStream.unread(bom);
	        }
	    }
	    return pushbackInputStream; }
}
