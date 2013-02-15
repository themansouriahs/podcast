package info.bottiger.podcast.listeners;

import info.bottiger.podcast.R;
import info.bottiger.podcast.utils.ThemeHelper;

import java.util.LinkedList;

import android.content.Context;
import android.widget.ImageView;

public class PlayerStatusListener {

	private static LinkedList<ImageView> mImageViews = new LinkedList<ImageView>();

	public enum STATUS {
		PLAYING, PAUSED, STOPPED
	}

	/**
	 * Register a ImageView to be updated on status changes
	 * 
	 * @param ImageView
	 */
	public static void registerImageView(Context context, ImageView imageView) {
		assert imageView != null;

		mImageViews.add(imageView);
	}

	/**
	 * Unregister a ImageView from being updated on status changes
	 * 
	 * @param imageView
	 */
	public static boolean unregisterImageView(ImageView imageView) {
		assert imageView != null;

		boolean isRemoved = false;

		if (mImageViews.contains(imageView))
			isRemoved = mImageViews.remove(imageView);

		return isRemoved;
	}

	/**
	 * Update the icons so they match the current status of the player
	 */
	public static void updateImages(Context context, STATUS status) {
		ThemeHelper themeHelper = new ThemeHelper(context);
		
		int iconResource = 0;
		switch (status) {
		case PLAYING:
			iconResource = themeHelper.getAttr(R.attr.pause_icon);
			break;
		case PAUSED:;
		case STOPPED:
			iconResource = themeHelper.getAttr(R.attr.play_icon);
			break;
		}
		
		for (ImageView imageView : mImageViews) {
			imageView.setImageResource(iconResource);
		}
	}

}
