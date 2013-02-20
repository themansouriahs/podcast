package org.bottiger.podcast.listeners;


import java.util.HashMap;

import org.bottiger.podcast.R;
import org.bottiger.podcast.utils.ThemeHelper;

import android.app.Activity;
import android.content.Context;
import android.widget.ImageView;

public class PlayerStatusListener {

	private static HashMap<ImageView, ImageViewButton> mImageViews = new HashMap<ImageView, ImageViewButton>();
	private static Activity mActivity;
	
	public enum STATUS {
		PLAYING, PAUSED, STOPPED
	}
	
	/**
	 * Set the activity to update the action bar in
	 * 
	 * @param activity
	 */
	public static void setActivity(Activity activity) {
		mActivity = activity;
	}

	/**
	 * Register a ImageView to be updated on status changes
	 * 
	 * @param ImageView
	 * @param Play button resource
	 * @param Pause button resource
	 */
	public static void registerImageView(ImageView imageView, int playResource,
			int pauseResource) {
		assert imageView != null;

		ImageViewButton imageViewButton = new ImageViewButton(imageView, playResource, pauseResource);
		mImageViews.put(imageView, imageViewButton);
	}
	
	/**
	 * Register a ImageView to be updated on status changes
	 * 
	 * @param ImageView
	 * @param Context
	 */
	public static void registerImageView(ImageView imageView, Context context) {
		assert imageView != null;

		ImageViewButton imageViewButton = new ImageViewButton(imageView, context);
		mImageViews.put(imageView, imageViewButton);
	}

	/**
	 * Unregister a ImageView from being updated on status changes
	 * 
	 * @param imageView
	 */
	public static boolean unregisterImageView(ImageView imageView) {
		assert imageView != null;

		boolean isRemoved = false;

		if (mImageViews.containsKey(imageView))
			isRemoved = mImageViews.remove(imageView) != null;

		return isRemoved;
	}

	/**
	 * Update the icons so they match the current status of the player
	 */
	public static void updateStatus(STATUS status) {
		
		// Update UI buttons
		for (ImageViewButton imageView : mImageViews.values()) {
			imageView.updateIcon(status);
		}
		
		// Update action bar
		if (mActivity != null) {
			mActivity.invalidateOptionsMenu();
		}
		
		// Update notification
	}
	
	/**
	 * Private class for holding the ImageView and the corresponding icons
	 */
	private static class ImageViewButton {
		
		private ImageView mImageView;
		private int playResource;
		private int pauseResource;
		
		public ImageViewButton(ImageView mImageView, int playResource,
				int pauseResource) {
			super();
			this.mImageView = mImageView;
			this.playResource = playResource;
			this.pauseResource = pauseResource;
		}
		
		public ImageViewButton(ImageView mImageView, Context context) {
			super();
			ThemeHelper themeHelper = new ThemeHelper(context);
			this.mImageView = mImageView;
			this.playResource = themeHelper.getAttr(R.attr.play_icon);
			this.pauseResource = themeHelper.getAttr(R.attr.pause_icon);
		}
		
		public void updateIcon(STATUS status) {
			switch (status) {
			case PLAYING:
				mImageView.setImageResource(playResource);
				break;
			case PAUSED:;
			case STOPPED:
				mImageView.setImageResource(pauseResource);
				break;
			}
		}
	}

}
