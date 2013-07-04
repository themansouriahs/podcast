package org.bottiger.podcast.images;

import java.io.File;

import org.bottiger.podcast.utils.SDCardManager;

import uk.co.senab.bitmapcache.BitmapLruCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class HybridBitmapLruCache implements ImageCache {

	private BitmapLruImageCache mMemoryCache;
	private DiskLruImageCache mDiskCache;
	
	public HybridBitmapLruCache(Context context, String uniqueName, int memoryCacheSzie, int distCacheSize, CompressFormat compressFormat, int quality) {
		mMemoryCache = new BitmapLruImageCache(memoryCacheSzie);
		mDiskCache = new DiskLruImageCache(context, uniqueName, distCacheSize, compressFormat, quality);
	}

	@Override
	public Bitmap getBitmap(String url) {
		Bitmap cachedBitmap = mMemoryCache.get(url);
		if (cachedBitmap == null) {
			cachedBitmap = mDiskCache.getBitmap(url);
			if (cachedBitmap != null) {
				mMemoryCache.put(url, cachedBitmap);
			}
		}
		return cachedBitmap;
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		mMemoryCache.put(url, bitmap);
		mDiskCache.putBitmap(url, bitmap);
	}

}
