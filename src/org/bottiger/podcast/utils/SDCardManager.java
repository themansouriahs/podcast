package org.bottiger.podcast.utils;


import java.io.File;

import org.bottiger.podcast.provider.FeedItem;

import android.os.Environment;

public class SDCardManager {

	public static final String APP_DIR = "/bottiger.podcast";
	public static final String DOWNLOAD_DIR = "/download";
	public static final String TMP_DIR = "/tmp";
	public static final String EXPORT_DIR = "/export";
	public static final String CACHE_DIR = "/cache";
	public static final String THUMBNAIL_CACHE = "/thumbnails";
	
	
	public static boolean getSDCardStatus()
	{
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}
	
	public static boolean getSDCardStatusAndCreate()
	{
		boolean b = getSDCardStatus();
		if(b)
			createDir();
		return b;
	}	

	public static String getExportDir()
	{
		return getSDCardDir() + APP_DIR + EXPORT_DIR;
	}	

	public static String getDownloadDir()
	{
		return getSDCardDir() + APP_DIR + DOWNLOAD_DIR;
	}
	
	public static String getTmpDir()
	{
		return getSDCardDir() + APP_DIR + TMP_DIR;
	}
	
	public static String getAppDir()
	{
		return getSDCardDir() + APP_DIR;
	}
	
	public static File getCacheDir() {
		return  returnDir(getSDCardDir() + APP_DIR + CACHE_DIR);
	}
	
	public static File getThumbnailCacheDir() {
		return  returnDir(getSDCardDir() + APP_DIR + CACHE_DIR + THUMBNAIL_CACHE);
	}
	
	private static File returnDir(String path) {
		File dir = new File(path);
		if (!dir.exists()) dir.mkdir();
		return dir;		
	}
	
	public static String getSDCardDir() {
		File sdDir = new File(Environment.getExternalStorageDirectory().getPath());
		return sdDir.getAbsolutePath();
	}


	private static boolean createDir()
	{
		File file = new File(getDownloadDir());
		boolean exists = (file.exists());
		if (!exists) {
			return file.mkdirs();
		}
		
		file = new File(getExportDir());
		exists = (file.exists());
		if (!exists) {
			return file.mkdirs();
		}		
		return true;
	}	
	
	public static String pathFromFilename(FeedItem item) {
		if (item.getFilename() == null || item.getFilename().equals("")) {
			return "";
		} else {
			return pathFromFilename(item.getFilename());
		}
	}
	
	public static String pathTmpFromFilename(FeedItem item) {
		if (item.getFilename() == null || item.getFilename().equals("")) {
			return "";
		} else {
			return pathTmpFromFilename(item.getFilename());
		}
	}
	
	public static String pathTmpFromFilename(String item) {
		String folder = SDCardManager.getTmpDir();
		returnDir(folder);
		return folder + "/" + item;
	}
	
	public static String pathFromFilename(String item) {
		String folder = SDCardManager.getDownloadDir();
		returnDir(folder);
		return folder + "/" + item;
	}

}
