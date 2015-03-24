package org.bottiger.podcast.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bottiger.podcast.R;
import org.bottiger.podcast.parser.opml.OpmlElement;
import org.bottiger.podcast.parser.opml.OpmlReader;
import org.bottiger.podcast.provider.DatabaseHelper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

public class OPMLImportExport {

	public static final String filename = "podcasts.opml";
	public static final File file = new File(SDCardManager.getSDCardDir() + "/"
			+ filename);
	private static CharSequence opmlNotFound;
	private static String nSubscriptionsImported;

	private Context mContext;
	private ContentResolver contentResolver;
	private DatabaseHelper mUpdater = new DatabaseHelper();

	public OPMLImportExport(Context context) {
		this.mContext = context;
		this.contentResolver = context.getContentResolver();

        Resources res = mContext.getResources();
        opmlNotFound = String.format(res.getString(R.string.opml_not_found), filename);
	}

	public int importSubscriptions() {
		int numImported = 0;
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));

			OpmlReader omplReader = new OpmlReader();
			ArrayList<OpmlElement> opmlElements = omplReader
					.readDocument(reader);
			numImported = importElements(opmlElements);

		} catch (FileNotFoundException e) {
			toastMsg(opmlNotFound);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (numImported > 0) {
            Resources res = mContext.getResources();
            String formattedString = res.getQuantityString(R.plurals.subscriptions_imported, numImported, numImported);
            toastMsg(formattedString);
		}

		return numImported;
	}

	private int importElements(ArrayList<OpmlElement> elements) {
		int numImported = 0;
        List<Subscription> importedSubscriptions = new LinkedList<>();

		Subscription subscription;

		for (OpmlElement element : elements) {
			String url = element.getXmlUrl();
			String title = element.getText();

			// Test we if already have the item in out database.
			// If not we add it.
			boolean isAdded = false;
			subscription = Subscription.getByUrl(contentResolver, url);
			if (subscription == null) {
				subscription = new Subscription();
				subscription.url = url;
				if (title != null && !title.equals(""))
					subscription.setTitle(title);
				subscription.subscribe(mContext);
				isAdded = true;
			} else if (subscription.getStatus() == Subscription.STATUS_UNSUBSCRIBED) {
				subscription.status = Subscription.STATUS_SUBSCRIBED;
				isAdded = true;
			}

			if (isAdded) {
				mUpdater.addOperation(subscription.update(contentResolver,
						true, true));
				numImported++;
                importedSubscriptions.add(subscription);
			}
		}

		if (numImported > 0) {
            mUpdater.commit(contentResolver);

            for (Subscription insertedSubscription : importedSubscriptions) {
                insertedSubscription.refresh(mContext);
            }
        }

		return numImported;
	}

	private void toastMsg(CharSequence msg) {
		int duration = Toast.LENGTH_LONG;
		Toast.makeText(mContext, msg, duration).show();
	}
}
