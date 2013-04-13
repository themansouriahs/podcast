package org.bottiger.podcast.adapters;


import java.io.File;
import java.util.HashMap;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.SDCardManager;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;

public abstract class AbstractPodcastAdapter extends SimpleDragSortCursorAdapter implements PodcastAdapterInterface {

	public AbstractPodcastAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
	}

	public AbstractPodcastAdapter(Context context, int listItem, Cursor cursor,
			String[] strings, int[] is, FieldHandler[] fields) {
		super(context, listItem, cursor, strings, is);
	}	
	
	protected LayoutInflater mInflater;
	protected Context mContext;
	
	
	public final static FieldHandler defaultTextFieldHandler = new FieldHandler.TextFieldHandler();
	
	public static class TextFieldHandler implements FieldHandler {
		@Override
		public void setViewValue(PodcastAdapterInterface adapter, Cursor cursor,
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

		@Override
		public void setViewValue(PodcastAdapterInterface adapter, Cursor cursor,
				View v, int fromColumnId) {
			((AbstractPodcastAdapter) adapter).setViewImageAsync((ImageView) v,
					cursor.getString(fromColumnId));
		}
	}

	/**
	 * Sets the listItems icon Async using the UrlImageViewHelper from
	 * https://github.com/koush/UrlImageViewHelper#readme
	 * 
	 * @param imageView
	 * @param imageURL
	 */
	void setViewImageAsync(ImageView imageView, String imageURL) {
		int cacheTime = 60000 * 60 * 24 * 31; // in ms
		UrlImageViewHelper.loadUrlDrawable(imageView.getContext(), imageURL);
		UrlImageViewHelper.setUrlDrawable(imageView, imageURL,
				R.drawable.generic_podcast, cacheTime);
	}
	
	@Override
	public ImageLoader getImageLoader(Context context) {
		File cacheDir = SDCardManager.getCacheDir();
		DisplayImageOptions options = new DisplayImageOptions.Builder()
				.showStubImage(R.drawable.generic_podcast).cacheInMemory()
				.cacheOnDisc().build();
		ImageLoader imageLoader = ImageLoader.getInstance();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				context)
				.memoryCacheExtraOptions(480, 800)
				// max width, max height
				.threadPoolSize(5)
				.offOutOfMemoryHandling()
				.memoryCache(new UsingFreqLimitedMemoryCache(10 * 1024 * 1024))
				// You can pass your own memory cache implementation
				.discCache(new UnlimitedDiscCache(cacheDir))
				// You can pass your own disc cache implementation
				.discCacheFileNameGenerator(new HashCodeFileNameGenerator())
				.imageDownloader(
						new URLConnectionImageDownloader(5 * 1000, 20 * 1000))
				// connectTimeout (5 s), readTimeout (20 s)
				.tasksProcessingOrder(QueueProcessingType.FIFO)
				.defaultDisplayImageOptions(options).build();
		// Initialize ImageLoader with configuration. Do it once.
		imageLoader.init(config);

		return imageLoader;
	}

}
