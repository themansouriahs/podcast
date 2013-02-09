package info.bottiger.podcast.adapters;

import java.util.HashMap;

import info.bottiger.podcast.PodcastBaseFragment;
import android.content.Context;
import android.database.Cursor;

public abstract class AbstractEpisodeCursorAdapter extends AbstractPodcastAdapter {
	
	public AbstractEpisodeCursorAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);
		// TODO Auto-generated constructor stub
	}

}
