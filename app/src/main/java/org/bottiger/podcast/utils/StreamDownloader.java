package org.bottiger.podcast.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.*;
import org.bottiger.podcast.provider.FeedItem;

public class StreamDownloader {

	private static FeedItem item;
	private static BufferedOutputStream os;

    private static String defaultFilename = "stream";
	private static File file = null;
	
	public static BufferedOutputStream getFileOutputStream(FeedItem argItem) throws FileNotFoundException {
        if (os == null) {
            os = new BufferedOutputStream(new FileOutputStream(getFile(argItem.getFilename())));
        }
        item = argItem;
		return os;
	}

    public static synchronized boolean persist() {
        if (item != null && file != null) {
            File newFile = new File(item.getAbsolutePath());
            try {
                org.apache.commons.io.FileUtils.moveFile(file, newFile);
                item.setDownloaded(true);
                reset();
                return true;
            } catch (IOException e) {
                item.setDownloaded(false);
                e.printStackTrace();
            }
        }
        return false;
    }
	
	public static File getFile(String argFilename) {
        String filename = argFilename == null ? defaultFilename : argFilename;
        file = new File(SDCardManager.getTmpDir() + "/" + filename);
		return file;
	}

    private static void reset() {
        item = null;
        file = null;
    }

}
