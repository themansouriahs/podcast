package info.bottiger.podcast.adapters;

import android.content.Context;
import android.database.Cursor;

public class SubscriptionCursorAdapter extends AbstractPodcastAdapter {

	public SubscriptionCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
	}

}
