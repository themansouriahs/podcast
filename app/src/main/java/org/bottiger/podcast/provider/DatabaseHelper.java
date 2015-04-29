package org.bottiger.podcast.provider;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v4.widget.CursorAdapter;

public class DatabaseHelper {

	private ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

	public static final String ACTION_TRANSACTION = "transaction";
	public static final String ACTION_UPDATE = "update";

	/**
	 * Add a operation to execute
	 * 
	 * @return
	 */
	public void addOperation(ContentProviderOperation op) {
		ops.add(op);
	}

	/**
	 * Commit batch update
	 */
	public void commit(ContentResolver contentResolver) {
		try {
			contentResolver.applyBatch(PodcastProvider.AUTHORITY, ops);
		} catch (RemoteException e) {
			// do s.th.
		} catch (OperationApplicationException e) {
			// do s.th.
		}
		ops.clear();
	}

	/**
	 * Executes a SQL query
	 */
	public void executeSQL(Context context, String sql) {
		SQLExecuter(context, sql, null, null);
	}

	/**
	 * Executes a SQL query. Notifies the adapter afterwards
	 */
	public void executeSQL(Context context, String sql, CursorAdapter adapter) {
		SQLExecuter(context, sql, adapter, null);
	}

	/**
	 * Executes a SQL query. Notifies the adapter afterwards
	 */
	public void executeFastUpdateSQL(Context context, String sql,
			CursorAdapter adapter) {
		SQLExecuter(context, sql, adapter, ACTION_UPDATE);
	}

	/**
	 * AsyncTask for executing SQL queries in the background.
	 * 
	 * @author Arvid Böttiger
	 * 
	 */
	private void SQLExecuter(final Context context, final String sql,
			final CursorAdapter adapter, final String action) {

		// new Thread(new Runnable() {
		// public void run() {

		boolean isSuccesful = false;
		String performAction = "";

		if (action == null || action.equals(""))
			performAction = ACTION_TRANSACTION;

		PodcastOpenHelper helper = PodcastOpenHelper.getInstance(context);//new PodcastOpenHelper(context);

		if (performAction.equals(ACTION_UPDATE)) {

			int numOfRows = 0;

			final SQLiteStatement stmt = helper.getWritableDatabase()
					.compileStatement(sql);
			try {
				numOfRows = stmt.executeUpdateDelete();
			} finally {
				stmt.close();
			}

			if (numOfRows > 0)
				isSuccesful = true;

		} else {
			SQLiteDatabase db = helper.getWritableDatabase();
			db.beginTransaction();
			try {
				db.execSQL(sql);
				db.setTransactionSuccessful();
				isSuccesful = true;
			} finally {
				db.endTransaction();
			}
		}

		if (adapter != null)
			adapter.notifyDataSetChanged();

		// }
		// }).start();
	}

	public static void executeStatment(SQLiteStatement statment,
			Object[] columnValues, String whereURL) {
		// String[] allArgs = ObjectArrays.concat(columnValues, new
		// String[]{whereURL}, String.class);
		int i = 1;
		for (Object value : columnValues) {
			if (value instanceof String) {
				String stringValue = (String)value;
				statment.bindString(i, stringValue);
			} else {
				Number numValue = (Number)value;
				Long longValue = (long)0;
				if (numValue != null) {
					longValue = numValue.longValue();
				}
				statment.bindLong(i, longValue);
			}
			i++;

		}

		// statment.bindAllArgsAsStrings(allArgs);
		statment.execute();
		statment.clearBindings();
	}

	public static SQLiteStatement prepareFeedUpdateQuery(Context context,
			String[] columnNames) {
		PodcastOpenHelper helper = PodcastOpenHelper.getInstance(context);//new PodcastOpenHelper(context);
		SQLiteDatabase database = helper.getWritableDatabase();
		SQLiteStatement statment = buildFeedUpdateQuery(database, columnNames);
		return statment;
	}

	public static SQLiteStatement buildFeedUpdateQuery(SQLiteDatabase database,
			String[] columnNames) {
		StringBuilder builder = new StringBuilder();
		builder.append("update " + ItemColumns.TABLE_NAME + " set ");
		
		int i = 0;
		int arrayLength = columnNames.length;
		for (String name : columnNames) {
			i++;
			builder.append(name + "=?");
			if (i != arrayLength)
				builder.append(", ");
		}
		builder.append(" where url=?");

		String sql = builder.toString();
		SQLiteStatement stmt = database.compileStatement(sql);

		return stmt;
	}

}
