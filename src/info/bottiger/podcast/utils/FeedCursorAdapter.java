package info.bottiger.podcast.utils;

import android.widget.SimpleCursorAdapter;
import android.database.Cursor;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import info.bottiger.podcast.R;
import info.bottiger.podcast.provider.FeedItem;
import info.bottiger.podcast.provider.ItemColumns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class FeedCursorAdapter extends SimpleCursorAdapter {

	public static final int ICON_DEFAULT_ID = -1;

	public interface FieldHandler {
		public void setViewValue(FeedCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId);
	}

	public static class TextFieldHandler implements FieldHandler {
		public void setViewValue(FeedCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId) {
			// Normal text column, just display what's in the database
			String text = cursor.getString(fromColumnId);

			if (text == null) {
				text = "";
			}

			if (v instanceof TextView) {
				adapter.setViewText((TextView) v, text);
			} else if (v instanceof ImageView) {
				adapter.setViewImage((ImageView) v, text);
			}
		}
	}

	public static class IconFieldHandler implements FieldHandler {
		public IconFieldHandler(HashMap<Integer, Integer> iconMap) {
		}

		public IconFieldHandler() {
		}

		public void setViewValue(FeedCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId) {
			adapter.setViewImage3((ImageView) v, cursor.getString(fromColumnId));
		}
	}

	/*
	 * public static class IconFieldHandler implements FieldHandler {
	 * HashMap<Integer, Integer> mIconMap; public
	 * IconFieldHandler(HashMap<Integer,Integer> iconMap) { mIconMap = iconMap;
	 * } public void setViewValue(IconCursorAdapter adapter, Cursor cursor, View
	 * v, int fromColumnId) { //The status column gets displayed as our icon int
	 * status = cursor.getInt(fromColumnId);
	 * 
	 * Integer iconI = mIconMap.get(status); if (iconI==null) iconI =
	 * mIconMap.get(ICON_DEFAULT_ID); //look for default value in map int icon =
	 * (iconI!=null)? iconI.intValue(): R.drawable.status_unknown; //Use this
	 * icon when not in map and no map default. //This allows going back to a
	 * previous version after data has been //added in a new version with
	 * additional status codes. adapter.setViewImage2((ImageView) v, icon); } }
	 */
	public final static FieldHandler defaultTextFieldHandler = new TextFieldHandler();

	protected int[] mFrom2;
	protected int[] mTo2;
	protected FieldHandler[] mFieldHandlers;

	private static final int TYPE_COLLAPS = 0;
	private static final int TYPE_EXPAND = 1;
	private static final int TYPE_MAX_COUNT = 2;

	private ArrayList<FeedItem> mData = new ArrayList<FeedItem>();
	private LayoutInflater mInflater;
	private Context mContext;

	private TreeSet<Number> mExpandedItemID = new TreeSet<Number>();

	// Create a set of FieldHandlers for methods calling using the legacy
	// constructor
	private static FieldHandler[] defaultFieldHandlers(String[] fromColumns,
			HashMap<Integer, Integer> iconMap) {
		FieldHandler[] handlers = new FieldHandler[fromColumns.length];
		for (int i = 0; i < handlers.length - 1; i++) {
			handlers[i] = defaultTextFieldHandler;
		}
		handlers[fromColumns.length - 1] = new IconFieldHandler(iconMap);
		return handlers;
	}

	// Legacy constructor
	public FeedCursorAdapter(Context context, int layout, Cursor cursor,
			String[] fromColumns, int[] to, HashMap<Integer, Integer> iconMap) {
		this(context, layout, cursor, fromColumns, to, defaultFieldHandlers(
				fromColumns, iconMap));
	}

	// Newer constructor allows custom FieldHandlers.
	// Would be better to bundle fromColumn/to/fieldHandler for each field and
	// pass a single array
	// of those objects, but the overhead of defining that value class in Java
	// is not worth it.
	// If this were a Scala program, that would be a one-line case class.
	public FeedCursorAdapter(Context context, int layout, Cursor cursor,
			String[] fromColumns, int[] to, FieldHandler[] fieldHandlers) {
		super(context, layout, cursor, fromColumns, to);

		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if (to.length < fromColumns.length) {
			mTo2 = new int[fromColumns.length];
			for (int i = 0; i < to.length; i++)
				mTo2[i] = to[i];
			mTo2[fromColumns.length - 1] = R.id.icon;
		} else
			mTo2 = to;
		mFieldHandlers = fieldHandlers;
		if (cursor != null) {
			int i;
			int count = fromColumns.length;
			if (mFrom2 == null || mFrom2.length != count) {
				mFrom2 = new int[count];
			}
			for (i = 0; i < count; i++) {
				mFrom2[i] = cursor.getColumnIndexOrThrow(fromColumns[i]);
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Cursor item = (Cursor) getItem(position);

		if (!item.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}
		View v;
		if (convertView == null) {
			v = newView(mContext, item, parent);
		} else {
			v = convertView;
		}
		bindView(v, mContext, item);

		Long itemID = item.getLong(item.getColumnIndex(ItemColumns._ID));
		if (mExpandedItemID.contains(itemID)) {
			ViewStub stub = (ViewStub) v.findViewById(R.id.stub);
			if (stub != null)
				stub.inflate();
			else {
				View playerView = v.findViewById(R.id.stub_player);
				playerView.setVisibility(View.VISIBLE);
			}
		} else {
			View playerView = v.findViewById(R.id.stub_player);
			if (playerView != null) {
				playerView.setVisibility(View.GONE);
			}
		}

		return v;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewHolder viewHolder = new ViewHolder();
		View v = null;

		Long cursorID = cursor.getLong(cursor.getColumnIndex("_id"));
		boolean doExpand = mExpandedItemID.contains(cursorID);
		// if (doExpand) {
		// v = mInflater.inflate(R.layout.list_item_expanded, null);
		// } else {
		v = mInflater.inflate(R.layout.list_item, null);
		// }

		viewHolder.textViewTitle = (TextView) v.findViewById(R.id.title);
		viewHolder.textViewSubTitle = (TextView) v.findViewById(R.id.podcast);
		viewHolder.textViewDuration = (TextView) v.findViewById(R.id.duration);
		viewHolder.textViewFileSize = (TextView) v.findViewById(R.id.filesize);
		viewHolder.imageView = (ImageView) v.findViewById(R.id.list_image);
		viewHolder.textViewCurrentTime = (TextView) v.findViewById(R.id.current_position);

		v.setTag(viewHolder);
		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();


		int titleIndex = cursor.getColumnIndex(ItemColumns.TITLE);
		String title = cursor.getString(titleIndex);
		
		int subtitleIndex = cursor.getColumnIndex(ItemColumns.SUB_TITLE);
		int imageIndex = cursor.getColumnIndex(ItemColumns.IMAGE_URL);
		
		int currentTimeIndex = cursor.getColumnIndex(ItemColumns.DURATION);
		
		try {
			int filesizeIndex = cursor.getColumnIndexOrThrow(ItemColumns.STATUS);
			int filesize = cursor.getInt(filesizeIndex);
			holder.textViewFileSize.setText("" + filesize);
		} catch (IllegalArgumentException e) {
			
		}
		
		holder.textViewTitle.setText(title);

		if (subtitleIndex > 0)
			holder.textViewSubTitle.setText(cursor.getString(subtitleIndex));

		this.setViewImage3(holder.imageView, cursor.getString(imageIndex));

	}

	public void toggleItem(Cursor item) {
		Long itemID = item.getLong(item.getColumnIndex(ItemColumns._ID));
		if (mExpandedItemID.contains(itemID)) // ItemColumns._ID
			mExpandedItemID.remove(itemID);
		else {
			if (!mExpandedItemID.isEmpty())
				mExpandedItemID.remove(mExpandedItemID.first()); // HACK: only show one expanded at the time
			mExpandedItemID.add(itemID);
		}
	}

	@Override
	public int getViewTypeCount() {
		return TYPE_MAX_COUNT;
	}

	@Override
	public int getItemViewType(int position) {
		return mExpandedItemID.contains(itemID(position)) ? TYPE_EXPAND
				: TYPE_COLLAPS;
	}

	public static class ViewHolder {
		public ImageView imageView;
		public TextView textViewTitle;
		public TextView textViewSubTitle;
		public TextView textViewDuration;
		public TextView textViewFileSize;
		public TextView textViewCurrentTime;
	}

	private void setViewImage3(ImageView v, String imageURL) {
		// https://github.com/koush/UrlImageViewHelper#readme
		int cacheTime = 60000 * 60 * 24 * 31; // in ms
		UrlImageViewHelper.loadUrlDrawable(v.getContext(), imageURL);
		UrlImageViewHelper.setUrlDrawable(v, imageURL, R.drawable.generic_podcast, cacheTime);
	}

	private Long itemID(int position) {
		Object item = getItem(position);
		if (item instanceof FeedItem) {
			FeedItem feedItem = (FeedItem) item;
			return Long.valueOf(feedItem.id);
		} else
			return new Long(1); // FIXME
	}

}
