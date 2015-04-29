package org.bottiger.podcast.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.ArrayAdapter;

import java.util.List;

/**
 * Created by apl on 19-02-2015.
 */
public class PlaylistContentSpinnerAdapter extends ArrayAdapter {
    public PlaylistContentSpinnerAdapter(Context context, int resource) {
        super(context, resource);
    }

    public PlaylistContentSpinnerAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public PlaylistContentSpinnerAdapter(Context context, int resource, Object[] objects) {
        super(context, resource, objects);
    }

    public PlaylistContentSpinnerAdapter(Context context, int resource, int textViewResourceId, Object[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public PlaylistContentSpinnerAdapter(Context context, int resource, List objects) {
        super(context, resource, objects);
    }

    public PlaylistContentSpinnerAdapter(Context context, int resource, int textViewResourceId, List objects) {
        super(context, resource, textViewResourceId, objects);
    }
}
