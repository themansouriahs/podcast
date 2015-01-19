package org.bottiger.podcast.playlist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.DatabaseHelper;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.ItemColumns;
import org.bottiger.podcast.provider.PodcastOpenHelper;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.provider.SubscriptionColumns;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.soundwaves.Soundwaves;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;

public class Playlist {

	private static int MAX_SIZE = 20;
    private static Playlist activePlaylist = null;

	private Context mContext;

	private ArrayList<FeedItem> mPlaylist = new ArrayList<FeedItem>();
	private SharedPreferences sharedPreferences;

	// Shared setting key/values
	private String showListenedKey = ApplicationConfiguration.showListenedKey;
	private Boolean showListenedVal = true;
	private String inputOrderKey = "inputOrder";
	private String defaultOrder = "DESC";
	private String amountKey = "amountOfEpisodes";
	private int amountValue = 20;

	// http://stackoverflow.com/questions/1036754/difference-between-wait-and-sleep
	private static Boolean lock = true;

    private static HashSet<PlaylistChangeListener> mPlaylistChangeListeners = new HashSet<PlaylistChangeListener>();

	public Playlist(Context context, int length) {
		this(context, length, false);
	}

	public Playlist(Context context, int length, boolean isLocked) {
			sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			this.mContext = context;

        if (activePlaylist == null) {
            activePlaylist = this;
        }
    }

	public Playlist(Context context) {
		this(context, MAX_SIZE);
	}

	/**
	 * @return The playlist as a list of episodes
	 */
	public ArrayList<FeedItem> getPlaylist() {
		return mPlaylist;
	}

	/**
	 * 
	 * @return the size of the playlist
	 */
	public int size() {
		return mPlaylist.size();
	}

	/**
	 * 
	 * @param position
	 *            in the playlist (0-indexed)
	 * @return The episode at the given position
	 */
	public FeedItem getItem(int position) {
		return mPlaylist.get(position);
	}

	/**
	 * 
	 * @param episode
	 * @return The position of the episode
	 */
	public int getPosition(FeedItem episode) {
		return mPlaylist.indexOf(episode);
	}

	/**
	 * 
	 * @param position
	 * @param item
	 */
	public void setItem(int position, FeedItem item) {
		int size = mPlaylist.size();
		if (size > position) {
            mPlaylist.add(position, item);
        } else if (size == position) {
			mPlaylist.add(item);
		}
	}

	/**
	 * 
	 * @param cursor
	 */
	public void setItem(Cursor cursor) {
		int position = cursor.getPosition();
		if (position < MAX_SIZE) {
			setItem(position, FeedItem.getByCursor(cursor));
		}
	}

    /**
     * When new items are fetched from a remote destination we can use this method
     * to notify the playlist about them instead of passing them back and forth in the database
     */
    public void notifyAbout(@NonNull FeedItem argEpisode) {

        // TODO: Expand this with filters
        int counter = 0;
        boolean isAfter;

        for (FeedItem episode : mPlaylist) {
            isAfter = argEpisode.getDateTime().after(episode.getDateTime());
            if (isAfter) {
                final int size = mPlaylist.size();

                if (size == MAX_SIZE) {
                    mPlaylist.remove(mPlaylist.size() - 1);
                }

                mPlaylist.add(counter, argEpisode);

                notifyPlaylistRangeChanged(counter, size - 1);
                return;
            }
            counter++;
        }
    }

	/**
	 * @return The next item in the playlist
	 */
	public FeedItem nextEpisode() {
		if (mPlaylist.size() > 1) {
            return mPlaylist.get(1);
        }
		return null;
	}

	/**
	 * 
	 * @param from
	 *            , old position in the playlist
	 * @param to
	 *            , the new position in the playlist
	 */
	public void move(int from, int to) {
        populatePlaylistIfEmpty();

        FeedItem fromItem = mPlaylist.get(from);
        mPlaylist.remove(from);
        mPlaylist.add(to,fromItem);

        FeedItem precedingItem = to == 0 ? null : mPlaylist.get(to-1);
        FeedItem movedItem = mPlaylist.get(from);
        persist(mContext, movedItem, precedingItem, from, to);
	}

	/**
	 * 
	 *
	 *            of episodes
	 * @return A SQL formatted string of the order
	 */
	public String getOrder() {

		String inputOrder = sharedPreferences.getString(inputOrderKey,
				defaultOrder);
		int amount = sharedPreferences.getInt(amountKey, amountValue);

		PlayerService playerService = PodcastBaseFragment.mPlayerServiceBinder;

		String playingFirst = "";
		if (playerService != null && playerService.getCurrentItem() != null) {
			playingFirst = "case " + ItemColumns._ID + " when "
					+ playerService.getCurrentItem().getId()
					+ " then 1 else 2 end, ";
		}
		String prioritiesSecond = "case " + ItemColumns.PRIORITY
				+ " when 0 then 2 else 1 end, " + ItemColumns.PRIORITY + ", ";
		String order = playingFirst + prioritiesSecond + ItemColumns.DATE + " "
				+ inputOrder + " LIMIT " + amount; // before:
		return order;
	}

