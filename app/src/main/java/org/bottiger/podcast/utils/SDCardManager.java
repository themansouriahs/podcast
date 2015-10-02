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

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static boolean getSDCardStatusAndCreate() throws SecurityException {
        boolean b = getSDCardStatus();
        if (b)
            createDir();
        return b;
    }

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public static String getSDCardRootDir() throws IOException, SecurityException {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public static String getExportDir() throws IOException, SecurityException {
        return getSDCardRootDir();
    }

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
	public static String getDownloadDir() throws IOException, SecurityException {
		//return getSDCardDir() + DOWNLOAD_DIR;
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PODCASTS).toString();
	}

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
	public static String getTmpDir() throws IOException, SecurityException {
		return getSDCardDir() + APP_DIR + TMP_DIR;
	}

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
	public static File getCacheDir() throws IOException, SecurityException {
		return  returnDir(getSDCardDir() + APP_DIR + CACHE_DIR);
	}

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
	public static File getThumbnailCacheDir() throws IOException, SecurityException {
		return  returnDir(getSDCardDir() + APP_DIR + CACHE_DIR + THUMBNAIL_CACHE);
	}

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
	private static File returnDir(String path) throws IOException {
		File dir = new File(path);
		if (!dir.exists() && !dir.mkdir()) {
            throw new IOException("Could not create folder: " + dir.toString()); // NoI18N
        }
		return dir;		
	}

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
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

            if (tmpDir == null)
                continue;

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

        /**
         * FIXME
         * We could not find an external location, find another one!
         * This seems to be an issue with some samsung devices,
         * I should probably investigate it at some point
         */
        if (externalDir == null) {
            externalDir = SoundWaves.getAppContext().getFilesDir();
        }


        if (externalDir == null) {
            throw new IOException("Cannot find external dir"); // NoI18N
        }

        sSDCardDirCache = externalDir.getAbsolutePath();
        return sSDCardDirCache;
	}


    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	private static boolean createDir() throws SecurityException {
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

        return exists || file.mkdirs();
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	public static String pathFromFilename(FeedItem item) throws IOException, SecurityException {
		if (item.getFilename() == null || item.getFilename().equals("")) {
			return "";
		} else {
			return pathFromFilename(item.getFilename());
		}
	}
	
	public static String pathTmpFromFilename(FeedItem item) throws IOException, SecurityException {
		if (item.getFilename() == null || item.getFilename().equals("")) {
			return "";
		} else {
			return pathTmpFromFilename(item.getFilename());
		}
	}

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	public static String pathTmpFromFilename(String item) throws IOException, SecurityException {
		String folder = SDCardManager.getTmpDir();
		returnDir(folder);
		return folder + "/" + item;
	}

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	public static String pathFromFilename(String item) throws IOException, SecurityException {
		String folder = SDCardManager.getDownloadDir();
		returnDir(folder);
		return folder + "/" + item;
	}

}
