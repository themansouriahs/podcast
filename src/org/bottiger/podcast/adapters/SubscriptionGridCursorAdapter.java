package org.bottiger.podcast.adapters;

import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.PodcastAdapterInterface.FieldHandler;
import org.bottiger.podcast.provider.Subscription;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;


public class SubscriptionGridCursorAdapter extends AbstractGridPodcastAdapter {

	
    public SubscriptionGridCursorAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c);
		// TODO Auto-generated constructor stub
	}

	public SubscriptionGridCursorAdapter(Context context,
			int subscriptionGridItem, Cursor cursor, String[] strings,
			int[] is, FieldHandler[] fields) {
		super(context, subscriptionGridItem, cursor);
		mContext = context;
	}

	/**
     * Called by bindView() to set the text for a TextView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to a TextView.
     *
     * Intended to be overridden by Adapters that need to filter strings
     * retrieved from the database.
     * 
     * @param v TextView to receive text
     * @param text the text to be set for the TextView
     */    
    public void setViewText(TextView v, String text) {
        v.setText(text);
    }
    
    /**
     * Called by bindView() to set the image for an ImageView but only if
     * there is no existing ViewBinder or if the existing ViewBinder cannot
     * handle binding to an ImageView.
     *
     * By default, the value will be treated as an image resource. If the
     * value cannot be used as an image resource, the value is used as an
     * image Uri.
     *
     * Intended to be overridden by Adapters that need to filter strings
     * retrieved from the database.
     *
     * @param v ImageView to receive an image
     * @param value the value retrieved from the cursor
     */
    public void setViewImage(ImageView v, String value) {
        try {
            v.setImageResource(Integer.parseInt(value));
        } catch (NumberFormatException nfe) {
            v.setImageURI(Uri.parse(value));
        }
    }
    
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View gridViewItem;
		Cursor subscriptionCursor = (Cursor) getItem(position);

		if (!subscriptionCursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}

		if (convertView == null) {
			gridViewItem = newView(mContext, subscriptionCursor, parent);
		} else {
			gridViewItem = convertView;
		}

		bindView(gridViewItem, mContext, subscriptionCursor);

		return gridViewItem;
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View view = mInflater.inflate(R.layout.subscription_grid_item, null);

		view.setTag(R.id.grid_image, view.findViewById(R.id.grid_image));
		view.setTag(R.id.grid_title, view.findViewById(R.id.grid_title));
		view.setTag(R.id.podcast, view.findViewById(R.id.podcast));
		return view;
	}
	
	@Override
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
		ImageView icon = (ImageView) view.getTag(R.id.grid_image);
		TextView gridTitle = (TextView) view.getTag(R.id.grid_title);
		//TextView subTitle = (TextView) view.getTag(R.id.podcast);
		
		if (sub != null) {


			if (sub.title != null)
				gridTitle.setText(sub.title);


			if (sub.imageURL != null && !sub.imageURL.equals("")) {
				ImageLoader imageLoader = getImageLoader(context);
				imageLoader.displayImage(sub.imageURL, icon);
			}

		}
	}


}
