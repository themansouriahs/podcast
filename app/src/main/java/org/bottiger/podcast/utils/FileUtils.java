package org.bottiger.podcast.utils;

import android.Manifest;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileUtils {

	public static boolean copy_file(String src, String dst)
	{
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        boolean b=true;
        try {
            File readFile = new File(src);

            File writeFile = new File(dst);

            fileInputStream = new FileInputStream(readFile);

            fileOutputStream = new FileOutputStream(writeFile);

            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = fileInputStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            b = false;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception ex) {}
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Exception ex) {}
        }	
        
        return b;
	}
	
	public static String get_export_file_name(String title, long id)
	{
		title = title.replaceAll("[\\s\\\\:\\<\\>\\[\\]\\*\\|\\/\\?\\{\\}]+", "_");		

		return title+"_"+id+".mp3";
	}

    /**
     * Delete the content of a directory.
     *
     * @param argDir The directory to be cleaned
     * @param argCleanRecursive If subdirectories should be clean out as well.
     * @return If the operation was succesfull
     */
    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static boolean cleanDirectory(@NonNull File argDir, boolean argCleanRecursive) throws SecurityException {
        if (!argDir.isDirectory()) {
            return false;
        }

        File[] flist = argDir.listFiles();

        if (flist == null)
            return true;

        File file;
        for (int i = 0; i < flist.length; i++) {
            file = flist[i];

            if (argCleanRecursive && file.isDirectory()) {
                cleanDirectory(file, argCleanRecursive);
            }

            file.delete();
        }

        return true;
    }

}
