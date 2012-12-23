package info.bottiger.podcast.utils;

import java.io.File;

import android.content.Context;
import android.os.Environment;

public class SDCardManager {

	public static String SDCARD_DIR = "/sdcard"; 
	public static final String APP_DIR = "/bottiger.podcast";
	public static final String DOWNLOAD_DIR = "/download";
	public static final String EXPORT_DIR = "/export";
	public static final String CACHE_DIR = "/cache";
	
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
	
	public static String getCacheDir()
	{
		return getSDCardDir().toString();
	}
	
	public static File getCaceDir() {
		File cacheDir = new File(getSDCardDir() + APP_DIR + CACHE_DIR);
		if (!cacheDir.exists()) cacheDir.mkdir();
		return cacheDir;
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
		if (filename.equals(""))
		return null;
		else
			return SDCardManager.getDownloadDir() + "/" + filename;
	}

}
