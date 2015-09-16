package org.bottiger.podcast.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.TopActivity;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.parser.opml.OpmlElement;
import org.bottiger.podcast.parser.opml.OpmlReader;
import org.bottiger.podcast.parser.opml.OpmlWriter;
import org.bottiger.podcast.provider.DatabaseHelper;
import org.bottiger.podcast.provider.ISubscription;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionLoader;
import org.xmlpull.v1.XmlPullParserException;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.widget.Toast;

public class OPMLImportExport {

	private static final String filename = "podcasts.opml";
	private static final String filenameOut = "podcasts_export.opml";

	public File file;
	public File fileOut;
	private CharSequence opmlNotFound;
	private CharSequence opmlFailedToExport;
	private CharSequence opmlSuccesfullyExported;

	private Activity mActivity;
	private ContentResolver contentResolver;
	private DatabaseHelper mUpdater = new DatabaseHelper();

    @RequiresPermission(allOf = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE})
	public OPMLImportExport(Activity context) {
		this.mActivity = context;
		this.contentResolver = context.getContentResolver();

		initInputOutputFiles();

		Resources res = mActivity.getResources();
		opmlNotFound = String.format(res.getString(R.string.opml_not_found), filename);
		opmlFailedToExport = res.getString(R.string.opml_export_failed);
		opmlSuccesfullyExported = String.format(res.getString(R.string.opml_export_succes), fileOut);
	}

	public int importSubscriptions() {
		int numImported = 0;
		BufferedReader reader;

		if (!initInputOutputFiles()) {
			return 0;
		}

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
			Resources res = mActivity.getResources();
            String formattedString = res.getQuantityString(R.plurals.subscriptions_imported, numImported, numImported);
            toastMsg(formattedString);
            SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.OPML_IMPORT);
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
			subscription = SubscriptionLoader.getByUrl(contentResolver, url);
			if (subscription == null) {
				subscription = new Subscription();
				subscription.url = url;
				if (title != null && !title.equals(""))
					subscription.setTitle(title);
				subscription.subscribe(mActivity);
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
                insertedSubscription.refreshAsync(mActivity);
            }
        }

		return numImported;
	}

	private void toastMsg(final CharSequence msg) {
        Activity activity = null;

        if (mActivity instanceof Activity) {
            activity = (Activity) mActivity;
        }

        if (activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int duration = Toast.LENGTH_LONG;
                Toast.makeText(mActivity, msg, duration).show();
            }
        });
	}

    public void exportSubscriptions() {

        if (!initInputOutputFiles()) {
            return;
        }

        FileWriter fileWriter = null;

        try {
            if (!fileOut.exists()) {
                fileOut.createNewFile();
            }

            fileWriter = new FileWriter(fileOut);
        } catch (IOException e) {
            VendorCrashReporter.handleException(e);
        }

        if (fileWriter == null) {
            toastMsg(opmlFailedToExport);
            return;
        }

        OpmlWriter opmlWriter = new OpmlWriter();
        List<Subscription> subscriptionList = SubscriptionLoader.allAsList(contentResolver);

        try {
            opmlWriter.writeDocument(subscriptionList, fileWriter);
        } catch (IOException e) {
            toastMsg(opmlFailedToExport);
            return;
        }

        SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.OPML_EXPORT);
        toastMsg(opmlSuccesfullyExported);
    }

	public static String toOPML(LongSparseArray<ISubscription> argSubscriptions) {
		OpmlWriter opmlWriter = new OpmlWriter();
		StringWriter sw = new StringWriter();

		try {
			opmlWriter.writeDocument(argSubscriptions, sw);
		} catch (IOException e) {
			Log.d("toOPML", "Failed converting subscriptions to OPML");
			return "";
		}

		return sw.toString();
	}

    @NonNull
    public static String getFilename() {
        return filename;
    }

	/**
	 *
	 * @return true if everything is OK. Returns false if there was an IOException
	 * @throws SecurityException
	 */
	@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	private boolean initInputOutputFiles() throws SecurityException {

		if (file != null && fileOut != null)
			return true;

		try {
			file = new File(SDCardManager.getSDCardDir() + "/" + filename);
			fileOut = new File(SDCardManager.getExportDir() + "/" + filenameOut);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
