package org.bottiger.podcast.cloud.drive;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.RecentItemFragment;
import org.bottiger.podcast.provider.BulkUpdater;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;
import org.bottiger.podcast.provider.WithIcon;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * From
 * https://github.com/googledrive/dredit/blob/master/android/src/com/example
 * /android/notepad/DriveSyncer.java
 * https://developers.google.com/drive/examples
 * /android#keeping_drive_in_sync_with_the_device
 * 
 * @author Arvid Böttiger
 * 
 */
public class DriveSyncer {

	/** For debugging set parent = root. Othervise = appdata */
	private static final String parent = "appdata";
	private static int MAX_RESULTS = 10;

	private static final String SEPARATOR = "|";
	private static final String TIMESTAMP_KEY = "timestamp";
	private static final String ID_KEY = "id";

	/** For logging and debugging purposes */
	private static final String TAG = "DriveSyncerAdapter";

	/** Projection used for querying the database. */
	private static final String[] SUBSCRIPTION_PROJECTION = SubscriptionColumns.ALL_COLUMNS;
	private static final String[] ITEM_PROJECTION = ItemColumns.ALL_COLUMNS;

	/** Field query parameter used on request to the drive.about.get endpoint. */
	private static final String ABOUT_GET_FIELDS = "largestChangeId";

	/** text/plain MIME type. */
	private static final String TEXT_PLAIN = "text/plain";

	private static enum DataType {
		SUBSCRIPTION, EPISODE
	};

	/** The URI to the subscriptions and episodes */
	private Uri subscriptionUri = null;
	private Uri itemUri = null;

	/** Drive Filesnames */
	private static final String SUBSCRIPTIONS_FILENAME = "subscription";
	private static final String PLAYLIST_FILENAME = "playlist";
	private static final String EPISODES_PREFIX = "episodes";

	private List<WithIcon> subscriptions = new LinkedList<WithIcon>();

	private BulkUpdater mUpdater = new BulkUpdater();
	private Map<String, File> files;

	private Gson gson = new Gson();

	private Context mContext;
	private ContentProviderClient mProvider;
	private Account mAccount;
	private Drive mService;
	private long mLargestChangeId;

	private int REQUEST_AUTHORIZATION = 1;

	/**
	 * Instantiate a new DriveSyncer.
	 * 
	 * @param context
	 *            Context to use on credential requests.
	 * @param provider
	 *            Provider to use for database requests.
	 * @param account
	 *            Account to perform sync for.
	 */
	public DriveSyncer(Context context, ContentProviderClient provider,
			Account account) {
		mContext = context;
		mProvider = provider;
		mAccount = account;
		mService = getDriveService();
		mLargestChangeId = getLargestChangeId();

		subscriptionUri = getSubscriptionsUri(mAccount.name);
		itemUri = getItemsUri(mAccount.name);

	}

	public static String getScope() {
		return parent.equals("appdata") ? "https://www.googleapis.com/auth/drive.appdata"
				: DriveScopes.DRIVE;
	}

	/**
	 * Perform a synchronization for the current account.
	 * 
	 * The strategy is:
	 * 
	 * if not sync before:
	 * 
	 * if synced before:
	 * 
	 * 1) Update the Playlist - most critical item The most recent wins
	 * 
	 * 2) Update the subscription list. The most recent wins
	 * 
	 * 3) Update episodes. For each subscription the most recent list of
	 * episodes wins.
	 * 
	 * 4) If Drive contains more files which have not been changed we insert
	 * them locally
	 * 
	 * 5) Now, if the Playlist does not exist on the remote server we create it
	 * 
	 * 6) If the subscription list does not exist on the remote server we create
	 * it
	 * 
	 * 7) If any of the subscriptions does not contain a ID for a remote file we
	 * upload the file list and add a remote_id to the subscription
	 * 
	 */
	public void performSync() {
		if (mService == null) {
			return;
		}
		Log.d(TAG, "Performing sync for " + mAccount.name);
		if (mLargestChangeId == -1) {
			// First time the sync adapter is run for the provided account.
			performFullSync();
		} else {

			// The the files which have changed since the apps changeID
			files = getChangedFiles(mLargestChangeId);

			// Fetch the ContentResolver
			ContentResolver contentResolver = mContext.getContentResolver();

			/*
			 * Merge FeedItems with Google Drive
			 */
			mergeEpisodes(files);

			mUpdater.commit(contentResolver);

			// Any remaining files in the map are files that do not exist in
			// the local database.
			if (!files.isEmpty())
				insertNewDriveFiles(files.values());

			// Update the changeID
			storeLargestChangeId(mLargestChangeId + 1);

		}

		// Insert new local files.
		files = getKeyFiles();
		
		/*
		 * Step 1
		 * 
		 * Merge playlist if it exists
		 */
		if (files.containsKey(PLAYLIST_FILENAME)) {
			List<WithIcon> playlist = getPlaylist();
			File file = files.get(PLAYLIST_FILENAME);

			mergeFiles(playlist, file, PLAYLIST_FILENAME, DataType.EPISODE);
		}

		/*
		 * Step 2
		 * 
		 * Merge Subscriptions with Google Drive (if the file exists)
		 */
		if (files.containsKey(SUBSCRIPTIONS_FILENAME)) {
			mergeSubscriptions(files);
		}
		
		insertNewLocalFiles();

		// Cleanup legacy files
		// cleanup(100);

		Log.d(TAG, "Done performing sync for " + mAccount.name);
	}

