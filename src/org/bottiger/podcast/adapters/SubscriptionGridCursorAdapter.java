package org.bottiger.podcast.adapters;

import org.bottiger.podcast.R;
import org.bottiger.podcast.adapters.PodcastAdapterInterface.FieldHandler;
import org.bottiger.podcast.images.ImageCacheManager;
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

import com.android.volley.toolbox.NetworkImageView;
import com.nostra13.universalimageloader.core.ImageLoader;


public class SubscriptionGridCursorAdapter extends AbstractGridPodcastAdapter {
	
	static class SubscriptionViewHolder {
		NetworkImageView image;
		TextView title;
	}

	
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
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View view = mInflater.inflate(R.layout.subscription_grid_item, null);
		
		SubscriptionViewHolder holder = new SubscriptionViewHolder();
		holder.image = (NetworkImageView) view.findViewById(R.id.grid_image);
		holder.title = (TextView) view.findViewById(R.id.grid_title);
		view.setTag(holder);

		return view;
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		
		SubscriptionViewHolder holder = (SubscriptionViewHolder)view.getTag();

		Subscription sub = null;
		try {
			sub = Subscription.getByCursor(cursor);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		
		if (sub != null) {


			if (sub.title != null)
				holder.title.setText(sub.title);


			if (sub.imageURL != null && !sub.imageURL.equals("")) {
				//ImageLoader imageLoader = getImageLoader(context);
				//imageLoader.displayImage(sub.imageURL, holder.image);
				
				com.android.volley.toolbox.ImageLoader imageLoader = ImageCacheManager
						.getInstance().getImageLoader();
				holder.image.setImageUrl(sub.imageURL, imageLoader);
			}

		}
	}


}
