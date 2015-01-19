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

}