	/**
	 * Performs a full sync, usually occurs the first time a sync occurs for the
	 * account.
	 */
	private void performFullSync() {
		Log.d(TAG, "Performing first sync");
		Long largestChangeId = (long) -1;
		try {
			// Get the largest change Id first to avoid race conditions.
			com.google.api.services.drive.model.About about = mService.about()
					.get().setFields(ABOUT_GET_FIELDS).execute();
			largestChangeId = about.getLargestChangeId();
		} catch (IOException e) {
			e.printStackTrace();
		}
		storeAllDriveFiles();
		storeLargestChangeId(largestChangeId);
		Log.d(TAG, "Done performing first sync: " + largestChangeId);
	}

	/**
	 * Insert all new local files in Google Drive.
	 */
	private void insertNewLocalFiles() {
		Uri uri = getSubscriptionsUri(mAccount.name);
		try {

			ContentResolver contenResolver = mContext.getContentResolver();

			// Insert the current Playlist if the file does not exist
			if (!files.containsKey(PLAYLIST_FILENAME)) {
				File newFile = newFile = new File();

				List<WithIcon> playlist = getPlaylist();
				newFile.setTitle(PLAYLIST_FILENAME);
				newFile.setMimeType(TEXT_PLAIN);
				String description = createFileDescription(
						mostRecentItem(playlist), PLAYLIST_FILENAME);
				newFile.setDescription(description);
				newFile.setParents(Arrays.asList(new ParentReference()
						.setId(parent)));

				mergeFiles(playlist, newFile, PLAYLIST_FILENAME,
						DataType.EPISODE);
			}

			// Insert the current list of subscriptions if the file does not
			// exist
			if (!files.containsKey(SUBSCRIPTIONS_FILENAME)) {
				insertSubscriptions();
			}

			// Insert the feedItems which does not have a DriveID
			Cursor subscriptionCursor = mProvider.query(uri,
					SUBSCRIPTION_PROJECTION, SubscriptionColumns.SERVER_ID
							+ " IS NULL", null, null);
			Log.d(TAG,
					"Got local subscriptions: " + subscriptionCursor.getCount());

			for (boolean more = subscriptionCursor.moveToFirst(); more; more = subscriptionCursor
					.moveToNext()) {
				Subscription subscription = Subscription
						.getByCursor(subscriptionCursor);
				String key = subscriptionKey(subscription);
				String filename = remoteFilename(subscription);

				// Test if the subscription is already at the remote destination
				boolean remoteFileExists = false;
				LinkedList<File> candidates = getRemoteSubscription(subscription);

				for (File file : candidates) {
					if (getDescriptionID(file).equals(subscription.url)) {
						if (!remoteFileExists) {
							remoteFileExists = true;
							subscription.setDriveId(file.getId());
							mUpdater.addOperation(subscription.update(contenResolver, true, true));
						} else {
							// If we already found a file with the correct
							// description we delete the next one
							try {
								Log.d(TAG, "Deleting: " + file.getDescription());
								mService.files().delete(file.getId()).execute();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
				}
				

				if (!remoteFileExists) {
					List<FeedItem> episodes = subscription
							.getFeedItems(contenResolver);

					long mostRecent = mostRecentFeedItem(episodes);

					File newFile = new File();
					File insertedFile = null;
					newFile.setTitle(filename);
					newFile.setMimeType(TEXT_PLAIN);
					String description = createFileDescription(mostRecent, key);
					newFile.setDescription(description);
					newFile.setParents(Arrays.asList(new ParentReference()
							.setId(parent)));

					Gson gson = new Gson();
					String content = gson.toJson(episodes);

					Log.d(TAG, "Inserting new local episodes: " + filename + ". Description: " + newFile.getDescription());
					try {
						if (content != null && content.length() > 0) {

							insertedFile = mService
									.files()
									.insert(newFile,
											ByteArrayContent.fromString(
													TEXT_PLAIN, content))
									.execute();
						} else {
							insertedFile = mService.files().insert(newFile)
									.execute();
						}
						// Update the local file to add the file ID.
						subscription.setDriveId(insertedFile.getId());
						mUpdater.addOperation(subscription.update(
								contenResolver, true, true));
					} catch (IOException e) {
						e.printStackTrace();
					}

					// files.remove(filename);

				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		mUpdater.commit(mContext.getContentResolver());
	}

	private String subscriptionKey(Subscription subscription) {
		return subscription.url;
	}

	/**
	 * Insert the SUBSCRIPTIONS_FILENAME containing all the subscriptions
	 */
	private void insertSubscriptions() {

		Gson gson = new Gson();
		File insertedFile = null;
		File newFile = new File();
		newFile.setTitle(SUBSCRIPTIONS_FILENAME);
		newFile.setMimeType(TEXT_PLAIN);

		newFile.setParents(Arrays.asList(new ParentReference().setId(parent)));

		List<WithIcon> subscriptions = getSubscriptions();

		long date1 = subscriptions.get(0).lastModificationDate();
		String description = createFileDescription(date1,
				SUBSCRIPTIONS_FILENAME);
		newFile.setDescription(description);

		if (subscriptions.size() > 0) {
			String content = gson.toJson(subscriptions);
			try {
				if (content != null && content.length() > 0) {

					insertedFile = mService
							.files()
							.insert(newFile,
									ByteArrayContent.fromString(TEXT_PLAIN,
											content)).execute();

				} else {
					insertedFile = mService.files().insert(newFile).execute();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * Insert new Google Drive files in the local database.
	 * 
	 * @param driveFiles
	 *            Collection of Google Drive files to insert.
	 */
	private void insertNewDriveFiles(Collection<File> driveFiles) {
		Uri uri = getSubscriptionsUri(mAccount.name);

		Log.d(TAG, "Inserting new Drive files: " + driveFiles.size());

		ContentResolver contentResolver = mContext.getContentResolver();

		for (File driveFile : driveFiles) {
			if (driveFile != null) {

				if (driveFile.getTitle().equals(SUBSCRIPTIONS_FILENAME)) {
					Type collectionType = new TypeToken<Collection<Subscription>>() {
					}.getType();
					String fileContent = this.getFileContent(driveFile);
					Collection<Subscription> subscriptions = gson.fromJson(
							fileContent, collectionType);

					for (Subscription subscription : subscriptions) {
						subscription.update(contentResolver);
					}
				} else if (driveFile.getTitle().equals(PLAYLIST_FILENAME)) {

				} else if (driveFile.getTitle().startsWith(EPISODES_PREFIX)) {
					Type collectionType = new TypeToken<Collection<FeedItem>>() {
					}.getType();
					String fileContent = this.getFileContent(driveFile);
					Collection<FeedItem> items = gson.fromJson(fileContent,
							collectionType);

					for (FeedItem item : items) {
						item.update(contentResolver);
					}
				}

			}
		}

		mContext.getContentResolver().notifyChange(uri, null, false);
	}

	/**
	 * 
	 */
	public AbstractInputStreamContent setFileContent(String mimeType,
			String content) {

		byte[] bytes = content.getBytes();

		ByteArrayOutputStream baos = null;
		Deflater dfl = new Deflater();
		dfl.setLevel(Deflater.DEFAULT_COMPRESSION);
		dfl.setInput(bytes);
		dfl.finish();
		baos = new ByteArrayOutputStream();
		byte[] tmp = new byte[4 * 1024];
		try {
			while (!dfl.finished()) {
				int size = dfl.deflate(tmp);
				baos.write(tmp, 0, size);
			}
		} catch (Exception ex) {

		} finally {
			try {
				if (baos != null)
					baos.close();
			} catch (Exception ex) {
			}
		}
		return new ByteArrayContent(mimeType, baos.toByteArray());
	}

	/**
	 * 
	 */
	public AbstractInputStreamContent setFileContent(String content) {
		return setFileContent(TEXT_PLAIN, content);
	}

	/**
	 * Retrieve a Google Drive file's content.
	 * 
	 * @param driveFile
	 *            Google Drive file to retrieve content from.
	 * @return Google Drive file's content if successful, {@code null}
	 *         otherwise.
	 */
	public String getFileContent(File driveFile) {
		String result = "";

		if (driveFile.getDownloadUrl() != null
				&& driveFile.getDownloadUrl().length() > 0) {
			try {
				GenericUrl downloadUrl = new GenericUrl(
						driveFile.getDownloadUrl());

				HttpResponse resp = mService.getRequestFactory()
						.buildGetRequest(downloadUrl).execute();
				InputStream inputStream = null;

				try {
					inputStream = resp.getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(inputStream));
					StringBuilder content = new StringBuilder();
					char[] buffer = new char[1024];
					int num;

					while ((num = reader.read(buffer)) > 0) {
						content.append(buffer, 0, num);
					}
					result = content.toString();
				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
				}
			} catch (IOException e) {
				// An error occurred.
				e.printStackTrace();
				return null;
			}
		} else {
			// The file doesn't have any content stored on Drive.
			return null;
		}

		return result;
	}

	/**
	 * Retrieve a collection of files that have changed since the provided
	 * {@code changeId}.
	 * 
	 * @param changeId
	 *            Change ID to retrieve changed files from.
	 * @return Map of changed files key'ed by their file ID.
	 */
	private Map<String, File> getChangedFiles(long changeId) {
		Map<String, File> result = new HashMap<String, File>();

		try {
			Changes.List request = mService.changes().list()
					.setStartChangeId(changeId);
			do {
				ChangeList changes = request.execute();
				long largestChangeId = changes.getLargestChangeId().longValue();

				for (Change change : changes.getItems()) {
					File file = change.getFile();
					if (file != null) {
						Log.d(TAG, "found: " + file.getTitle());
						try {
							String key = getDescriptionID(file);

							result.put(key, file);
						} catch (Exception e) {
							Log.d(TAG, "invalid and unparsable description: "
									+ file.getTitle() + ". Description: "
									+ file.getDescription());
						}
					}
				}

				if (largestChangeId > mLargestChangeId) {
					mLargestChangeId = largestChangeId;
				}
				request.setPageToken(changes.getNextPageToken());
			} while (request.getPageToken() != null
					&& request.getPageToken().length() > 0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "Got changed Drive files: " + result.size() + " - "
				+ mLargestChangeId);
		return result;
	}

	/**
	 * Retrieve a collection of all files created by the app {@code changeId}.
	 * 
	 * @param changeId
	 *            Change ID to retrieve changed files from.
	 * @return Map of changed files key'ed by their file ID.
	 */
	private Map<String, File> getAllFiles() {
		Map<String, File> result = new HashMap<String, File>();
		String[] titles = { EPISODES_PREFIX, PLAYLIST_FILENAME,
				SUBSCRIPTIONS_FILENAME };

		for (int i = 0; i < titles.length; i++) {
			try {
				Files.List request = mService
						.files()
						.list()
						.setMaxResults(MAX_RESULTS)
						.setQ("title contains '" + titles[i] + "' and '"
								+ parent + "' in parents");
				do {
					FileList files = request.execute();

					String content = null;
					for (File file : files.getItems()) {
						Log.d(TAG, "found: " + file.getTitle());
						// mService.files().delete(file.getId());
						result.put(file.getDescription(), file);
						// content = getFileContent(file);
					}
					request.setPageToken(files.getNextPageToken());
				} while (request.getPageToken() != null
						&& request.getPageToken().length() > 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Log.d(TAG, "Got Drive files: " + result.size() + " - "
				+ mLargestChangeId);
		return result;
	}

	private LinkedList<File> getRemoteSubscription(Subscription subscription) {
		LinkedList<File> result = new LinkedList<File>();
		String filename = remoteFilename(subscription);
		try {
			Files.List request = mService
					.files()
					.list()
					.setMaxResults(MAX_RESULTS)
					.setQ("title contains '" + filename + "' and '" + parent
							+ "' in parents");
			do {
				FileList files = request.execute();

				String content = null;
				for (File file : files.getItems()) {
					Log.d(TAG, "found: " + file.getTitle() + " description: "
							+ file.getDescription());
					// mService.files().delete(file.getId());
					try {
						String key = getDescriptionID(file);
						result.add(file);
					} catch (Exception e) {
						Log.d(TAG,
								"invalid and unparsable description: "
										+ file.getTitle() + ". Description: "
										+ file.getDescription());
					}
				}
				request.setPageToken(files.getNextPageToken());
			} while (request.getPageToken() != null
					&& request.getPageToken().length() > 0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "Got Drive files: " + result.size() + " - "
				+ mLargestChangeId);
		return result;
	}

	/**
	 * Retrieve a collection of all files created by the app {@code changeId}.
	 * 
	 * @param changeId
	 *            Change ID to retrieve changed files from.
	 * @return Map of changed files key'ed by their file ID.
	 */
	private Map<String, File> getKeyFiles() {
		Map<String, File> result = new HashMap<String, File>();
		String[] titles = { PLAYLIST_FILENAME, SUBSCRIPTIONS_FILENAME };

		for (int i = 0; i < titles.length; i++) {
			int filesFound = 0;
			try {
				Files.List request = mService
						.files()
						.list()
						.setMaxResults(MAX_RESULTS)
						.setQ("title contains '" + titles[i] + "' and '"
								+ parent + "' in parents");
				do {
					FileList files = request.execute();

					String content = null;
					for (File file : files.getItems()) {
						Log.d(TAG, "found: " + file.getTitle());
						// mService.files().delete(file.getId());
						try {
							String key = null;
							if (titles[i].equals(EPISODES_PREFIX)) {
								key = getDescriptionID(file);
							} else {
								key = titles[i];
							}
							result.put(key, file);
							filesFound++;

							if (filesFound > 1) {
								Log.d(TAG, "Delete: " + file.getTitle());
								mService.files().delete(file.getId()).execute();
							}

						} catch (Exception e) {
							Log.d(TAG, "invalid and unparsable description: "
									+ file.getTitle() + ". Description: "
									+ file.getDescription());
						}
					}
					request.setPageToken(files.getNextPageToken());
				} while (request.getPageToken() != null
						&& request.getPageToken().length() > 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Log.d(TAG, "Got Drive files: " + result.size() + " - "
				+ mLargestChangeId);
		return result;
	}

	/**
	 * Cleans up the appdata by removing old files.
	 */
	private void cleanup(int filesToDelete) {

		for (int k = 0; k < 10; k++) {

			int counter = 0;
			try {
				Files.List request = mService
						.files()
						.list()
						.setMaxResults(100)
						.setQ("not title contains '" + EPISODES_PREFIX
								+ "' and not title contains '"
								+ SUBSCRIPTIONS_FILENAME
								+ "' and not title contains '"
								+ PLAYLIST_FILENAME + "' and 'root' in parents");

				do {
					FileList files = request.execute();

					for (File file : files.getItems()) {
						Log.d(TAG, "delete: " + file.getTitle() + " <- "
								+ ++counter);
						mService.files().delete(file.getId()).execute();
						filesToDelete--;
					}
					request.setPageToken(files.getNextPageToken());
				} while (request.getPageToken() != null
						&& request.getPageToken().length() > 0
						&& filesToDelete > 0);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Retrieve a authorized service object to send requests to the Google Drive
	 * API. On failure to retrieve an access token, a notification is sent to
	 * the user requesting that authorization be granted for the
	 * {@code https://www.googleapis.com/auth/drive.file} scope.
	 * 
	 * @return An authorized service object.
	 */
	private Drive getDriveService() {
		if (mService == null) {
			try {
				GoogleAccountCredential mainCredentials = MainActivity
						.getCredentials();
				GoogleAccountCredential credential = null;
				if (mainCredentials == null) {
					credential = GoogleAccountCredential.usingOAuth2(mContext,
							"https://www.googleapis.com/auth/drive.appdata"); // DriveScopes.DRIVE_FILE);
					// credential =
					// GoogleAccountCredential.usingOAuth2(mContext,
					// DriveScopes.DRIVE_FILE);
					credential.setSelectedAccountName(mAccount.name);
				} else {
					credential = mainCredentials;
				}
				// Trying to get a token right away to see if we are authorized
				credential.getToken();
				mService = new Drive.Builder(
						AndroidHttp.newCompatibleTransport(),
						new GsonFactory(), credential).build();
			} catch (Exception e) {
				Log.e(TAG, "Failed to get token");
				// If the Exception is User Recoverable, we display a
				// notification that will trigger the
				// intent to fix the issue.
				if (e instanceof UserRecoverableAuthException) {

					UserRecoverableAuthException exception = (UserRecoverableAuthException) e;
					NotificationManager notificationManager = (NotificationManager) mContext
							.getSystemService(Context.NOTIFICATION_SERVICE);
					Intent authorizationIntent = exception.getIntent();

					// .startActivityForResult(authorizationIntent,
					// REQUEST_AUTHORIZATION);

					authorizationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
							.addFlags(Intent.FLAG_FROM_BACKGROUND);
					PendingIntent pendingIntent = PendingIntent.getActivity(
							mContext, REQUEST_AUTHORIZATION,
							authorizationIntent, 0);

					NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
							mContext)
							.setSmallIcon(android.R.drawable.ic_dialog_alert)
							.setTicker("Permission requested")
							.setContentTitle("Permission requested")
							.setContentText("for account " + mAccount.name)
							.setContentIntent(pendingIntent)
							.setAutoCancel(true);
					notificationManager.notify(0, notificationBuilder.build());

				} else {
					e.printStackTrace();
				}
			}
		}
		return mService;
	}

	/**
	 * Store all text/plain files from Drive that the app has access to. This is
	 * called the first time the app synchronize the database with Google Drive
	 * for the current user.
	 */
	private void storeAllDriveFiles() {
		Map<String, File> textFiles = getAllFiles();

		ContentResolver contentResolver = mContext.getContentResolver();

		// Insert subscriptions from remote
		if (textFiles.containsKey(SUBSCRIPTIONS_FILENAME)) {
			mergeSubscriptions(textFiles);
		}

		/*
		 * Merge FeedItems with Google Drive
		 */
		mergeEpisodes(textFiles);

		mUpdater.commit(contentResolver);

		// insertNewDriveFiles(textFiles.values());
	}

	/**
	 * Merge subscriptions
	 */
	private void mergeSubscriptions(Map<String, File> files) {
		File file = files.get(SUBSCRIPTIONS_FILENAME);
		List<WithIcon> subscriptions = getSubscriptions();
		mergeFiles(subscriptions, file, SUBSCRIPTIONS_FILENAME,
				DataType.SUBSCRIPTION);
	}

	/**
	 * Merge episodes
	 */
	private void mergeEpisodes(Map<String, File> files) {
		ContentResolver contentResolver = mContext.getContentResolver();

		ConcurrentHashMap<String, File> cFiles = new ConcurrentHashMap<String, File>(
				files);
		for (String fileKey : cFiles.keySet()) {

			File file = files.get(fileKey);

			if (file.getTitle().startsWith(EPISODES_PREFIX)) {

				// Find the subscription the current file belongs to
				Cursor subscriptionCursor = null;
				try {
					subscriptionCursor = mProvider.query(subscriptionUri,
							SUBSCRIPTION_PROJECTION, SubscriptionColumns.URL
									+ "==\"" + getDescriptionID(file) + "\"",
							null, null);
				} catch (RemoteException e) {
					e.printStackTrace();
				}

				Log.d(TAG,
						"Got local episodes: " + subscriptionCursor.getCount());

				List<WithIcon> episodes = new LinkedList<WithIcon>();
				int counter = 0;
				Subscription subscription = null;

				// Find all the episodes (hint: the is always only one
				// subscription here.
				// The loops is redundant
				for (boolean more = subscriptionCursor.moveToFirst(); more; more = subscriptionCursor
						.moveToNext()) {
					counter++;
					subscription = Subscription.getByCursor(subscriptionCursor);
					Log.d(TAG, "merged episode: " + counter + " of "
							+ subscriptionCursor.getCount());

					episodes.addAll(subscription.getFeedItems(contentResolver));
				}

				// Find the most recent item in order to keep track of whether
				// the file should be synced in the future
				long mostRecent = mostRecentItem(episodes);

				if (subscription != null) {
					String description = this.createFileDescription(mostRecent,
							subscriptionKey(subscription));
					file.setDescription(description);
					mergeFiles(episodes, file,
							remoteFilename(episodes, subscription),
							DataType.EPISODE);
				}
				files.remove(fileKey);
			}
		}
	}

	/**
	 * Store the largest change ID for the current user.
	 * 
	 * @param changeId
	 *            The largest change ID to store.
	 */
	private void storeLargestChangeId(long changeId) {
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(mContext).edit();
		editor.putLong("largest_change_" + mAccount.name, changeId);
		editor.commit();
		mLargestChangeId = changeId;
	}

	/**
	 * Merge a local file with a Google Drive File.
	 * 
	 * The last modification is used to check which file to sync from. Then, the
	 * md5 checksum of the file is used to check whether or not the file's
	 * content should be sync'ed.
	 * 
	 * @param localFile
	 *            Local file URI to save local changes against.
	 * @param localFile
	 *            Local file cursor to retrieve data from.
	 * @param driveFile
	 *            Google Drive file.
	 */
	private void mergeFiles(List<WithIcon> localFile, File driveFile,
			String title, DataType type) {

		Gson gson = new Gson();
		long localFileModificationDate = 0;
		for (WithIcon item : localFile)
			localFileModificationDate = (item.lastModificationDate() > localFileModificationDate) ? item
					.lastModificationDate() : localFileModificationDate;

		long driveFileModificationTime = (driveFile.getDescription() != null) ? Long
				.parseLong(getDescriptionTimestamp(driveFile)) : 0;

		boolean updateFileOnDrive = false;
		boolean updateFileOnDevice = false;

		// Always update the remote playlist if the local one is more recent
		updateFileOnDrive = localFileModificationDate > driveFileModificationTime
				|| driveFile.getId() == null;
		updateFileOnDevice = localFileModificationDate < driveFileModificationTime;

		Log.d(TAG, "Modification dates. Local: " + localFileModificationDate
				+ " - Remote: " + driveFileModificationTime);
		if (updateFileOnDrive) {

			String uid = getDescriptionID(driveFile);
			driveFile.setDescription(createFileDescription(
					localFileModificationDate, uid));
			updateDrive(localFile, driveFile, title);

		} else if (updateFileOnDevice) {

			// Update local file.
			Log.d(TAG, " > Updating local file.");

			if (type.equals(DataType.SUBSCRIPTION)) {
				// Type collectionType = new
				// TypeToken<List<Subscription>>(){}.getType();
				Collection<Subscription> subscriptions = gson.fromJson(
						getFileContent(driveFile),
						new TypeToken<List<Subscription>>() {
						}.getType());

				for (Subscription subscription : subscriptions) {
					subscription.update(mContext.getContentResolver(), false,
							true);
				}
			} else if (type.equals(DataType.EPISODE)) {
				Collection<FeedItem> episodes = gson.fromJson(
						getFileContent(driveFile),
						new TypeToken<List<FeedItem>>() {
						}.getType());

				for (FeedItem episode : episodes) {
					episode.update(mContext.getContentResolver(), false, true);
				}
			}
		}
	}

	private String getDeviceID() {
		return Secure.getString(mContext.getContentResolver(),
				Secure.ANDROID_ID);
	}

	private File updateDrive(List<WithIcon> localFiles, File driveFile,
			String title) {
		File updatedFile = null;
		try {

			// Update drive file.
			Log.d(TAG, " > Updating Drive file.");

			// String driveTitle = remoteFilename(localFile);
			String driveTitle = title;
			driveFile.setTitle(driveTitle);

			// String driveContent = getContent(localFile);
			String driveContent = gson.toJson(localFiles);
			// String driveContent = localFile.toJSON();

			if (md5(driveContent) != driveFile.getMd5Checksum()) {
				// Update both content and metadata.
				ByteArrayContent content = ByteArrayContent.fromString(
						TEXT_PLAIN, driveContent);

				if (driveFile.getId() != null) {
					updatedFile = mService.files()
							.update(driveFile.getId(), driveFile, content)
							.execute();
				} else {
					updatedFile = mService.files().insert(driveFile, content)
							.execute();
				}
			} else {
				// Only update the metadata.
				updatedFile = mService.files()
						.update(driveFile.getId(), driveFile).execute();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return updatedFile;
	}

	private String getContent(WithIcon localFile) {
		StringBuilder content = new StringBuilder();
		if (localFile instanceof Subscription) {
			Subscription subscription = (Subscription) localFile;
			// content.append(subscription.getTitle());
			try {
				content.append(subscription.getURL().toString());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (localFile instanceof Subscription) {
			FeedItem item = (FeedItem) localFile;
			content.append(item.getTitle());
			content.append(item.getURL());
		}
		return content.toString();
	}

	private List<WithIcon> getSubscriptions() {

		Cursor subscriptionCursor = null;
		try {
			subscriptionCursor = mProvider.query(
					getSubscriptionsUri(mAccount.name),
					SUBSCRIPTION_PROJECTION, null, null,
					SubscriptionColumns.LAST_UPDATED + " DESC");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d(TAG, "Got local subscriptions: " + subscriptionCursor.getCount());

		for (boolean more = subscriptionCursor.moveToFirst(); more; more = subscriptionCursor
				.moveToNext()) {
			Subscription subscription = Subscription
					.getByCursor(subscriptionCursor);
			subscriptions.add(subscription);
		}

		return subscriptions;
	}

	private List<WithIcon> getPlaylist() {
		Cursor itemCursor = null;
		try {
			itemCursor = mProvider.query(itemUri, ITEM_PROJECTION,
					ItemColumns.LISTENED + "== 0", null,
					RecentItemFragment.getOrder("DESC", 20));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<WithIcon> playlist = new LinkedList<WithIcon>();
		for (boolean more = itemCursor.moveToFirst(); more; more = itemCursor
				.moveToNext()) {
			FeedItem item = FeedItem.getByCursor(itemCursor);
			playlist.add(item);
		}
		return playlist;
	}

	private String md5(String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Retrieve the largest change ID for the current user if available.
	 * 
	 * @return The largest change ID, {@code -1} if not available.
	 */
	private long getLargestChangeId() {
		return PreferenceManager.getDefaultSharedPreferences(mContext).getLong(
				"largest_change_" + mAccount.name, -1);
	}

	private static Uri getSubscriptionsUri(String accountName) {
		return SubscriptionColumns.URI;
	}

	private static Uri getItemsUri(String accountName) {
		return ItemColumns.URI;
	}

	private static Uri getItemUri(String accountName, String fileId) {
		return Uri.parse(SubscriptionColumns.URI + "/" + fileId);
	}

	private String remoteFilename(Subscription subscription) {
		return EPISODES_PREFIX + "_" + subscription.getId() + "_"
				+ subscription.getTitle();
	}

	private String remoteFilename(List<WithIcon> items,
			Subscription subscription) {
		String driveTitle = EPISODES_PREFIX + "_";

		if (driveTitle.startsWith(EPISODES_PREFIX)) {
			driveTitle = driveTitle + subscription.getId();
		}
		return driveTitle;
	}

	private String getDescriptionID(File driveFile) {
		HashMap<String, String> values = parseDescription(driveFile
				.getDescription());
		return values.get(ID_KEY);
	}

	private String getDescriptionTimestamp(File driveFile) {
		HashMap<String, String> values = parseDescription(driveFile
				.getDescription());
		return values.get(TIMESTAMP_KEY);
	}

	private String getDescriptionIDFromDriveFile(WithIcon item) {
		if (item.getTitle().equals(PLAYLIST_FILENAME))
			return PLAYLIST_FILENAME;

		if (item.getTitle().equals(SUBSCRIPTIONS_FILENAME))
			return SUBSCRIPTIONS_FILENAME;

		return ((Subscription) item).url;
	}

	private String createFileDescription(long timestamp, String id) {
		return String.valueOf(timestamp) + SEPARATOR + id;
	}

	private HashMap<String, String> parseDescription(String description) {
		HashMap<String, String> values = new HashMap<String, String>();
		String[] parts = description.split("\\" + SEPARATOR);
		values.put(TIMESTAMP_KEY, parts[0]);
		values.put(ID_KEY, parts[1]);
		return values;
	}

	// FIXME remove this
	private long mostRecentFeedItem(List<FeedItem> items) {
		long localFileModificationDate = 0;
		for (WithIcon item : items)
			localFileModificationDate = (item.lastModificationDate() > localFileModificationDate) ? item
					.lastModificationDate() : localFileModificationDate;
		return localFileModificationDate;
	}

	private long mostRecentItem(List<WithIcon> items) {
		long localFileModificationDate = 0;
		for (WithIcon item : items) {
			long itemModificationDate = item.lastModificationDate();
			localFileModificationDate = (itemModificationDate > localFileModificationDate) ? itemModificationDate
					: localFileModificationDate;
		}
		return localFileModificationDate;
	}
}