	/**
	 * 
	 * @return A SQL formatted string of the where clause
	 */
	public String getWhere() {
		Boolean showListened = sharedPreferences.getBoolean(showListenedKey,
				showListenedVal);
		String where = (showListened) ? "1" : ItemColumns.LISTENED + "== 0";
        where += " AND ";
        where += ItemColumns.SUBS_ID + " IN (SELECT " + SubscriptionColumns._ID + " FROM "  +
                SubscriptionColumns.TABLE_NAME + " WHERE " + SubscriptionColumns.STATUS + "<>"
                + Subscription.STATUS_UNSUBSCRIBED + ")";


		return where;
	}

	/**
	 * 
	 */
	public void resetPlaylist(CursorAdapter adapter) {
		// Update the database
		String currentTime = String.valueOf(System.currentTimeMillis());
		String updateLastUpdate = ", " + ItemColumns.LAST_UPDATE + "="
				+ currentTime + " ";

		// We remove the playlist position for all items in the playlist.
		String action = "UPDATE " + ItemColumns.TABLE_NAME + " SET ";
		String value = ItemColumns.PRIORITY + "=0" + updateLastUpdate;
		String where = "WHERE " + ItemColumns.PRIORITY + "<> 0";

		// Also update the timestamp of the top item in order to indicate to the
		// drivesyncer
		// Our data is up tp date.
		String where2 = " OR " + ItemColumns._ID + "==(select "
				+ ItemColumns._ID + " from " + ItemColumns.TABLE_NAME
				+ " order by " + ItemColumns.DATE + " desc limit 1)";

		String sql = action + value + where + where2;

		DatabaseHelper dbHelper = new DatabaseHelper();
		dbHelper.executeSQL(mContext, sql, adapter);
	}

	/**
	 * Populates the playlist up to a certain length if the playlist is empty
	 */
	public boolean populatePlaylistIfEmpty() {
		if (mPlaylist.isEmpty()) {
            populatePlaylist(MAX_SIZE);
            return true;
        }
        return false;
	}

	/**
	 * Populates the playlist up to a certain length
	 * 
	 * @param length
	 *            of the playlist
	 */
	public void populatePlaylist(int length) {
		if (mPlaylist.size() >= length) {
            return;
        }

		PodcastOpenHelper helper = new PodcastOpenHelper(mContext);
		SQLiteDatabase database = helper.getWritableDatabase();
		Cursor cursor = database.query(ItemColumns.TABLE_NAME,
				ItemColumns.ALL_COLUMNS, getWhere(), null, null, null,
				getOrder());

		mPlaylist.clear();
		cursor.moveToPosition(-1);

        while (cursor.moveToNext()) {
			setItem(cursor);
		}
	}

    /**
     * Write changes to the playlist back to the database.
     * @param context
     * @param from
     * @param to
     */
    public static void persist(final Context context,
                        final FeedItem movedItem, final FeedItem precedingItem, final int from, final int to) {
        new Thread(new Runnable() {
            public void run() {

                if (from != to) {
                    movedItem.setPriority(precedingItem, context);
                }
            }
        }).start();
    }

    public boolean contains(FeedItem argItem) {
        return mPlaylist.contains(argItem);
    }

    public boolean isEmpty() {
        return mPlaylist.isEmpty();
    }

    public FeedItem first() {
        if (mPlaylist.size() <= 0) {
            throw new IllegalStateException("Playlist is empty");
        }
        return mPlaylist.get(0);
    }

    public static Playlist getActivePlaylist() {
        if (activePlaylist == null) {
            throw new IllegalStateException("No Active Playlist");
        }
        return activePlaylist;
    }

    public static void setActivePlaylist(@NonNull Playlist argPlaylist) {
        if (argPlaylist != activePlaylist) {
            activePlaylist = argPlaylist;
            activePlaylist.notifyPlaylistChanged();
        } else {
            throw new IllegalStateException("New playlist is the same");
        }
    }

    public interface PlaylistChangeListener {
        public void notifyPlaylistChanged();
        public void notifyPlaylistRangeChanged(int from, int to);
    }

    public void registerPlaylistChangeListener(@NonNull PlaylistChangeListener argChangeListener) {
        mPlaylistChangeListeners.add(argChangeListener);
    }

    public void unregisterPlaylistChangeListener(@NonNull PlaylistChangeListener argChangeListener) {
        mPlaylistChangeListeners.remove(argChangeListener);
    }

    public void notifyPlaylistChanged() {
        for (PlaylistChangeListener listener : mPlaylistChangeListeners) {
            if (listener == null) {
                throw new IllegalStateException("Listener can ot be null");
            }
            listener.notifyPlaylistChanged();
        }
    }

    public void notifyPlaylistRangeChanged(final int argFrom, final int argTo) {
        for (PlaylistChangeListener listener : mPlaylistChangeListeners) {
            if (listener == null) {
                throw new IllegalStateException("Listener can ot be null");
            }
            //listener.notifyPlaylistRangeChanged(argFrom, argTo);

            final PlaylistChangeListener finalListener = listener;
            if (mContext instanceof Activity) {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finalListener.notifyPlaylistRangeChanged(argFrom, argTo);
                    }
                });
            }
        }
    }
}
