package org.bottiger.podcast.provider;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.database.SQLException;
import android.net.Uri;
import android.provider.BaseColumns;

public class ItemColumns implements BaseColumns {

	public static final int ITEM_STATUS_UNREAD = 0;

	public static final int ITEM_STATUS_MAX_DOWNLOADING_VIEW = 30;
	public static final int ITEM_STATUS_NO_PLAY = 50;

	// KEEP status has been replaced by the KEEP database column starting in DB
	// version 13,
	// so is no longer used except for upgrading from version 12, which strings
	// are all in this file.
	private static final int ITEM_STATUS_KEEP = 63;
	public static final int ITEM_STATUS_PLAYED = 66;
	public static final int ITEM_STATUS_MAX_PLAYLIST_VIEW = 100;

	public static final Uri URI = Uri.parse("content://"
			+ PodcastProvider.AUTHORITY + "/items");

    public static final Uri PLAYLIST_URI = Uri.parse("content://"
            + PodcastProvider.AUTHORITY + "/playlist");

	public static final String TABLE_NAME = "item";

	// feed
	public static final String SUBS_ID = "subs_id";

	public static final String TITLE = "title";

	public static final String REMOTE_ID = "remote_id";

	public static final String AUTHOR = "author";

	public static final String DATE = "date";

	public static final String LAST_UPDATE = "last_update";

	public static final String CONTENT = "content";
	public static final String EPISODE_NUMBER = "episode_number";

	// download
	public static final String STATUS = "status";

	public static final String URL = "url";

	public static final String RESOURCE = "res";

	public static final String FILESIZE = "filesize";
	public static final String CHUNK_FILESIZE = "chunk_filesize";
	public static final String DOWNLOAD_REFERENCE = "download_reference";
	public static final String IS_DOWNLOADED = "is_download";
	public static final String DURATION = "duration";
	public static final String DURATION_MS = "duration_ms";
	public static final String IMAGE_URL = "image";

	public static final String LENGTH = "length";

	public static final String OFFSET = "offset";

	public static final String PATHNAME = "path";

	public static final String FAIL_COUNT = "fail";

	// play
	public static final String MEDIA_URI = "uri";

	public static final String SUB_TITLE = "sub_title";
	public static final String CREATED = "created";
	public static final String TYPE = "audio_type";

	public static final String LISTENED = "keep";
	public static final String PRIORITY = "priority";

	public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static final String[] ALL_COLUMNS = { _ID, SUBS_ID, TITLE, AUTHOR,
			DATE, LAST_UPDATE, CONTENT, STATUS, URL, RESOURCE, FILESIZE,
			CHUNK_FILESIZE, DURATION, LENGTH, OFFSET, PATHNAME, FAIL_COUNT,
			MEDIA_URI, SUB_TITLE, CREATED, TYPE, LISTENED, IMAGE_URL,
			DOWNLOAD_REFERENCE, EPISODE_NUMBER, IS_DOWNLOADED, DURATION_MS,
			PRIORITY, REMOTE_ID };

	public static final String DEFAULT_SORT_ORDER = CREATED + " DESC";

	public static final String sql_create_table = "CREATE TABLE " + TABLE_NAME
			+ " ("
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + REMOTE_ID + " VARCHAR(128), "
            + SUBS_ID + " INTEGER, "
            + TITLE + " VARCHAR(128), "
            + AUTHOR + " VARCHAR(128), "
            + DATE + " VARCHAR(64), "
            + LAST_UPDATE + " INTEGER, "
            + CONTENT + " TEXT, "
            + STATUS + " INTEGER, "
            + URL + " VARCHAR(1024) NOT NULL UNIQUE, "
			+ RESOURCE + " VARCHAR(1024), "
            + FILESIZE + " INTEGER, "
			+ CHUNK_FILESIZE + " INTEGER, "
            + DOWNLOAD_REFERENCE + " INTEGER, "
			+ IS_DOWNLOADED + " INTEGER, "
            + EPISODE_NUMBER + " INTEGER, "
			+ DURATION + " VARCHAR(16), "
            + IMAGE_URL + " VARCHAR(1024), "
			+ DURATION_MS + " INTEGER, "
            + LENGTH + " INTEGER, "
            + OFFSET + " INTEGER, "
            + PATHNAME + " VARCHAR(128), "
            + FAIL_COUNT + " INTEGER, "
            + MEDIA_URI + " VARCHAR(128), "
            + SUB_TITLE + " VARCHAR(128), "
            + TYPE + " VARCHAR(64), "
            + CREATED + " INTEGER, "
            + LISTENED + " INTEGER NOT NULL DEFAULT 0, "
			+ PRIORITY + " INTEGER NOT NULL DEFAULT 0" + ");";

