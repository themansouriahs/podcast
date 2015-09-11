package org.bottiger.podcast.utils;


import java.io.File;
import java.io.IOException;

import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.ContextCompat;

public class SDCardManager {

    public static final String APP_DIR = ""; // "/SoundWaves"
    public static final String DOWNLOAD_DIR = "/Podcasts";
    public static final String TMP_DIR = "/tmp";
    public static final String EXPORT_DIR = "/export";
    public static final String CACHE_DIR = "/cache";
    public static final String THUMBNAIL_CACHE = "/thumbnails";

    private static String sSDCardDirCache = null;

    public static boolean getSDCardStatus() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static boolean getSDCardStatusAndCreate() {
        boolean b = getSDCardStatus();
        if (b)
            createDir();
        return b;
    }

    public static String getExportDir() throws IOException {
        return getSDCardDir() + APP_DIR + EXPORT_DIR;
    }

	public static String getDownloadDir() throws IOException {
		return getSDCardDir() + DOWNLOAD_DIR;
	}
	
	public static String getTmpDir() throws IOException {
		return getSDCardDir() + APP_DIR + TMP_DIR;
	}
	
	public static File getCacheDir() throws IOException {
		return  returnDir(getSDCardDir() + APP_DIR + CACHE_DIR);
	}
	
	public static File getThumbnailCacheDir() throws IOException {
		return  returnDir(getSDCardDir() + APP_DIR + CACHE_DIR + THUMBNAIL_CACHE);
	}
	
	private static File returnDir(String path) {
		File dir = new File(path);
		if (!dir.exists()) dir.mkdir();
		return dir;		
	}

	@RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static String getSDCardDir() throws IOException {

        if (sSDCardDirCache != null) {
            return sSDCardDirCache;
        }

        File[] dirs = ContextCompat.getExternalFilesDirs(SoundWaves.getAppContext(), null);

        File externalDir = null;

        File tmpDir;
        long largestFreeBytes = 0;

        // Find the external dir with the most free space.
        for (int i = 0; i < dirs.length; i++) {
            tmpDir = dirs[i];
            StatFs statFs = new StatFs(tmpDir.getAbsolutePath());
            long freeBytes = 0;

            if (Build.VERSION.SDK_INT < 18) {
                freeBytes = statFs.getFreeBlocks() * statFs.getBlockSize();
            } else {
                freeBytes = statFs.getFreeBytes();
            }

            if (freeBytes > largestFreeBytes) {
                largestFreeBytes = freeBytes;
                externalDir = tmpDir;
            }
        }

        if (externalDir == null) {
            throw new IOException("Cannot find external dir"); // NoI18N
        }

        sSDCardDirCache = externalDir.getAbsolutePath();
        return sSDCardDirCache;
		//File sdDir = new File(Environment.getExternalStorageDirectory().getPath());
		//return sdDir.getAbsolutePath();
	}


	private static boolean createDir()
	{
        File file = null;
        try {
            file = new File(getDownloadDir());
        } catch (IOException e) {
            return false;
        }
        boolean exists = (file.exists());
		if (!exists) {
			return file.mkdirs();
		}

        try {
            file = new File(getExportDir());
        } catch (IOException e) {
            return false;
        }
        exists = (file.exists());
		if (!exists) {
			return file.mkdirs();
		}		
		return true;
	}	
	
	public static String pathFromFilename(FeedItem item) throws IOException {
		if (item.getFilename() == null || item.getFilename().equals("")) {
			return "";
		} else {
			return pathFromFilename(item.getFilename());
		}
	}
	
	public static String pathTmpFromFilename(FeedItem item) throws IOException {
		if (item.getFilename() == null || item.getFilename().equals("")) {
			return "";
		} else {
			return pathTmpFromFilename(item.getFilename());
		}
	}
	
	public static String pathTmpFromFilename(String item) throws IOException {
		String folder = SDCardManager.getTmpDir();
		returnDir(folder);
		return folder + "/" + item;
	}
	
	public static String pathFromFilename(String item) throws IOException {
		String folder = SDCardManager.getDownloadDir();
		returnDir(folder);
		return folder + "/" + item;
	}

}
