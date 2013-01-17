package info.bottiger.podcast.adapters;

import android.content.Context;
import android.database.Cursor;
import android.widget.SimpleCursorAdapter;

public abstract class AbstractPodcastAdapter extends SimpleCursorAdapter {

	public AbstractPodcastAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
	}	

}
