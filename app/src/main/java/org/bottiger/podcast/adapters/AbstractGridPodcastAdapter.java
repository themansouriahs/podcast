package org.bottiger.podcast.adapters;


import java.io.File;
import java.util.HashMap;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.SDCardManager;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;
public abstract class AbstractGridPodcastAdapter extends CursorAdapter {

	protected LayoutInflater mInflater;
	protected Context mContext;
	
    @Deprecated
    public AbstractGridPodcastAdapter(Context context, int layout, Cursor c) {
        super(context, c);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

}
