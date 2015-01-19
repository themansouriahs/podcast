package org.bottiger.podcast.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import org.bottiger.podcast.MainActivity;
import org.bottiger.podcast.PodcastBaseFragment;
import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.FeedItem;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.service.PlayerService;
import org.bottiger.podcast.utils.ControlButtons;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class CompactListCursorAdapter extends AbstractEpisodeCursorAdapter {

	public static final int ICON_DEFAULT_ID = -1;

	protected int[] mFrom2;
	protected int[] mTo2;

	private static final int TYPE_COLLAPS = 0;
	private static final int TYPE_EXPAND = 1;
	private static final int TYPE_MAX_COUNT = 2;

	private ArrayList<FeedItem> mData = new ArrayList<FeedItem>();
	private PodcastBaseFragment mFragment = null;

	private TreeSet<Number> mExpandedItemID = new TreeSet<Number>();

    public CompactListCursorAdapter(Cursor c) {
        super(c);
    }

	public View getView(int position, View convertView, ViewGroup parent) {

        /*
		View listViewItem;
		Cursor itemCursor = (Cursor) getItem(position);

		if (!itemCursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}

		if (convertView == null) {
			listViewItem = newView(mContext, itemCursor, parent);
		} else {
			listViewItem = convertView;
		}

		bindView(listViewItem, mContext, itemCursor);
		return listViewItem;*/
        return null;
	}

	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View listView = mInflater.inflate(R.layout.episode_list_compact, null);

		listView.setTag(R.id.title, listView.findViewById(R.id.title));
		listView.setTag(R.id.play_episode, listView.findViewById(R.id.play_episode));
		listView.setTag(R.id.queue_episode, listView.findViewById(R.id.queue_episode));
		return listView;
	}

	public void bindView(View view, Context context, Cursor cursor) {

		FeedItem item = null;
		try {
			item = FeedItem.getByCursor(cursor);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

		Subscription sub = null;
		try {
			sub = Subscription.getByCursor(cursor);
		} catch (IllegalStateException e) {
		}

		TextView mainTitle = (TextView) view.getTag(R.id.title);
		mainTitle.setText(item.title);
		
		final ToggleButton playToggleButton = (ToggleButton) view.getTag(R.id.play_episode);
		final ImageButton queueButton = (ImageButton) view.getTag(R.id.queue_episode);

		if (PodcastBaseFragment.mPlayerServiceBinder != null) {
			PlayerService ps = PodcastBaseFragment.mPlayerServiceBinder;
			if (ps.isPlaying() && item.getId() == ps.getCurrentItem().getId()) {
				playToggleButton.setBackgroundResource(R.drawable.av_pause_dark);
			}
		}
		
		final FeedItem item2 = item;
		final Context context2 = context;
		final Activity activity2 = mActivity;

		playToggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	ControlButtons.playPauseToggle(item2, null, playToggleButton); //FIXME
            }
		});
		
		queueButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	item2.queue(context2);
            	((MainActivity) activity2).onRefreshPlaylist();
            }
		});
	}

	public int getViewTypeCount() {
		return TYPE_MAX_COUNT;
	}

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

    }

    @Override
	public int getItemViewType(int position) {
		return mExpandedItemID.contains(itemID(position)) ? TYPE_EXPAND
				: TYPE_COLLAPS;
	}

    @Override
    public int getItemCount() {
        return 0;
    }

    /**
	 * Returns the ID of the item at the position
	 * 
	 * @param position
	 * @return ID of the FeedItem
	 */
	private Long itemID(int position) {
        return 0L;
		/*
        Object item = getItem(position);
		if (item instanceof FeedItem) {
			FeedItem feedItem = (FeedItem) item;
			return Long.valueOf(feedItem.id);
		} else
			return new Long(1); // FIXME*/
	}

}
