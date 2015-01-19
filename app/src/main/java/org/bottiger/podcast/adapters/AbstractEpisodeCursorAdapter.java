package org.bottiger.podcast.adapters;

import android.content.Context;
import android.database.Cursor;

public abstract class AbstractEpisodeCursorAdapter<T> extends AbstractPodcastAdapter {


    public AbstractEpisodeCursorAdapter(Cursor c) {
        super(c);
    }
}
