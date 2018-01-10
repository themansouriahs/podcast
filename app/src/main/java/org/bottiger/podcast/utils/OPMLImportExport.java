package org.bottiger.podcast.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v7.util.SortedList;
import android.util.Log;
import android.widget.Toast;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.activities.openopml.OPMLImportExportActivity;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.flavors.CrashReporter.VendorCrashReporter;
import org.bottiger.podcast.parser.opml.OpmlElement;
import org.bottiger.podcast.parser.opml.OpmlReader;
import org.bottiger.podcast.parser.opml.OpmlWriter;
import org.bottiger.podcast.provider.DatabaseHelper;
import org.bottiger.podcast.provider.SlimImplementations.SlimSubscription;
import org.bottiger.podcast.provider.Subscription;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OPMLImportExport {

	private static final String TAG = OPMLImportExport.class.getSimpleName();

	private static final String filename = "podcasts.opml";
	private static final String filenameOut = "podcasts_export.opml";
	private static final int ACTIVITY_CHOOSE_FILE = 3;
	private static final int FILE_SELECT_CODE = 0;

	private static final String CLIPBOARD_LABEL = "opml_data";

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
	public OPMLImportExport(Activity argActivity) {
		Log.i(TAG, "OPMLImportExport");
		this.mActivity = argActivity;
		this.contentResolver = argActivity.getContentResolver();

		initInputOutputFiles();

		Resources res = mActivity.getResources();
		opmlNotFound = String.format(res.getString(R.string.opml_not_found), filename);
		opmlFailedToExport = res.getString(R.string.opml_export_failed);
		opmlSuccesfullyExported = String.format(res.getString(R.string.opml_export_succes), fileOut);
	}

	@RequiresPermission(allOf = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE})
	public List<SlimSubscription> readSubscriptionsFromOPML(@NonNull Reader argOPMLReader) {
		Log.i(TAG, "readSubscriptionsFromOPML(): " + argOPMLReader.toString());

		int numImported = 0;
		BufferedReader reader;
		ArrayList<OpmlElement> elements = new ArrayList<>();
		LinkedList<SlimSubscription> opmlSubscriptions = new LinkedList<>();

		try {
			Log.i(TAG, "try to read the opml buffer");
			reader = new BufferedReader(argOPMLReader);

			OpmlReader omplReader = new OpmlReader();
			elements = omplReader.readDocument(reader);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "FileNotFoundException: " + e.toString());
			toastMsg(opmlNotFound);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "XmlPullParserException: " + e.toString());
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "IOException: " + e.toString());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Log.i(TAG, "Read the OPML file. Found a number of elements: " + elements.size());
		for (int i = 0; i < elements.size(); i++) {
			OpmlElement element = elements.get(i);

			String url = element.getXmlUrl();
			String title = element.getText();

			URL parsedUrl = null;
			try {
				parsedUrl = new URL(url);
			} catch (MalformedURLException e) {
				Log.e(TAG, "Malform URL: " + url);
				e.printStackTrace();
				continue;
			}

			SlimSubscription slimSubscription = new SlimSubscription(title, parsedUrl, null);

			// Test we if already have the item in out database.
			// If not we add it.
			boolean isAlreadySubscribed = false;
			Subscription subscription = SoundWaves.getAppContext(mActivity).getLibraryInstance().getSubscription(url);
			if (subscription != null && subscription.status == Subscription.STATUS_SUBSCRIBED) {
				isAlreadySubscribed = true;
			}

			slimSubscription.setIsSubscribed(isAlreadySubscribed);
			opmlSubscriptions.add(slimSubscription);
		}

		//return opmlSubscriptions.toArray(new SlimSubscription[opmlSubscriptions.size()]);
		return opmlSubscriptions;
	}

	private void toastMsg(final CharSequence msg) {
		Activity activity = null;

		if (mActivity != null) {
			activity = mActivity;
		}

		if (activity == null) {
			Log.wtf(TAG, "no activity to post to!");
			return;
		}

		Log.i(TAG, "posting toast on main thread " + msg);
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "On main thread " + msg);
				int duration = Toast.LENGTH_LONG;
				Toast.makeText(mActivity, msg, duration).show();
			}
		});
	}

	public void exportSubscriptions(File fileOut) {
		Log.i(TAG, "exportSubscriptions(): " + fileOut);

		if (!initInputOutputFiles()) {
			Log.e(TAG, "Could not initialize input/output files");
			return;
		}

		FileWriter fileWriter = null;

		try {
			if (!fileOut.exists()) {
				Log.i(TAG, "Creating output file");
				fileOut.createNewFile();
			}

			fileWriter = new FileWriter(fileOut);
		} catch (IOException e) {
			Log.e(TAG, "Could not create output file: " + e.toString());
			toastMsg(opmlFailedToExport);
			VendorCrashReporter.handleException(e);
		}

		if (fileWriter == null) {
			Log.e(TAG, "Could not create output file. Toast: " + opmlFailedToExport);
			toastMsg(opmlFailedToExport);
			return;
		}

		OpmlWriter opmlWriter = new OpmlWriter();
		List<Subscription> subscriptionList = SoundWaves.getAppContext(mActivity).getLibraryInstance().getLiveSubscriptions().getValue();

		try {
			Log.i(TAG, "Writing output");
			opmlWriter.writeDocument(subscriptionList, fileWriter);
		} catch (IOException e) {
			Log.e(TAG, "Could not write output. Toast: " + opmlFailedToExport);
			toastMsg(opmlFailedToExport);
			return;
		}

		SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.OPML_EXPORT);
	}

	public void exportSubscriptionsToClipboard() {
		Log.i(TAG, "exportSubscriptionsToClipboard()");

		Writer writer = new StringWriter();
		OpmlWriter opmlWriter = new OpmlWriter();

		List<Subscription> subscriptionList = SoundWaves.getAppContext(mActivity).getLibraryInstance().getLiveSubscriptions().getValue();

		try {
			opmlWriter.writeDocument(subscriptionList, writer);
		} catch (IOException e) {
			Log.e(TAG, "opmlFailedToExport()");
			toastMsg(opmlFailedToExport);
			return;
		}

		String opmldata = writer.toString();
		ClipData clipData = ClipData.newPlainText(CLIPBOARD_LABEL, opmldata);

		ClipboardManager clipboard = (ClipboardManager)mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
		clipboard.setPrimaryClip(clipData);

		SoundWaves.sAnalytics.trackEvent(IAnalytics.EVENT_TYPE.OPML_EXPORT);
	}

	@NonNull
	public static String getFilename() {
		return filename;
	}

	/**
	 *
	 * @return just launches the file manager, return true if everything is ok
	 * @throws SecurityException
	 */
	private boolean initInputOutputFiles() throws SecurityException {
		Log.i(TAG, "initInputOutputFiles()");
		try {
			file = new File(SDCardManager.getSDCardRootDir() + "/" + filename);
			fileOut = new File(SDCardManager.getExportDir() + "/" + filenameOut);
		} catch (IOException e) {
			Log.i(TAG, "Could not create OPML input/output files: " + e.toString());
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public File getExportFile() {
		Log.i(TAG, "getExportFile()");
		initInputOutputFiles();
		return fileOut;
	}

	public File getImportFile() {
		Log.i(TAG, "getImportFile()");
		initInputOutputFiles();
		return file;
	}

	public static String getImportFilename() {
		return filename;
	}

	public static String getExportFilename() {
		return filenameOut;
	}
}
