package info.bottiger.podcast.utils;

import java.io.File;

import android.os.Environment;

public class SDCardManager {

	public static String SDCARD_DIR = "/sdcard"; 
	public static final String APP_DIR = "/bottiger.podcast";
	public static final String DOWNLOAD_DIR = "/download";
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
	
	private static String getSDCardDir() {
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
	
	public static String pathFromFilename(String filename) {
		if (filename == null || filename.equals(""))
			return "";
		else
			return SDCardManager.getDownloadDir() + "/" + filename;
	}

}
