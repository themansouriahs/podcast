package org.bottiger.podcast.adapters;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.R;
import org.bottiger.podcast.images.ImageCacheManager;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.views.SquareImageView;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.squareup.picasso.Picasso;


public class SubscriptionGridCursorAdapter extends AbstractGridPodcastAdapter {
	
	static class SubscriptionViewHolder {
		SquareImageView image;
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
		holder.image = (SquareImageView) view.findViewById(R.id.grid_image);
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
			
			String title = sub.title;
			String logo = sub.imageURL;


			if (title != null && !title.equals(""))
				holder.title.setText(title);
			else
				holder.title.setText(R.string.subscription_no_title);

			if (logo != null && !logo.equals("")) {
				//ImageLoader imageLoader = getImageLoader(context);
				//imageLoader.displayImage(sub.imageURL, holder.image);
				
				if (ApplicationConfiguration.USE_PICASSO) {
					Picasso.with(mContext).load(logo)
							.placeholder(R.drawable.generic_podcast)
							.into(holder.image);
				} else {
					/*
				com.android.volley.toolbox.ImageLoader imageLoader = ImageCacheManager
						.getInstance().getImageLoader();
				((NetworkImageView)holder.image).setImageUrl(logo, imageLoader);
				*/
				}
			} else {
				holder.image.setImageResource(R.drawable.generic_podcast);
			}

		}
	}


}
