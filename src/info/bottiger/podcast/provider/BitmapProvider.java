package info.bottiger.podcast.provider;

import info.bottiger.podcast.R;
import info.bottiger.podcast.service.PodcastDownloadManager;
import info.bottiger.podcast.utils.SDCardManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaMetadataRetriever;

public class BitmapProvider {

	private Context mContext;
	private WithIcon mItem;

	private DiskLruImageCache mDiskLruImageCache; // From
													// https://github.com/JakeWharton/DiskLruCache
	private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
	private final CompressFormat mCompressFormat = CompressFormat.PNG;
	private final int mCompressQuality = 70;

	private static final String DISK_CACHE_SUBDIR = "thumbnails";

	/*
	 * Class for extracting, downloading, caching and resizing bitmaps.
	 */
	public BitmapProvider(Context context, WithIcon item) {
		super();
		this.mContext = context;
		this.mItem = item;

		mDiskLruImageCache = new DiskLruImageCache(context, DISK_CACHE_SUBDIR,
				DISK_CACHE_SIZE, mCompressFormat, mCompressQuality);
	}


	/**
	 * 
	 * @return The path to the Items icons
	 */
	public String getThumbnailPath() {
		return getThumbnailPath(0, 0);
	}
	
	/**
	 * 
	 * @param thumbnail width
	 * @param thumbnail height
	 * @return thumbnail of the width and height given as input
	 */
	public String getThumbnailPath(int width, int height) {

		/* Calculate the imagePath */
		String imageURL = null;
		Bitmap generatedFile = null;
		File thumbnail = getThumbnailFile();
		
		// make sure mItem is defined
		if (mItem == null) return defaultIcon().getAbsolutePath();

		// 1. Attempt: Extract thumbnail from local file.
		//
		// Test if the file is downloaded and everything is okay
		// Less confusing test would be appreciated
		if (mItem instanceof FeedItem) {
			FeedItem feedItem = (FeedItem) mItem;
			if (mediaFileExist(feedItem)) {

				// create the thumbnail if it doesn't exist
				if (!thumbnail.exists()) {
					generatedFile = bitmapFromFile(thumbnail, width, height);
				}

				if (thumbnail.exists() || generatedFile != null) {
					String urlPrefix = "file://";
					String thumbnailPath = thumbnailCacheURL(mItem, width, height);
					return imageURL = urlPrefix + thumbnailPath;
				}
			}
		}

		// 2. Attempt: Extract thumbnail from remote file.
		/*
		 * generatedFile = bitmapFromFile(new File(mItem.image)); if
		 * (generatedFile != null) { return thumbnail.getAbsolutePath(); }
		 */

		// 3. Attempt: Extract thumbnail from podcast feed.
		imageURL = mItem.getImageURL(mContext);
		if (imageURL != null)
			return imageURL;
		
		// 4. Attempt: If everything fails return a dummy image.
		return defaultIcon().getAbsolutePath();
		
	}

	public Bitmap createBitmapFromMediaFile(int width, int height) {
		if (mItem instanceof FeedItem) {
			FeedItem feedItem = (FeedItem)mItem;
			
			return bitmapFromFile(new File(feedItem.getAbsolutePath()), width, height);
		}
		return null;
	}

	/**
	 * Extracts the Bitmap from the MP3/media file
	 * 
	 * @param fd
	 * @return Bitmap from the file or feed
	 */
	public Bitmap createBitmapFromMediaFile(FileDescriptor fd, int height, int width) {

		Bitmap cover = null;
		String cacheKey = thumbnailCacheName(mItem, height, width);

		// Return if we have the bitmap cached
		if (mDiskLruImageCache.containsKey(cacheKey))
			return mDiskLruImageCache.getBitmap(cacheKey);

		// if (mediaFileExist()) {
		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
		mmr.setDataSource(fd);
		byte[] embeddedPicture = mmr.getEmbeddedPicture();

		// If the image exists, cache and return
		if (embeddedPicture != null) {
			cover = BitmapFactory.decodeByteArray(embeddedPicture, 0,
					embeddedPicture.length);
			mDiskLruImageCache.put(cacheKey, cover);
			
			// Resize the bitmap
			cover = Bitmap.createScaledBitmap(cover, width, height, false);

			try {
				saveBitmap(cover, getThumbnailFile(width, height));
				return cover;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			}
			
		}
		// }

		// If we could not extract the bitmap from the media file - return a
		// dummy image
		// Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(),
		// R.drawable.soundwaves);
		return cover;
	}

	/*
	 * Saves the bitmap to a given file.
	 */
	public void saveBitmap(Bitmap bitmap, File file)
			throws FileNotFoundException {
		FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
		bitmap.compress(mCompressFormat, mCompressQuality, out);
	}

	/**
	 * 
	 * @return The default size thumbnail
	 */
	private File getThumbnailFile() {
		return getThumbnailFile(0, 0);
	}
	private File getThumbnailFile(int width, int height) {
		String thumbnailPath = thumbnailCacheURL(mItem, width, height);
		return new File(thumbnailPath);
	}

	private String thumbnailCacheURL(WithIcon item, int width, int height) {
		String thumbURL = SDCardManager.getThumbnailCacheDir() + "/"
				+ thumbnailCacheName(item, width, height) + ".png";
		return thumbURL;
	}

	@SuppressLint("UseValueOf")
	private String thumbnailCacheName(WithIcon item, int height, int width) {
		String type = null;

		if (item instanceof FeedItem) {
			FeedItem feedItem = (FeedItem) item;
			PodcastDownloadManager.DownloadStatus ds = PodcastDownloadManager
					.getStatus(feedItem);
			switch (ds) {
			case DONE:
				// file icon
				type = "local";
				break;
			default:
				// feed icon
				type = "remote";
				break;
			}
		} else {
			type = "subscription";
		}

		String StringID = new Long(item.getId()).toString();
		return StringID + "_x" + width + "_y" + height + "_" + type;

	}

	/**
	 * Test if the media file is on disk.
	 * 
	 * @param feedItem
	 * @return boolean
	 */
	private boolean mediaFileExist(FeedItem feedItem) {
		String fullPath = SDCardManager
				.pathFromFilename(feedItem.getAbsolutePath());
		PodcastDownloadManager.DownloadStatus ds = PodcastDownloadManager
				.getStatus(feedItem);

		return feedItem.getAbsolutePath() != null
				&& feedItem.getAbsolutePath().length() > 0
				&& new File(fullPath).exists()
				&& ds == PodcastDownloadManager.DownloadStatus.DONE;
	}

	@SuppressWarnings("resource")
	private Bitmap bitmapFromFile(File file, int width, int height) {
		FileInputStream fis;
		Bitmap generatedFile = null;

		try {
			fis = new FileInputStream(file);
			generatedFile = createBitmapFromMediaFile(fis.getFD(), width, height);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return generatedFile;
	}
	
	private File defaultIcon() {
		String path = SDCardManager.pathFromFilename("default");
		File defaultFile = new File(path);
		if (defaultFile.exists())
			return defaultFile;
		
		Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.soundwaves);
		try {
			saveBitmap(icon, defaultFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return defaultFile;
	}
}
