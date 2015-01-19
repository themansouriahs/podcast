package org.bottiger.podcast.adapters;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.R;
import org.bottiger.podcast.images.PicassoWrapper;
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

    private OnPopulateSubscriptionList mObersever;

	static class SubscriptionViewHolder {
		SquareImageView image;
		TextView title;
	}

	public SubscriptionGridCursorAdapter(Context context,
			int subscriptionGridItem, OnPopulateSubscriptionList observer, Cursor cursor) {
		super(context, subscriptionGridItem, cursor);
		mContext = context;
        mObersever = observer;
	}

    public void notifyChange(Cursor cursor) {
        /*if (mObersever != null) {
            mObersever.listPopulated(cursor);
        }*/
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
				//PicassoWrapper.load(mContext, logo, holder.image);
                PicassoWrapper.simpleLoad(mContext, logo, holder.image);
			} else {
				holder.image.setImageResource(R.drawable.generic_podcast);
			}

		}

        if (mObersever != null)
            mObersever.listPopulated(cursor);
	}

    public interface OnPopulateSubscriptionList {
        public void listPopulated(Cursor cursor);
    }


}
