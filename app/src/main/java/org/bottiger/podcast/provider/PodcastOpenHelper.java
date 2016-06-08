package org.bottiger.podcast.provider;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.flavors.Analytics.IAnalytics;
import org.bottiger.podcast.utils.PodcastLog;

import android.content.Context;
import android.database.Cursor;
import io.requery.android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import io.requery.android.database.sqlite.SQLiteOpenHelper;

public class PodcastOpenHelper extends SQLiteOpenHelper {

	private final PodcastLog log = PodcastLog.getLog(getClass());

	private final static int DBVERSION = 20;
	private final static String DBNAME = "podcast.db";

    private static PodcastOpenHelper mInstance = null;

	public PodcastOpenHelper(Context context) {
		super(context, DBNAME, null, DBVERSION);
	}

    public static synchronized PodcastOpenHelper getInstance(Context argContext) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new PodcastOpenHelper(argContext.getApplicationContext());
        }
        return mInstance;
    }

	public static synchronized Cursor runQuery(Context argContext, String argQuery) {
		SQLiteDatabase database = getInstance(argContext).getWritableDatabase();
		return database.rawQuery(argQuery, null);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL(SubscriptionColumns.sql_create_table);
		db.execSQL(SubscriptionColumns.sql_index_subs_url);
		db.execSQL(SubscriptionColumns.sql_index_last_update);

		db.execSQL(ItemColumns.sql_create_table);
		db.execSQL(ItemColumns.sql_index_item_res);
		db.execSQL(ItemColumns.sql_index_item_created);
		db.execSQL(ItemColumns.sql_index_item_subid);
		db.execSQL(ItemColumns.sql_index_item_status);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        IAnalytics analytics = SoundWaves.sAnalytics;
        if (analytics != null) {
            analytics.trackEvent(IAnalytics.EVENT_TYPE.DATABASE_UPGRADE);
        }

		// Add new stuff to the bottom

        // In order to fix the horrible sqli column does not exist bug we add
        // the colums to all versions from here
        // This should just fail silently if the columns have been created
        // properly in the past.
        if (newVersion == 18) {
            // Add new column
            String new_item_column1 = "ALTER TABLE " + SubscriptionColumns.TABLE_NAME
                    + " ADD COLUMN " + SubscriptionColumns.PRIMARY_COLOR + " INTEGER DEFAULT 0;";
            String new_item_column2 = "ALTER TABLE " + SubscriptionColumns.TABLE_NAME
                    + " ADD COLUMN " + SubscriptionColumns.PRIMARY_TINT_COLOR + " INTEGER DEFAULT 0;";
            String new_item_column3 = "ALTER TABLE " + SubscriptionColumns.TABLE_NAME
                    + " ADD COLUMN " + SubscriptionColumns.SECONDARY_COLOR + " INTEGER DEFAULT 0;";

            try {
                log.debug("Upgrading database to 187");
                db.execSQL(new_item_column1);
                db.execSQL(new_item_column2);
                db.execSQL(new_item_column3);
            } catch (SQLiteException sqle) {
                log.debug("Tables already existed");
            }
        }

        if (oldVersion == 16 && newVersion == 17) {
            // Add new column
            String new_item_column1 = "ALTER TABLE " + SubscriptionColumns.TABLE_NAME
                    + " ADD COLUMN " + SubscriptionColumns.PRIMARY_COLOR + " INTEGER DEFAULT 0;";
            String new_item_column2 = "ALTER TABLE " + SubscriptionColumns.TABLE_NAME
                    + " ADD COLUMN " + SubscriptionColumns.PRIMARY_TINT_COLOR + " INTEGER DEFAULT 0;";
            String new_item_column3 = "ALTER TABLE " + SubscriptionColumns.TABLE_NAME
                    + " ADD COLUMN " + SubscriptionColumns.SECONDARY_COLOR + " INTEGER DEFAULT 0;";

            log.debug("Upgrading database from version 16 to 17");
            db.execSQL(new_item_column1);
            db.execSQL(new_item_column2);
            db.execSQL(new_item_column3);
        }
		if (oldVersion == 15 && newVersion == 16) {
			// Add new column
			String new_item_column = "ALTER TABLE " + ItemColumns.TABLE_NAME
					+ " ADD COLUMN " + ItemColumns.REMOTE_ID + " VARCHAR(128);";

			log.debug("Upgrading database from version 15 to 16");
			db.execSQL(new_item_column);
		}
		if (oldVersion == 14 && newVersion == 15) {
			// http://stackoverflow.com/questions/8442147/how-to-delete-or-add-column-in-sqlite

			String new_name = SubscriptionColumns.TABLE_NAME + "_new";

			// Create new table
			// http://stackoverflow.com/questions/2361921/select-into-statement-in-sqlite
			String sql_create_table = "CREATE TABLE " + new_name + " ("
					+ SubscriptionColumns._ID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ SubscriptionColumns.URL + " VARCHAR(1024), "
					+ SubscriptionColumns.LINK + " VARCHAR(256), "
					+ SubscriptionColumns.TITLE + " VARCHAR(128), "
					+ SubscriptionColumns.DESCRIPTION + " TEXT, "
					+ SubscriptionColumns.LAST_UPDATED + " INTEGER, "
					+ SubscriptionColumns.LAST_ITEM_UPDATED + " INTEGER, "
					+ SubscriptionColumns.FAIL_COUNT + " INTEGER, "
					+ SubscriptionColumns.STATUS + " INTEGER, "
					+ SubscriptionColumns.COMMENT + " TEXT, "
					+ SubscriptionColumns.RATING + " INTEGER, "
					+ SubscriptionColumns.SERVER_ID + " INTEGER, "
					+ SubscriptionColumns.REMOTE_ID + " VARCHAR(128), "
					+ SubscriptionColumns.AUTO_DOWNLOAD + " INTEGER , "
					+ SubscriptionColumns.PLAYLIST_POSITION + " INTEGER , "
					+ SubscriptionColumns.IMAGE_URL + " VARCHAR(1024), "

                    + SubscriptionColumns.PRIMARY_COLOR + " INTEGER DEFAULT 0 , "
                    + SubscriptionColumns.PRIMARY_TINT_COLOR + " INTEGER DEFAULT 0 , "
                    + SubscriptionColumns.SECONDARY_COLOR + " INTEGER DEFAULT 0 "

                    + ");";

			// http://www.sqlite.org/faq.html#q11
			// INSERT INTO t1_backup SELECT a,b FROM t1;
			String insert_into = "INSERT INTO " + new_name + " SELECT "
					+ SubscriptionColumns._ID + ", " + SubscriptionColumns.URL
					+ ", " + SubscriptionColumns.LINK + ", "
					+ SubscriptionColumns.TITLE + ", "
					+ SubscriptionColumns.DESCRIPTION + ", "
					+ SubscriptionColumns.LAST_UPDATED + ", "
					+ SubscriptionColumns.LAST_ITEM_UPDATED + ", "
					+ SubscriptionColumns.FAIL_COUNT + ", "
					+ SubscriptionColumns.STATUS + ", "
					+ SubscriptionColumns.COMMENT + ", "
					+ SubscriptionColumns.RATING + ", "
					+ SubscriptionColumns.SERVER_ID + ", " + "\"\", "
					+ SubscriptionColumns.AUTO_DOWNLOAD + ", "
					+ SubscriptionColumns.PLAYLIST_POSITION + ", "
					+ SubscriptionColumns.IMAGE_URL + ", "
                    + SubscriptionColumns.PRIMARY_COLOR + ", "
                    + SubscriptionColumns.PRIMARY_TINT_COLOR + ", "
                    + SubscriptionColumns.SECONDARY_COLOR + " "
                    + " FROM subs;";

			// Add new column
			String new_column = "ALTER TABLE " + SubscriptionColumns.TABLE_NAME
					+ "_new ADD " + SubscriptionColumns.REMOTE_ID
					+ " VARCHAR(128)";

			// Drop old table
			// http://stackoverflow.com/questions/3675032/drop-existing-table-in-sqlite-when-if-exists-operator-is-not-supported
			String drop_old_table = "DROP TABLE IF EXISTS subs;";

			// rename new table
			// http://stackoverflow.com/questions/426495/how-do-you-rename-a-table-in-sqlite-3-0
			String rename_new_table = "ALTER TABLE " + new_name + " RENAME TO "
					+ SubscriptionColumns.TABLE_NAME + ";";

			log.debug("Upgrading database from version 14 to 15");
			// db.execSQL("BEGIN TRANSACTION;");
			db.execSQL(sql_create_table);
			db.execSQL(insert_into);
			db.execSQL(drop_old_table);
			db.execSQL(rename_new_table);
			// db.execSQL("COMMIT;");
		}
		if (oldVersion == 13 && newVersion == 14) {
			log.debug("Upgrading database from version 13 to 14");
			db.execSQL("CREATE INDEX idx_date ON " + ItemColumns.TABLE_NAME
					+ " (" + ItemColumns.DATE + ")");
			db.execSQL("CREATE INDEX idx_priority ON " + ItemColumns.TABLE_NAME
					+ " (" + ItemColumns.PRIORITY + ")");
		}
		if (oldVersion == 12 && newVersion == 13) {
			log.debug("Upgrading database from version 12 to 13");
			// Add the KEEP column to the items table,
			// use that rather than the old KEEP status
			log.debug("executing sql: "
					+ ItemColumns.sql_upgrade_table_add_keep_column);
			db.execSQL(ItemColumns.sql_upgrade_table_add_keep_column);
			log.debug("executing sql: "
					+ ItemColumns.sql_populate_keep_from_status);
			db.execSQL(ItemColumns.sql_populate_keep_from_status);
			log.debug("executing sql: "
					+ ItemColumns.sql_change_keep_status_to_played);
			db.execSQL(ItemColumns.sql_change_keep_status_to_played);
			log.debug("Done upgrading database");
		}

        /*
        else if (oldVersion != newVersion) {
			// drop db
			db.execSQL("DROP TABLE " + ItemColumns.TABLE_NAME);
			db.execSQL("DROP TABLE " + SubscriptionColumns.TABLE_NAME);
			onCreate(db);
		}*/


		if (oldVersion < 19) {
			String new_settings_column = "ALTER TABLE " + SubscriptionColumns.TABLE_NAME
					+ " ADD COLUMN " + SubscriptionColumns.SETTINGS + " INTEGER DEFAULT -1;";

			log.debug("Upgrading database to version 19");
			db.execSQL(new_settings_column);
		}

		if (oldVersion < 20) {
			String new_settings_column = "ALTER TABLE " + ItemColumns.TABLE_NAME
					+ " ADD COLUMN " + ItemColumns.PUB_DATE + " INTEGER NOT NULL DEFAULT -1;";

			log.debug("Upgrading database to version 20");
			db.execSQL(new_settings_column);
			db.execSQL(ItemColumns.sql_index_item_subid);
			db.execSQL(ItemColumns.sql_index_item_status);
		}
	}
}