	// To upgrade from database version 12 to version 13
	public static final String sql_upgrade_table_add_keep_column = "ALTER TABLE "
			+ TABLE_NAME
			+ " ADD COLUMN "
			+ LISTENED
			+ " INTEGER NOT NULL DEFAULT 0;";
	public static final String sql_populate_keep_from_status = "UPDATE "
			+ TABLE_NAME + " SET " + LISTENED + " = 1 " + " WHERE " + STATUS
			+ "=" + ITEM_STATUS_KEEP;

	public static final String sql_change_keep_status_to_played = "UPDATE "
			+ TABLE_NAME + " SET " + STATUS + "=" + ITEM_STATUS_PLAYED
			+ " WHERE " + STATUS + "=" + ITEM_STATUS_KEEP;

	public static final String sql_index_item_res = "CREATE INDEX IDX_"
			+ TABLE_NAME + "_" + URL + " ON " + TABLE_NAME + " ("
			+ URL + ");";

	public static final String sql_index_item_created = "CREATE INDEX IDX_"
			+ TABLE_NAME + "_" + LAST_UPDATE + " ON " + TABLE_NAME + " ("
			+ LAST_UPDATE + ");";

	public static ContentValues checkValues(ContentValues values, Uri uri) {
		if (values.containsKey(SUBS_ID) == false) {
			throw new SQLException(
					"Fail to insert row because SUBS_ID is needed " + uri);
		}

		if (values.containsKey(URL) == false) {
			values.put(URL, "");
		}

		if (values.containsKey(TITLE) == false) {
			values.put(TITLE, "unknow");
		}

		if (values.containsKey(REMOTE_ID) == false) {
			values.put(REMOTE_ID, "");
		}

		if (values.containsKey(AUTHOR) == false) {
			values.put(AUTHOR, "");
		}

		Long now = Long.valueOf(System.currentTimeMillis());

		if (values.containsKey(LAST_UPDATE) == false) {
			values.put(LAST_UPDATE, now);
		}

		if (values.containsKey(DATE) == false) {
			SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
			Date currentTime = new Date();
			values.put(DATE, formatter.format(currentTime));
		}

		if (values.containsKey(CONTENT) == false) {
			values.put(CONTENT, "");
		}

		if (values.containsKey(STATUS) == false) {
			values.put(STATUS, ITEM_STATUS_UNREAD);
		}

		if (values.containsKey(RESOURCE) == false) {
			throw new SQLException(
					"Fail to insert row because RESOURCE is needed " + uri);
		}

		if (values.containsKey(DURATION) == false) {
			values.put(DURATION, "");
		}

		if (values.containsKey(FILESIZE) == false) {
			values.put(FILESIZE, "");
		}

		if (values.containsKey(CHUNK_FILESIZE) == false) {
			values.put(CHUNK_FILESIZE, "");
		}

		if (values.containsKey(IMAGE_URL) == false) {
			values.put(IMAGE_URL, "");
		}

		if (values.containsKey(LENGTH) == false) {
			values.put(LENGTH, 0);
		}

		if (values.containsKey(OFFSET) == false) {
			values.put(OFFSET, 0);
		}

		if (values.containsKey(PATHNAME) == false) {
			values.put(PATHNAME, "");
		}

		if (values.containsKey(FAIL_COUNT) == false) {
			values.put(FAIL_COUNT, 0);
		}

		if (values.containsKey(MEDIA_URI) == false) {
			values.put(MEDIA_URI, "");
		}

		if (values.containsKey(SUB_TITLE) == false) {
			values.put(SUB_TITLE, "");
		}

		if (values.containsKey(CREATED) == false) {
			values.put(CREATED, now);
		}
		if (values.containsKey(TYPE) == false) {
			values.put(TYPE, "");
		}
		if (values.containsKey(LISTENED) == false) {
			values.put(LISTENED, 0);
		}

		if (values.containsKey(PRIORITY) == false) {
			values.put(PRIORITY, 0);
		}
		return values;
	}

}