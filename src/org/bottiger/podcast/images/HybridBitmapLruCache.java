package org.bottiger.podcast.images;

import java.io.File;

import org.bottiger.podcast.utils.SDCardManager;

import uk.co.senab.bitmapcache.BitmapLruCache;

import android.content.Context;
import android.graphics.Bitmap;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class HybridBitmapLruCache implements ImageCache {

	private BitmapLruCache mCache;
	
	public HybridBitmapLruCache(Context context) {
        File cacheLocation = SDCardManager.getThumbnailCacheDir();

        BitmapLruCache.Builder builder = new BitmapLruCache.Builder(context);
        builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
        builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheLocation);

        mCache = builder.build();
	}

	@Override
	public Bitmap getBitmap(String url) {
		return mCache.get(url).getBitmap();
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		mCache.put(url, bitmap);
	}

}
