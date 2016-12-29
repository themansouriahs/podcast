package org.bottiger.podcast.utils;


import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.ContextCompat;

import org.bottiger.podcast.R;
import org.bottiger.podcast.SoundWaves;
import org.bottiger.podcast.provider.FeedItem;

import java.io.File;
import java.io.IOException;

public class SDCardManager {

    public static final String APP_DIR = ""; // "/SoundWaves"
    public static final String DOWNLOAD_DIR = "/Podcasts";
    public static final String TMP_DIR = "/tmp";
    public static final String EXPORT_DIR = "/export";
    public static final String CACHE_DIR = "/cache";
    public static final String THUMBNAIL_CACHE = "/thumbnails";

    @Nullable
    private static File sSdCardDir = null;
    private static String sSDCardDirCache = null;

    public static boolean getSDCardStatus() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static boolean getSDCardStatusAndCreate(@NonNull Context argContext) throws SecurityException, IOException {
        boolean b = getSDCardStatus();
        if (b)
            createDir(argContext);
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
    @NonNull
	public static File getDownloadDir(@NonNull Context argContext) throws IOException, SecurityException {
        boolean useSDCard = PreferenceHelper.getBooleanPreferenceValue(argContext, R.string.pref_delete_when_finished_key, R.bool.pref_store_on_sdcard_default);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return !useSDCard ? getLocalDownloadDir() : getRemovableDownloadDir(argContext);
        }

        return getLocalDownloadDir();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public static File getRemovableDownloadDir(@NonNull Context argContext) throws SecurityException {

        if (sSdCardDir != null) {
            return sSdCardDir;
        }

        File[] dirs = argContext.getExternalFilesDirs(Environment.DIRECTORY_PODCASTS);
        File localDir = getLocalDownloadDir();
        sSdCardDir = localDir;

        int shortestPrefix = Integer.MAX_VALUE;

        // The SD card is probably the second in the array.
        // But to be sure we find the path with the shortest common prefix with the local dir
        if (dirs.length > 1) {
            for (int i = 0; i < dirs.length; i++) {
                File sdcardDirTest = dirs[i];

                String prefix = StrUtils.greatestCommonPrefix(localDir.toString(), sdcardDirTest.toString());
                int prefixLenght = prefix.length();

                if (prefixLenght < shortestPrefix) {
                    shortestPrefix = prefixLenght;
                    sSdCardDir = sdcardDirTest;
                }
            }
        }

        return sSdCardDir;
    }

    public static boolean hasSdCard(@NonNull Context argContext) {

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            return false;
        }

        File[] dirs = argContext.getExternalFilesDirs(Environment.DIRECTORY_PODCASTS);
        return dirs != null && dirs.length > 1;
    }

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public static File getLocalDownloadDir() throws SecurityException {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
    }

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
	public static String getTmpDir(@NonNull Context argContext) throws IOException, SecurityException {
		return getSDCardDir(argContext) + APP_DIR + TMP_DIR;
	}

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
	public static File getCacheDir(@NonNull Context argContext) throws IOException, SecurityException {
		return  returnDir(getSDCardDir(argContext) + APP_DIR + CACHE_DIR);
	}

    @RequiresPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
	public static File getThumbnailCacheDir(@NonNull Context argContext) throws IOException, SecurityException {
		return  returnDir(getSDCardDir(argContext) + APP_DIR + CACHE_DIR + THUMBNAIL_CACHE);
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
    public static String getSDCardDir(@NonNull Context argContext) throws IOException {

        if (sSDCardDirCache != null) {
            return sSDCardDirCache;
        }

        File[] dirs = ContextCompat.getExternalFilesDirs(argContext, null);

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
            externalDir = SoundWaves.getAppContext(argContext).getFilesDir();
        }


        if (externalDir == null) {
            throw new IOException("Cannot find external dir"); // NoI18N
        }

        sSDCardDirCache = externalDir.getAbsolutePath();
        return sSDCardDirCache;
	}


    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	private static boolean createDir(@NonNull Context argContext) throws SecurityException, IOException {
        File file = getDownloadDir(argContext);
        boolean exists = file.exists();
        if (!exists) {
            return file.mkdirs();
        }

        return exists || file.mkdirs();
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	public static String pathFromFilename(FeedItem item, @NonNull Context argContext) throws IOException, SecurityException {
		if (item.getFilename() == null || item.getFilename().equals("")) {
			return "";
		} else {
			return pathFromFilename(item.getFilename(), argContext);
		}
	}
	
	public static String pathTmpFromFilename(@NonNull Context argContext, FeedItem item) throws IOException, SecurityException {
		if (item.getFilename() == null || item.getFilename().equals("")) {
			return "";
		} else {
			return pathTmpFromFilename(argContext, item.getFilename());
		}
	}

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	public static String pathTmpFromFilename(@NonNull Context argContext, String item) throws IOException, SecurityException {
		String folder = SDCardManager.getTmpDir(argContext);
		returnDir(folder);
		return folder + "/" + item;
	}

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	public static String pathFromFilename(String item, @NonNull Context argContext) throws IOException, SecurityException {
		String folder = SDCardManager.getDownloadDir(argContext).toString();
		returnDir(folder);
		return folder + "/" + item;
	}

}
