package info.bottiger.podcast.adapters;

import java.util.HashMap;

import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public interface PodcastAdapterInterface {

	void setViewText(TextView v, String text);

	void setViewImage(ImageView v, String text);
	
	ImageLoader getImageLoader(Context context);
	
	interface FieldHandler
	{
		public void setViewValue(PodcastAdapterInterface adapter, Cursor cursor,
				View v, int fromColumnId);
		
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
	}
}
