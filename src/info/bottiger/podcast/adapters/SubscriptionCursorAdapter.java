package info.bottiger.podcast.adapters;

import info.bottiger.podcast.PlayerActivity;
import info.bottiger.podcast.PodcastBaseFragment;
import info.bottiger.podcast.R;
import info.bottiger.podcast.listeners.DownloadProgressListener;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;
import info.bottiger.podcast.provider.Subscription;
import info.bottiger.podcast.service.DownloadStatus;
import info.bottiger.podcast.utils.ControlButtons;
import info.bottiger.podcast.utils.StrUtils;

import java.util.HashMap;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class SubscriptionCursorAdapter extends AbstractPodcastAdapter {

	public SubscriptionCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
	}
	
	public SubscriptionCursorAdapter(
			Context context,
			int listItem,
			Cursor cursor,
			String[] strings,
			int[] is,
			info.bottiger.podcast.adapters.SubscriptionCursorAdapter.FieldHandler[] fields) {
		super(context, listItem, cursor, strings, is, fields);
		
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

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

		return listViewItem;
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {

		View view = mInflater.inflate(R.layout.list_item, null);

		view.setTag(R.id.list_image, view.findViewById(R.id.list_image));
		view.setTag(R.id.title, view.findViewById(R.id.title));
		view.setTag(R.id.podcast, view.findViewById(R.id.podcast));
		return view;
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		Subscription sub = null;
		try {
			sub = Subscription.getByCursor(cursor);
		} catch (IllegalStateException e) {
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


			if (sub.imageURL != null && !sub.imageURL.equals("")) {
				ImageLoader imageLoader = getImageLoader(context);
				imageLoader.displayImage(sub.imageURL, icon);
			}

		}
	}


}
