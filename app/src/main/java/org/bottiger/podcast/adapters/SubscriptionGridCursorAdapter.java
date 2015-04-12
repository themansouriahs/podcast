package org.bottiger.podcast.adapters;

import org.bottiger.podcast.ApplicationConfiguration;
import org.bottiger.podcast.R;
import org.bottiger.podcast.images.PicassoWrapper;
import org.bottiger.podcast.listeners.PaletteObservable;
import org.bottiger.podcast.provider.Subscription;
import org.bottiger.podcast.utils.ColorExtractor;
import org.bottiger.podcast.utils.PaletteCache;
import org.bottiger.podcast.views.SquareImageView;
import org.bottiger.podcast.views.TintedFramelayout;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;


public class SubscriptionGridCursorAdapter extends AbstractGridPodcastAdapter {

    private OnPopulateSubscriptionList mObersever;
    private int mItemLayout;

	static class SubscriptionViewHolder {
		SquareImageView image;
		TextView title;
        View gradient;
	}

	public SubscriptionGridCursorAdapter(Context context,
			int subscriptionGridItem, OnPopulateSubscriptionList observer, Cursor cursor, int argItemLayout) {
		super(context, subscriptionGridItem, cursor);
		mContext = context;
        mObersever = observer;
        mItemLayout = argItemLayout;
	}

    public void notifyChange(Cursor cursor) {
    }

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View view = mInflater.inflate(mItemLayout, null);
		
		SubscriptionViewHolder holder = new SubscriptionViewHolder();
		holder.image = (SquareImageView) view.findViewById(R.id.grid_image);
		holder.title = (TextView) view.findViewById(R.id.grid_title);
        holder.gradient = view.findViewById(R.id.grid_item_gradient);
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
		
		if (sub == null) {
            return;
        }
			
        String title = sub.title;
        final String logo = sub.imageURL;

        if (isListView()) {
            holder.gradient.setVisibility(View.GONE);

            boolean missingColor = sub.getPrimaryColor() == -1;

            if (missingColor) {

                TintedFramelayout tview = (TintedFramelayout) view;
                tview.unsetSubscription();
                tview.setSubscription(sub);

                Palette palette = PaletteCache.get(logo);
                if (palette != null) {
                    PaletteObservable.updatePalette(logo, palette);
                } else {
                    Target mTarget = new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                            PaletteCache.generate(logo, bitmap);
                        }

                        @Override
                        public void onBitmapFailed(Drawable errorDrawable) {

                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {

                        }
                    };


                    PicassoWrapper.simpleLoad(mContext, logo, mTarget);
                }
            } else {
                view.setBackgroundColor(sub.getPrimaryColor());
            }

        } else {
            holder.gradient.setVisibility(View.VISIBLE);
        }


        if (title != null && !title.equals(""))
            holder.title.setText(title);
        else
            holder.title.setText(R.string.subscription_no_title);

        if (holder.image != null) {
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

    private boolean isListView() {
        return mItemLayout == R.layout.subscription_list_item;
    }

    public interface OnPopulateSubscriptionList {
        public void listPopulated(Cursor cursor);
    }

    private class BitmapTarget implements Target {

        private Subscription mSubscription;
        private View mView;
        private ContentResolver mContentResolver;

        public BitmapTarget(Subscription subscription, View view, ContentResolver contentResolver) {
            mSubscription = subscription;
            mView = view;
            mContentResolver = contentResolver;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Palette palette = PaletteCache.generate(mSubscription.getImageURL(), bitmap);
            ColorExtractor colorExtractor = new ColorExtractor(palette);

            mSubscription.setPrimaryColor(colorExtractor.getPrimary());
            mSubscription.setSecondaryColor(colorExtractor.getSecondary());
            mSubscription.setPrimaryTintColor(colorExtractor.getPrimaryTint());
            mSubscription.update(mContentResolver);

            mView.setBackgroundColor(mSubscription.getPrimaryColor());
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }


}
