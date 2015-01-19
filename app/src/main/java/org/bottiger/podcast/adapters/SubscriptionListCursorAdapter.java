package org.bottiger.podcast.adapters;

import org.bottiger.podcast.R;
import org.bottiger.podcast.provider.Subscription;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class SubscriptionListCursorAdapter extends AbstractPodcastAdapter {

	public SubscriptionListCursorAdapter(
			Context context,
			int listItem,
			Cursor cursor,
			String[] strings,
			int[] is) {
		super(context, listItem, cursor, strings, is);
		
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public View getView(int position, View convertView, ViewGroup parent) {

        /*
		View listViewItem;
		Cursor subscriptionCursor = (Cursor) getItem(position);

		if (!subscriptionCursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}

		if (convertView == null) {
			listViewItem = newView(mContext, subscriptionCursor, parent);
		} else {
			listViewItem = convertView;
		}

		bindView(listViewItem, mContext, subscriptionCursor);

		return listViewItem;*/
        return null;
	}
	
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View view = mInflater.inflate(R.layout.subscription_list_item, null);

		view.setTag(R.id.list_image, view.findViewById(R.id.list_image));
		view.setTag(R.id.title, view.findViewById(R.id.title));
		view.setTag(R.id.podcast, view.findViewById(R.id.podcast));
		return view;
	}
	
	public void bindView(View view, Context context, Cursor cursor) {

		Subscription sub = null;
		try {
			sub = Subscription.getByCursor(cursor);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

		/*
		 * http://drasticp.blogspot.dk/2012/04/viewholder-is-dead.html
		 */
		ImageView icon = (ImageView) view.getTag(R.id.list_image);
		TextView mainTitle = (TextView) view.getTag(R.id.title);
		TextView subTitle = (TextView) view.getTag(R.id.podcast);
		
		if (sub != null) {


			if (sub.title != null)
				mainTitle.setText(sub.title);

		}
	}

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
